package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class IngredientDto(
    val id: String,
    val name_de: String,
    val brand: String?,
    val barcode: String?,
    val source: String,
    val energy_kcal_per_100g: Double?,
    val protein_g_per_100g: Double?,
    val carbs_g_per_100g: Double?,
    val sugar_g_per_100g: Double?,
    val fat_g_per_100g: Double?,
    val satfat_g_per_100g: Double?,
    val fiber_g_per_100g: Double?,
    val salt_g_per_100g: Double?,
    val histamine_score: Int?,
    val allergens: List<String> = emptyList(),
    val fodmap_flags: List<String> = emptyList(),
    val locked: Boolean = true,
)

interface IngredientApi {
    @GET("v1/ingredients")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("excludeAllergens") excludeAllergens: List<String>? = null,
        @Query("excludeFodmap") excludeFodmap: List<String>? = null,
    ): List<IngredientDto>

    @GET("v1/ingredients/{id}")
    suspend fun byId(@Path("id") id: String): IngredientDto

    @GET("v1/ingredients/by-barcode/{barcode}")
    suspend fun byBarcode(@Path("barcode") barcode: String): IngredientDto
}
