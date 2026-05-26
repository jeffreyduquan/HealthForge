package de.healthforge.etl

import de.healthforge.auth.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class EtlRunDto(
    val id: UUID,
    val source: EtlSource,
    val status: EtlStatus,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val rowsInserted: Int,
    val rowsUpdated: Int,
    val rowsSkipped: Int,
    val errorMessage: String?,
) {
    companion object {
        fun from(e: EtlRunEntity) = EtlRunDto(
            id = e.id, source = e.source, status = e.status,
            startedAt = e.startedAt, finishedAt = e.finishedAt,
            rowsInserted = e.rowsInserted, rowsUpdated = e.rowsUpdated,
            rowsSkipped = e.rowsSkipped, errorMessage = e.errorMessage,
        )
    }
}

@RestController
@RequestMapping("/admin/v1/etl")
class EtlController(
    private val orchestrator: EtlOrchestrator,
    private val runs: EtlRunRepository,
    private val users: UserRepository,
) {

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    fun trigger(
        @RequestParam("source") source: EtlSource,
        @AuthenticationPrincipal principal: User?,
    ): ResponseEntity<EtlRunDto> {
        val triggeredBy = principal?.username?.let { email ->
            users.findByEmail(email).orElse(null)?.id
        }
        val run = orchestrator.run(source, triggeredBy)
        return ResponseEntity.ok(EtlRunDto.from(run))
    }

    @GetMapping("/runs/{source}")
    @PreAuthorize("hasRole('ADMIN')")
    fun history(@PathVariable source: EtlSource): List<EtlRunDto> =
        runs.findTop20BySourceOrderByStartedAtDesc(source).map(EtlRunDto::from)
}
