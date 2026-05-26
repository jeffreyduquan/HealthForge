package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class AutoPlanGenerateRequest(
    val start_date: String,
    val days: Int,
    val slots: List<String>,
    val exclude_allergens: List<String> = emptyList(),
    val prep_minutes_max: Int? = null,
    val more_often: List<String> = emptyList(),
    val avoid: List<String> = emptyList(),
    val beam_width: Int = 4,
    val seed: Long? = null,
)

@JsonClass(generateAdapter = true)
data class AutoPlanRecipeSlotDto(
    val slot_tag: String,
    val recipe_id: String,
    val title: String,
    val prep_minutes: Int,
    val score: Double,
)

@JsonClass(generateAdapter = true)
data class AutoPlanDayDto(
    val date: String,
    val slots: List<AutoPlanRecipeSlotDto>,
)

@JsonClass(generateAdapter = true)
data class AutoPlanGenerateResponse(
    val days: List<AutoPlanDayDto>,
    val total_score: Double,
    val unfilled_slot_count: Int,
)

interface AutoPlanApi {
    @POST("v1/plans/generate")
    suspend fun generate(@Body body: AutoPlanGenerateRequest): AutoPlanGenerateResponse
}
