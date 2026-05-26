package de.healthforge.autoplan

import com.fasterxml.jackson.annotation.JsonProperty
import de.healthforge.recipe.SlotTag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class AutoPlanGenerateRequest(
    @field:NotNull @JsonProperty("start_date") val startDate: LocalDate,
    @field:Min(1) @field:Max(14) val days: Int = 7,
    @field:NotEmpty val slots: List<SlotTag>,
    @JsonProperty("exclude_allergens") val excludeAllergens: List<String> = emptyList(),
    @field:Min(0) @JsonProperty("prep_minutes_max") val prepMinutesMax: Int? = null,
    @JsonProperty("more_often") val moreOften: List<UUID> = emptyList(),
    val avoid: List<UUID> = emptyList(),
    @field:Min(1) @field:Max(16) @JsonProperty("beam_width") val beamWidth: Int = 4,
    @field:Min(0) val seed: Long? = null,
)

data class AutoPlanRecipeSlot(
    @JsonProperty("slot_tag") val slotTag: SlotTag,
    @JsonProperty("recipe_id") val recipeId: UUID,
    val title: String,
    @JsonProperty("prep_minutes") val prepMinutes: Int,
    val score: Double,
)

data class AutoPlanDay(
    val date: LocalDate,
    val slots: List<AutoPlanRecipeSlot>,
)

data class AutoPlanGenerateResponse(
    val days: List<AutoPlanDay>,
    @JsonProperty("total_score") val totalScore: Double,
    @JsonProperty("unfilled_slot_count") val unfilledSlotCount: Int,
)
