package de.healthforge.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import de.healthforge.BuildConfig
import de.healthforge.data.network.LatestReleaseDto
import de.healthforge.data.network.ReleaseApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles checking for APK updates and triggering the download.
 *
 * Flow:
 *  1. [checkForUpdate] → calls GET /v1/releases/latest → compares with BuildConfig.VERSION_NAME
 *  2. If newer → [downloadAndInstall] → DownloadManager → Notification → Intent.ACTION_VIEW
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val api: ReleaseApi,
) {

    /**
     * Checks the server for the latest release.
     * @return [LatestReleaseDto] if a newer version exists, null if up-to-date.
     */
    suspend fun checkForUpdate(): Result<LatestReleaseDto?> = runCatching {
        val latest = api.latest()
        val currentVersion = BuildConfig.VERSION_NAME
        // Simple version comparison (string compare works for semver-like "1.0.0" < "1.0.1")
        if (compareVersions(latest.version, currentVersion) > 0) latest else null
    }

    /**
     * Triggers Android's DownloadManager to download the APK.
     * After download completes (handled by a BroadcastReceiver), the APK can be installed.
     *
     * @return downloadId from DownloadManager (can be used to observe progress)
     */
    fun downloadAndInstall(ctx: Context, release: LatestReleaseDto): Long {
        val downloadUrl = release.downloadUrl
            ?: throw IllegalStateException("No downloadUrl in release $release.version")
        val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("HealthForge ${release.version}")
            .setDescription("Update wird heruntergeladen…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "HealthForge-${release.version}.apk",
            )
            .setMimeType("application/vnd.android.package-archive")

        return downloadManager.enqueue(request)
    }

    /**
     * Sends an Intent to open the Package Installer for the downloaded APK.
     * Call this after the DownloadManager reports success.
     */
    fun installApk(ctx: Context, downloadId: Long): Boolean {
        val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = downloadManager.getUriForDownloadedFile(downloadId) ?: return false

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Android 14+ requires explicit user consent for installs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startActivity(intent)
        } else {
            @Suppress("DEPRECATION")
            ctx.startActivity(intent)
        }
        return true
    }

    /**
     * Simple semantic version comparison (e.g. "1.2.3" > "1.2.2").
     * Supports up to 3 segments.
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
