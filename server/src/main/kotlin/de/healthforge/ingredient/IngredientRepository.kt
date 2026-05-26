package de.healthforge.ingredient

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

interface IngredientRepository : JpaRepository<IngredientEntity, UUID> {
    fun findByBarcode(barcode: String): Optional<IngredientEntity>
    fun findBySourceAndSourceId(source: IngredientSource, sourceId: String): Optional<IngredientEntity>
}

/**
 * Custom full-text search using PostgreSQL `to_tsvector('german', unaccent(...))`.
 * Falls back to ILIKE if the query is shorter than 3 chars (avoid `:` parser issues with prefix `*` operator).
 */
@Repository
class IngredientSearchRepository(
    @PersistenceContext private val em: EntityManager,
) {
    @Suppress("UNCHECKED_CAST")
    fun search(
        query: String,
        limit: Int = 20,
        excludeAllergens: List<String> = emptyList(),
        excludeFodmap: List<String> = emptyList(),
    ): List<IngredientEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val safeLimit = limit.coerceIn(1, 100)

        // Build dynamic NOT-LIKE clauses for the (denormalised) JSON arrays.
        // The arrays are stored as TEXT JSON like `["GLUTEN","SOY"]`; an ILIKE match
        // on `"<CODE>"` (with quotes!) is unambiguous because codes have no quote chars.
        // Sanitise codes to A–Z/0–9/_ to keep the native-SQL injection-safe.
        val safeAllergens = excludeAllergens.mapNotNull { sanitiseCode(it) }
        val safeFodmap = excludeFodmap.mapNotNull { sanitiseCode(it) }
        val filterClauses = buildString {
            safeAllergens.forEach { code ->
                append(" AND allergens_json NOT ILIKE '%\"").append(code).append("\"%'")
            }
            safeFodmap.forEach { code ->
                append(" AND fodmap_flags_json NOT ILIKE '%\"").append(code).append("\"%'")
            }
        }

        val sql = """
            SELECT * FROM ingredients
            WHERE (
                hf_immutable_unaccent(lower(name_de)) ILIKE hf_immutable_unaccent(lower(:q))
                OR hf_immutable_unaccent(lower(coalesce(brand,''))) ILIKE hf_immutable_unaccent(lower(:q))
            )
            $filterClauses
            ORDER BY
                CASE WHEN hf_immutable_unaccent(lower(name_de)) ILIKE hf_immutable_unaccent(lower(:qPrefix)) THEN 0 ELSE 1 END,
                length(name_de),
                name_de
            LIMIT :lim
            """.trimIndent()

        return em.createNativeQuery(sql, IngredientEntity::class.java)
            .setParameter("q", "%$trimmed%")
            .setParameter("qPrefix", "$trimmed%")
            .setParameter("lim", safeLimit)
            .resultList as List<IngredientEntity>
    }

    private fun sanitiseCode(code: String): String? {
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty() || trimmed.length > 32) return null
        return if (trimmed.all { it.isLetterOrDigit() || it == '_' }) trimmed else null
    }
}
