package de.healthforge.presentation.lebensmittel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientButton
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import kotlin.math.roundToInt

/**
 * REQ-INGREDIENT-CREATE-WIZARD-001 (P6.S5) — 4-Step Wizard zum Vorschlagen eines
 * neuen Lebensmittels. Ersetzt den ehemaligen `IngredientSuggestDialog`.
 *
 * Steps:
 * 1. Name + Marke + Barcode
 * 2. Nährwerte pro 100g (Slider kcal/Protein/Carbs/Fat + optional Sub-Nährstoffe)
 * 3. Allergene + Histamin-SIGHI + FODMAP-Flags (Chip-Multi-Select)
 * 4. Vorschau + Submit (Status PENDING)
 */
@Composable
fun IngredientSuggestWizardScreen(
    initialName: String = "",
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    vm: IngredientSuggestWizardViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    LaunchedEffect(initialName) { vm.init(initialName) }
    LaunchedEffect(s.done) { if (s.done) onSubmitted() }

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
                    text = "Lebensmittel vorschlagen",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            StepDotsRow(currentIndex = s.stepIndex, total = INGREDIENT_WIZARD_TOTAL_STEPS)
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (s.stepIndex) {
                    0 -> StepIdentity(s, vm)
                    1 -> StepNutrients(s, vm)
                    2 -> StepFlags(s, vm)
                    3 -> StepPreview(s)
                }
                s.submitError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
            }

            WizardNav(
                stepIndex = s.stepIndex,
                total = INGREDIENT_WIZARD_TOTAL_STEPS,
                nextEnabled = s.stepIndex != 0 || s.canAdvanceFromStep1,
                submitting = s.submitting,
                submitLabel = "Einreichen",
                onBack = vm::back,
                onNext = vm::next,
                onSubmit = vm::submit,
            )
            Spacer(Modifier.height(8.dp).navigationBarsPadding())
        }
    }
}

// ---------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------

