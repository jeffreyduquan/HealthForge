package de.healthforge.ingredient

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * REQ-FIELDPR-001..003 — Vorschlag zur Änderung eines einzelnen Feldes eines vorhandenen
 * Ingredients. Bleibt PENDING bis Admin entscheidet; angezeigter Wert bleibt unverändert
 * solange PENDING.
 */
@Entity
@Table(name = "ingredient_field_pr")
class IngredientFieldPrEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "ingredient_id", nullable = false, columnDefinition = "uuid")
    var ingredientId: UUID,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "proposer_id", nullable = false, columnDefinition = "uuid")
    var proposerId: UUID,

    @Column(name = "field_name", nullable = false)
    var fieldName: String,

    @Column(name = "old_value")
    var oldValue: String? = null,

    @Column(name = "new_value", nullable = false)
    var newValue: String,

    @Column(name = "rationale")
    var rationale: String? = null,

    @Column(name = "status", nullable = false)
    var status: String = IngredientStatus.PENDING.name,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "reviewer_id", columnDefinition = "uuid")
    var reviewerId: UUID? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_note")
    var reviewNote: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)

@Repository
interface IngredientFieldPrRepository : JpaRepository<IngredientFieldPrEntity, UUID> {
    fun findAllByStatusOrderByCreatedAtAsc(status: String): List<IngredientFieldPrEntity>
    fun findAllByOrderByCreatedAtDesc(): List<IngredientFieldPrEntity>
}
