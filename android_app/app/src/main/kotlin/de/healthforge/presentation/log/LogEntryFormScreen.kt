package de.healthforge.presentation.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.SymptomDefEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryFormScreen(
    onBack: () -> Unit,
    vm: LogFormViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }

    LaunchedEffect(s.saved, s.deleted) {
        if (s.saved || s.deleted) onBack()
    }
    LaunchedEffect(s.message) {
        s.message?.let {
            scope.launch {
                snackbar.showSnackbar(it)
                vm.clearMessage()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.entryId == 0L) "Neuer Eintrag" else "Eintrag bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (s.entryId != 0L && s.editable) {
                        IconButton(onClick = { deleteOpen = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (s.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!s.editable) {
                Text(
                    "Eintrag älter als 7 Tage — nur lesend.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Text("Mood: ${s.mood}/10", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = s.mood.toFloat(),
                onValueChange = { vm.setMood(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
                enabled = s.editable,
            )

            Text(
                "Schlafqualität: ${s.sleepQuality?.let { "$it/5" } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { q ->
                    FilterChip(
                        selected = s.sleepQuality == q,
                        onClick = { if (s.editable) vm.setSleepQuality(if (s.sleepQuality == q) null else q) },
                        label = { Text(q.toString()) },
                        enabled = s.editable,
                    )
                }
            }

            OutlinedTextField(
                value = s.sleepHours,
                onValueChange = vm::setSleepHours,
                label = { Text("Schlafdauer (h)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = s.editable,
            )

            Text("Symptome", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (s.selectedSymptoms.isEmpty()) {
                Text("Keine Symptome.", style = MaterialTheme.typography.bodySmall)
            } else {
                s.selectedSymptoms.forEach { (id, sev) ->
                    val name = s.symptoms.firstOrNull { it.id == id }?.name ?: "?"
                    SymptomSeverityChip(
                        name = name,
                        severity = sev,
                        onSeverity = { if (s.editable) vm.setSeverity(id, it) },
                        onRemove = { if (s.editable) vm.toggleSymptom(id) },
                    )
                }
            }
            if (s.editable) {
                OutlinedButton(onClick = { pickerOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Symptom")
                }
            }

            Text("Tags", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (s.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.tags.forEach { t ->
                        AssistChip(
                            onClick = { if (s.editable) vm.removeTag(t) },
                            label = { Text(t) },
                            trailingIcon = { if (s.editable) Icon(Icons.Filled.Close, contentDescription = null) },
                        )
                    }
                }
            }
            if (s.editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag hinzufügen") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { vm.addTag(tagInput); tagInput = "" }) { Text("+") }
                }
            }

            OutlinedTextField(
                value = s.note,
                onValueChange = vm::setNote,
                label = { Text("Notiz") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                enabled = s.editable,
            )

            if (s.editable) {
                Button(onClick = vm::save, modifier = Modifier.fillMaxWidth()) { Text("Speichern") }
            }
        }
    }

    if (pickerOpen) {
        EditPickerDialog(
            available = s.symptoms,
            selected = s.selectedSymptoms.keys,
            onToggle = vm::toggleSymptom,
            onDismiss = { pickerOpen = false },
        )
    }
    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Eintrag löschen?") },
            text = { Text("Dieser Schritt kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = { deleteOpen = false; vm.delete() }) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun EditPickerDialog(
    available: List<SymptomDefEntity>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Symptome wählen") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                items(available, key = { it.id }) { sym ->
                    FilterChip(
                        selected = selected.contains(sym.id),
                        onClick = { onToggle(sym.id) },
                        label = { Text(sym.name) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}
