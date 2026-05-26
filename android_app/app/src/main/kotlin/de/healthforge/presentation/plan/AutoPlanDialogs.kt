package de.healthforge.presentation.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import de.healthforge.data.network.AutoPlanGenerateRequest
import de.healthforge.data.network.AutoPlanGenerateResponse
import java.time.LocalDate

private val ALL_SLOTS = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
private val SLOT_LABEL_AUTO = mapOf(
    "BREAKFAST" to "Frühstück",
    "LUNCH" to "Mittag",
    "DINNER" to "Abend",
    "SNACK" to "Snack",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlanGenerateDialog(
    onDismiss: () -> Unit,
    onSubmit: (AutoPlanGenerateRequest) -> Unit,
) {
    var days by remember { mutableStateOf("7") }
    var prepMax by remember { mutableStateOf("") }
    val selectedSlots = remember { mutableStateOf(setOf("BREAKFAST", "LUNCH", "DINNER")) }
    var allergens by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plan generieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Lässt den Server einen 1-Klick-Wochenplan vorschlagen. Vor dem Übernehmen kannst du noch Slots entfernen.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = days, onValueChange = { days = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Anzahl Tage (1–14)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prepMax, onValueChange = { prepMax = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Max. Zubereitungszeit (Min, optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Text("Mahlzeiten pro Tag:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALL_SLOTS.forEach { slot ->
                        FilterChip(
                            selected = slot in selectedSlots.value,
                            onClick = {
                                selectedSlots.value = if (slot in selectedSlots.value)
                                    selectedSlots.value - slot
                                else
                                    selectedSlots.value + slot
                            },
                            label = { Text(SLOT_LABEL_AUTO[slot] ?: slot) },
                        )
                    }
                }
                OutlinedTextField(
                    value = allergens, onValueChange = { allergens = it.uppercase() },
                    label = { Text("Allergene ausschließen (Komma-Liste, z.B. GLUTEN,LACTOSE)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedSlots.value.isNotEmpty() && (days.toIntOrNull() ?: 0) in 1..14,
                onClick = {
                    val req = AutoPlanGenerateRequest(
                        start_date = LocalDate.now().toString(),
                        days = days.toIntOrNull() ?: 7,
                        slots = selectedSlots.value.toList().sortedBy { ALL_SLOTS.indexOf(it) },
                        exclude_allergens = allergens.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        prep_minutes_max = prepMax.toIntOrNull(),
                    )
                    onSubmit(req)
                },
            ) { Text("Generieren") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPlanPreviewScreen(
    preview: AutoPlanGenerateResponse,
    committing: Boolean,
    onRemoveSlot: (LocalDate, String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Column {
                Text("Vorschau", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Score ${"%.1f".format(preview.total_score)} • ${preview.unfilled_slot_count} Slots leer",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                items(preview.days) { day ->
                    val parsed = remember(day.date) { LocalDate.parse(day.date) }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(parsed.toString(), fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            if (day.slots.isEmpty()) {
                                Text("Keine Vorschläge", style = MaterialTheme.typography.bodySmall)
                            } else {
                                day.slots.forEach { s ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            "${SLOT_LABEL_AUTO[s.slot_tag] ?: s.slot_tag}: ${s.title} (${s.prep_minutes} Min)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = { onRemoveSlot(parsed, s.slot_tag) }) { Text("Entfernen") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = !committing, onClick = onCommit) {
                if (committing) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Übernehmen")
            }
        },
        dismissButton = { OutlinedButton(onClick = onCancel, enabled = !committing) { Text("Abbrechen") } },
    )
}
