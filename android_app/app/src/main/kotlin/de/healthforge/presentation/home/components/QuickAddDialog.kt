package de.healthforge.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import de.healthforge.data.network.IngredientDto

/**
 * QuickAddDialog: tippe Such-Query, wähle Treffer, gib Portion (g) ein, bestätige.
 * REQ-HOME-003 / REQ-INTAKE-001.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialog(
    query: String,
    results: List<IngredientDto>,
    portionGrams: String,
    selected: IngredientDto?,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (IngredientDto) -> Unit,
    onClearSelection: () -> Unit,
    onPortionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lebensmittel hinzuf\u00fcgen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Suche") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (loading) {
                    Text("Suche \u2026", style = MaterialTheme.typography.bodySmall)
                }
                if (selected == null) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(results, key = { it.id }) { ing ->
                            Card(
                                onClick = { onSelect(ing) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                                    Text(
                                        ing.name_de,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    val brand = ing.brand
                                    val kcal = ing.energy_kcal_per_100g
                                    val subtitle = buildString {
                                        if (!brand.isNullOrBlank()) append(brand)
                                        if (kcal != null) {
                                            if (isNotEmpty()) append(" \u00b7 ")
                                            append("${kcal.toInt()} kcal/100g")
                                        }
                                    }
                                    if (subtitle.isNotBlank()) {
                                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Gew\u00e4hlt: ${selected.name_de}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onClearSelection) {
                        Text("Andere ausw\u00e4hlen")
                    }
                    OutlinedTextField(
                        value = portionGrams,
                        onValueChange = onPortionChange,
                        label = { Text("Portion (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selected != null && portionGrams.toDoubleOrNull()?.let { it > 0 } == true,
            ) { Text("Hinzuf\u00fcgen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
