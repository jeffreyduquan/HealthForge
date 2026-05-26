package de.healthforge.recipe

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

    fun compute(items: List<RecipeIngredientEntity>): RecipeNutritionDto {
        if (items.isEmpty()) return zero()

        val byId: Map<UUID, IngredientEntity> = ingredientRepo
            .findAllById(items.map { it.ingredientId })
            .associateBy { it.id }

        var kcal = BigDecimal.ZERO
        var protein = BigDecimal.ZERO
        var carbs = BigDecimal.ZERO
        var fat = BigDecimal.ZERO
        var fiber = BigDecimal.ZERO
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
            fat = fat.add((ing.fatGPer100g ?: BigDecimal.ZERO).multiply(factor))
            fiber = fiber.add((ing.fiberGPer100g ?: BigDecimal.ZERO).multiply(factor))
        }

        return RecipeNutritionDto(
            energyKcal = kcal.setScale(1, RoundingMode.HALF_UP),
            proteinG = protein.setScale(1, RoundingMode.HALF_UP),
            carbsG = carbs.setScale(1, RoundingMode.HALF_UP),
            fatG = fat.setScale(1, RoundingMode.HALF_UP),
            fiberG = fiber.setScale(1, RoundingMode.HALF_UP),
            missingIngredients = missing.distinct(),
        )
    }

    private fun zero() = RecipeNutritionDto(
        energyKcal = BigDecimal.ZERO,
        proteinG = BigDecimal.ZERO,
        carbsG = BigDecimal.ZERO,
        fatG = BigDecimal.ZERO,
        fiberG = BigDecimal.ZERO,
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
}
