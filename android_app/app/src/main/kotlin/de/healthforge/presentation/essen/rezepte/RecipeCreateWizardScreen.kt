package de.healthforge.presentation.essen.rezepte

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.healthforge.data.repository.MediaRepository
import de.healthforge.presentation.common.PhotoSourceDialog
import de.healthforge.presentation.lebensmittel.StepDotsRow
import de.healthforge.presentation.lebensmittel.WizardNav
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import kotlin.math.roundToInt

/**
 * REQ-RECIPE-CREATE-WIZARD-001 (P6.S5) — 6-Step Wizard zum Erstellen eines Rezepts.
 *
 * Reutilises [RecipeEditViewModel] (create-mode → `id == null`). Forward-only mit
 * Validation pro Step. Schritte:
 * 0. Name + Foto (mit Vorschau)
 * 1. Mahlzeit wählen (Frühstück/Mittag/Abend/Snack) ← NEU
 * 2. Zutaten-Liste (Search aus ingredients + Mengen/Einheit pro Zutat)
 * 3. Portionen + Zubereitungszeit (Slider)
 * 4. Zubereitungstext (multiline, Schritt für Schritt empfohlen)
 * 5. Vorschau + Speichern (mit Foto)
 */
private const val RECIPE_WIZARD_STEPS = 6

