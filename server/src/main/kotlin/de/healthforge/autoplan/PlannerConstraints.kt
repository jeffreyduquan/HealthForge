package de.healthforge.autoplan

import de.healthforge.recipe.SlotTag
import java.util.UUID

/**
 * Pure-data view of all hard- and soft-constraints used by the Beam-Search planner.
 *
 *  - excludeAllergens / avoidRecipeIds → HARD: never appear in the generated plan.
 *  - moreOftenRecipeIds                → SOFT: large positive score boost.
 *  - prepMinutesMax                    → HARD when set: recipes above it are filtered before planning.
 *  - variety windows                   → SOFT: repeated recipe within `varietyDaySpan` slots is penalised.
 */
data class PlannerConstraints(
    val slots: List<SlotTag>,
    val days: Int,
    val excludeAllergens: List<String>,
    val prepMinutesMax: Int?,
    val moreOftenRecipeIds: Set<UUID>,
    val avoidRecipeIds: Set<UUID>,
    val beamWidth: Int,
    val varietyDaySpan: Int = 3,
    val moreOftenBoost: Double = 100.0,
    val varietyPenalty: Double = 25.0,
)
