package de.healthforge.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/** M:N Join-Tabelle: Welche Rezepte sind in welchen Gruppen (V21). */
data class GroupRecipeKey(
    val groupId: UUID = UUID.randomUUID(),
    val recipeId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "group_recipes")
@IdClass(GroupRecipeKey::class)
class GroupRecipeEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    var groupId: UUID,

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    var recipeId: UUID,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "added_by", columnDefinition = "uuid")
    var addedBy: UUID? = null,

    @Column(name = "added_at", nullable = false)
    var addedAt: Instant = Instant.now(),
)
