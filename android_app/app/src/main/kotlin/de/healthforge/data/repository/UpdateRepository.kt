package de.healthforge.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import de.healthforge.BuildConfig
import de.healthforge.data.network.LatestReleaseDto
import de.healthforge.data.network.ReleaseApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles checking for APK updates, downloading, and installing.
 *
 * Flow:
 *  1. [checkForUpdate] → calls GET /v1/releases/latest → compares version numbers
 *  2. If newer → [downloadAndInstall] → download APK directly → install immediately
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val api: ReleaseApi,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Checks the server for the latest release.
     * @return [LatestReleaseDto] if a newer version exists, null if up-to-date.
     */
    suspend fun checkForUpdate(): Result<LatestReleaseDto?> = runCatching {
        val latest = api.latest()
        val currentVersion = stripSuffix(BuildConfig.VERSION_NAME)
        val latestVersion = stripSuffix(latest.version)
        if (compareVersions(latestVersion, currentVersion) > 0) latest else null
    }

    /**
     * Downloads the APK directly and immediately opens the Package Installer.
     *
     * @return the APK file (already saved to cache)
     */
    fun downloadAndInstall(ctx: Context, release: LatestReleaseDto): Result<File> = runCatching {
        val downloadUrl = release.downloadUrl
            ?: throw IllegalStateException("No downloadUrl in release $release.version")

        // Download to cache dir (camera/ subdir already registered in file_paths.xml)
        val updateDir = File(ctx.cacheDir, "camera")
        updateDir.mkdirs()
        val apkFile = File(updateDir, "HealthForge-${release.version}.apk")

        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw RuntimeException("Download failed: HTTP ${response.code}")

        response.body?.byteStream()?.use { input ->
            FileOutputStream(apkFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw RuntimeException("Empty response body")

        // Install immediately
        installApk(ctx, apkFile)
        apkFile
    }

    /**
     * Opens the Package Installer for the given APK file.
     */
    private fun installApk(ctx: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            apkFile,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        ctx.startActivity(intent)
    }

    /**
     * Strips non-numeric suffix from each version segment.
     * "0.1.0-debug" → "0.1.0"
     * "0.1.0-ci.30.abc" → "0.1.0.30"
     * @return version string with only numeric segments, for direct comparison.
     */
    private fun stripSuffix(v: String): String {
        return v.split(".")
            .map { seg -> seg.takeWhile { c -> c.isDigit() } }
            .filter { it.isNotBlank() }
            .joinToString(".")
    }

    /**
     * Compares two version strings (numeric segments only).
     * "0.1.0" == "0.1.0"
     * "0.1.0.30" > "0.1.0.29"
     * "1.0.0" > "0.9.9"
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }
}
