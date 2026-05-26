package de.healthforge.community

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

enum class RecipeReportStatus { OPEN, RESOLVED, DISMISSED }

@Entity
@Table(name = "recipe_reports")
class RecipeReportEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    var recipeId: UUID,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "reporter_id", nullable = false, columnDefinition = "uuid")
    var reporterId: UUID,

    @Column(name = "reason", nullable = false)
    var reason: String,

    @Column(name = "status", nullable = false)
    var status: String = RecipeReportStatus.OPEN.name,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "resolved_by", columnDefinition = "uuid")
    var resolvedBy: UUID? = null,

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
