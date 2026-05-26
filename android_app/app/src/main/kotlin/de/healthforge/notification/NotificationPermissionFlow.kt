package de.healthforge.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Requests POST_NOTIFICATIONS at first use of a reminder feature on API 33+.
 * Pre-Tiramisu: no-op (permission auto-granted). REQ-REMIND-004.
 *
 * @param trigger flip to `true` to launch the request. The composable resets behaviour
 *   based on the current permission state; caller manages its own boolean.
 */
@Composable
fun RequestNotificationPermissionEffect(
    trigger: Boolean,
    onResult: (granted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true)
            return@LaunchedEffect
        }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) onResult(true) else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
