package de.healthforge.ingredient

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * P4.S1 — Admin-Review für User-eingereichte Ingredients + Field-PRs.
 * Beide Endpoints sind `ROLE_ADMIN`-gated (REQ-FIELDPR-003, REQ-INGR-USER-001).
 */
@RestController
@RequestMapping("/admin/v1/ingredients")
@PreAuthorize("hasRole('ADMIN')")
class AdminIngredientReviewController(
    private val service: IngredientSubmissionService,
) {
    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping("/queue")
    fun queue(): List<IngredientQueueEntryDto> = service.listPendingIngredients()

    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.approveIngredient(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: UUID,
        @Valid @RequestBody(required = false) body: RejectReviewRequest?,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.rejectIngredient(id, p.userId, body?.note)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/field-prs")
    fun fieldPrs(
        @RequestParam("onlyPending", required = false, defaultValue = "true") onlyPending: Boolean,
    ): List<FieldPrAdminDto> = service.listFieldPrs(onlyPending)

    @PostMapping("/field-prs/{id}/approve")
    fun approveFieldPr(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.approveFieldPr(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/field-prs/{id}/reject")
    fun rejectFieldPr(
        @PathVariable id: UUID,
        @Valid @RequestBody(required = false) body: RejectReviewRequest?,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.rejectFieldPr(id, p.userId, body?.note)
        return ResponseEntity.noContent().build()
    }
}
