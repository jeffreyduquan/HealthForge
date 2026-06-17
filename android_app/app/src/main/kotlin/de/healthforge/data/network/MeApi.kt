package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class MySubmissionsDto(
    val ingredients: List<SubmissionDto>,
    val recipes: List<SubmissionDto>,
    val supplements: List<SubmissionDto>,
)

@JsonClass(generateAdapter = true)
data class SubmissionDto(
    val id: String,
    val name_de: String,
    val status: String,
    val created_at: String,
    val review_note: String?,
)

interface MeApi {
    @GET("v1/me/submissions")
    suspend fun mySubmissions(): MySubmissionsDto
}
