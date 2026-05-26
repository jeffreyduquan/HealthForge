package de.healthforge.community

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** User-facing endpoint to report a recipe to the moderation queue. REQ-GROUP-007. */
@RestController
@RequestMapping("/v1/recipes")
class RecipeReportController(
    private val service: ReportService,
) {
    @PostMapping("/{id}/reports")
    fun report(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @Valid @RequestBody req: CreateReportRequest,
    ): ResponseEntity<Map<String, UUID>> {
        val p = principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")
        val reportId = service.createReport(id, p.userId, req.reason)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to reportId))
    }
}
