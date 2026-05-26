package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ===================== DTOs =====================

@JsonClass(generateAdapter = true)
data class RecipeNutritionDto(
    val energy_kcal: Double,
    val protein_g: Double,
    val carbs_g: Double,
    val fat_g: Double,
    val fiber_g: Double,
    val missing_ingredients: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RecipeIngredientDto(
    val position: Int,
    val ingredient_id: String,
    val ingredient_name: String?,
    val quantity: Double,
    val unit: String,
    val is_optional: Boolean,
    val note: String?,
)

@JsonClass(generateAdapter = true)
data class RecipeStepDto(
    val position: Int,
    val text: String,
    val image_key: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecipeListItemDto(
    val id: String,
    val title: String,
    val description: String?,
    val image_key: String?,
    val servings: Int,
    val prep_minutes: Int,
    val slot_tags: List<String>,
    val visibility: String,
    val author_id: String,
    val created_at: String,
    val like_count: Long,
    val community_recommend_count: Long,
    val community_not_recommend_count: Long,
)

@JsonClass(generateAdapter = true)
data class RecipeDetailDto(
    val id: String,
    val title: String,
    val description: String?,
    val image_key: String?,
    val servings: Int,
    val prep_minutes: Int,
    val cook_minutes: Int?,
    val slot_tags: List<String>,
    val status: String,
    val visibility: String,
    val group_id: String?,
    val is_official: Boolean,
    val author_id: String,
    val created_at: String,
    val updated_at: String,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>,
    val nutrition: RecipeNutritionDto,
    val like_count: Long,
    val liked_by_me: Boolean,
    val community_recommend_count: Long,
    val community_not_recommend_count: Long,
    val my_community_rating: String?,
)

@JsonClass(generateAdapter = true)
data class RecipeIngredientInput(
    val ingredient_id: String,
    val quantity: Double,
    val unit: String,
    val is_optional: Boolean = false,
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecipeStepInput(
    val text: String,
    val image_key: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecipeUpsertRequest(
    val title: String,
    val description: String? = null,
    val image_key: String? = null,
    val servings: Int = 1,
    val prep_minutes: Int,
    val cook_minutes: Int? = null,
    val slot_tags: List<String>,
    val visibility: String,
    val group_id: String? = null,
    val ingredients: List<RecipeIngredientInput>,
    val steps: List<RecipeStepInput>,
)

@JsonClass(generateAdapter = true)
data class CommunityRatingRequest(
    val value: String,
)

@JsonClass(generateAdapter = true)
data class CreateReportRequest(
    val reason: String,
)

@JsonClass(generateAdapter = true)
data class CreatedIdDto(val id: String)

// ===================== API =====================

interface RecipeApi {

    @GET("v1/recipes")
    suspend fun browse(
        @Query("q") q: String? = null,
        @Query("slot") slot: List<String>? = null,
        @Query("prepMax") prepMax: Int? = null,
        @Query("excludeAllergens") excludeAllergens: List<String>? = null,
        @Query("scope") scope: String = "PUBLIC",
        @Query("author") author: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
    ): List<RecipeListItemDto>

    @GET("v1/recipes/{id}")
    suspend fun detail(@Path("id") id: String): RecipeDetailDto

    @POST("v1/recipes")
    suspend fun create(@Body req: RecipeUpsertRequest): CreatedIdDto

    @PUT("v1/recipes/{id}")
    suspend fun update(@Path("id") id: String, @Body req: RecipeUpsertRequest)

    @DELETE("v1/recipes/{id}")
    suspend fun delete(@Path("id") id: String)

    @POST("v1/recipes/{id}/like")
    suspend fun like(@Path("id") id: String)

    @DELETE("v1/recipes/{id}/like")
    suspend fun unlike(@Path("id") id: String)

    @PUT("v1/recipes/{id}/community-rating")
    suspend fun upsertCommunityRating(@Path("id") id: String, @Body req: CommunityRatingRequest)

    @DELETE("v1/recipes/{id}/community-rating")
    suspend fun revokeCommunityRating(@Path("id") id: String)

    @POST("v1/recipes/{id}/reports")
    suspend fun report(@Path("id") id: String, @Body req: CreateReportRequest): CreatedIdDto
}
