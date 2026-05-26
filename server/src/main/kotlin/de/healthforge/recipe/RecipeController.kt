package de.healthforge.recipe

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/recipes")
class RecipeController(
    private val service: RecipeService,
) {

    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping
    fun browse(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("q", required = false) q: String? = null,
        @RequestParam("slot", required = false) slot: List<String>? = null,
        @RequestParam("prepMax", required = false) prepMax: Int? = null,
        @RequestParam("excludeAllergens", required = false) excludeAllergens: List<String>? = null,
        @RequestParam("scope", required = false, defaultValue = "PUBLIC_OR_MINE") scope: BrowseScope,
        @RequestParam("author", required = false) author: UUID? = null,
        @RequestParam("limit", required = false, defaultValue = "20") limit: Int,
        @RequestParam("offset", required = false, defaultValue = "0") offset: Int,
    ): List<RecipeListItemDto> {
        val p = require(principal)
        val tags = slot?.mapNotNull { runCatching { SlotTag.valueOf(it.uppercase()) }.getOrNull() }.orEmpty()
        return service.browse(
            q = q,
            slotTags = tags,
            prepMinutesMax = prepMax,
            excludeAllergens = excludeAllergens.orEmpty(),
            scope = scope,
            viewerId = p.userId,
            authorId = author,
            limit = limit,
            offset = offset,
        )
    }

    @GetMapping("/{id}")
    fun detail(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): RecipeDetailDto {
        val p = require(principal)
        return service.detail(id, p.userId)
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @Valid @RequestBody req: RecipeUpsertRequest,
    ): ResponseEntity<Map<String, UUID>> {
        val p = require(principal)
        val id = service.create(req, p.userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to id))
    }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @Valid @RequestBody req: RecipeUpsertRequest,
    ): RecipeDetailDto {
        val p = require(principal)
        service.update(id, req, p.userId)
        return service.detail(id, p.userId)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.softDelete(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/like")
    fun like(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.like(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/like")
    fun unlike(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.unlike(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}/community-rating")
    fun upsertCommunityRating(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @Valid @RequestBody req: CommunityRatingRequest,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.upsertCommunityRating(id, p.userId, req.value)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/community-rating")
    fun revokeCommunityRating(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.revokeCommunityRating(id, p.userId)
        return ResponseEntity.noContent().build()
    }
}
