package de.healthforge.presentation.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            // Header — Histamind §6.3: SectionPill+Glass-Tile-Row oben, GradientText darunter.
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionPill(label = "PLAN")
                    Spacer(Modifier.weight(1f))
                    GlassIconTile(
                        onClick = { autoVm.open() },
                        contentDescription = "Plan generieren",
                    ) { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = hm.fgPrimary, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(8.dp))
                    GlassIconTile(
                        onClick = onOpenShoppingList,
                        contentDescription = "Einkaufsliste",
                    ) { Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = hm.fgPrimary, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                GradientText(
                    text = "Wochenplan",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.W800,
                        letterSpacing = (-0.5).sp,
                    ),
                )
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
                        DayHeader(date = state.selectedDay)
                    }
                    item {
                        DayWaterGoalSlider(
                            effectiveMl = state.effectiveWaterGoalMl,
                            profileDefaultMl = state.profileWaterDefaultMl,
                            isOverride = state.hasWaterGoalOverride,
                            onCommit = { v -> vm.setWaterGoalForDay(v) },
                            onReset = { vm.resetWaterGoalForDay() },
                        )
                    }
                    item {
                        DaySummary(slots = state.slots)
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
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }

        // Bottom-right GradientFab — Histamind PlanFab idiom.
        GradientFab(
            onClick = { addSlotDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Mahlzeit hinzufügen", tint = Color.White) }

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
 * 7-Tage Day-Strip — Histamind §6.3.
 *
 * Visualisierung (Histamind-konform):
 *  - Selected → accentGradient-Pill mit Violet-Glow-Shadow.
 *  - Today (nicht selected) → kleiner 5dp Violet-Dot ÜBER der Glass-Pill.
 *  - Sonst  → Glass-Pill (verticalGradient + 1dp glassBorder Hairline).
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
            val isToday = day == today
            val isSelected = day == selected
            val pillShape = RoundedCornerShape(20.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Today-Dot über der Pill — nur wenn heute UND nicht selected.
                Box(Modifier.height(8.dp), contentAlignment = Alignment.Center) {
                    if (isToday && !isSelected) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(SolidColor(hm.ambientViolet)),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                val pillModifier = Modifier
                    .clip(pillShape)
                    .let {
                        if (isSelected) {
                            it.background(hm.accentGradient)
                        } else {
                            it
                                .background(Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom)))
                                .border(1.dp, hm.glassBorder, pillShape)
                        }
                    }
                    .clickable { onPick(day) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                Column(
                    modifier = pillModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val topLabel = if (isToday) "Heute" else day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                    Text(
                        topLabel.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected || isToday) FontWeight.W700 else FontWeight.W500,
                            letterSpacing = 0.4.sp,
                        ),
                        color = when {
                            isSelected -> hm.fgPrimary
                            else -> hm.fgTertiary
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.format(fmt),
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
                        color = hm.fgPrimary,
                    )
                }
            }
        }
    }
}

/**
 * Glass-Tile-Icon-Button (40×40dp) — Histamind Plan-Header rechts.
 * Transparent vertikal-Gradient + 1dp glassBorder + 12dp Radius.
 */
@Composable
private fun GlassIconTile(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom)))
            .border(1.dp, hm.glassBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // contentDescription propagiert über das innere Icon der Caller-Site.
        @Suppress("UNUSED_EXPRESSION") contentDescription
        content()
    }
}

/**
 * SlotLabelPill — Histamind §SlotLabelPill: 3×14dp Accent-Gradient-Stripe + 8dp Gap
 * + UPPERCASE labelSmall w800 +1.4sp Letterspacing fgSecondary.
 */
@Composable
private fun SlotLabelPill(text: String) {
    val hm = LocalHmTokens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(hm.accentGradient),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = 1.4.sp,
            ),
            color = hm.fgSecondary,
        )
    }
}

/**
 * DayHeader — Histamind: UPPERCASE-Wochentag (mit Heute-Dot bei isToday)
 * + ShaderMask GradientText "26. Mai" headlineSmall w700 -0.3sp.
 */
@Composable
private fun DayHeader(date: LocalDate) {
    val hm = LocalHmTokens.current
    val today = LocalDate.now()
    val isToday = date == today
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
    val month = remember(date) { date.format(DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)) }
    Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                weekday.uppercase(),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.W700,
                    letterSpacing = 1.4.sp,
                ),
                color = if (isToday) hm.ambientViolet else hm.fgTertiary,
            )
            if (isToday) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(SolidColor(hm.ambientViolet)),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        GradientText(
            text = month,
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.3).sp,
            ),
        )
    }
}

