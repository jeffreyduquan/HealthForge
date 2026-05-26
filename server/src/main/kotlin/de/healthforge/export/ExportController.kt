package de.healthforge.export

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * REQ-EXPORT-001..004 — Server-Side Datenexport.
 *
 *  - `GET /v1/export/full?format=json` → application/json mit `ServerExportPayload`
 *  - `GET /v1/export/full?format=pdf`  → application/pdf, attachment.
 *
 * Lokale Android-Daten (Intake/Water/Symptom/Reminder) werden client-side separat
 * exportiert; per Drift-Entscheidung Slice 3 wird kein Combined-PDF auf dem
 * Server gebaut, damit der Server keine lokale Domäne kennen muss.
 */
@RestController
@RequestMapping("/v1/export")
class ExportController(
    private val service: ExportService,
) {
    @GetMapping("/full")
    fun export(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam(name = "format", required = false, defaultValue = "json") format: String,
    ): ResponseEntity<ByteArray> {
        val p = principal ?: throw ApiException(
            HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required",
        )
        val payload = service.buildPayload(p.userId)
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
            .withZone(ZoneId.of("Europe/Berlin"))
            .format(Instant.now())
        return when (format.lowercase()) {
            "json" -> {
                val bytes = service.toJson(payload)
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"healthforge-export-$stamp.json\"",
                    )
                    .body(bytes)
            }
            "pdf" -> {
                val bytes = service.toPdf(payload)
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"healthforge-export-$stamp.pdf\"",
                    )
                    .body(bytes)
            }
            else -> throw ApiException(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_FORMAT",
                "format must be 'json' or 'pdf'",
            )
        }
    }
}
