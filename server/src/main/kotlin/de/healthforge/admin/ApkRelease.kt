package de.healthforge.admin

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "apk_releases")
class ApkRelease(
    @Id @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val version: String,

    @Column(columnDefinition = "TEXT")
    val changelog: String? = null,

    @Column(nullable = false)
    val filename: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Column(name = "minio_key", nullable = false)
    val minioKey: String,

    @Column(name = "uploaded_by")
    val uploadedBy: UUID? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
