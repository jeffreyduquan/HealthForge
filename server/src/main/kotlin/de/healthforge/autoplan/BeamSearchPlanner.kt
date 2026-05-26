package de.healthforge.autoplan

import de.healthforge.recipe.RecipeEntity
import de.healthforge.recipe.SlotTag
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.random.Random

/**
 * Plan-Slot identifier: (dayIndex 0..days-1, slot enum).
 */
data class PlanSlotKey(val dayIndex: Int, val slot: SlotTag)

/**
 * A single placement inside a candidate plan: recipe + score earned at that slot.
 */
data class PlannedSlot(
    val key: PlanSlotKey,
    val recipeId: UUID?,
    val score: Double,
)

/**
 * Beam-Search Mahlzeitenplaner (REQ-AUTOPLAN-002, server-side, LOCKED Q4).
 *
 *  - Inputs: pre-fetched candidate pool per [SlotTag] + [PlannerConstraints].
 *  - State : ordered list of [PlannedSlot] (one entry per (day, slot) in expansion-order).
 *  - Score : sum of slot-scores; higher = better.
 *  - Slot score = base + moreOftenBoost (if in moreOftenRecipeIds)
 *               – varietyPenalty * #occurrencesInPrevWindow.
 *  - Hard filters (allergens, prepMinutesMax, avoid-ids) applied to candidate pool BEFORE beam.
 *  - If a slot has no candidate left → emit recipeId=null (unfilled), no score impact.
 */
@Component
class BeamSearchPlanner {

    fun plan(
        candidatePool: Map<SlotTag, List<RecipeEntity>>,
        constraints: PlannerConstraints,
        seed: Long?,
    ): List<PlannedSlot> {
        val rng = if (seed != null) Random(seed) else Random.Default
        val expansionOrder: List<PlanSlotKey> = buildList {
            for (d in 0 until constraints.days) {
                for (s in constraints.slots) add(PlanSlotKey(d, s))
            }
        }
        if (expansionOrder.isEmpty()) return emptyList()

        // Beam: list of (plan-so-far, total-score).
        var beam: List<Pair<List<PlannedSlot>, Double>> = listOf(emptyList<PlannedSlot>() to 0.0)

        for (key in expansionOrder) {
            val pool = candidatePool[key.slot].orEmpty()
            val expansions = mutableListOf<Pair<List<PlannedSlot>, Double>>()
            for ((plan, score) in beam) {
                if (pool.isEmpty()) {
                    expansions += (plan + PlannedSlot(key, null, 0.0)) to score
                    continue
                }
                // Score every candidate against this plan.
                val scored = pool.map { r ->
                    val sc = slotScore(r.id, plan, key, constraints, rng)
                    r to sc
                }.sortedByDescending { it.second }
                // Keep top-beamWidth candidates per parent (heuristic to keep state explosion bounded).
                val topK = scored.take(constraints.beamWidth)
                for ((r, sc) in topK) {
                    expansions += (plan + PlannedSlot(key, r.id, sc)) to (score + sc)
                }
            }
            // Prune to global beamWidth.
            beam = expansions.sortedByDescending { it.second }.take(constraints.beamWidth)
        }

        return beam.firstOrNull()?.first ?: emptyList()
    }

    private fun slotScore(
        recipeId: UUID,
        planSoFar: List<PlannedSlot>,
        currentKey: PlanSlotKey,
        constraints: PlannerConstraints,
        rng: Random,
    ): Double {
        var score = 10.0 // base
        if (recipeId in constraints.moreOftenRecipeIds) score += constraints.moreOftenBoost
        // Variety: count occurrences of this recipe within prior varietyDaySpan days.
        val windowStartDay = currentKey.dayIndex - constraints.varietyDaySpan
        val recent = planSoFar.count { it.recipeId == recipeId && it.key.dayIndex >= windowStartDay }
        score -= constraints.varietyPenalty * recent
        // Tiny random tiebreaker so equally-scored candidates rotate.
        score += rng.nextDouble() * 0.01
        return score
    }
}
