package de.healthforge.recipe

import de.healthforge.auth.AuthPrincipal
import de.healthforge.auth.UserRole
import de.healthforge.common.ApiException
import de.healthforge.group.GroupRecipeRepo
import de.healthforge.group.GroupRecipeEntity
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
    private val groupRecipeRepo: GroupRecipeRepo,
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
        groupId: UUID? = null,
        limit: Int,
        offset: Int,
    ): List<RecipeListItemDto> {
        val viewerGroupIds = groupService.groupIdsForUser(viewerId)
        val vf = when (scope) {
            BrowseScope.PUBLIC -> VisibilityFilter.PublicOnly
            BrowseScope.MINE -> VisibilityFilter.OwnOnly(viewerId)
            BrowseScope.PUBLIC_OR_MINE -> VisibilityFilter.PublicOrOwnOrGroup(viewerId, viewerGroupIds)
        }
        val ids = browse.browseIds(q, slotTags, prepMinutesMax, excludeAllergens, vf, authorId, groupId, limit, offset)
        if (ids.isEmpty()) return emptyList()
        val byId = recipeRepo.findAllById(ids).associateBy { it.id }
        // Pre-load ingredient rows for all recipes to compute per-100g nutrition
        val ingredientRowsById = ingredientRowRepo.findByRecipeIdIn(ids).groupBy { it.recipeId }
        return ids.mapNotNull { byId[it] }.map { r ->
            val summary = ingredientRowsById[r.id]?.let { nutritionCompute.computeSummary(it) }
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
                totalWeightGrams = summary?.totalWeightGrams,
                kcalPer100g = summary?.kcalPer100g,
                proteinPer100g = summary?.proteinPer100g,
                carbsPer100g = summary?.carbsPer100g,
                fatPer100g = summary?.fatPer100g,
                fiberPer100g = summary?.fiberPer100g,
            )
        }
    }

    @Transactional(readOnly = true)
    fun detail(id: UUID, viewerId: UUID): RecipeDetailDto {
        // Allow viewing own recipes even if PENDING_REVIEW/REJECTED (owner must see what they submitted)
        val r = recipeRepo.findById(id).orElse(null)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found")
        // Only published recipes are visible to non-owners (except admins)
        val isOwner = r.authorId == viewerId
        if (!isOwner && r.status != RecipeStatus.PUBLISHED.name) {
            throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found")
        }
        // Check PRIVATE visibility (owner-only)
        if (RecipeVisibility.valueOf(r.visibility) == RecipeVisibility.PRIVATE && !isOwner) {
            throw ApiException(HttpStatus.FORBIDDEN, "PRIVATE_RECIPE", "private recipe")
        }
        val items = ingredientRowRepo.findByRecipeIdOrderByPositionAsc(id)
        val steps = stepRowRepo.findByRecipeIdOrderByPositionAsc(id)
        val nutrition = nutritionCompute.compute(items)
        val myRating = ratingRepo.findByRecipeIdAndUserId(id, viewerId)
        // Load ingredient entities once for names + allergens
        val ingIds = items.map { it.ingredientId }.distinct()
        val ingById: Map<UUID, de.healthforge.ingredient.IngredientEntity> = if (ingIds.isEmpty()) emptyMap()
            else ingredientRepo.findAllById(ingIds).associateBy { it.id }
        val allAllergens = ingById.values.flatMap { parseJsonArray(it.allergensJson) }.distinct().sorted()
        val allFodmap = ingById.values.flatMap { parseJsonArray(it.fodmapFlagsJson) }.distinct().sorted()
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
            ingredients = items.map {
                RecipeIngredientDto(
                    it.position, it.ingredientId, ingById[it.ingredientId]?.nameDe,
                    it.quantity, it.unit, it.isOptional, it.note,
                )
            },
            steps = steps.map { RecipeStepDto(it.position, it.text, it.imageKey) },
            nutrition = nutrition,
            allergens = allAllergens,
            fodmapFlags = allFodmap,
            likeCount = likeRepo.countByRecipeId(id),
            likedByMe = likeRepo.existsByRecipeIdAndUserId(id, viewerId),
            communityRecommendCount = ratingRepo.countByRecipeIdAndValue(id, CommunityRatingValue.RECOMMEND.name),
            communityNotRecommendCount = ratingRepo.countByRecipeIdAndValue(id, CommunityRatingValue.NOT_RECOMMEND.name),
            myCommunityRating = myRating?.value?.let { runCatching { CommunityRatingValue.valueOf(it) }.getOrNull() },
        )
    }

    // ---------- Create / Update / Delete ----------

    @Transactional
    fun create(req: RecipeUpsertRequest, authorId: UUID, principalRole: String? = null): UUID {
        validate(req)
        ensureGroupMembership(req, authorId)
        val now = Instant.now()
        // Öffentliche Rezepte von Nicht-Admin-Usern brauchen Review
        val isPublic = req.visibility == RecipeVisibility.PUBLIC
        val isAdmin = principalRole == UserRole.ADMIN.name
        val status = if (isPublic && !isAdmin) RecipeStatus.PENDING_REVIEW.name else RecipeStatus.PUBLISHED.name
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
            status = status,
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

    /** Batch: Rezepte anhand von IDs als ListItemDtos zurückgeben (für Plan/Home). */
    @Transactional(readOnly = true)
    fun batchItems(ids: List<UUID>, viewerId: UUID): List<RecipeListItemDto> {
        if (ids.isEmpty()) return emptyList()
        val recipes = recipeRepo.findAllById(ids)
        val ingredientRowsById = ingredientRowRepo.findByRecipeIdIn(ids).groupBy { it.recipeId }
        return recipes.filter { it.status == RecipeStatus.PUBLISHED.name || it.authorId == viewerId }.map { r ->
            val summary = ingredientRowsById[r.id]?.let { nutritionCompute.computeSummary(it) }
            RecipeListItemDto(
                id = r.id, title = r.title, description = r.description,
                imageKey = r.imageKey, servings = r.servings, prepMinutes = r.prepMinutes,
                slotTags = r.slotTags.mapNotNull { runCatching { SlotTag.valueOf(it) }.getOrNull() },
                visibility = RecipeVisibility.valueOf(r.visibility), authorId = r.authorId,
                createdAt = r.createdAt,
                likeCount = likeRepo.countByRecipeId(r.id),
                communityRecommendCount = ratingRepo.countByRecipeIdAndValue(r.id, CommunityRatingValue.RECOMMEND.name),
                communityNotRecommendCount = ratingRepo.countByRecipeIdAndValue(r.id, CommunityRatingValue.NOT_RECOMMEND.name),
                totalWeightGrams = summary?.totalWeightGrams,
                kcalPer100g = summary?.kcalPer100g,
                proteinPer100g = summary?.proteinPer100g,
                carbsPer100g = summary?.carbsPer100g,
                fatPer100g = summary?.fatPer100g,
                fiberPer100g = summary?.fiberPer100g,
            )
        }
    }

    /** Weißt ein Rezept einer Gruppe zu (via group_recipes Join-Tabelle, V21).
     *  Erlaubt fuer: Rezept-Owner ODER Gruppen-Mitglieder mit OWNER/ADMIN/CONTRIBUTOR.
     *  Das Rezept behaelt seine visibility (PUBLIC/PRIVATE) bei. */
    @Transactional
    fun assignToGroup(recipeId: UUID, groupId: UUID, callerId: UUID) {
        val recipe = recipeRepo.findById(recipeId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $recipeId not found")
        }
        val isOwner = recipe.authorId == callerId
        val callerRole = groupService.getMemberRole(callerId, groupId)
        val canManage = callerRole in listOf("OWNER", "ADMIN", "CONTRIBUTOR")
        if (!isOwner && !canManage) {
            throw ApiException(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED",
                "must be recipe owner or group admin/contributor")
        }
        if (groupRecipeRepo.existsByGroupIdAndRecipeId(groupId, recipeId)) {
            return // already assigned, idempotent
        }
        groupRecipeRepo.save(GroupRecipeEntity(
            groupId = groupId,
            recipeId = recipeId,
            addedBy = callerId,
        ))
    }

    /** Entfernt ein Rezept aus einer Gruppe (via Join-Table). */
    @Transactional
    fun removeFromGroup(recipeId: UUID, groupId: UUID, callerId: UUID) {
        val callerRole = groupService.getMemberRole(callerId, groupId)
        val canManage = callerRole in listOf("OWNER", "ADMIN", "CONTRIBUTOR")
        val isOwner = recipeRepo.findById(recipeId).orElse(null)?.authorId == callerId
        if (!canManage && !isOwner) {
            throw ApiException(HttpStatus.FORBIDDEN, "NOT_AUTHORIZED",
                "must be group admin/contributor or recipe owner")
        }
        groupRecipeRepo.deleteByGroupIdAndRecipeId(groupId, recipeId)
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

    private fun parseJsonArray(json: String): List<String> =
        runCatching {
            com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List::class.java) as? List<String>
        }.getOrDefault(emptyList()).orEmpty()
}

enum class BrowseScope { PUBLIC, MINE, PUBLIC_OR_MINE }
