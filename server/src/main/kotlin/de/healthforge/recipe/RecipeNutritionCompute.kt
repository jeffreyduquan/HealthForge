package de.healthforge.recipe

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.healthforge.ingredient.IngredientEntity
import de.healthforge.ingredient.IngredientRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * REQ-RECIPE-007: Recipe nutrition is computed live from its ingredients (no stored nutrition row).
 *
 * Aggregates kcal + macros across all recipe ingredients. The naming
 * `*_g_per_100g` on [IngredientEntity] is the per-100-gram baseline. To turn a recipe
 * ingredient `(quantity, unit)` into a 100g-fraction we apply [normaliseToGrams]:
 *  - `g` / `gramm`  → identity
 *  - `kg`           → ×1000
 *  - `mg`           → ÷1000
 *  - `ml` / `l`     → assumed 1g≈1ml (water-equivalence — flagged in `missingIngredients` if non-applicable
 *                      ingredients dominate, future work can refine via density table)
 *  - everything else (Stück / TL / EL / Prise / …) → not convertible → counted into `missingIngredients`
 *
 * The result represents the *full recipe* (not per serving — that is the client's responsibility,
 * because servings can be re-scaled at view time).
 */
@Component
class RecipeNutritionCompute(
    private val ingredientRepo: IngredientRepository,
) {
    private val objectMapper = jacksonObjectMapper()

    fun compute(items: List<RecipeIngredientEntity>): RecipeNutritionDto {
        if (items.isEmpty()) return zero()

        val byId: Map<UUID, IngredientEntity> = ingredientRepo
            .findAllById(items.map { it.ingredientId })
            .associateBy { it.id }

        var kcal = BigDecimal.ZERO
        var protein = BigDecimal.ZERO
        var carbs = BigDecimal.ZERO
        var sugar = BigDecimal.ZERO
        var fat = BigDecimal.ZERO
        var satfat = BigDecimal.ZERO
        var fiber = BigDecimal.ZERO
        var salt = BigDecimal.ZERO
        val micronutrients = mutableMapOf<String, BigDecimal>()
        val missing = mutableListOf<UUID>()

        for (it in items) {
            if (it.isOptional) continue
            val ing = byId[it.ingredientId]
            if (ing == null) {
                missing.add(it.ingredientId)
                continue
            }
            val grams = normaliseToGrams(it.quantity, it.unit)
            if (grams == null) {
                missing.add(it.ingredientId)
                continue
            }
            val factor = grams.divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
            kcal = kcal.add((ing.energyKcalPer100g ?: BigDecimal.ZERO).multiply(factor))
            protein = protein.add((ing.proteinGPer100g ?: BigDecimal.ZERO).multiply(factor))
            carbs = carbs.add((ing.carbsGPer100g ?: BigDecimal.ZERO).multiply(factor))
            sugar = sugar.add((ing.sugarGPer100g ?: BigDecimal.ZERO).multiply(factor))
            fat = fat.add((ing.fatGPer100g ?: BigDecimal.ZERO).multiply(factor))
            satfat = satfat.add((ing.satfatGPer100g ?: BigDecimal.ZERO).multiply(factor))
            fiber = fiber.add((ing.fiberGPer100g ?: BigDecimal.ZERO).multiply(factor))
            salt = salt.add((ing.saltGPer100g ?: BigDecimal.ZERO).multiply(factor))

            // Aggregate micronutrients from JSON
            try {
                val micros: Map<String, Double> = objectMapper.readValue(ing.micronutrientsJson)
                for ((key, value) in micros) {
                    val scaled = BigDecimal.valueOf(value).multiply(factor)
                    micronutrients.merge(key, scaled) { a, b -> a.add(b) }
                }
            } catch (_: Exception) {
                // ignore malformed JSON
            }
        }

        return RecipeNutritionDto(
            energyKcal = kcal.setScale(1, RoundingMode.HALF_UP),
            proteinG = protein.setScale(1, RoundingMode.HALF_UP),
            carbsG = carbs.setScale(1, RoundingMode.HALF_UP),
            sugarG = sugar.setScale(1, RoundingMode.HALF_UP),
            fatG = fat.setScale(1, RoundingMode.HALF_UP),
            satfatG = satfat.setScale(1, RoundingMode.HALF_UP),
            fiberG = fiber.setScale(1, RoundingMode.HALF_UP),
            saltG = salt.setScale(1, RoundingMode.HALF_UP),
            micronutrients = micronutrients.mapValues { (_, v) -> v.setScale(3, RoundingMode.HALF_UP) },
            missingIngredients = missing.distinct(),
        )
    }

    private fun zero() = RecipeNutritionDto(
        energyKcal = BigDecimal.ZERO,
        proteinG = BigDecimal.ZERO,
        carbsG = BigDecimal.ZERO,
        sugarG = BigDecimal.ZERO,
        fatG = BigDecimal.ZERO,
        satfatG = BigDecimal.ZERO,
        fiberG = BigDecimal.ZERO,
        saltG = BigDecimal.ZERO,
        micronutrients = emptyMap(),
        missingIngredients = emptyList(),
    )

    private fun normaliseToGrams(quantity: BigDecimal, unit: String): BigDecimal? {
        val u = unit.trim().lowercase()
        return when (u) {
            "g", "gramm" -> quantity
            "kg" -> quantity.multiply(BigDecimal(1000))
            "mg" -> quantity.divide(BigDecimal(1000), 6, RoundingMode.HALF_UP)
            "ml" -> quantity   // water-equivalence assumption
            "l", "liter" -> quantity.multiply(BigDecimal(1000))
            else -> null
        }
    }

    /**
     * P7.S5 — Computes total weight + per-100g macros for the MasterTile list view.
     * Returns null if total weight cannot be determined (missing/unconvertible ingredients).
     */
    fun computeSummary(items: List<RecipeIngredientEntity>): RecipeNutritionSummary? {
        if (items.isEmpty()) return null

        val byId: Map<UUID, IngredientEntity> = ingredientRepo
            .findAllById(items.map { it.ingredientId })
            .associateBy { it.id }

        var totalWeight = BigDecimal.ZERO
        var kcal = BigDecimal.ZERO
        var protein = BigDecimal.ZERO
        var carbs = BigDecimal.ZERO
        var sugar = BigDecimal.ZERO
        var fat = BigDecimal.ZERO
        var satfat = BigDecimal.ZERO
        var fiber = BigDecimal.ZERO
        var salt = BigDecimal.ZERO
        val microTotals = mutableMapOf<String, BigDecimal>()

        for (it in items) {
            if (it.isOptional) continue
            val grams = normaliseToGrams(it.quantity, it.unit) ?: return null
            totalWeight = totalWeight.add(grams)
            val ing = byId[it.ingredientId] ?: continue
            val factor = grams.divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
            kcal = kcal.add((ing.energyKcalPer100g ?: BigDecimal.ZERO).multiply(factor))
            protein = protein.add((ing.proteinGPer100g ?: BigDecimal.ZERO).multiply(factor))
            carbs = carbs.add((ing.carbsGPer100g ?: BigDecimal.ZERO).multiply(factor))
            sugar = sugar.add((ing.sugarGPer100g ?: BigDecimal.ZERO).multiply(factor))
            fat = fat.add((ing.fatGPer100g ?: BigDecimal.ZERO).multiply(factor))
            satfat = satfat.add((ing.satfatGPer100g ?: BigDecimal.ZERO).multiply(factor))
            fiber = fiber.add((ing.fiberGPer100g ?: BigDecimal.ZERO).multiply(factor))
            salt = salt.add((ing.saltGPer100g ?: BigDecimal.ZERO).multiply(factor))
            // Aggregate micronutrients
            try {
                val micros: Map<String, Double> = objectMapper.readValue(ing.micronutrientsJson)
                for ((key, value) in micros) {
                    val scaled = BigDecimal.valueOf(value).multiply(factor)
                    microTotals.merge(key, scaled) { a, b -> a.add(b) }
                }
            } catch (_: Exception) { }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) return null

        val factor100 = BigDecimal(100).divide(totalWeight, 6, RoundingMode.HALF_UP)
        return RecipeNutritionSummary(
            totalWeightGrams = totalWeight.setScale(1, RoundingMode.HALF_UP),
            kcalPer100g = kcal.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            proteinPer100g = protein.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            carbsPer100g = carbs.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            sugarPer100g = sugar.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            fatPer100g = fat.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            satfatPer100g = satfat.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            fiberPer100g = fiber.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            saltPer100g = salt.multiply(factor100).setScale(1, RoundingMode.HALF_UP),
            micronutrientsPer100g = microTotals.mapValues { (_, v) -> v.multiply(factor100).setScale(3, RoundingMode.HALF_UP) }.filterValues { it > BigDecimal.ZERO },
        )
    }
}

data class RecipeNutritionSummary(
    val totalWeightGrams: BigDecimal,
    val kcalPer100g: BigDecimal,
    val proteinPer100g: BigDecimal,
    val carbsPer100g: BigDecimal,
    val sugarPer100g: BigDecimal,
    val fatPer100g: BigDecimal,
    val satfatPer100g: BigDecimal,
    val fiberPer100g: BigDecimal,
    val saltPer100g: BigDecimal,
    val micronutrientsPer100g: Map<String, BigDecimal>,
)
