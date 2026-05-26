package de.healthforge.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.domain.IsIntakeEditableUseCase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeHistoryScreen(
    onBack: () -> Unit,
    vm: IntakeHistoryViewModel = hiltViewModel(),
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val editable = IsIntakeEditableUseCase()
    val grouped = entries.groupBy { it.dayDateIso }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verlauf") },
                navigationIcon = {
                    @Suppress("DEPRECATION")
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zur\u00fcck")
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Noch keine Eintr\u00e4ge.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            grouped.forEach { (dayIso, dayEntries) ->
                item(key = "head-$dayIso") {
                    Text(
                        formatDay(dayIso),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                items(dayEntries, key = { it.id }) { e ->
                    HistoryRow(entry = e, editable = editable(e.loggedAt), onDelete = { vm.delete(e.id) })
                }
            }
        }
    }
}

private fun formatDay(iso: String): String =
    runCatching {
        val d = LocalDate.parse(iso)
        val today = LocalDate.now()
        when (d) {
            today -> "Heute"
            today.minusDays(1) -> "Gestern"
            else -> d.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))
        }
    }.getOrDefault(iso)

@Composable
private fun HistoryRow(entry: IntakeEntryEntity, editable: Boolean, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.snapshotName, style = MaterialTheme.typography.bodyMedium)
                val portion = "${entry.portionGrams.toInt()} g"
                val kcal = entry.snapshotKcalPer100g?.let {
                    " \u00b7 ${(it * entry.portionGrams / 100.0).toInt()} kcal"
                } ?: ""
                Text("$portion$kcal", style = MaterialTheme.typography.bodySmall)
            }
            if (editable) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "L\u00f6schen")
                }
            } else {
                Text(
                    "read-only",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
