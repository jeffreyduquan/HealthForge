package de.healthforge.admin

import de.healthforge.auth.AuthPrincipal
import de.healthforge.auth.InviteEntity
import de.healthforge.auth.InviteRepository
import de.healthforge.common.ApiException
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.beans.factory.annotation.Value
import java.io.ByteArrayInputStream
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/admin/v1/releases")
@PreAuthorize("hasRole('ADMIN')")
class AdminReleaseController(
    private val repo: ApkReleaseRepo,
    private val inviteRepo: InviteRepository,
    private val minio: MinioClient,
    @Value("\${healthforge.minio.public-base-url}") private val publicBaseUrl: String,
    @Value("\${healthforge.api.public-url}") private val apiPublicUrl: String,
) {
    private val bucket = "releases"
    private val secureRandom = SecureRandom()
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Ersetzt den internen MinIO-Host (z.B. minio:9000) in einer Presigned-URL
     * durch den öffentlich erreichbaren CDN-Host.
     */
    private fun fixMinioUrl(rawUrl: String): String {
        return rawUrl.replace(Regex("https?://[^/]+"), publicBaseUrl.trimEnd('/'))
    }

    @GetMapping
    fun list(): List<ApkReleaseDto> =
        repo.findAllByOrderByCreatedAtDesc().map { it.toDto() }

    @PostMapping
    @Transactional
    fun upload(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("version") version: String,
        @RequestParam("changelog") changelog: String?,
    ): ApkReleaseDto {
        val admin = principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")
        if (file.isEmpty) throw ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "file is empty")
        if (!file.originalFilename.orEmpty().endsWith(".apk")) {
            throw ApiException(HttpStatus.BAD_REQUEST, "NOT_AN_APK", "nur .apk-Dateien erlaubt")
        }

        val minioKey = "v${version}/${file.originalFilename}"
        val bytes = file.bytes

        ByteArrayInputStream(bytes).use { input ->
            minio.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(minioKey)
                    .stream(input, bytes.size.toLong(), -1)
                    .contentType("application/vnd.android.package-archive")
                    .build()
            )
        }

        val release = repo.save(ApkRelease(
            version = version,
            changelog = changelog?.takeIf { it.isNotBlank() },
            filename = file.originalFilename ?: "app-release.apk",
            fileSize = bytes.size.toLong(),
            minioKey = minioKey,
            uploadedBy = admin.userId,
        ))
        return release.toDto()
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val release = repo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RELEASE_NOT_FOUND", "Release nicht gefunden")
        }
        runCatching {
            minio.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(release.minioKey)
                    .build()
            )
        }
        repo.delete(release)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/download")
    fun downloadUrl(@PathVariable id: UUID): ResponseEntity<Void> {
        val release = repo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RELEASE_NOT_FOUND", "Release nicht gefunden")
        }
        val internalUrl = minio.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .`object`(release.minioKey)
                .method(Method.GET)
                .expiry(3600)
                .build()
        )
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, fixMinioUrl(internalUrl))
            .build()
    }

    /** Generates a one-time download link for a release (used instead of invite codes). */
    @PostMapping("/{id}/download-link")
    @Transactional
    fun generateDownloadLink(@PathVariable id: UUID): Map<String, String> {
        val release = repo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RELEASE_NOT_FOUND", "Release nicht gefunden")
        }
        val code = (1..12).map { alphabet[secureRandom.nextInt(alphabet.length)] }.joinToString("")
        val invite = InviteEntity(
            code = code,
            expiresAt = Instant.now().plusSeconds(7 * 86_400), // 7 days gültig
            downloadUsed = false,
        )
        inviteRepo.save(invite)
        val downloadUrl = "${apiPublicUrl}/v1/releases/${release.id}/download?code=${code}"
        return mapOf("code" to code, "url" to downloadUrl, "filename" to release.filename)
    }
}

data class ApkReleaseDto(
    val id: UUID,
    val version: String,
    val changelog: String?,
    val filename: String,
    val fileSize: Long,
    val uploadedBy: UUID?,
    val createdAt: String,
)

private fun ApkRelease.toDto() = ApkReleaseDto(
    id = id,
    version = version,
    changelog = changelog,
    filename = filename,
    fileSize = fileSize,
    uploadedBy = uploadedBy,
    createdAt = createdAt.toString(),
)
