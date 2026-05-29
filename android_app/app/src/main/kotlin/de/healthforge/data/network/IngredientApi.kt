package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class IngredientDto(
    val id: String,
    val name_de: String,
    val brand: String?,
    val barcode: String?,
    val source: String,
    val fdc_id: Long? = null,
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
    /** P7.S5 — Map<NutrientCatalog.key, Wert pro 100 g> (Vitamine + Mineralstoffe). */
    val micronutrients: Map<String, Double> = emptyMap(),
    val locked: Boolean = true,
)

interface IngredientApi {
    @GET("v1/ingredients")
    suspend fun search(
        @Query("q") query: String = "",
        @Query("limit") limit: Int = 20,
        @Query("excludeAllergens") excludeAllergens: List<String>? = null,
        @Query("excludeFodmap") excludeFodmap: List<String>? = null,
    ): List<IngredientDto>

    @GET("v1/ingredients/{id}")
    suspend fun byId(@Path("id") id: String): IngredientDto

    @GET("v1/ingredients/by-barcode/{barcode}")
    suspend fun byBarcode(@Path("barcode") barcode: String): IngredientDto

    @POST("v1/ingredients/suggest")
    suspend fun suggest(@Body body: IngredientSuggestRequest): IngredientSuggestResponse

    @POST("v1/ingredients/{id}/field-pr")
    suspend fun proposeFieldChange(
        @Path("id") ingredientId: String,
        @Body body: FieldPrRequest,
    ): FieldPrResponse
}

@JsonClass(generateAdapter = true)
data class IngredientSuggestRequest(
    val name_de: String,
    val brand: String? = null,
    val barcode: String? = null,
    val energy_kcal_per_100g: Double? = null,
    val protein_g_per_100g: Double? = null,
    val carbs_g_per_100g: Double? = null,
    val sugar_g_per_100g: Double? = null,
    val fat_g_per_100g: Double? = null,
    val satfat_g_per_100g: Double? = null,
    val fiber_g_per_100g: Double? = null,
    val salt_g_per_100g: Double? = null,
    val histamine_score: Int? = null,
    val allergens: List<String> = emptyList(),
    val fodmap_flags: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IngredientSuggestResponse(val id: String, val status: String)

@JsonClass(generateAdapter = true)
data class FieldPrRequest(
    val field_name: String,
    val new_value: String,
    val rationale: String? = null,
)

@JsonClass(generateAdapter = true)
data class FieldPrResponse(val id: String, val status: String)
