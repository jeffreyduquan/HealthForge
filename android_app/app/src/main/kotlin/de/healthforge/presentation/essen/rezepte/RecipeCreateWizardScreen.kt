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
import de.healthforge.presentation.lebensmittel.StepDotsRow
import de.healthforge.presentation.lebensmittel.WizardNav
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import kotlin.math.roundToInt

/**
 * REQ-RECIPE-CREATE-WIZARD-001 (P6.S5) — 5-Step Wizard zum Erstellen eines Rezepts.
 *
 * Reutilises [RecipeEditViewModel] (create-mode → `id == null`). Forward-only mit
 * Validation pro Step. Schritte:
 * 1. Name + optional Foto
 * 2. Zutaten-Liste (Search aus ingredients + Mengen/Einheit pro Zutat)
 * 3. Portionen + Zubereitungszeit (Slider)
 * 4. Zubereitungstext (multiline, Schritt für Schritt empfohlen)
 * 5. Vorschau + Speichern
 */
private const val RECIPE_WIZARD_STEPS = 5

@Composable
fun RecipeCreateWizardScreen(
    onBack: () -> Unit,
    onSaved: (id: String) -> Unit,
    vm: RecipeEditViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { vm.pickImage(ctx, it) }
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
                    0 -> StepName(s, vm, onPickImage = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    1 -> StepIngredients(s, vm)
                    2 -> StepPortionsTime(s, vm)
                    3 -> StepInstructions(s, vm)
                    4 -> StepRecipePreview(s)
                }
                s.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
            }

            val canAdvance = when (stepIndex) {
                0 -> s.title.trim().isNotEmpty()
                1 -> s.ingredients.isNotEmpty()
                2 -> s.prepMinutes.isNotBlank()
                3 -> true // Zubereitungstext optional
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
        Text(if (s.imageKey != null) "Foto ausgewählt — ersetzen" else "Foto auswählen (optional)")
    }
    if (s.imageKey != null) {
        Text("Bild-Key: ${s.imageKey}", color = hm.fgTertiary, style = MaterialTheme.typography.bodySmall)
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
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.title.ifBlank { "(kein Name)" }, color = hm.fgPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge)
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
