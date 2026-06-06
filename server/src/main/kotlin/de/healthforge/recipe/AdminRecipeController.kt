package de.healthforge.recipe

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import de.healthforge.common.AuditLogService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * Admin Recipe Review Queue (P7.S4).
 * Admins können auf PUBLIC-PENDING_REVIEW-Rezepte approve/reject.
 */
@RestController
@RequestMapping("/admin/v1/recipes")
@PreAuthorize("hasRole('ADMIN')")
class AdminRecipeController(
    private val recipeRepo: RecipeRepo,
    private val auditService: AuditLogService,
) {
    private fun require(p: AuthPrincipal?): AuthPrincipal =
        p ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping("/queue")
    fun queue(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("onlyPending", required = false, defaultValue = "true") onlyPending: Boolean,
    ): List<RecipeQueueEntryDto> {
        require(principal)
        val statuses = if (onlyPending) listOf(RecipeStatus.PENDING_REVIEW.name)
            else listOf(RecipeStatus.PENDING_REVIEW.name, RecipeStatus.PUBLISHED.name, RecipeStatus.REJECTED.name)
        return recipeRepo.findAllByStatusInOrderByCreatedAtAsc(statuses)
            .map { it.toQueueEntryDto() }
    }

    @PostMapping("/{id}/approve")
    @Transactional
    fun approve(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        val recipe = recipeRepo.findByIdAndStatus(id, RecipeStatus.PENDING_REVIEW.name)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found or not pending")
        recipe.status = RecipeStatus.PUBLISHED.name
        recipe.updatedAt = Instant.now()
        recipeRepo.save(recipe)
        auditService.record(
            action = "RECIPE_APPROVE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "RECIPE",
            targetId = id.toString(),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/reject")
    @Transactional
    fun reject(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @RequestBody(required = false) body: RejectRecipeRequest?,
    ): ResponseEntity<Void> {
        val p = require(principal)
        val recipe = recipeRepo.findByIdAndStatus(id, RecipeStatus.PENDING_REVIEW.name)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $id not found or not pending")
        recipe.status = RecipeStatus.REJECTED.name
        recipe.updatedAt = Instant.now()
        recipeRepo.save(recipe)
        auditService.record(
            action = "RECIPE_REJECT",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "RECIPE",
            targetId = id.toString(),
            detail = body?.note,
        )
        return ResponseEntity.noContent().build()
    }
}

data class RecipeQueueEntryDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val status: String,
    val visibility: String,
    val authorId: UUID,
    val slotTags: List<String>,
    val createdAt: String,
)

data class RejectRecipeRequest(
    val note: String? = null,
)

private fun RecipeEntity.toQueueEntryDto() = RecipeQueueEntryDto(
    id = id,
    title = title,
    description = description,
    status = status,
    visibility = visibility,
    authorId = authorId,
    slotTags = slotTags.toList(),
    createdAt = createdAt.toString(),
)
