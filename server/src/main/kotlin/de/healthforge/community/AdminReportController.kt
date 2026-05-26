package de.healthforge.community

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Admin moderation queue. REQ-GROUP-007 + REQ-ADMIN-FULL-001. */
@RestController
@RequestMapping("/admin/v1")
@PreAuthorize("hasRole('ADMIN')")
class AdminReportController(
    private val service: ReportService,
) {
    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping("/reports")
    fun list(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("onlyOpen", required = false, defaultValue = "true") onlyOpen: Boolean,
    ): List<ReportAdminDto> {
        require(principal)
        return service.listReports(onlyOpen)
    }

    @PostMapping("/reports/{id}/resolve")
    fun resolve(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.resolve(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reports/{id}/dismiss")
    fun dismiss(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.dismiss(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/recipes/{id}")
    fun deleteRecipe(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.adminRemoveRecipe(id, p.userId)
        return ResponseEntity.noContent().build()
    }
}
