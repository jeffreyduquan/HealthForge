package de.healthforge.presentation.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.data.db.entities.ShoppingListItemEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    vm: ShoppingListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val items by vm.items.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Einkaufsliste") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.generate() }, enabled = !state.isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Neu generieren")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RangePickerRow(
                start = state.start,
                end = state.end,
                onChange = vm::setRange,
            )
            Button(
                onClick = { vm.generate() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Einkaufsliste erstellen")
            }
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.runId == null)
                            "Noch keine Einkaufsliste. Wähle einen Zeitraum und tippe „Einkaufsliste erstellen“."
                        else
                            "Keine Einträge im aktuellen Lauf.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                val grouped = items.groupBy { it.category }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grouped.forEach { (category, list) ->
                        item(key = "h-$category") {
                            Text(
                                category,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(list, key = { "i-${it.id}" }) { item ->
                            ShoppingItemRow(item = item, onToggle = { vm.toggle(item.id, it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItemEntity,
    onToggle: (Boolean) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.checked, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    "${formatQty(item.quantity)} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RangePickerRow(
    start: LocalDate,
    end: LocalDate,
    onChange: (LocalDate, LocalDate) -> Unit,
) {
    var startText by remember(start) { mutableStateOf(start.toString()) }
    var endText by remember(end) { mutableStateOf(end.toString()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = startText,
            onValueChange = { v ->
                startText = v
                runCatching { LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE) }
                    .onSuccess { onChange(it, end) }
            },
            label = { Text("Von") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = endText,
            onValueChange = { v ->
                endText = v
                runCatching { LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE) }
                    .onSuccess { onChange(start, it) }
            },
            label = { Text("Bis") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatQty(v: Double): String {
    val whole = v.toLong()
    return if (v == whole.toDouble()) whole.toString() else "%.2f".format(v).trimEnd('0').trimEnd(',')
}
