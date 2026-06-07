package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class LatestReleaseDto(
    val id: String,
    val version: String,
    val filename: String,
    val fileSize: Long,
    val changelog: String?,
    val downloadUrl: String?,
    @Json(name = "createdAt") val createdAt: String,
)

interface ReleaseApi {
    /** GET /v1/releases/latest — public, returns newest APK metadata + download URL. */
    @GET("v1/releases/latest")
    suspend fun latest(): LatestReleaseDto
}
