package de.healthforge.ingredient

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.UUID

data class IngredientDto(
    val id: UUID,
    @JsonProperty("name_de") val nameDe: String,
    val brand: String?,
    val barcode: String?,
    val source: IngredientSource,
    @JsonProperty("fdc_id") val fdcId: Long?,
    @JsonProperty("energy_kcal_per_100g") val energyKcalPer100g: BigDecimal?,
    @JsonProperty("protein_g_per_100g") val proteinGPer100g: BigDecimal?,
    @JsonProperty("carbs_g_per_100g") val carbsGPer100g: BigDecimal?,
    @JsonProperty("sugar_g_per_100g") val sugarGPer100g: BigDecimal?,
    @JsonProperty("fat_g_per_100g") val fatGPer100g: BigDecimal?,
    @JsonProperty("satfat_g_per_100g") val satfatGPer100g: BigDecimal?,
    @JsonProperty("fiber_g_per_100g") val fiberGPer100g: BigDecimal?,
    @JsonProperty("salt_g_per_100g") val saltGPer100g: BigDecimal?,
    @JsonProperty("histamine_score") val histamineScore: Short?,
    val allergens: List<String>,
    @JsonProperty("fodmap_flags") val fodmapFlags: List<String>,
    /** P7.S1 — Map<NutrientCatalog.key, Wert pro 100g>. */
    val micronutrients: Map<String, Double>,
    val locked: Boolean,
) {
    companion object {
        private val ARRAY_REGEX = Regex("\"([^\"]+)\"")
        private val MAPPER = ObjectMapper()

        fun from(e: IngredientEntity): IngredientDto = IngredientDto(
            id = e.id,
            nameDe = e.nameDe,
            brand = e.brand,
            barcode = e.barcode,
            source = e.source,
            fdcId = e.fdcId,
            energyKcalPer100g = e.energyKcalPer100g,
            proteinGPer100g = e.proteinGPer100g,
            carbsGPer100g = e.carbsGPer100g,
            sugarGPer100g = e.sugarGPer100g,
            fatGPer100g = e.fatGPer100g,
            satfatGPer100g = e.satfatGPer100g,
            fiberGPer100g = e.fiberGPer100g,
            saltGPer100g = e.saltGPer100g,
            histamineScore = e.histamineScore,
            allergens = parseStringArray(e.allergensJson),
            fodmapFlags = parseStringArray(e.fodmapFlagsJson),
            micronutrients = parseMicronutrients(e.micronutrientsJson),
            locked = e.locked,
        )

        private fun parseStringArray(json: String): List<String> =
            ARRAY_REGEX.findAll(json).map { it.groupValues[1] }.toList()

        @Suppress("UNCHECKED_CAST")
        private fun parseMicronutrients(json: String): Map<String, Double> = try {
            (MAPPER.readValue(json, Map::class.java) as Map<String, Any?>)
                .mapNotNull { (k, v) -> (v as? Number)?.toDouble()?.let { k to it } }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
