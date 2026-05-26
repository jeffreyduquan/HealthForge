package de.healthforge.data.repository

import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientApi
import de.healthforge.data.network.IngredientDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val api: IngredientApi,
) {

    /**
     * REQ-INGR-002, REQ-QUALITY-FIX-001: if [applyProfileFilters] is true, the search
     * automatically excludes the user's known allergens / FODMAP intolerances.
     */
    suspend fun search(
        query: String,
        limit: Int = 20,
        excludeAllergens: Set<AllergenType> = emptySet(),
        excludeFodmap: Set<FodmapType> = emptySet(),
    ): Result<List<IngredientDto>> = runCatching {
        api.search(
            query = query,
            limit = limit,
            excludeAllergens = excludeAllergens.takeIf { it.isNotEmpty() }?.map { it.name },
            excludeFodmap = excludeFodmap.takeIf { it.isNotEmpty() }?.map { it.name },
        )
    }

    suspend fun byId(id: String): Result<IngredientDto> = runCatching { api.byId(id) }

    suspend fun byBarcode(barcode: String): Result<IngredientDto> = runCatching { api.byBarcode(barcode) }
}
