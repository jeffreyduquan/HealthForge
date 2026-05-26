package de.healthforge.common

import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class ActorKind { USER, ADMIN, SYSTEM }

@Entity
@Table(name = "audit_log")
class AuditLogEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.now(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "actor_user_id", columnDefinition = "uuid")
    var actorUserId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_kind", nullable = false)
    var actorKind: ActorKind = ActorKind.SYSTEM,

    @Column(nullable = false)
    var action: String = "",

    @Column(name = "target_type")
    var targetType: String? = null,

    @Column(name = "target_id")
    var targetId: String? = null,

    @Column(name = "ip_address")
    var ipAddress: String? = null,

    @Column(columnDefinition = "text")
    var detail: String? = null, // serialized JSON (stored as TEXT)
)

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "DELETE FROM AuditLogEntity a WHERE a.occurredAt < :cutoff",
    )
    fun deleteOlderThan(cutoff: Instant): Int
}

@Service
class AuditLogService(private val repo: AuditLogRepository) {
    fun record(
        action: String,
        actorUserId: UUID? = null,
        actorKind: ActorKind = if (actorUserId != null) ActorKind.USER else ActorKind.SYSTEM,
        targetType: String? = null,
        targetId: String? = null,
        ipAddress: String? = null,
        detail: String? = null,
    ) {
        repo.save(
            AuditLogEntity(
                action = action,
                actorUserId = actorUserId,
                actorKind = actorKind,
                targetType = targetType,
                targetId = targetId,
                ipAddress = ipAddress,
                detail = detail,
            ),
        )
    }
}

/** LOCKED Q11: 90-day rolling retention. Runs daily at 04:00. */
@Component
class AuditCleanupJob(
    private val repo: AuditLogRepository,
    private val auditService: AuditLogService,
) {
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    fun cleanup() {
        val cutoff = Instant.now().minusSeconds(90L * 86_400)
        val deleted = repo.deleteOlderThan(cutoff)
        auditService.record("AUDIT_CLEANUP", actorKind = ActorKind.SYSTEM, detail = "{\"deleted\":$deleted}")
    }
}
