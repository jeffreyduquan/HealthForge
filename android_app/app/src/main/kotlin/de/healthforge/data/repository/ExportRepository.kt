package de.healthforge.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.healthforge.data.network.ExportApi
import de.healthforge.domain.usecase.BuildLocalExportUseCase
import okhttp3.Headers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ExportApi,
    private val localExport: BuildLocalExportUseCase,
) {

    /** REQ-EXPORT-001/-002/-003/-004 — Server-Side JSON-Export. */
    suspend fun downloadServerJson(): Result<Uri> = downloadServer("json", "application/json", "json")

    /** REQ-EXPORT-001/-002/-003/-004 — Server-Side PDF-Export. */
    suspend fun downloadServerPdf(): Result<Uri> = downloadServer("pdf", "application/pdf", "pdf")

    /** REQ-EXPORT-001/-003/-004 — Lokaler Datenbank-Export (Privacy-by-Design). */
    suspend fun exportLocalJson(): Result<Uri> = runCatching {
        val bytes = localExport()
        val fileName = "healthforge-local-${timestamp()}.json"
        writeToDownloads(fileName, "application/json", bytes)
    }

    private suspend fun downloadServer(
        format: String,
        mime: String,
        ext: String,
    ): Result<Uri> = runCatching {
        val response = api.downloadFullExport(format)
        if (!response.isSuccessful) {
            error("HTTP ${response.code()}")
        }
        val body = response.body() ?: error("empty response body")
        val bytes = body.use { it.bytes() }
        val fileName = parseFilenameFromHeaders(response.headers())
            ?: "healthforge-export-${timestamp()}.$ext"
        writeToDownloads(fileName, mime, bytes)
    }

    private fun parseFilenameFromHeaders(headers: Headers): String? {
        val disposition = headers["Content-Disposition"] ?: return null
        val match = Regex("filename=\"?([^\"]+)\"?").find(disposition) ?: return null
        return match.groupValues[1]
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.GERMANY).format(Date())

    /**
     * Schreibt nach `Downloads/HealthForge/` via MediaStore (Android 10+)
     * oder app-internem External-Files-Dir als Fallback.
     */
    private fun writeToDownloads(fileName: String, mime: String, bytes: ByteArray): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HealthForge")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore.insert returned null")
            resolver.openOutputStream(uri).use { out ->
                requireNotNull(out) { "MediaStore.openOutputStream returned null" }
                out.write(bytes)
                out.flush()
            }
            uri
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "HealthForge",
            ).apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            Uri.fromFile(file)
        }
    }
}
