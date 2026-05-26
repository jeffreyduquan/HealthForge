package de.healthforge.data.repository

import de.healthforge.data.network.CommunityRatingRequest
import de.healthforge.data.network.CreateReportRequest
import de.healthforge.data.network.RecipeApi
import de.healthforge.data.network.RecipeDetailDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.network.RecipeUpsertRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recipe-Client (P2.S2). Wraps [RecipeApi] in Result-Wrapper for error handling in ViewModels.
 *
 * REQ-RECIPE-001..009, REQ-RATING-002/003/005.
 */
@Singleton
class RecipeRepository @Inject constructor(
    private val api: RecipeApi,
) {

    suspend fun browse(
        q: String? = null,
        slot: List<String>? = null,
        prepMax: Int? = null,
        excludeAllergens: List<String>? = null,
        scope: String = "PUBLIC_OR_MINE",
        author: String? = null,
        limit: Int = 30,
        offset: Int = 0,
    ): Result<List<RecipeListItemDto>> = runCatching {
        api.browse(
            q = q?.takeIf { it.isNotBlank() },
            slot = slot?.takeIf { it.isNotEmpty() },
            prepMax = prepMax,
            excludeAllergens = excludeAllergens?.takeIf { it.isNotEmpty() },
            scope = scope,
            author = author,
            limit = limit,
            offset = offset,
        )
    }

    suspend fun detail(id: String): Result<RecipeDetailDto> = runCatching { api.detail(id) }

    suspend fun create(req: RecipeUpsertRequest): Result<String> = runCatching { api.create(req).id }

    suspend fun update(id: String, req: RecipeUpsertRequest): Result<Unit> = runCatching { api.update(id, req) }

    suspend fun delete(id: String): Result<Unit> = runCatching { api.delete(id) }

    suspend fun like(id: String): Result<Unit> = runCatching { api.like(id) }

    suspend fun unlike(id: String): Result<Unit> = runCatching { api.unlike(id) }

    suspend fun communityRate(id: String, value: String): Result<Unit> = runCatching {
        api.upsertCommunityRating(id, CommunityRatingRequest(value))
    }

    suspend fun revokeCommunityRating(id: String): Result<Unit> = runCatching { api.revokeCommunityRating(id) }

    suspend fun report(id: String, reason: String): Result<Unit> = runCatching {
        api.report(id, CreateReportRequest(reason.trim()))
    }
}
