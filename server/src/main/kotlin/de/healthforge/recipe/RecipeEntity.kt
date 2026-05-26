package de.healthforge.recipe

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class RecipeVisibility { PUBLIC, PRIVATE, GROUP }
enum class RecipeStatus { PUBLISHED, REMOVED }
enum class SlotTag { BREAKFAST, LUNCH, DINNER, SNACK }
enum class CommunityRatingValue { RECOMMEND, NOT_RECOMMEND }

@Entity
@Table(name = "recipes")
class RecipeEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "author_id", nullable = false, columnDefinition = "uuid")
    var authorId: UUID,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "image_key")
    var imageKey: String? = null,

    @Column(name = "servings", nullable = false)
    var servings: Int = 1,

    @Column(name = "prep_minutes", nullable = false)
    var prepMinutes: Int = 0,

    @Column(name = "cook_minutes")
    var cookMinutes: Int? = null,

    /** Stored as Postgres text[]; values from [SlotTag]. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "slot_tags", nullable = false, columnDefinition = "text[]")
    var slotTags: Array<String> = emptyArray(),

    @Column(name = "status", nullable = false)
    var status: String = RecipeStatus.PUBLISHED.name,

    @Column(name = "visibility", nullable = false)
    var visibility: String = RecipeVisibility.PUBLIC.name,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "group_id", columnDefinition = "uuid")
    var groupId: UUID? = null,

    @Column(name = "is_official", nullable = false)
    var isOfficial: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

/** Composite PK (recipe_id, position) for recipe_ingredients + recipe_steps. */
data class RecipePositionKey(
    var recipeId: UUID = UUID.randomUUID(),
    var position: Int = 0,
) : Serializable

@Entity
@Table(name = "recipe_ingredients")
@IdClass(RecipePositionKey::class)
class RecipeIngredientEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    var recipeId: UUID,

    @Id
    @Column(name = "position", nullable = false)
    var position: Int,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "ingredient_id", nullable = false, columnDefinition = "uuid")
    var ingredientId: UUID,

    @Column(name = "quantity", nullable = false)
    var quantity: BigDecimal,

    @Column(name = "unit", nullable = false)
    var unit: String,

    @Column(name = "is_optional", nullable = false)
    var isOptional: Boolean = false,

    @Column(name = "note")
    var note: String? = null,
)

@Entity
@Table(name = "recipe_steps")
@IdClass(RecipePositionKey::class)
class RecipeStepEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    var recipeId: UUID,

    @Id
    @Column(name = "position", nullable = false)
    var position: Int,

    @Column(name = "text", nullable = false)
    var text: String,

    @Column(name = "image_key")
    var imageKey: String? = null,
)

/** Composite PK (recipe_id, user_id) for recipe_likes + recipe_ratings_community. */
data class RecipeUserKey(
    var recipeId: UUID = UUID.randomUUID(),
    var userId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "recipe_likes")
@IdClass(RecipeUserKey::class)
class RecipeLikeEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    var recipeId: UUID,

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "recipe_ratings_community")
@IdClass(RecipeUserKey::class)
class RecipeCommunityRatingEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    var recipeId: UUID,

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,

    @Column(name = "value", nullable = false)
    var value: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
