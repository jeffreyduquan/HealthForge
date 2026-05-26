package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * REQ-SUPP-004 — Public supplement catalog (read-only for users) and
 * user-submitted suggestions which an admin reviews on the server.
 */

@JsonClass(generateAdapter = true)
data class PublicSupplementDto(
    val id: String,
    val name_de: String,
    val brand: String?,
    val unit_label: String,
    val default_dose: Double,
    val kcal_per_dose: Double?,
    val protein_per_dose: Double?,
    val carbs_per_dose: Double?,
    val fat_per_dose: Double?,
    val micronutrients_json: String?,
    val notes: String?,
    val created_at: String,
)

@JsonClass(generateAdapter = true)
data class CreateSupplementSuggestionRequest(
    val name_de: String,
    val brand: String?,
    val unit_label: String,
    val default_dose: Double,
    val kcal_per_dose: Double?,
    val protein_per_dose: Double?,
    val carbs_per_dose: Double?,
    val fat_per_dose: Double?,
    val micronutrients_json: String?,
    val notes: String?,
)

@JsonClass(generateAdapter = true)
data class SupplementSuggestionCreatedDto(
    val id: String,
    val status: String,
)

interface SupplementApi {
    @GET("v1/supplements/public")
    suspend fun listPublic(): List<PublicSupplementDto>

    @POST("v1/supplements/suggestions")
    suspend fun suggest(@Body body: CreateSupplementSuggestionRequest): SupplementSuggestionCreatedDto
}
