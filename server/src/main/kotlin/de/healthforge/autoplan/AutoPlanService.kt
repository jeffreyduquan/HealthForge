package de.healthforge.autoplan

import de.healthforge.common.ApiException
import de.healthforge.group.GroupService
import de.healthforge.recipe.RecipeBrowseRepo
import de.healthforge.recipe.RecipeEntity
import de.healthforge.recipe.RecipeRepo
import de.healthforge.recipe.SlotTag
import de.healthforge.recipe.VisibilityFilter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class AutoPlanService(
    private val browseRepo: RecipeBrowseRepo,
    private val recipeRepo: RecipeRepo,
    private val groupService: GroupService,
    private val planner: BeamSearchPlanner,
) {

    @Transactional(readOnly = true)
    fun generate(req: AutoPlanGenerateRequest, viewerId: UUID): AutoPlanGenerateResponse {
        if (req.slots.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "EMPTY_SLOTS", "slots must not be empty")
        }
        val groupIds = groupService.groupIdsForUser(viewerId)
        val vf = VisibilityFilter.PublicOrOwnOrGroup(viewerId, groupIds)

        // For each requested slot, fetch up to CANDIDATE_LIMIT IDs (allergen + prep-time filtered),
        // load entities, then drop avoid-list.
        val pool: MutableMap<SlotTag, List<RecipeEntity>> = mutableMapOf()
        for (slot in req.slots.distinct()) {
            val ids = browseRepo.browseIds(
                q = null,
                slotTags = listOf(slot),
                prepMinutesMax = req.prepMinutesMax,
                excludeAllergens = req.excludeAllergens,
                visibilityFilter = vf,
                authorId = null,
                limit = CANDIDATE_LIMIT,
                offset = 0,
            )
            val avoidSet: Set<UUID> = req.avoid.toSet()
            val entities = if (ids.isEmpty()) emptyList() else recipeRepo.findAllById(ids)
                .filter { it.id !in avoidSet }
            pool[slot] = entities
        }

        val constraints = PlannerConstraints(
            slots = req.slots,
            days = req.days,
            excludeAllergens = req.excludeAllergens,
            prepMinutesMax = req.prepMinutesMax,
            moreOftenRecipeIds = req.moreOften.toSet(),
            avoidRecipeIds = req.avoid.toSet(),
            beamWidth = req.beamWidth,
        )
        val planned = planner.plan(pool, constraints, req.seed)

        // Resolve to DTOs (group by day).
        val entityById: Map<UUID, RecipeEntity> = pool.values.flatten().associateBy { it.id }
        val byDay = planned.groupBy { it.key.dayIndex }
        val days = (0 until req.days).map { d ->
            val date: LocalDate = req.startDate.plusDays(d.toLong())
            val slots = byDay[d].orEmpty().mapNotNull { ps ->
                val rid = ps.recipeId ?: return@mapNotNull null
                val r = entityById[rid] ?: return@mapNotNull null
                AutoPlanRecipeSlot(
                    slotTag = ps.key.slot,
                    recipeId = r.id,
                    title = r.title,
                    prepMinutes = r.prepMinutes,
                    score = ps.score,
                )
            }
            AutoPlanDay(date = date, slots = slots)
        }
        val total = planned.sumOf { it.score }
        val unfilled = planned.count { it.recipeId == null }
        return AutoPlanGenerateResponse(days = days, totalScore = total, unfilledSlotCount = unfilled)
    }

    companion object {
        /** Per-slot candidate pool size sent to the planner — keeps beam-search bounded. */
        const val CANDIDATE_LIMIT = 50
    }
}
