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
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
<meta name="theme-color" content="#0f172a"/>
<title>HealthForge · ${release.version}</title>
<style>
  *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
  body{
    font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
    background:#0f172a;color:#e2e8f0;min-height:100dvh;
    display:flex;align-items:center;justify-content:center;
    padding:16px;
    -webkit-font-smoothing:antialiased
  }
  .card{
    background:linear-gradient(180deg,#1e293b 0%,#0f172a 100%);
    border:1px solid rgba(255,255,255,.06);
    border-radius:32px;max-width:400px;width:100%;
    padding:0;overflow:hidden;position:relative
  }
  .card-header{
    background:linear-gradient(135deg,#1e40af 0%,#312e81 50%,#581c87 100%);
    padding:40px 24px 32px;text-align:center;position:relative
  }
  .card-header::after{
    content:'';position:absolute;bottom:-24px;left:0;right:0;
    height:48px;background:linear-gradient(180deg,#1e293b00 0%,#1e293b 100%)
  }
  .app-icon{
    width:80px;height:80px;margin:0 auto 16px;
    background:rgba(255,255,255,.15);backdrop-filter:blur(8px);
    border-radius:22px;display:flex;align-items:center;justify-content:center;
    font-size:40px;border:1px solid rgba(255,255,255,.1)
  }
  .app-name{font-size:13px;font-weight:600;color:rgba(255,255,255,.6);text-transform:uppercase;letter-spacing:1.5px;margin-bottom:4px}
  .app-version{font-size:24px;font-weight:700;color:#fff;margin-bottom:4px}
  .app-filename{font-size:14px;color:rgba(255,255,255,.5);word-break:break-all}
  .card-body{padding:16px 20px 24px}
  .status-row{text-align:center;margin-bottom:16px}
  .badge{
    display:inline-flex;align-items:center;gap:6px;
    padding:8px 16px;border-radius:100px;font-size:13px;font-weight:600
  }
  .badge-valid{background:rgba(34,197,94,.15);color:#4ade80;border:1px solid rgba(34,197,94,.25)}
  .badge-used{background:rgba(234,179,8,.12);color:#facc15;border:1px solid rgba(234,179,8,.2)}
  .badge-expired,.badge-invalid{background:rgba(239,68,68,.12);color:#f87171;border:1px solid rgba(239,68,68,.2)}
  .meta-grid{
    display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:16px
  }
  .meta-item{
    background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.06);
    border-radius:14px;padding:14px;text-align:center
  }
  .meta-label{font-size:11px;color:rgba(255,255,255,.4);text-transform:uppercase;letter-spacing:.5px;margin-bottom:4px}
  .meta-value{font-size:16px;font-weight:600;color:#e2e8f0}
  .changelog{
    background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.06);
    border-radius:14px;padding:14px;margin-bottom:16px;
    font-size:13px;line-height:1.6;color:rgba(255,255,255,.7);
    max-height:160px;overflow-y:auto;white-space:pre-wrap
  }
  .changelog:empty{display:none}
  .changelog::-webkit-scrollbar{width:4px}
  .changelog::-webkit-scrollbar-thumb{background:rgba(255,255,255,.1);border-radius:2px}
  .btn{
    display:flex;align-items:center;justify-content:center;gap:10px;
    width:100%;padding:18px;border:none;border-radius:16px;
    font-size:17px;font-weight:600;cursor:pointer;text-decoration:none;
    transition:transform .12s,box-shadow .12s
  }
  .btn:active{transform:scale(.97)}
  .btn-primary{
    background:linear-gradient(135deg,#3b82f6,#6366f1);
    color:#fff;box-shadow:0 4px 20px rgba(59,130,246,.35)
  }
  .btn-primary:hover{box-shadow:0 6px 28px rgba(59,130,246,.5)}
  .btn-disabled{
    background:rgba(255,255,255,.06);color:rgba(255,255,255,.25);
    cursor:not-allowed;border:1px solid rgba(255,255,255,.06)
  }
  .footer{
    text-align:center;padding:16px 20px 20px;
    border-top:1px solid rgba(255,255,255,.06);
    font-size:12px;color:rgba(255,255,255,.25)
  }
  .footer a{color:rgba(255,255,255,.4);text-decoration:none}
  .spinner{width:20px;height:20px;border:2px solid rgba(255,255,255,.3);border-top-color:#fff;border-radius:50%;animation:spin .6s linear infinite;display:none}
  @keyframes spin{to{transform:rotate(360deg)}}
  .btn-loading .spinner{display:block}
  .btn-loading .btn-text{display:none}
</style>
</head>
<body>
<div class="card">
  <div class="card-header">
    <div class="app-icon">📦</div>
    <div class="app-name">HealthForge</div>
    <div class="app-version">${escapeHtml(release.version)}</div>
    <div class="app-filename">${escapeHtml(release.filename)}</div>
  </div>
  <div class="card-body">
    <div class="status-row">
      ${when(status) {
        "valid" -> """<span class="badge badge-valid">● Bereit zum Download</span>"""
        "used" -> """<span class="badge badge-used">✓ Bereits heruntergeladen</span>"""
        "expired" -> """<span class="badge badge-expired">✗ Link abgelaufen</span>"""
        else -> """<span class="badge badge-invalid">✗ Ungültiger Link</span>"""
      }}
    </div>

    <div class="meta-grid">
      <div class="meta-item">
        <div class="meta-label">Größe</div>
        <div class="meta-value">${sizeStr}</div>
      </div>
      <div class="meta-item">
        <div class="meta-label">Gültig bis</div>
        <div class="meta-value">${expiresAt}</div>
      </div>
    </div>

    ${if (release.changelog != null) """<div class="changelog">${escapeHtml(release.changelog)}</div>""" else ""}

    ${if (status == "valid") """
    <a href="${downloadUrl}" class="btn btn-primary" id="downloadBtn" onclick="this.classList.add('btn-loading')">
      <span class="spinner"></span>
      <span class="btn-text">⬇ APK herunterladen</span>
    </a>
    """ else """
    <button class="btn btn-disabled" disabled>
      ${when(status) {
        "used" -> "✓ Bereits heruntergeladen"
        "expired" -> "✗ Abgelaufen"
        else -> "✗ Ungültig"
      }}
    </button>
    """}
  </div>
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
