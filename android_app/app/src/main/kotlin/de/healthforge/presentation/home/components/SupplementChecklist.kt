package de.healthforge.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.presentation.home.SupplementChecklistItem

/**
 * Today's supplements as a checklist (REQ-SUPP-003 follow-up + REQ-HOME-Supplement-Checkliste).
 *
 * Tap an unchecked item to log it as taken (creates a SUPPLEMENT-source IntakeEntry).
 * Already-taken items are shown checked & dimmed.
 */
@Composable
fun SupplementChecklist(
    items: List<SupplementChecklistItem>,
    onMarkTaken: (SupplementChecklistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Supplements heute", style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { if (!item.taken) onMarkTaken(item) }) {
                        if (item.taken) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Genommen",
                                tint = Color(0xFF2E7D32),
                            )
                        } else {
                            Icon(
                                Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = "Als genommen markieren",
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.supplement.nameDe,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.taken) TextDecoration.LineThrough else null,
                            color = if (item.taken) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            reminderSubtitle(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${formatDose(item.supplement.defaultDose)} ${item.supplement.unitLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun reminderSubtitle(item: SupplementChecklistItem): String {
    val r = item.reminder
    val time = if (r.hourOfDay != null && r.minute != null) {
        "%02d:%02d".format(r.hourOfDay, r.minute)
    } else null
    val freqLabel = when (r.frequency) {
        ReminderFrequency.DAILY -> "Täglich"
        ReminderFrequency.WEEKLY -> "Wöchentlich"
        ReminderFrequency.ONCE -> "Einmalig"
    }
    return if (time != null) "$freqLabel · $time" else freqLabel
}

private fun formatDose(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)
