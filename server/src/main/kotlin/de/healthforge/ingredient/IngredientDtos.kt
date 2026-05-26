package de.healthforge.ingredient

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID

data class IngredientDto(
    val id: UUID,
    @JsonProperty("name_de") val nameDe: String,
    val brand: String?,
    val barcode: String?,
    val source: IngredientSource,
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
    val locked: Boolean,
) {
    companion object {
        private val ARRAY_REGEX = Regex("\"([^\"]+)\"")
        fun from(e: IngredientEntity): IngredientDto = IngredientDto(
            id = e.id,
            nameDe = e.nameDe,
            brand = e.brand,
            barcode = e.barcode,
            source = e.source,
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
            locked = e.locked,
        )

        private fun parseStringArray(json: String): List<String> =
            ARRAY_REGEX.findAll(json).map { it.groupValues[1] }.toList()
    }
}
