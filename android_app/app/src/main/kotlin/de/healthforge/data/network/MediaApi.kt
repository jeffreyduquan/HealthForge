package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class MediaUploadResponse(val key: String, val bucket: String)

interface MediaApi {
    /** POST /v1/media/upload?bucket=recipes (multipart `file`). REQ-RECIPE-006. */
    @Multipart
    @POST("v1/media/upload")
    suspend fun upload(
        @Query("bucket") bucket: String,
        @Part file: MultipartBody.Part,
    ): MediaUploadResponse
}
