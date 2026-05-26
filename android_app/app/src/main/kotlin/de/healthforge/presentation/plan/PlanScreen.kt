package de.healthforge.presentation.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.LocalSemanticColors
import de.healthforge.presentation.theme.SectionPill
import de.healthforge.presentation.theme.SegmentedTabs
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
    val hm = LocalHmTokens.current
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

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackdrop()

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Header: Title + AutoPlan + ShoppingList
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GradientText(
                    text = "Wochenplan",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { autoVm.open() }) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Plan generieren", tint = hm.fgPrimary)
                }
                IconButton(onClick = onOpenShoppingList) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Einkaufsliste", tint = hm.fgPrimary)
                }
            }

            DayStrip(selected = state.selectedDay, onPick = vm::selectDay)

            Spacer(Modifier.height(8.dp))

            if (state.slots.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Noch keine Mahlzeiten geplant",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = hm.fgSecondary,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { addSlotDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = hm.ambientViolet)
                            Spacer(Modifier.width(6.dp))
                            Text("Mahlzeit hinzufügen", color = hm.ambientViolet)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        SectionPill(label = "MAHLZEITEN", modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp))
                    }
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
                        TextButton(
                            onClick = { addSlotDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = hm.ambientViolet)
                            Spacer(Modifier.width(6.dp))
                            Text("Weitere Mahlzeit", color = hm.ambientViolet)
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
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

/**
 * 7-Tage Day-Strip mit Gradient-Pill für „heute" + ausgewählten Tag.
 */
@Composable
private fun DayStrip(selected: LocalDate, onPick: (LocalDate) -> Unit) {
    val hm = LocalHmTokens.current
    val today = LocalDate.now()
    val days = (-1..5).map { today.plusDays(it.toLong()) }
    val fmt = remember { DateTimeFormatter.ofPattern("d.M.") }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days) { day ->
            val isSelected = day == selected
            val pillShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .clip(pillShape)
                    .then(
                        if (isSelected) Modifier.background(hm.accentGradient)
                        else Modifier.background(SolidColor(hm.glassFillTop))
                    )
                    .clickable { onPick(day) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).uppercase(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color.White else hm.fgSecondary,
                )
                Text(
                    day.format(fmt),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else hm.fgPrimary,
                )
            }
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
    val hm = LocalHmTokens.current
    val sem = LocalSemanticColors.current
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SLOT_LABEL[slotType] ?: slotType,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = hm.fgPrimary,
                )
                if (consumed) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "gegessen",
                        tint = sem.statusGood,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDeleteSlot) {
                    Icon(Icons.Filled.Close, contentDescription = "Slot löschen", tint = hm.fgSecondary)
                }
            }
            if (items.isEmpty()) {
                Text(
                    "Noch nichts geplant",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = hm.fgSecondary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.snapshotName, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = hm.fgPrimary)
                            val unit = if (item.sourceType.name == "RECIPE") "Portion(en)" else "g"
                            Text(
                                "${"%g".format(item.amount)} $unit",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = hm.fgSecondary,
                            )
                        }
                        IconButton(onClick = { onDeleteItem(item.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Item löschen", tint = hm.fgSecondary)
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAddItem) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = hm.ambientViolet)
                    Spacer(Modifier.width(4.dp))
                    Text("Hinzufügen", color = hm.ambientViolet)
                }
                Spacer(Modifier.weight(1f))
                if (!consumed && items.isNotEmpty()) {
                    GradientFab(
                        onClick = onMarkConsumed,
                        size = 44.dp,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Habe gegessen", tint = Color.White)
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
    val hm = LocalHmTokens.current
    val picker by vm.picker.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp)) {
        // F-008 Wording-Lock: Sheet-Titel + Tab-Labels „Rezept / Lebensmittel" (kein „Zutat")
        Text(
            "Rezept oder Lebensmittel",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = hm.fgPrimary,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        SegmentedTabs(
            options = listOf("Rezept", "Lebensmittel"),
            selectedIndex = tab,
            onSelect = {
                tab = it
                vm.clearPicker()
                q = ""
            },
        )
        Spacer(Modifier.height(12.dp))
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
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tab == 0) {
                items(picker.recipes) { r ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.addRecipeItem(slotId, r)
                                onPick()
                            },
                        padding = PaddingValues(12.dp),
                    ) {
                        Column {
                            Text(r.title, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                            Text(
                                "${r.prep_minutes} min",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = hm.fgSecondary,
                            )
                        }
                    }
                }
            } else {
                items(picker.ingredients) { ing ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.addIngredientItem(slotId, ing)
                                onPick()
                            },
                        padding = PaddingValues(12.dp),
                    ) {
                        Column {
                            Text(ing.name_de, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                            ing.energy_kcal_per_100g?.let {
                                Text(
                                    "${it.toInt()} kcal / 100g",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    color = hm.fgSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
