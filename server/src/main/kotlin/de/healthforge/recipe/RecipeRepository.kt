package de.healthforge.recipe

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface RecipeRepo : JpaRepository<RecipeEntity, UUID> {
    fun findByIdAndStatus(id: UUID, status: String): RecipeEntity?
    fun findAllByAuthorIdAndStatusOrderByCreatedAtDesc(authorId: UUID, status: String): List<RecipeEntity>
}

interface RecipeIngredientRepo : JpaRepository<RecipeIngredientEntity, RecipePositionKey> {
    fun findByRecipeIdOrderByPositionAsc(recipeId: UUID): List<RecipeIngredientEntity>
    fun deleteByRecipeId(recipeId: UUID)
}

interface RecipeStepRepo : JpaRepository<RecipeStepEntity, RecipePositionKey> {
    fun findByRecipeIdOrderByPositionAsc(recipeId: UUID): List<RecipeStepEntity>
    fun deleteByRecipeId(recipeId: UUID)
}

interface RecipeLikeRepo : JpaRepository<RecipeLikeEntity, RecipeUserKey> {
    fun existsByRecipeIdAndUserId(recipeId: UUID, userId: UUID): Boolean
    fun deleteByRecipeIdAndUserId(recipeId: UUID, userId: UUID)
    fun countByRecipeId(recipeId: UUID): Long
}

interface RecipeCommunityRatingRepo : JpaRepository<RecipeCommunityRatingEntity, RecipeUserKey> {
    fun findByRecipeIdAndUserId(recipeId: UUID, userId: UUID): RecipeCommunityRatingEntity?
    fun deleteByRecipeIdAndUserId(recipeId: UUID, userId: UUID)
    fun countByRecipeIdAndValue(recipeId: UUID, value: String): Long
}

/**
 * Native-SQL browse query supporting:
 *  - free-text FTS via the same hf_immutable_unaccent('german' dict) used for ingredients,
 *  - slot-tag overlap (`slot_tags && ARRAY[...]`),
 *  - prep-time ceiling,
 *  - allergen exclusion (check the *joined* ingredient.allergens_json textual array;
 *    if any ingredient of the recipe contains any of the excluded allergens → drop the recipe),
 *  - visibility scope (PUBLIC always; PRIVATE/GROUP only when explicitly requested with caller-context),
 *  - cursor-style pagination (offset + limit; limit ≤ 50).
 *
 *  Returns id list — the service then fetches full entities to assemble DTOs.
 */
@Repository
class RecipeBrowseRepo(
    @PersistenceContext private val em: EntityManager,
) {
    fun browseIds(
        q: String?,
        slotTags: List<SlotTag>,
        prepMinutesMax: Int?,
        excludeAllergens: List<String>,
        visibilityFilter: VisibilityFilter,
        authorId: UUID?,
        limit: Int,
        offset: Int,
    ): List<UUID> {
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val params = mutableMapOf<String, Any>("lim" to safeLimit, "off" to safeOffset)
        val where = StringBuilder("r.status = 'PUBLISHED'")

        when (visibilityFilter) {
            is VisibilityFilter.PublicOnly -> where.append(" AND r.visibility = 'PUBLIC'")
            is VisibilityFilter.OwnOnly -> {
                where.append(" AND r.author_id = :ownAuthor")
                params["ownAuthor"] = visibilityFilter.userId
            }
            is VisibilityFilter.PublicOrOwn -> {
                where.append(" AND (r.visibility = 'PUBLIC' OR r.author_id = :viewerForPrivate)")
                params["viewerForPrivate"] = visibilityFilter.userId
            }
            is VisibilityFilter.PublicOrOwnOrGroup -> {
                if (visibilityFilter.groupIds.isEmpty()) {
                    where.append(" AND (r.visibility = 'PUBLIC' OR r.author_id = :viewerForPrivate)")
                    params["viewerForPrivate"] = visibilityFilter.userId
                } else {
                    where.append(
                        " AND (r.visibility = 'PUBLIC' OR r.author_id = :viewerForPrivate" +
                            " OR (r.visibility = 'GROUP' AND r.group_id IN (:viewerGroupIds)))"
                    )
                    params["viewerForPrivate"] = visibilityFilter.userId
                    params["viewerGroupIds"] = visibilityFilter.groupIds
                }
            }
        }

        if (!q.isNullOrBlank()) {
            where.append(
                " AND (hf_immutable_unaccent(lower(r.title)) ILIKE hf_immutable_unaccent(lower(:q))" +
                    " OR hf_immutable_unaccent(lower(coalesce(r.description,''))) ILIKE hf_immutable_unaccent(lower(:q)))"
            )
            params["q"] = "%${q.trim()}%"
        }

        if (slotTags.isNotEmpty()) {
            // Use array overlap; emit as a literal array constructed from sanitised enum names.
            val arr = slotTags.joinToString(",") { "'${it.name}'" }
            where.append(" AND r.slot_tags && ARRAY[$arr]::text[]")
        }

        if (prepMinutesMax != null && prepMinutesMax >= 0) {
            where.append(" AND r.prep_minutes <= :prepMax")
            params["prepMax"] = prepMinutesMax
        }

        if (authorId != null && visibilityFilter !is VisibilityFilter.OwnOnly) {
            where.append(" AND r.author_id = :authorFilter")
            params["authorFilter"] = authorId
        }

        // Allergen exclusion: NOT EXISTS any ingredient referenced by this recipe whose
        // `ingredients.allergens_json` contains any of the requested allergen codes.
        // We sanitise codes to [A-Z0-9_] to keep the inlined ILIKE clauses injection-safe.
        val safeAllergens = excludeAllergens.mapNotNull { sanitiseCode(it) }
        if (safeAllergens.isNotEmpty()) {
            val orClauses = safeAllergens.joinToString(" OR ") {
                "i.allergens_json ILIKE '%\"$it\"%'"
            }
            where.append(
                " AND NOT EXISTS (" +
                    "SELECT 1 FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id " +
                    "WHERE ri.recipe_id = r.id AND ($orClauses)" +
                    ")"
            )
        }

        val sql = """
            SELECT r.id FROM recipes r
            WHERE $where
            ORDER BY r.created_at DESC, r.id
            LIMIT :lim OFFSET :off
        """.trimIndent()

        val nq = em.createNativeQuery(sql)
        params.forEach { (k, v) -> nq.setParameter(k, v) }
        @Suppress("UNCHECKED_CAST")
        val rows = nq.resultList as List<Any>
        return rows.map {
            when (it) {
                is UUID -> it
                is String -> UUID.fromString(it)
                else -> UUID.fromString(it.toString())
            }
        }
    }

    private fun sanitiseCode(code: String): String? {
        val u = code.trim().uppercase()
        if (u.isEmpty() || u.length > 32) return null
        return if (u.all { it.isLetterOrDigit() || it == '_' }) u else null
    }
}

sealed interface VisibilityFilter {
    object PublicOnly : VisibilityFilter
    data class OwnOnly(val userId: UUID) : VisibilityFilter
    data class PublicOrOwn(val userId: UUID) : VisibilityFilter
    data class PublicOrOwnOrGroup(val userId: UUID, val groupIds: List<UUID>) : VisibilityFilter
}
