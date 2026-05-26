package de.healthforge.presentation.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.ReminderFrequency
import de.healthforge.data.db.entities.SupplementReminderEntity
import de.healthforge.notification.AlarmScheduler
import de.healthforge.notification.RequestNotificationPermissionEffect
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementEditScreen(
    supplementId: Long,
    onBack: () -> Unit,
    vm: SupplementEditViewModel = hiltViewModel(),
) {
    LaunchedEffect(supplementId) { if (supplementId > 0) vm.load(supplementId) }
    val s by vm.state.collectAsStateWithLifecycle()
    var editingReminder by remember { mutableStateOf<SupplementReminderEntity?>(null) }
    var requestNotifPerm by remember { mutableStateOf(false) }
    var confirmSuggest by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(s.suggestMessage) {
        s.suggestMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSuggestMessage()
        }
    }

    RequestNotificationPermissionEffect(trigger = requestNotifPerm) { granted ->
        requestNotifPerm = false
        if (granted && editingReminder == null) {
            editingReminder = vm.newReminderTemplate()
        }
    }

    LaunchedEffect(s.saved) { if (s.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (supplementId > 0) "Supplement bearbeiten" else "Neues Supplement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = s.name, onValueChange = vm::setName,
                label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.brand, onValueChange = vm::setBrand,
                label = { Text("Marke (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s.defaultDose, onValueChange = vm::setDose,
                    label = { Text("Dosis *") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = s.unitLabel, onValueChange = vm::setUnit,
                    label = { Text("Einheit") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
            Text("Nährwerte pro Dosis (optional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s.kcal, onValueChange = vm::setKcal,
                    label = { Text("kcal") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = s.protein, onValueChange = vm::setProtein,
                    label = { Text("Eiweiß (g)") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s.carbs, onValueChange = vm::setCarbs,
                    label = { Text("KH (g)") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = s.fat, onValueChange = vm::setFat,
                    label = { Text("Fett (g)") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = s.notes, onValueChange = vm::setNotes,
                label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(),
            )

            s.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { vm.save() },
                enabled = !s.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (s.saving) "Speichern…" else "Speichern") }

            OutlinedButton(
                onClick = { confirmSuggest = true },
                enabled = !s.suggesting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (s.suggesting) "Wird gesendet…"
                    else "Für globalen Katalog vorschlagen",
                )
            }

            if (s.id > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Erinnerungen", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { requestNotifPerm = true }) { Text("+ Neu") }
                }
                if (s.reminders.isEmpty()) {
                    Text(
                        "Noch keine Erinnerungen.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(items = s.reminders, key = { it.id }) { r ->
                            ReminderRow(
                                r = r,
                                onToggle = { vm.toggleReminderEnabled(r) },
                                onEdit = { editingReminder = r },
                                onDelete = { vm.deleteReminder(r.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    editingReminder?.let { r ->
        ReminderEditDialog(
            initial = r,
            onDismiss = { editingReminder = null },
            onSave = {
                vm.saveReminder(it)
                editingReminder = null
            },
        )
    }

    if (confirmSuggest) {
        AlertDialog(
            onDismissRequest = { confirmSuggest = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmSuggest = false
                    vm.suggestPublic()
                }) { Text("Senden") }
            },
            dismissButton = { TextButton(onClick = { confirmSuggest = false }) { Text("Abbrechen") } },
            title = { Text("Vorschlag einreichen?") },
            text = {
                Text(
                    "Dieses Supplement wird zur Aufnahme in den globalen Katalog vorgeschlagen. " +
                        "Ein Administrator prüft den Eintrag manuell. " +
                        "Deine User-ID wird mit dem Vorschlag verknüpft.",
                )
            },
        )
    }
}

@Composable
private fun ReminderRow(
    r: SupplementReminderEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(r.label, style = MaterialTheme.typography.titleSmall)
                Text(reminderSubtitle(r), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = r.enabled, onCheckedChange = { onToggle() })
            TextButton(onClick = onEdit) { Text("Bearb.") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}

private fun reminderSubtitle(r: SupplementReminderEntity): String {
    val time = "%02d:%02d".format(r.hourOfDay ?: 0, r.minute ?: 0)
    return when (r.frequency) {
        ReminderFrequency.ONCE -> "Einmalig"
        ReminderFrequency.DAILY -> "Täglich um $time"
        ReminderFrequency.WEEKLY -> {
            val mask = r.daysOfWeekMask ?: 0
            val days = DayOfWeek.values().filter { AlarmScheduler.containsDay(mask, it) }
                .joinToString(",") { it.name.take(2) }
            "Wöchentlich ($days) um $time"
        }
    }
}

@Composable
private fun ReminderEditDialog(
    initial: SupplementReminderEntity,
    onDismiss: () -> Unit,
    onSave: (SupplementReminderEntity) -> Unit,
) {
    var label by remember { mutableStateOf(initial.label) }
    var freq by remember { mutableStateOf(initial.frequency) }
    var hour by remember { mutableStateOf((initial.hourOfDay ?: 9).toString()) }
    var minute by remember { mutableStateOf((initial.minute ?: 0).toString()) }
    var days by remember {
        mutableStateOf(
            DayOfWeek.values().filter { AlarmScheduler.containsDay(initial.daysOfWeekMask ?: 0, it) }.toSet()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 9
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                val mask = if (freq == ReminderFrequency.WEEKLY) AlarmScheduler.maskOf(days) else null
                onSave(
                    initial.copy(
                        label = label.ifBlank { "Erinnerung" },
                        frequency = freq,
                        hourOfDay = h,
                        minute = m,
                        daysOfWeekMask = mask,
                        triggerAtMillis = null,
                    )
                )
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text(if (initial.id == 0L) "Neue Erinnerung" else "Erinnerung bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label, onValueChange = { label = it },
                    label = { Text("Bezeichnung") }, singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderFrequency.values().forEach { f ->
                        FilterChip(
                            selected = freq == f,
                            onClick = { freq = f },
                            label = { Text(when (f) {
                                ReminderFrequency.ONCE -> "Einmalig"
                                ReminderFrequency.DAILY -> "Täglich"
                                ReminderFrequency.WEEKLY -> "Wöchentlich"
                            }) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour, onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Stunde") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minute, onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                if (freq == ReminderFrequency.WEEKLY) {
                    Text("Wochentage", style = MaterialTheme.typography.labelMedium)
                    Column {
                        DayOfWeek.values().forEach { d ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = d in days,
                                    onCheckedChange = { checked ->
                                        days = if (checked) days + d else days - d
                                    },
                                )
                                Text(germanDay(d))
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun germanDay(d: DayOfWeek): String = when (d) {
    DayOfWeek.MONDAY -> "Montag"
    DayOfWeek.TUESDAY -> "Dienstag"
    DayOfWeek.WEDNESDAY -> "Mittwoch"
    DayOfWeek.THURSDAY -> "Donnerstag"
    DayOfWeek.FRIDAY -> "Freitag"
    DayOfWeek.SATURDAY -> "Samstag"
    DayOfWeek.SUNDAY -> "Sonntag"
}
