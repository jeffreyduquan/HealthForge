package de.healthforge.supplement

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

/** Admin queue für Supplement-Vorschläge (REQ-SUPP-004 / REQ-ADMIN-FULL-001). */
@RestController
@RequestMapping("/admin/v1/supplements")
@PreAuthorize("hasRole('ADMIN')")
class AdminSupplementController(
    private val service: SupplementService,
) {
    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping("/suggestions")
    fun list(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("onlyPending", required = false, defaultValue = "true") onlyPending: Boolean,
    ): List<SupplementSuggestionAdminDto> {
        require(principal)
        return service.listSuggestions(onlyPending)
    }

    @PostMapping("/suggestions/{id}/approve")
    fun approve(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, UUID>> {
        val p = require(principal)
        val publicId = service.approve(id, p.userId)
        return ResponseEntity.ok(mapOf("public_id" to publicId))
    }

    @PostMapping("/suggestions/{id}/reject")
    fun reject(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @Valid @RequestBody(required = false) body: RejectRequest?,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.reject(id, p.userId, body?.note)
        return ResponseEntity.noContent().build()
    }
}
