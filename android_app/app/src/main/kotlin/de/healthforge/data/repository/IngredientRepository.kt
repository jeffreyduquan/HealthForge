package de.healthforge.data.repository

import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientApi
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.IngredientSuggestRequest
import de.healthforge.data.network.IngredientSuggestResponse
import de.healthforge.data.network.FieldPrRequest
import de.healthforge.data.network.FieldPrResponse
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

    /** REQ-INGR-USER-001 — submit a new ingredient (PENDING until admin review). */
    suspend fun suggest(body: IngredientSuggestRequest): Result<IngredientSuggestResponse> = runCatching {
        api.suggest(body)
    }

    /** REQ-FIELDPR-001 — propose a single-field correction on an existing ingredient. */
    suspend fun proposeFieldChange(
        ingredientId: String,
        body: FieldPrRequest,
    ): Result<FieldPrResponse> = runCatching {
        api.proposeFieldChange(ingredientId, body)
    }
}