@Composable
private fun StepIdentity(s: IngredientWizardState, vm: IngredientSuggestWizardViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Was möchtest du eintragen?", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Vorschläge sind nur für dich sichtbar, bis ein Admin sie freigibt.",
        color = hm.fgSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedTextField(
        value = s.name,
        onValueChange = vm::setName,
        label = { Text("Name (Deutsch) *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = s.brand,
        onValueChange = vm::setBrand,
        label = { Text("Marke (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = s.barcode,
        onValueChange = vm::setBarcode,
        label = { Text("Barcode (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepNutrients(s: IngredientWizardState, vm: IngredientSuggestWizardViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Nährwerte pro 100 g", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Schiebe die Regler. Genauere Werte kannst du später per Korrektur-Vorschlag verbessern.",
        color = hm.fgSecondary,
        style = MaterialTheme.typography.bodySmall,
    )
    SliderField("Kalorien", "${s.kcal.roundToInt()} kcal", s.kcal, 0f, 900f, vm::setKcal)
    SliderField("Protein", "${"%.1f".format(s.proteinG)} g", s.proteinG, 0f, 100f, vm::setProtein)
    SliderField("Kohlenhydrate", "${"%.1f".format(s.carbsG)} g", s.carbsG, 0f, 100f, vm::setCarbs)
    SliderField("Fett", "${"%.1f".format(s.fatG)} g", s.fatG, 0f, 100f, vm::setFat)
    OutlinedButton(onClick = vm::toggleAdvanced, modifier = Modifier.fillMaxWidth()) {
        Text(if (s.showAdvancedNutrients) "Weitere Felder ausblenden" else "Zucker, Ballaststoffe, Salz…")
    }
    if (s.showAdvancedNutrients) {
        OptionalSliderField("Zucker", s.sugarG, 0f, 100f, "g", vm::setSugar)
        OptionalSliderField("Ges. Fettsäuren", s.satfatG, 0f, 100f, "g", vm::setSatfat)
        OptionalSliderField("Ballaststoffe", s.fiberG, 0f, 30f, "g", vm::setFiber)
        OptionalSliderField("Salz", s.saltG, 0f, 10f, "g", vm::setSalt)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepFlags(s: IngredientWizardState, vm: IngredientSuggestWizardViewModel) {
    val hm = LocalHmTokens.current
    GradientText("Diäten & Histamin", style = MaterialTheme.typography.headlineSmall)

    Text("Histamin-Stufe (SIGHI)", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..3).forEach { score ->
            FilterChip(
                selected = s.histamineScore == score,
                onClick = { vm.setHistamine(if (s.histamineScore == score) null else score) },
                label = { Text(when (score) { 0 -> "0 — unbedenklich"; 1 -> "1 — niedrig"; 2 -> "2 — mittel"; else -> "3 — hoch" }) },
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("Allergene (EU-14)", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AllergenType.values().forEach { a ->
            FilterChip(
                selected = a in s.allergens,
                onClick = { vm.toggleAllergen(a) },
                label = { Text(a.germanLabel) },
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("FODMAP-Flags", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FodmapType.values().forEach { f ->
            FilterChip(
                selected = f in s.fodmap,
                onClick = { vm.toggleFodmap(f) },
                label = { Text(f.germanLabel) },
            )
        }
    }
}

@Composable
private fun StepPreview(s: IngredientWizardState) {
    val hm = LocalHmTokens.current
    GradientText("Vorschau", style = MaterialTheme.typography.headlineSmall)
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.name.ifBlank { "(kein Name)" }, fontWeight = FontWeight.Bold, color = hm.fgPrimary)
            if (s.brand.isNotBlank()) Text(s.brand, color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall)
            if (s.barcode.isNotBlank()) Text("Barcode: ${s.barcode}", color = hm.fgTertiary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("Pro 100 g:", color = hm.fgPrimary, fontWeight = FontWeight.SemiBold)
            Text("${s.kcal.roundToInt()} kcal — ${"%.1f".format(s.proteinG)} g P / ${"%.1f".format(s.carbsG)} g KH / ${"%.1f".format(s.fatG)} g F",
                color = hm.fgSecondary, style = MaterialTheme.typography.bodyMedium)
            s.histamineScore?.let {
                Text("Histamin: $it / 3", color = hm.fgSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            if (s.allergens.isNotEmpty()) Text(
                "Allergene: " + s.allergens.joinToString(", ") { it.germanLabel },
                color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall,
            )
            if (s.fodmap.isNotEmpty()) Text(
                "FODMAP: " + s.fodmap.joinToString(", ") { it.germanLabel },
                color = hm.fgSecondary, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nach \u201eEinreichen\u201c landet der Vorschlag in der Admin-Queue (Status PENDING) und ist " +
                    "zunächst nur für dich sichtbar.",
                color = hm.fgTertiary, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Shared wizard helpers
// ---------------------------------------------------------------------------

@Composable
internal fun StepDotsRow(currentIndex: Int, total: Int) {
    val hm = LocalHmTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val active = i <= currentIndex
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .let { if (active) it.width(20.dp) else it.size(8.dp) }
                    .clip(if (active) RoundedCornerShape(4.dp) else CircleShape)
                    .background(if (active) hm.accentGradient else SolidColor(hm.glassBorder)),
            )
        }
    }
}

@Composable
internal fun WizardNav(
    stepIndex: Int,
    total: Int,
    nextEnabled: Boolean,
    submitting: Boolean,
    submitLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (stepIndex > 0) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Zurück") }
        }
        if (stepIndex < total - 1) {
            GradientButton(
                text = "Weiter",
                onClick = onNext,
                enabled = nextEnabled,
                modifier = Modifier.weight(1f),
            )
        } else {
            GradientButton(
                text = if (submitting) "Wird gesendet…" else submitLabel,
                onClick = onSubmit,
                enabled = !submitting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    valueDisplay: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
) {
    val hm = LocalHmTokens.current
    Column {
        Row {
            Text(label, color = hm.fgPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(valueDisplay, color = hm.fgSecondary)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
private fun OptionalSliderField(
    label: String,
    value: Float?,
    min: Float,
    max: Float,
    unit: String,
    onChange: (Float?) -> Unit,
) {
    val hm = LocalHmTokens.current
    Column {
        Row {
            Text(label, color = hm.fgPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(value?.let { "${"%.1f".format(it)} $unit" } ?: "—", color = hm.fgSecondary)
        }
        Slider(
            value = value ?: min,
            onValueChange = { onChange(it) },
            valueRange = min..max,
        )
        if (value != null) {
            Text(
                "Tippen um zu entfernen",
                color = hm.fgTertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .padding(2.dp),
            )
        }
    }
}
