package de.healthforge.presentation.lebensmittel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.healthforge.data.network.FieldPrRequest
import de.healthforge.data.network.IngredientSuggestRequest

/**
 * REQ-INGR-USER-001 — Dialog zum Vorschlagen eines neuen Lebensmittels.
 */
@Composable
fun IngredientSuggestDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onSubmit: (IngredientSuggestRequest) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var brand by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    val canSubmit = name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    onSubmit(
                        IngredientSuggestRequest(
                            name_de = name.trim(),
                            brand = brand.trim().ifBlank { null },
                            energy_kcal_per_100g = kcal.toDoubleOrNull(),
                            protein_g_per_100g = protein.toDoubleOrNull(),
                            carbs_g_per_100g = carbs.toDoubleOrNull(),
                            fat_g_per_100g = fat.toDoubleOrNull(),
                        ),
                    )
                },
            ) { Text("Einreichen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Lebensmittel vorschlagen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Vorschläge sind nur für dich sichtbar, bis ein Admin sie freigibt.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(name, { name = it }, label = { Text("Name (DE) *") }, singleLine = true)
                OutlinedTextField(brand, { brand = it }, label = { Text("Marke") }, singleLine = true)
                OutlinedTextField(kcal, { kcal = it }, label = { Text("kcal/100g") }, singleLine = true)
                OutlinedTextField(protein, { protein = it }, label = { Text("Protein g/100g") }, singleLine = true)
                OutlinedTextField(carbs, { carbs = it }, label = { Text("Kohlenhydrate g/100g") }, singleLine = true)
                OutlinedTextField(fat, { fat = it }, label = { Text("Fett g/100g") }, singleLine = true)
            }
        },
    )
}

private val FIELD_PR_OPTIONS = listOf(
    "histamine_score" to "Histamin-Score (0–3)",
    "energy_kcal_per_100g" to "kcal pro 100 g",
    "protein_g_per_100g" to "Protein g/100g",
    "carbs_g_per_100g" to "Kohlenhydrate g/100g",
    "sugar_g_per_100g" to "Zucker g/100g",
    "fat_g_per_100g" to "Fett g/100g",
    "satfat_g_per_100g" to "Ges. Fett g/100g",
    "fiber_g_per_100g" to "Ballaststoffe g/100g",
    "salt_g_per_100g" to "Salz g/100g",
    "allergens_json" to "Allergene-Liste (JSON-Array)",
    "fodmap_flags_json" to "FODMAP-Flags (JSON-Array)",
)

/**
 * REQ-FIELDPR-001 — Dialog zum Vorschlagen einer Einzelfeld-Korrektur.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldPrDialog(
    ingredientId: String,
    ingredientName: String,
    onDismiss: () -> Unit,
    onSubmit: (String, FieldPrRequest) -> Unit,
) {
    var fieldName by remember { mutableStateOf(FIELD_PR_OPTIONS.first().first) }
    var newValue by remember { mutableStateOf("") }
    var rationale by remember { mutableStateOf("") }
    val canSubmit = newValue.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    onSubmit(
                        ingredientId,
                        FieldPrRequest(
                            field_name = fieldName,
                            new_value = newValue.trim(),
                            rationale = rationale.trim().ifBlank { null },
                        ),
                    )
                },
            ) { Text("Einreichen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Korrektur vorschlagen") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Für: $ingredientName",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                Text("Feld auswählen:", style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FIELD_PR_OPTIONS.forEach { (key, label) ->
                        FilterChip(
                            selected = fieldName == key,
                            onClick = { fieldName = key },
                            label = { Text(label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    label = { Text("Neuer Wert *") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = rationale,
                    onValueChange = { rationale = it },
                    label = { Text("Begründung (optional)") },
                )
                Text(
                    "Der angezeigte Wert ändert sich erst nach Admin-Freigabe.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}
