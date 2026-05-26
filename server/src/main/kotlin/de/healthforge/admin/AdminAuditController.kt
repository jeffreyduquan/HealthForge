package de.healthforge.admin

import com.fasterxml.jackson.annotation.JsonProperty
import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ActorKind
import de.healthforge.common.ApiException
import de.healthforge.common.AuditLogEntity
import de.healthforge.common.AuditLogRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID
import kotlin.math.min

/**
 * Admin Audit-Log read endpoint. REQ-ADMIN-FULL-001 / REQ-AUDIT-001 — P4.S5.
 *
 * Filter:
 * - `actor` — partial match auf `actor_user_id` (UUID-Präfix als String) ODER `actorKind` (USER/ADMIN/SYSTEM).
 * - `action` — exact match.
 * - `from`/`to` — Instant ISO-8601 (z.B. `2026-05-01T00:00:00Z`).
 * - `limit` — 1..500 (Default 100).
 *
 * Read-only via JPA Criteria-API (vermeidet pageable-Komplexität).
 */
@RestController
@RequestMapping("/admin/v1/audit")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditController(
    @Suppress("unused") private val repo: AuditLogRepository, // ensure bean wiring
    @PersistenceContext private val em: EntityManager,
) {
    private fun require(p: AuthPrincipal?): AuthPrincipal =
        p ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam(required = false) actor: String?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false, defaultValue = "100") limit: Int,
    ): List<AuditLogDto> {
        require(principal)
        val cb = em.criteriaBuilder
        val cq = cb.createQuery(AuditLogEntity::class.java)
        val root = cq.from(AuditLogEntity::class.java)
        val predicates = mutableListOf<Predicate>()

        if (!actor.isNullOrBlank()) {
            val upper = actor.trim().uppercase()
            val kindMatch = runCatching { ActorKind.valueOf(upper) }.getOrNull()
            if (kindMatch != null) {
                predicates += cb.equal(root.get<ActorKind>("actorKind"), kindMatch)
            } else {
                // Treat as UUID prefix; tolerate non-UUID by string compare.
                val uuid = runCatching { UUID.fromString(actor.trim()) }.getOrNull()
                if (uuid != null) {
                    predicates += cb.equal(root.get<UUID>("actorUserId"), uuid)
                } else {
                    // No-op if cannot interpret: return empty result by an always-false predicate.
                    predicates += cb.equal(cb.literal(1), 0)
                }
            }
        }
        if (!action.isNullOrBlank()) {
            predicates += cb.equal(root.get<String>("action"), action.trim())
        }
        if (!from.isNullOrBlank()) {
            val ts = runCatching { Instant.parse(from) }.getOrNull()
                ?: throw ApiException(HttpStatus.BAD_REQUEST, "BAD_FROM", "from must be ISO-8601 instant")
            predicates += cb.greaterThanOrEqualTo(root.get("occurredAt"), ts)
        }
        if (!to.isNullOrBlank()) {
            val ts = runCatching { Instant.parse(to) }.getOrNull()
                ?: throw ApiException(HttpStatus.BAD_REQUEST, "BAD_TO", "to must be ISO-8601 instant")
            predicates += cb.lessThan(root.get("occurredAt"), ts)
        }

        if (predicates.isNotEmpty()) cq.where(*predicates.toTypedArray())
        cq.orderBy(cb.desc(root.get<Instant>("occurredAt")))

        val effectiveLimit = limit.coerceIn(1, 500)
        val rows = em.createQuery(cq).setMaxResults(effectiveLimit).resultList
        return rows.map { it.toDto() }
    }

    private fun AuditLogEntity.toDto() = AuditLogDto(
        id = this.id ?: 0L,
        occurredAt = this.occurredAt.toString(),
        actorUserId = this.actorUserId?.toString(),
        actorKind = this.actorKind.name,
        action = this.action,
        targetType = this.targetType,
        targetId = this.targetId,
        ipAddress = this.ipAddress,
        detail = this.detail?.let { it.substring(0, min(it.length, 2_000)) },
    )
}

data class AuditLogDto(
    val id: Long,
    @JsonProperty("occurred_at") val occurredAt: String,
    @JsonProperty("actor_user_id") val actorUserId: String?,
    @JsonProperty("actor_kind") val actorKind: String,
    val action: String,
    @JsonProperty("target_type") val targetType: String?,
    @JsonProperty("target_id") val targetId: String?,
    @JsonProperty("ip_address") val ipAddress: String?,
    val detail: String?,
)
