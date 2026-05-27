package de.healthforge.etl

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

enum class EtlSource { BLS, SIGHI, OFF, USDA_FDC }

enum class EtlStatus { RUNNING, SUCCESS, FAILED, SKIPPED_NO_FILE }

@Entity
@Table(name = "etl_runs")
class EtlRunEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    var source: EtlSource,

    @Column(name = "started_at", nullable = false, updatable = false)
    var startedAt: Instant = Instant.now(),

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: EtlStatus = EtlStatus.RUNNING,

    @Column(name = "rows_inserted", nullable = false)
    var rowsInserted: Int = 0,

    @Column(name = "rows_updated", nullable = false)
    var rowsUpdated: Int = 0,

    @Column(name = "rows_skipped", nullable = false)
    var rowsSkipped: Int = 0,

    @Column(name = "error_message")
    var errorMessage: String? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "triggered_by", columnDefinition = "uuid")
    var triggeredBy: UUID? = null,
)

interface EtlRunRepository : JpaRepository<EtlRunEntity, UUID> {
    fun findTop20BySourceOrderByStartedAtDesc(source: EtlSource): List<EtlRunEntity>
}
