package de.healthforge.recipe

import de.healthforge.common.ApiException
import de.healthforge.group.GroupService
import de.healthforge.ingredient.IngredientRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class RecipeService(
    private val recipeRepo: RecipeRepo,
    private val ingredientRowRepo: RecipeIngredientRepo,
    private val stepRowRepo: RecipeStepRepo,
    private val likeRepo: RecipeLikeRepo,
    private val ratingRepo: RecipeCommunityRatingRepo,
    private val browse: RecipeBrowseRepo,
    private val nutritionCompute: RecipeNutritionCompute,
    private val ingredientRepo: IngredientRepository,
    private val groupService: GroupService,
) {

    // ---------- Browse / Detail ----------

    @Transactional(readOnly = true)
    fun browse(
        q: String?,
        slotTags: List<SlotTag>,
        prepMinutesMax: Int?,
        excludeAllergens: List<String>,
        scope: BrowseScope,
        viewerId: UUID,
        authorId: UUID?,
        limit: Int,
        offset: Int,
    ): List<RecipeListItemDto> {
        val viewerGroupIds = groupService.groupIdsForUser(viewerId)
        val vf = when (scope) {
            BrowseScope.PUBLIC -> VisibilityFilter.PublicOnly
            BrowseScope.MINE -> VisibilityFilter.OwnOnly(viewerId)
            BrowseScope.PUBLIC_OR_MINE -> VisibilityFilter.PublicOrOwnOrGroup(viewerId, viewerGroupIds)
        }
        val ids = browse.browseIds(q, slotTags, prepMinutesMax, excludeAllergens, vf, authorId, limit, offset)
        if (ids.isEmpty()) return emptyList()
        val byId = recipeRepo.findAllById(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }.map { r ->
            RecipeListItemDto(
                id = r.id,
                title = r.title,
                description = r.description,
                imageKey = r.imageKey,
                servings = r.servings,
                prepMinutes = r.prepMinutes,
                slotTags = r.slotTags.mapNotNull { runCatching { SlotTag.valueOf(it) }.getOrNull() },
                visibility = RecipeVisibility.valueOf(r.visibility),
                authorId = r.authorId,
                createdAt = r.createdAt,
                likeCount = likeRepo.countByRecipeId(r.id),
                communityRecommendCount = ratingRepo.countByRecipeIdAndValue(r.id, CommunityRatingValue.RECOMMEND.name),
                communityNotRecommendCount = ratingRepo.countByRecipeIdAndValue(r.id, CommunityRatingValue.NOT_RECOMMEND.name),
            )
        }
    }

    @Transactional(readOnly = true)
    fun detail(id: UUID, viewerId: UUID): RecipeDetailDto {
        val r = recipeRepo.findByIdAndStatus(id, RecipeStatus.PUBLISHED.name)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found")
        when (RecipeVisibility.valueOf(r.visibility)) {
            RecipeVisibility.PUBLIC -> { /* ok */ }
            RecipeVisibility.PRIVATE -> if (r.authorId != viewerId) throw ApiException(HttpStatus.FORBIDDEN, "PRIVATE_RECIPE", "private recipe")
            RecipeVisibility.GROUP -> {
                val gid = r.groupId
                val allowed = r.authorId == viewerId || (gid != null && groupService.isMember(viewerId, gid))
                if (!allowed) throw ApiException(HttpStatus.FORBIDDEN, "GROUP_RECIPE_FORBIDDEN", "not a member of recipe's group")
            }
        }
        val items = ingredientRowRepo.findByRecipeIdOrderByPositionAsc(id)
        val steps = stepRowRepo.findByRecipeIdOrderByPositionAsc(id)
        val nutrition = nutritionCompute.compute(items)
        val myRating = ratingRepo.findByRecipeIdAndUserId(id, viewerId)
        return RecipeDetailDto(
            id = r.id,
            title = r.title,
            description = r.description,
            imageKey = r.imageKey,
            servings = r.servings,
            prepMinutes = r.prepMinutes,
            cookMinutes = r.cookMinutes,
            slotTags = r.slotTags.mapNotNull { runCatching { SlotTag.valueOf(it) }.getOrNull() },
            status = RecipeStatus.valueOf(r.status),
            visibility = RecipeVisibility.valueOf(r.visibility),
            groupId = r.groupId,
            isOfficial = r.isOfficial,
            authorId = r.authorId,
            createdAt = r.createdAt,
            updatedAt = r.updatedAt,
            ingredients = run {
                val ids = items.map { it.ingredientId }.distinct()
                val nameById = if (ids.isEmpty()) emptyMap()
                    else ingredientRepo.findAllById(ids).associate { it.id to it.nameDe }
                items.map {
                    RecipeIngredientDto(
                        it.position, it.ingredientId, nameById[it.ingredientId],
                        it.quantity, it.unit, it.isOptional, it.note,
                    )
                }
            },
            steps = steps.map { RecipeStepDto(it.position, it.text, it.imageKey) },
            nutrition = nutrition,
            likeCount = likeRepo.countByRecipeId(id),
            likedByMe = likeRepo.existsByRecipeIdAndUserId(id, viewerId),
            communityRecommendCount = ratingRepo.countByRecipeIdAndValue(id, CommunityRatingValue.RECOMMEND.name),
            communityNotRecommendCount = ratingRepo.countByRecipeIdAndValue(id, CommunityRatingValue.NOT_RECOMMEND.name),
            myCommunityRating = myRating?.value?.let { runCatching { CommunityRatingValue.valueOf(it) }.getOrNull() },
        )
    }

    // ---------- Create / Update / Delete ----------

    @Transactional
    fun create(req: RecipeUpsertRequest, authorId: UUID): UUID {
        validate(req)
        ensureGroupMembership(req, authorId)
        val now = Instant.now()
        val recipe = RecipeEntity(
            id = UUID.randomUUID(),
            authorId = authorId,
            title = req.title.trim(),
            description = req.description?.trim()?.ifEmpty { null },
            imageKey = req.imageKey,
            servings = req.servings,
            prepMinutes = req.prepMinutes,
            cookMinutes = req.cookMinutes,
            slotTags = req.slotTags.map { it.name }.toTypedArray(),
            status = RecipeStatus.PUBLISHED.name,
            visibility = req.visibility.name,
            groupId = if (req.visibility == RecipeVisibility.GROUP) req.groupId else null,
            isOfficial = false,
            createdAt = now,
            updatedAt = now,
        )
        recipeRepo.save(recipe)
        persistChildren(recipe.id, req)
        return recipe.id
    }

    @Transactional
    fun update(id: UUID, req: RecipeUpsertRequest, callerId: UUID) {
        validate(req)
        ensureGroupMembership(req, callerId)
        val existing = recipeRepo.findByIdAndStatus(id, RecipeStatus.PUBLISHED.name)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found")
        if (existing.authorId != callerId) {
            // REQ-RECIPE-008
            throw ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "not the recipe owner")
        }
        existing.title = req.title.trim()
        existing.description = req.description?.trim()?.ifEmpty { null }
        existing.imageKey = req.imageKey
        existing.servings = req.servings
        existing.prepMinutes = req.prepMinutes
        existing.cookMinutes = req.cookMinutes
        existing.slotTags = req.slotTags.map { it.name }.toTypedArray()
        existing.visibility = req.visibility.name
        existing.groupId = if (req.visibility == RecipeVisibility.GROUP) req.groupId else null
        recipeRepo.save(existing)
        // Replace children
        ingredientRowRepo.deleteByRecipeId(id)
        stepRowRepo.deleteByRecipeId(id)
        ingredientRowRepo.flush()
        stepRowRepo.flush()
        persistChildren(id, req)
    }

    @Transactional
    fun softDelete(id: UUID, callerId: UUID) {
        val existing = recipeRepo.findByIdAndStatus(id, RecipeStatus.PUBLISHED.name)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found")
        if (existing.authorId != callerId) throw ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "not the recipe owner")
        existing.status = RecipeStatus.REMOVED.name
        recipeRepo.save(existing)
    }

    // ---------- Likes ----------

    @Transactional
    fun like(recipeId: UUID, userId: UUID) {
        if (!recipeRepo.existsById(recipeId)) throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $recipeId not found")
        if (!likeRepo.existsByRecipeIdAndUserId(recipeId, userId)) {
            likeRepo.save(RecipeLikeEntity(recipeId = recipeId, userId = userId))
        }
    }

    @Transactional
    fun unlike(recipeId: UUID, userId: UUID) {
        likeRepo.deleteByRecipeIdAndUserId(recipeId, userId)
    }

    // ---------- Community-Rating ----------

    @Transactional
    fun upsertCommunityRating(recipeId: UUID, userId: UUID, value: CommunityRatingValue) {
        if (!recipeRepo.existsById(recipeId)) throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $recipeId not found")
        val existing = ratingRepo.findByRecipeIdAndUserId(recipeId, userId)
        if (existing == null) {
            ratingRepo.save(
                RecipeCommunityRatingEntity(
                    recipeId = recipeId,
                    userId = userId,
                    value = value.name,
                )
            )
        } else {
            existing.value = value.name
            ratingRepo.save(existing)
        }
    }

    @Transactional
    fun revokeCommunityRating(recipeId: UUID, userId: UUID) {
        ratingRepo.deleteByRecipeIdAndUserId(recipeId, userId)
    }

    // ---------- Helpers ----------

    private fun validate(req: RecipeUpsertRequest) {
        if (req.title.isBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "title required")
        if (req.prepMinutes < 0) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "prep_minutes must be >= 0")
        if (req.servings < 1) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "servings must be >= 1")
        if (req.slotTags.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "≥1 slot_tag required")
        if (req.ingredients.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "≥1 ingredient required")
        req.ingredients.forEach {
            if (it.quantity.signum() <= 0) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "ingredient quantity must be > 0")
            if (it.unit.isBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "ingredient unit required")
        }
        if (req.steps.isEmpty()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "≥1 step required")
        req.steps.forEach {
            if (it.text.isBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "step text required")
        }
        if (req.visibility == RecipeVisibility.GROUP && req.groupId == null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "group_id required when visibility=GROUP")
        }
        if (req.visibility != RecipeVisibility.GROUP && req.groupId != null) {
            throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "group_id only allowed when visibility=GROUP")
        }
    }

    private fun ensureGroupMembership(req: RecipeUpsertRequest, callerId: UUID) {
        if (req.visibility == RecipeVisibility.GROUP) {
            val gid = req.groupId ?: return // already validated above
            if (!groupService.isMember(callerId, gid)) {
                throw ApiException(HttpStatus.FORBIDDEN, "NOT_GROUP_MEMBER", "caller is not a member of the target group")
            }
        }
    }

    private fun persistChildren(recipeId: UUID, req: RecipeUpsertRequest) {
        req.ingredients.forEachIndexed { idx, i ->
            ingredientRowRepo.save(
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    position = idx,
                    ingredientId = i.ingredientId,
                    quantity = i.quantity,
                    unit = i.unit.trim(),
                    isOptional = i.isOptional,
                    note = i.note?.trim()?.ifEmpty { null },
                )
            )
        }
        req.steps.forEachIndexed { idx, s ->
            stepRowRepo.save(
                RecipeStepEntity(
                    recipeId = recipeId,
                    position = idx,
                    text = s.text.trim(),
                    imageKey = s.imageKey,
                )
            )
        }
    }
}

enum class BrowseScope { PUBLIC, MINE, PUBLIC_OR_MINE }
