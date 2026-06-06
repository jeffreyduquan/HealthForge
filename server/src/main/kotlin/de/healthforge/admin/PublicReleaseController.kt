package de.healthforge.admin

import de.healthforge.auth.InviteRepository
import de.healthforge.common.ApiException
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

/**
 * Öffentlicher APK-Download via gültigem Invite-Code (einmalig).
 * User mit Einladungscode können die APK genau einmal herunterladen.
 */
@RestController
@RequestMapping("/v1/releases")
class PublicReleaseController(
    private val releaseRepo: ApkReleaseRepo,
    private val inviteRepo: InviteRepository,
    private val minio: MinioClient,
) {
    private val bucket = "releases"

    @GetMapping("/latest")
    fun latest(): Map<String, Any?> {
        val latest = releaseRepo.findFirstByOrderByCreatedAtDesc()
            ?: throw ApiException(HttpStatus.NOT_FOUND, "NO_RELEASES", "Keine Releases vorhanden")
        return mapOf(
            "id" to latest.id,
            "version" to latest.version,
            "filename" to latest.filename,
            "fileSize" to latest.fileSize,
            "changelog" to latest.changelog,
            "createdAt" to latest.createdAt.toString(),
        )
    }

    @GetMapping("/{id}/download")
    fun download(
        @PathVariable id: UUID,
        @RequestParam("code") inviteCode: String,
    ): Map<String, String> {
        // Release prüfen
        val release = releaseRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RELEASE_NOT_FOUND", "Release nicht gefunden")
        }
        // Invite-Code prüfen
        val invite = inviteRepo.findByCode(inviteCode).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "Ungültiger Einladungscode")
        }
        if (invite.expiresAt.isBefore(Instant.now())) {
            throw ApiException(HttpStatus.GONE, "INVITE_EXPIRED", "Einladungscode ist abgelaufen")
        }
        if (invite.downloadUsed) {
            throw ApiException(HttpStatus.CONFLICT, "DOWNLOAD_ALREADY_USED", "Download bereits genutzt")
        }
        // Einmaligen Download markieren
        invite.downloadUsed = true
        invite.downloadUsedAt = Instant.now()
        inviteRepo.save(invite)

        // Presigned URL generieren (1h gültig)
        val url = minio.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .`object`(release.minioKey)
                .method(Method.GET)
                .expiry(3600)
                .build()
        )
        return mapOf("url" to url, "filename" to release.filename)
    }
}
