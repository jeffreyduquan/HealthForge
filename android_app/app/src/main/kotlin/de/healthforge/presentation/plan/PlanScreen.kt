package de.healthforge.presentation.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.data.db.entities.MealPlanItemEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SLOT_LABEL = mapOf(
    "BREAKFAST" to "Frühstück",
    "LUNCH" to "Mittag",
    "DINNER" to "Abend",
    "SNACK" to "Snack",
)
private val SLOT_ORDER = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onOpenShoppingList: () -> Unit = {},
    vm: PlanViewModel = hiltViewModel(),
    autoVm: AutoPlanViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val autoState by autoVm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pickerForSlot by remember { mutableStateOf<Long?>(null) }
    var addSlotDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(autoState.error) {
        autoState.error?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(autoState.committed) {
        if (autoState.committed) {
            snackbar.showSnackbar("Plan übernommen")
            autoVm.dismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Mahlzeitenplan") },
                actions = {
                    IconButton(onClick = { autoVm.open() }) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Plan generieren",
                        )
                    }
                    IconButton(onClick = onOpenShoppingList) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "Einkaufsliste",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            DaySelectorRow(selected = state.selectedDay, onPick = vm::selectDay)
            if (state.slots.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Noch keine Mahlzeiten geplant", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { addSlotDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Mahlzeit hinzufügen")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.slots, key = { it.slot.id }) { sw ->
                        SlotCard(
                            slotType = sw.slot.slotType,
                            consumed = sw.slot.consumed,
                            items = sw.items,
                            onAddItem = { pickerForSlot = sw.slot.id },
                            onMarkConsumed = { vm.markConsumed(sw.slot.id) },
                            onDeleteSlot = { vm.deleteSlot(sw.slot.id) },
                            onDeleteItem = { id -> vm.deleteItem(id) },
                        )
                    }
                    item {
                        TextButton(onClick = { addSlotDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Weitere Mahlzeit")
                        }
                    }
                }
            }
        }
    }

    if (addSlotDialog) {
        AlertDialog(
            onDismissRequest = { addSlotDialog = false },
            title = { Text("Mahlzeit hinzufügen") },
            text = {
                Column {
                    SLOT_ORDER.forEach { slot ->
                        TextButton(
                            onClick = {
                                vm.addSlot(slot)
                                addSlotDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(SLOT_LABEL[slot] ?: slot) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { addSlotDialog = false }) { Text("Abbrechen") } },
        )
    }

    pickerForSlot?.let { slotId ->
        val sheet = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { pickerForSlot = null; vm.clearPicker() }, sheetState = sheet) {
            SlotItemPicker(
                vm = vm,
                onPick = { pickerForSlot = null; vm.clearPicker() },
                slotId = slotId,
            )
        }
    }

    if (autoState.visible && autoState.preview == null) {
        AutoPlanGenerateDialog(
            onDismiss = { autoVm.dismiss() },
            onSubmit = { req -> autoVm.generate(req) },
        )
    }
    autoState.preview?.let { p ->
        AutoPlanPreviewScreen(
            preview = p,
            committing = autoState.committing,
            onRemoveSlot = autoVm::removeSlot,
            onCommit = { autoVm.commit() },
            onCancel = { autoVm.dismiss() },
        )
    }
}

@Composable
private fun DaySelectorRow(selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val days = (-1..5).map { today.plusDays(it.toLong()) }
    val fmt = remember { DateTimeFormatter.ofPattern("d.M.") }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(days) { day ->
            FilterChip(
                selected = day == selected,
                onClick = { onPick(day) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN))
                        Text(day.format(fmt), style = MaterialTheme.typography.labelSmall)
                    }
                },
            )
        }
    }
}

@Composable
private fun SlotCard(
    slotType: String,
    consumed: Boolean,
    items: List<MealPlanItemEntity>,
    onAddItem: () -> Unit,
    onMarkConsumed: () -> Unit,
    onDeleteSlot: () -> Unit,
    onDeleteItem: (Long) -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SLOT_LABEL[slotType] ?: slotType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (consumed) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Check, contentDescription = "gegessen", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDeleteSlot) {
                    Icon(Icons.Filled.Close, contentDescription = "Slot löschen")
                }
            }
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.snapshotName, style = MaterialTheme.typography.bodyMedium)
                        val unit = if (item.sourceType.name == "RECIPE") "Portion(en)" else "g"
                        Text("${"%g".format(item.amount)} $unit", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { onDeleteItem(item.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Item löschen")
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onAddItem) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Hinzufügen")
                }
                Spacer(Modifier.weight(1f))
                if (!consumed && items.isNotEmpty()) {
                    Button(onClick = onMarkConsumed) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Habe gegessen")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotItemPicker(
    vm: PlanViewModel,
    onPick: () -> Unit,
    slotId: Long,
) {
    val picker by vm.picker.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var q by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0; vm.clearPicker() }, text = { Text("Rezept") })
            Tab(selected = tab == 1, onClick = { tab = 1; vm.clearPicker() }, text = { Text("Zutat") })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = q,
            onValueChange = {
                q = it
                if (tab == 0) vm.searchRecipes(it) else vm.searchIngredients(it)
            },
            label = { Text("Suchen…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
            if (tab == 0) {
                items(picker.recipes) { r ->
                    Card(
                        onClick = {
                            vm.addRecipeItem(slotId, r)
                            onPick()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(r.title, fontWeight = FontWeight.SemiBold)
                            Text("${r.prep_minutes} min", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                items(picker.ingredients) { ing ->
                    Card(
                        onClick = {
                            vm.addIngredientItem(slotId, ing)
                            onPick()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(ing.name_de, fontWeight = FontWeight.SemiBold)
                            ing.energy_kcal_per_100g?.let {
                                Text("${it.toInt()} kcal / 100g", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