/**
 * DaySummary — Histamind: zählt gefüllte Slots pro Typ.
 * Beispiel: "1 Frühstück · 1 Mittag · 1 Abend · 2 Snacks".
 */
@Composable
private fun DaySummary(slots: List<SlotWithItems>) {
    val hm = LocalHmTokens.current
    var b = 0; var lu = 0; var di = 0; var sn = 0
    slots.forEach { sw ->
        if (sw.items.isEmpty()) return@forEach
        when (sw.slot.slotType) {
            "BREAKFAST" -> b++
            "LUNCH" -> lu++
            "DINNER" -> di++
            "SNACK" -> sn++
        }
    }
    val parts = buildList {
        if (b > 0) add("$b Frühstück")
        if (lu > 0) add("$lu Mittag")
        if (di > 0) add("$di Abend")
        if (sn > 0) add("$sn ${if (sn == 1) "Snack" else "Snacks"}")
    }
    val text = if (parts.isEmpty()) "Noch nichts gegessen heute" else parts.joinToString(" · ")
    Text(
        text = text,
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = hm.fgSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

/**
 * P7.S4 / REQ-PLAN-WATER-GOAL-001 — Tages-Wasserziel-Slider im PlanScreen.
 *
 * - Range: 500…5000 ml, Step: 50 ml.
 * - `effectiveMl` = aktueller Wert (Override falls vorhanden, sonst Profil-Default).
 * - `isOverride=true` zeigt ein Reset-Icon, das den Override löscht.
 * - Commit erst auf Slider-Release (`onValueChangeFinished`), nicht bei jedem Drag-Step.
 * - Beschriftung kommuniziert klar Default vs. Override.
 */
@Composable
private fun DayWaterGoalSlider(
    effectiveMl: Int,
    profileDefaultMl: Int,
    isOverride: Boolean,
    onCommit: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val range = 500f..5000f
    val steps = ((5000 - 500) / 50) - 1 // 89 steps
    // Lokaler Drag-State, damit Slider während Drag flüssig ist und nur Release committed wird.
    var sliderPos by remember(effectiveMl) { mutableStateOf(effectiveMl.toFloat()) }

    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotLabelPill(text = "Wasserziel heute")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${sliderPos.toInt()} ml",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
                    color = hm.fgPrimary,
                )
                if (isOverride) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onReset, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.RestartAlt,
                            contentDescription = "Auf Profil-Default zurücksetzen",
                            tint = hm.fgSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isOverride) {
                    "Override für diesen Tag (Profil-Default: $profileDefaultMl ml)"
                } else {
                    "Profil-Default — Slider verschieben für tagesspezifisches Ziel"
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = hm.fgTertiary,
            )
            Slider(
                value = sliderPos,
                onValueChange = { v ->
                    // auf 50-ml-Raster snappen
                    sliderPos = (kotlin.math.round(v / 50f) * 50f).coerceIn(range)
                },
                onValueChangeFinished = { onCommit(sliderPos.toInt()) },
                valueRange = range,
                steps = steps,
                modifier = Modifier.fillMaxWidth(),
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
    val hm = LocalHmTokens.current
    val sem = LocalSemanticColors.current
    val label = SLOT_LABEL[slotType] ?: slotType

    // Histamind: leere Slots = kompakte Glass-Row mit "+"-Circle rechts (1 Tap → Picker).
    if (items.isEmpty()) {
        val shape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom)))
                .border(1.dp, hm.glassBorder, shape)
                .clickable(onClick = onAddItem)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlotLabelPill(text = label)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDeleteSlot, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Slot löschen", tint = hm.fgTertiary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0x337C5CFF),
                                Color(0x334DD0E1),
                            ),
                        ),
                    )
                    .border(1.dp, hm.glassBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = hm.fgPrimary, modifier = Modifier.size(14.dp))
            }
        }
        return
    }

    // Gefüllter Slot: GlassCard mit SlotLabelPill-Header + Items + Actions.
    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlotLabelPill(text = label)
                if (consumed) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "gegessen",
                        tint = sem.statusGood,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDeleteSlot) {
                    Icon(Icons.Filled.Close, contentDescription = "Slot löschen", tint = hm.fgSecondary)
                }
            }
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
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onAddItem) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = hm.ambientViolet)
                    Spacer(Modifier.width(4.dp))
                    Text("Hinzufügen", color = hm.ambientViolet)
                }
                Spacer(Modifier.weight(1f))
                if (!consumed) {
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
