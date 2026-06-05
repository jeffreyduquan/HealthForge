package de.healthforge.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Prüft ob eine Kamera-App auf dem Gerät verfügbar ist.
 */
fun isCameraAvailable(context: Context): Boolean {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    return intent.resolveActivity(context.packageManager) != null
}

/**
 * Helfer zum Erstellen einer temp. Camera-URI via FileProvider.
 * Das Foto landen in [context.cacheDir]/camera/.
 */
fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera")
    dir.mkdirs()
    val file = File(dir, "recipe_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

/**
 * Dialog zur Auswahl der Foto-Quelle: Galerie oder Kamera.
 *
 * @param onGalleryClick Wird aufgerufen wenn "Galerie" gewählt wird
 * @param onCameraClick  Wird aufgerufen wenn "Kamera" gewählt wird (erhält die temp. Camera-URI)
 * @param onDismiss      Wird bei Abbruch aufgerufen
 */
@Composable
fun PhotoSourceDialog(
    onGalleryClick: () -> Unit,
    onCameraClick: (Uri) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val cameraAvailable = remember { isCameraAvailable(ctx) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Foto-Quelle wählen") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("Aus Galerie auswählen")
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        val uri = createCameraUri(ctx)
                        onCameraClick(uri)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cameraAvailable,
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text(if (cameraAvailable) "Foto aufnehmen" else "Kamera nicht verfügbar")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

/**
 * Erstellt zwei Launcher (Galerie + Kamera) und einen Dialog-State für die Foto-Quelle.
 * Nützlich für Screens, die Fotos unterstützen.
 *
 * Usage:
 * ```
 * val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
 * val photoPicker = rememberPhotoPicker(
 *     onImagePicked = { uri -> vm.pickImage(ctx, uri) }
 * )
 *
 * // Button:
 * OutlinedButton(onClick = { setShowDialog(true) }) { ... }
 *
 * // Dialog:
 * if (showDialog) photoPicker.Dialog(onDismiss = { setShowDialog(false) })
 * ```
 */
@Composable
fun rememberPhotoPicker(
    onImagePicked: (Uri) -> Unit,
): PhotoPickerHandles {
    val ctx = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImagePicked(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        // URI wird vorab via createCameraUri erstellt und als input-Parameter übergeben
    }

    // Camera-URI muss vor dem Launch gespeichert werden, weil TakePicture()
    // den input-Parameter `uri` direkt aus dem launch()-Aufruf nimmt.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    return PhotoPickerHandles(
        launchGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        launchCamera = { uri ->
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        },
        onCameraResult = { success ->
            if (success && pendingCameraUri != null) {
                onImagePicked(pendingCameraUri!!)
            }
            pendingCameraUri = null
        },
    )
}

class PhotoPickerHandles(
    val launchGallery: () -> Unit,
    val launchCamera: (Uri) -> Unit,
    val onCameraResult: (Boolean) -> Unit,
)