@Composable
fun RecipeCreateWizardScreen(
    preselectedGroupId: String? = null,
    onBack: () -> Unit,
    onSaved: (id: String) -> Unit,
    vm: RecipeEditViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }
    var cameraPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Vorausgewählte Gruppe setzen (wenn aus Gruppen-Detail aufgerufen)
    LaunchedEffect(preselectedGroupId) {
        if (!preselectedGroupId.isNullOrBlank()) {
            vm.setVisibility("GROUP")
            vm.setGroupId(preselectedGroupId)
        }
    }

    // Galerie-Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { vm.pickImage(ctx, it) }
    }
    // Kamera-Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            cameraPhotoUri?.let { uri -> vm.pickImage(ctx, uri) }
        } else {
            vm.setError("Kamera wurde abgebrochen oder Foto konnte nicht gespeichert werden")
        }
    }
    // Kamera-Runtime-Permission (Android 6+)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingCameraUri?.let { uri ->
                cameraPhotoUri = uri
                try {
                    cameraLauncher.launch(uri)
                } catch (e: android.content.ActivityNotFoundException) {
                    vm.setError("Keine Kamera-App gefunden")
                } catch (e: Exception) {
                    vm.setError("Kamera konnte nicht gestartet werden: ${e.message}")
                }
            }
        } else {
            vm.setError("Kamerazugriff verweigert – bitte Berechtigung in den Einstellungen erlauben")
        }
        pendingCameraUri = null
    }

    LaunchedEffect(s.savedId) { s.savedId?.let { onSaved(it) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(hm.background),
    ) {
        AmbientBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück", tint = hm.fgPrimary)
                }
                Spacer(Modifier.width(4.dp))
                GradientText(
                    text = "Rezept erstellen",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            StepDotsRow(currentIndex = stepIndex, total = RECIPE_WIZARD_STEPS)
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (stepIndex) {
                    0 -> StepName(s, vm, onPickImage = { showPhotoDialog = true })
                    1 -> StepSlots(s, vm)
                    2 -> StepIngredients(s, vm)
                    3 -> StepPortionsTime(s, vm)
                    4 -> StepInstructions(s, vm)
                    5 -> StepRecipePreview(s)
                }
                s.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
            }

            val canAdvance = when (stepIndex) {
                0 -> s.title.trim().isNotEmpty()
                1 -> s.slotTags.isNotEmpty()
                2 -> s.ingredients.isNotEmpty()
                3 -> s.prepMinutes.isNotBlank()
                4 -> true // Zubereitungstext optional
                else -> true
            }
            WizardNav(
                stepIndex = stepIndex,
                total = RECIPE_WIZARD_STEPS,
                nextEnabled = canAdvance,
                submitting = s.isSaving,
                submitLabel = "Speichern",
                onBack = { if (stepIndex > 0) stepIndex -= 1 },
                onNext = { if (canAdvance && stepIndex < RECIPE_WIZARD_STEPS - 1) stepIndex += 1 },
                onSubmit = vm::save,
            )
            Spacer(Modifier.height(8.dp).navigationBarsPadding())
        }
    }

    // Foto-Quellen-Dialog (Galerie oder Kamera)
    if (showPhotoDialog) {
        PhotoSourceDialog(
            onGalleryClick = {
                showPhotoDialog = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCameraClick = { uri ->
                showPhotoDialog = false
                pendingCameraUri = uri
                if (android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        ctx, android.Manifest.permission.CAMERA
                    )
                ) {
                    cameraPhotoUri = uri
                    try {
                        cameraLauncher.launch(uri)
                    } catch (e: android.content.ActivityNotFoundException) {
                        vm.setError("Keine Kamera-App gefunden")
                    } catch (e: Exception) {
                        vm.setError("Kamera konnte nicht gestartet werden: ${e.message}")
                    }
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            onDismiss = { showPhotoDialog = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------

@Composable
private fun StepName(s: RecipeEditUiState, vm: RecipeEditViewModel, onPickImage: () -> Unit) {
    val hm = LocalHmTokens.current
    GradientText("Wie heißt dein Rezept?", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(
        value = s.title,
        onValueChange = vm::setTitle,
        label = { Text("Name *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Photo, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (s.imageKey.isNotBlank()) "Foto ausgewählt — ersetzen" else "Foto auswählen (Pflicht)")
    }
    // Foto-Vorschau
    val imgUrl = MediaRepository.imageUrl("recipes", s.imageKey.takeIf { it.isNotBlank() }, variant = "medium")
    if (imgUrl != null) {
        AsyncImage(
            model = imgUrl,
            contentDescription = "Rezeptfoto",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else if (s.imageKey.isNotBlank()) {
        // Falls imageKey keine URL ist, trotzdem anzeigen
        Text("📷 Bild hochgeladen (Key: ${s.imageKey})",
            color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall)
    } else {
        Text("Bitte wähle ein Foto aus", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
    }
}

// ===== NEU: Schritt 1 – Mahlzeit wählen =====

@Composable
private fun StepSlots(s: RecipeEditUiState, vm: RecipeEditViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Für welche Mahlzeit?", style = MaterialTheme.typography.headlineSmall)
    Text("Wähle mindestens eine Mahlzeit aus.", color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall)

    val allSlots = listOf(
        "BREAKFAST" to "🌅 Frühstück",
        "LUNCH" to "☀️ Mittagessen",
        "DINNER" to "🌙 Abendessen",
        "SNACK" to "🍿 Snack",
    )

    Spacer(Modifier.height(8.dp))
    allSlots.forEach { (key, label) ->
        val selected = key in s.slotTags
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { vm.toggleSlot(key) },
            padding = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = selected,
                    onCheckedChange = { vm.toggleSlot(key) },
                )
                Spacer(Modifier.width(8.dp))
                Text(label, color = hm.fgPrimary, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
    if (s.slotTags.isEmpty()) {
        Text("Bitte wähle mindestens eine Mahlzeit", color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StepIngredients(s: RecipeEditUiState, vm: RecipeEditViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Was kommt rein?", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Such ein Lebensmittel, tippe drauf um es hinzuzufügen, dann passe die Menge an.",
        color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall,
    )
    OutlinedTextField(
        value = s.ingredientSearchQuery,
        onValueChange = vm::setIngredientQuery,
        label = { Text("Lebensmittel suchen…") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (s.ingredientSuggestions.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(s.ingredientSuggestions) { ing ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { vm.addIngredient(ing) },
                    padding = PaddingValues(12.dp),
                ) {
                    Column {
                        Text(ing.name_de, color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
                        ing.energy_kcal_per_100g?.let {
                            Text("${it.toInt()} kcal / 100 g", color = hm.fgSecondary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    Text("Hinzugefügt (${s.ingredients.size})", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
    s.ingredients.forEachIndexed { idx, line ->
        GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(10.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(line.name, modifier = Modifier.weight(1f), color = hm.fgPrimary,
                        fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { vm.removeIngredient(idx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Entfernen", tint = hm.fgSecondary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = line.quantity,
                        onValueChange = { vm.updateIngredientQuantity(idx, it) },
                        label = { Text("Menge") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = line.unit,
                        onValueChange = { vm.updateIngredientUnit(idx, it) },
                        label = { Text("Einheit") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun StepPortionsTime(s: RecipeEditUiState, vm: RecipeEditViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Portionen & Zeit", style = MaterialTheme.typography.headlineSmall)
    Column {
        Row {
            Text("Portionen", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text("${s.servings}", color = hm.fgSecondary)
        }
        Slider(
            value = s.servings.toFloat(),
            onValueChange = { vm.setServings(it.roundToInt()) },
            valueRange = 1f..20f,
            steps = 18,
        )
    }
    val prepMin = s.prepMinutes.toIntOrNull() ?: 30
    Column {
        Row {
            Text("Zubereitungszeit", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text("$prepMin min", color = hm.fgSecondary)
        }
        Slider(
            value = prepMin.toFloat(),
            onValueChange = { vm.setPrep((it.roundToInt() / 5 * 5).toString()) },
            valueRange = 0f..240f,
        )
    }
    val cookMin = s.cookMinutes.toIntOrNull()
    Column {
        Row {
            Text("Kochzeit (optional)", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text(cookMin?.let { "$it min" } ?: "—", color = hm.fgSecondary)
        }
        Slider(
            value = (cookMin ?: 0).toFloat(),
            onValueChange = { vm.setCook((it.roundToInt() / 5 * 5).toString()) },
            valueRange = 0f..240f,
        )
    }
}

@Composable
private fun StepInstructions(s: RecipeEditUiState, vm: RecipeEditViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Zubereitung", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Schritt für Schritt empfohlen. Du kannst weitere Schritte hinzufügen.",
        color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall,
    )
    s.steps.forEachIndexed { idx, line ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(hm.accentGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text("${idx + 1}", color = hm.fgPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = line.text,
                onValueChange = { vm.updateStep(idx, it) },
                label = { Text("Schritt ${idx + 1}") },
                modifier = Modifier.weight(1f),
            )
            if (s.steps.size > 1) {
                IconButton(onClick = { vm.removeStep(idx) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Entfernen", tint = hm.fgSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    OutlinedButton(onClick = vm::addStep, modifier = Modifier.fillMaxWidth()) {
        Text("+ Weiterer Schritt")
    }
}

@Composable
private fun StepRecipePreview(s: RecipeEditUiState) {
    val hm = LocalHmTokens.current
    GradientText("Vorschau", style = MaterialTheme.typography.headlineSmall)

    // Foto
    val imgUrl = MediaRepository.imageUrl("recipes", s.imageKey.takeIf { it.isNotBlank() }, variant = "medium")
    if (imgUrl != null) {
        AsyncImage(
            model = imgUrl,
            contentDescription = "Rezeptfoto",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(8.dp))
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.title.ifBlank { "(kein Name)" }, color = hm.fgPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)

            // Slot-Tags
            if (s.slotTags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.slotTags.sorted().forEach { tag ->
                        val label = when (tag) {
                            "BREAKFAST" -> "🌅 Frühstück"
                            "LUNCH" -> "☀️ Mittag"
                            "DINNER" -> "🌙 Abend"
                            "SNACK" -> "🍿 Snack"
                            else -> tag
                        }
                        androidx.compose.material3.SuggestionChip(
                            onClick = {},
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            Row {
                Text("${s.servings} Portion(en)", color = hm.fgSecondary,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    listOfNotNull(
                        s.prepMinutes.takeIf { it.isNotBlank() }?.let { "$it min Zubereitung" },
                        s.cookMinutes.takeIf { it.isNotBlank() }?.let { "$it min Kochen" },
                    ).joinToString(" • "),
                    color = hm.fgSecondary, style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (s.ingredients.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Zutaten", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
                s.ingredients.forEach { ing ->
                    Row {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SolidColor(hm.ambientCyan)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${ing.quantity} ${ing.unit} ${ing.name}", color = hm.fgSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            val realSteps = s.steps.filter { it.text.isNotBlank() }
            if (realSteps.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Zubereitung", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
                realSteps.forEachIndexed { i, step ->
                    Text("${i + 1}. ${step.text}", color = hm.fgSecondary,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
