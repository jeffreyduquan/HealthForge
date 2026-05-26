package de.healthforge.recipe

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// ===================== Request DTOs =====================

data class RecipeIngredientInput(
    @field:NotNull @JsonProperty("ingredient_id") val ingredientId: UUID,
    @field:NotNull @field:Positive val quantity: BigDecimal,
    @field:NotBlank val unit: String,
    @JsonProperty("is_optional") val isOptional: Boolean = false,
    val note: String? = null,
)

data class RecipeStepInput(
    @field:NotBlank val text: String,
    @JsonProperty("image_key") val imageKey: String? = null,
)

data class RecipeUpsertRequest(
    @field:NotBlank val title: String,
    val description: String? = null,
    @JsonProperty("image_key") val imageKey: String? = null,
    @field:Positive val servings: Int = 1,
    @field:PositiveOrZero @JsonProperty("prep_minutes") val prepMinutes: Int,
    @JsonProperty("cook_minutes") val cookMinutes: Int? = null,
    @field:NotEmpty @JsonProperty("slot_tags") val slotTags: List<SlotTag>,
    @field:NotNull val visibility: RecipeVisibility,
    @JsonProperty("group_id") val groupId: UUID? = null,
    @field:NotEmpty val ingredients: List<RecipeIngredientInput>,
    @field:NotEmpty val steps: List<RecipeStepInput>,
)

data class CommunityRatingRequest(
    @field:NotNull val value: CommunityRatingValue,
)

// ===================== Response DTOs =====================

data class RecipeIngredientDto(
    val position: Int,
    @JsonProperty("ingredient_id") val ingredientId: UUID,
    @JsonProperty("ingredient_name") val ingredientName: String?,
    val quantity: BigDecimal,
    val unit: String,
    @JsonProperty("is_optional") val isOptional: Boolean,
    val note: String?,
)

data class RecipeStepDto(
    val position: Int,
    val text: String,
    @JsonProperty("image_key") val imageKey: String?,
)

/** Aggregated live-computed nutrition (REQ-RECIPE-007). Per *full recipe*. */
data class RecipeNutritionDto(
    @JsonProperty("energy_kcal") val energyKcal: BigDecimal,
    @JsonProperty("protein_g") val proteinG: BigDecimal,
    @JsonProperty("carbs_g") val carbsG: BigDecimal,
    @JsonProperty("fat_g") val fatG: BigDecimal,
    @JsonProperty("fiber_g") val fiberG: BigDecimal,
    @JsonProperty("missing_ingredients") val missingIngredients: List<UUID>,
)

data class RecipeListItemDto(
    val id: UUID,
    val title: String,
    val description: String?,
    @JsonProperty("image_key") val imageKey: String?,
    val servings: Int,
    @JsonProperty("prep_minutes") val prepMinutes: Int,
    @JsonProperty("slot_tags") val slotTags: List<SlotTag>,
    val visibility: RecipeVisibility,
    @JsonProperty("author_id") val authorId: UUID,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("like_count") val likeCount: Long,
    @JsonProperty("community_recommend_count") val communityRecommendCount: Long,
    @JsonProperty("community_not_recommend_count") val communityNotRecommendCount: Long,
)

data class RecipeDetailDto(
    val id: UUID,
    val title: String,
    val description: String?,
    @JsonProperty("image_key") val imageKey: String?,
    val servings: Int,
    @JsonProperty("prep_minutes") val prepMinutes: Int,
    @JsonProperty("cook_minutes") val cookMinutes: Int?,
    @JsonProperty("slot_tags") val slotTags: List<SlotTag>,
    val status: RecipeStatus,
    val visibility: RecipeVisibility,
    @JsonProperty("group_id") val groupId: UUID?,
    @JsonProperty("is_official") val isOfficial: Boolean,
    @JsonProperty("author_id") val authorId: UUID,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("updated_at") val updatedAt: Instant,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<RecipeStepDto>,
    val nutrition: RecipeNutritionDto,
    @JsonProperty("like_count") val likeCount: Long,
    @JsonProperty("liked_by_me") val likedByMe: Boolean,
    @JsonProperty("community_recommend_count") val communityRecommendCount: Long,
    @JsonProperty("community_not_recommend_count") val communityNotRecommendCount: Long,
    @JsonProperty("my_community_rating") val myCommunityRating: CommunityRatingValue?,
)
