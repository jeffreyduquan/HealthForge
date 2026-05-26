package de.healthforge.domain.shopping

import de.healthforge.data.db.dao.MealPlanDao
import de.healthforge.data.db.dao.ShoppingListDao
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.ShoppingListItemEntity
import de.healthforge.data.repository.RecipeRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/**
 * Aggregiert geplante Mahlzeiten innerhalb eines Datumsbereichs zu einer Einkaufsliste
 * (REQ-SHOP-001/002/003).
 *
 * Strategie:
 *  - Liest [MealPlanSlotEntity]s im Bereich `[start, end]` (inkl.) und ihre Items.
 *  - INGREDIENT-Items: nutzt direkt `snapshotName` + `amount` (Gramm) — Unit-Bucket "g".
 *  - RECIPE-Items: lädt Rezept-Detail vom Server, skaliert Zutaten-Menge anhand
 *    `amount` (Portionen) / `servings`. Bucket je `(ingredient_id, unit)`.
 *  - Aggregat-Key: `(ingredientId, unit)` — Mengen werden summiert, Mengen mit unterschiedlicher
 *    Einheit bleiben getrennt (kein impliziter Konversions-Fehler).
 *  - Kategorie: best-effort `"Sonstiges"` für MVP (kein lokaler Ingredient-Cache).
 *  - Persistiert als neuer `runId` (= `System.currentTimeMillis()`), löscht alte Runs.
 *
 * Returns: `runId` of the newly created list, or `null` if no items found.
 */
@Singleton
class BuildShoppingListUseCase @Inject constructor(
    private val planDao: MealPlanDao,
    private val recipeRepo: RecipeRepository,
    private val shoppingDao: ShoppingListDao,
) {

    /**
     * @param start inklusive Start-Datum
     * @param end inklusive End-Datum (muss `>= start` sein)
     */
    suspend fun build(start: LocalDate, end: LocalDate): Long? {
        require(!end.isBefore(start)) { "end must be >= start" }
        val slots = planDao.slotsBetween(start.toString(), end.toString())
        if (slots.isEmpty()) return null
        val items = planDao.itemsForSlotsOnce(slots.map { it.id })
        if (items.isEmpty()) return null

        // Aggregation buckets: Key = (ingredientId | snapshotName, unit) — value = (name, qty, category)
        data class Bucket(
            val name: String,
            var quantity: Double,
            val unit: String,
            val ingredientId: String?,
            val category: String,
        )

        val buckets = LinkedHashMap<String, Bucket>()
        fun key(id: String?, name: String, unit: String) = "${id ?: "name:$name"}|$unit"

        // Resolve recipe details only once per recipe-id (cache during build).
        val recipeCache = mutableMapOf<String, de.healthforge.data.network.RecipeDetailDto?>()
        suspend fun resolveRecipe(id: String): de.healthforge.data.network.RecipeDetailDto? {
            if (recipeCache.containsKey(id)) return recipeCache[id]
            val r = recipeRepo.detail(id).getOrNull()
            recipeCache[id] = r
            return r
        }

        for (item in items) {
            when (item.sourceType) {
                IntakeSourceType.INGREDIENT -> {
                    val k = key(item.sourceId, item.snapshotName, "g")
                    val b = buckets[k]
                    if (b != null) b.quantity += item.amount
                    else buckets[k] = Bucket(
                        name = item.snapshotName,
                        quantity = item.amount,
                        unit = "g",
                        ingredientId = item.sourceId,
                        category = "Sonstiges",
                    )
                }
                IntakeSourceType.RECIPE -> {
                    val recipe = resolveRecipe(item.sourceId) ?: continue
                    val servings = recipe.servings.coerceAtLeast(1)
                    val scale = item.amount / servings.toDouble()
                    for (ing in recipe.ingredients) {
                        if (ing.is_optional) continue
                        val qty = ing.quantity * scale
                        val name = ing.ingredient_name ?: "Zutat ${ing.ingredient_id.take(6)}"
                        val k = key(ing.ingredient_id, name, ing.unit)
                        val b = buckets[k]
                        if (b != null) b.quantity += qty
                        else buckets[k] = Bucket(
                            name = name,
                            quantity = qty,
                            unit = ing.unit,
                            ingredientId = ing.ingredient_id,
                            category = "Sonstiges",
                        )
                    }
                }
                IntakeSourceType.SUPPLEMENT -> {
                    // Supplements gehören nicht auf eine Lebensmittel-Einkaufsliste.
                }
            }
        }

        if (buckets.isEmpty()) return null

        val runId = System.currentTimeMillis()
        val now = runId
        val entities = buckets.values.map { b ->
            ShoppingListItemEntity(
                runId = runId,
                ingredientId = b.ingredientId,
                name = b.name,
                quantity = roundQty(b.quantity),
                unit = b.unit,
                category = b.category,
                checked = false,
                createdAt = now,
            )
        }
        shoppingDao.insertAll(entities)
        shoppingDao.deleteOldRuns(runId)
        return runId
    }

    private fun roundQty(v: Double): Double = round(v * 100.0) / 100.0
}
