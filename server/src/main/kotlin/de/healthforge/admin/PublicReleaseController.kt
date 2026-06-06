package de.healthforge.admin

import de.healthforge.auth.InviteRepository
import de.healthforge.common.ApiException
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    @Value("\${healthforge.minio.public-base-url}") private val publicBaseUrl: String,
) {
    private val bucket = "releases"

    /**
     * Ersetzt den internen MinIO-Host (z.B. minio:9000) in einer Presigned-URL
     * durch den öffentlich erreichbaren CDN-Host.
     */
    private fun fixMinioUrl(rawUrl: String): String {
        return rawUrl.replace(Regex("https?://[^/]+"), publicBaseUrl.trimEnd('/'))
    }

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

    @GetMapping("/{id}")
    fun sharePage(
        @PathVariable id: UUID,
        @RequestParam("code") inviteCode: String,
    ): ResponseEntity<String> {
        val release = releaseRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RELEASE_NOT_FOUND", "Release nicht gefunden")
        }
        val invite = inviteRepo.findByCode(inviteCode).orElse(null)
        val now = Instant.now()

        val status: String
        val downloadUrl: String
        val expiresAt: String

        if (invite == null) {
            status = "invalid"
            downloadUrl = ""
            expiresAt = ""
        } else if (invite.expiresAt.isBefore(now)) {
            status = "expired"
            downloadUrl = ""
            expiresAt = formatDateTime(invite.expiresAt)
        } else if (invite.downloadUsed) {
            status = "used"
            downloadUrl = ""
            expiresAt = formatDateTime(invite.expiresAt)
        } else {
            status = "valid"
            downloadUrl = "/v1/releases/${release.id}/download?code=${inviteCode}"
            expiresAt = formatDateTime(invite.expiresAt)
        }

        val sizeStr = formatFileSize(release.fileSize)
        val html = buildSharePage(
            release = release,
            sizeStr = sizeStr,
            status = status,
            downloadUrl = downloadUrl,
            expiresAt = expiresAt,
        )
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }

    @GetMapping("/{id}/download")
    fun download(
        @PathVariable id: UUID,
        @RequestParam("code") inviteCode: String,
    ): ResponseEntity<Void> {
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
        val internalUrl = minio.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .`object`(release.minioKey)
                .method(Method.GET)
                .expiry(3600)
                .build()
        )
        val url = fixMinioUrl(internalUrl)
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, url)
            .build()
    }

    private fun formatDateTime(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Berlin"))
            .withLocale(Locale.GERMAN)
        return formatter.format(instant)
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }

    private fun buildSharePage(
        release: ApkRelease,
        sizeStr: String,
        status: String,
        downloadUrl: String,
        expiresAt: String,
    ): String = """
<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>HealthForge · ${release.version}</title>
<style>
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f0f2f5; color: #1a1a2e; min-height: 100vh;
    display: flex; align-items: center; justify-content: center; padding: 16px;
  }
  .card {
    background: #fff; border-radius: 20px; box-shadow: 0 4px 24px rgba(0,0,0,0.10);
    max-width: 420px; width: 100%; padding: 32px 24px; text-align: center;
  }
  .icon {
    width: 72px; height: 72px; background: #e8f0fe; border-radius: 18px;
    display: flex; align-items: center; justify-content: center;
    margin: 0 auto 16px; font-size: 36px;
  }
  h1 { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
  .version { color: #4a5568; font-size: 14px; margin-bottom: 20px; }
  .meta { display: flex; justify-content: center; gap: 24px; margin-bottom: 24px; }
  .meta-item { text-align: center; }
  .meta-item .label { font-size: 11px; color: #a0aec0; text-transform: uppercase; letter-spacing: .5px; }
  .meta-item .value { font-size: 15px; font-weight: 600; margin-top: 2px; }
  .changelog {
    background: #f7fafc; border-radius: 12px; padding: 16px;
    text-align: left; font-size: 14px; line-height: 1.6; margin-bottom: 24px;
    color: #2d3748; max-height: 200px; overflow-y: auto; white-space: pre-wrap;
  }
  .changelog:empty { display: none; }
  .btn {
    display: block; width: 100%; padding: 16px; border: none; border-radius: 12px;
    font-size: 17px; font-weight: 600; cursor: pointer; text-decoration: none;
    transition: background .15s, opacity .15s;
  }
  .btn-primary { background: #2563eb; color: #fff; }
  .btn-primary:hover { background: #1d4ed8; }
  .btn-primary:active { background: #1e40af; }
  .btn-disabled { background: #cbd5e1; color: #94a3b8; cursor: not-allowed; }
  .status-badge {
    display: inline-block; padding: 6px 14px; border-radius: 20px;
    font-size: 13px; font-weight: 600; margin-bottom: 16px;
  }
  .status-valid { background: #dcfce7; color: #166534; }
  .status-used { background: #fef3c7; color: #92400e; }
  .status-expired { background: #fee2e2; color: #991b1b; }
  .footer { margin-top: 20px; font-size: 12px; color: #a0aec0; }
  a { color: #2563eb; text-decoration: none; }
</style>
</head>
<body>
<div class="card">
  <div class="icon">📦</div>
  <h1>${escapeHtml(release.filename)}</h1>
  <div class="version">Version ${escapeHtml(release.version)}</div>

  ${when(status) {
    "valid" -> ""
    "used" -> """<div class="status-badge status-used">✓ Bereits heruntergeladen</div>"""
    "expired" -> """<div class="status-badge status-expired">✗ Download-Link abgelaufen</div>"""
    else -> """<div class="status-badge status-expired">✗ Ungültiger Download-Link</div>"""
  }}

  <div class="meta">
    <div class="meta-item"><div class="label">Größe</div><div class="value">${sizeStr}</div></div>
    <div class="meta-item"><div class="label">Gültig bis</div><div class="value">${expiresAt}</div></div>
  </div>

  ${if (release.changelog != null) """<div class="changelog">${escapeHtml(release.changelog)}</div>""" else ""}

  ${if (status == "valid") """
    <a href="${downloadUrl}" class="btn btn-primary" id="downloadBtn">⬇ APK herunterladen</a>
  """ else """
    <button class="btn btn-disabled" disabled>${when(status) {
      "used" -> "✓ Bereits heruntergeladen"
      "expired" -> "✗ Abgelaufen"
      else -> "✗ Ungültig"
    }}</button>
  """}

  <div class="footer">
    Bereitgestellt von <a href="http://admin.healthforge.endgear.de:8080">HealthForge</a>
  </div>
</div>
</body>
</html>
""".trimIndent()

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
