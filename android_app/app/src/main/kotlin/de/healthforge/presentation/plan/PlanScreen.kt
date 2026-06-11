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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.presentation.common.PickerData
import de.healthforge.presentation.common.PlanItemPicker
import de.healthforge.presentation.essen.rezepte.RecipeCard
import de.healthforge.presentation.home.HomeViewModel
import de.healthforge.presentation.home.components.PinnedNutrientCard
import de.healthforge.presentation.home.components.PinnedNutrientEntry
import de.healthforge.presentation.home.components.Sparkline
import de.healthforge.presentation.home.components.SupplementChecklist
import de.healthforge.presentation.home.components.WaterStageSlider
import de.healthforge.presentation.lebensmittel.components.IngredientDetailSheet
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.GradientText
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.LocalSemanticColors
import de.healthforge.presentation.theme.NeoCard
import de.healthforge.presentation.theme.NeoSectionLabel
import de.healthforge.presentation.theme.SectionPill
import de.healthforge.presentation.theme.SegmentedTabs
import de.healthforge.presentation.theme.StatusOverUl
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
    onOpenHistory: () -> Unit = {},
    onOpenShoppingList: () -> Unit = {},
    onOpenRecipe: (String) -> Unit = {},
    vm: PlanViewModel = hiltViewModel(),
    autoVm: AutoPlanViewModel = hiltViewModel(),
    homeVm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val autoState by autoVm.state.collectAsState()
    val homeState by homeVm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    val snackbar = remember { SnackbarHostState() }
    var pickerForSlot by remember { mutableStateOf<Long?>(null) }
    var addSlotDialog by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<IngredientDto?>(null) }

    // Sync date PlanViewModel ↔ HomeViewModel
    LaunchedEffect(state.selectedDay) { homeVm.setDate(state.selectedDay) }

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

        // Einzige durchgehend scrollbare LazyColumn — Ernährung + Wasser + Tagesplan
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── AKTIONEN ──
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionPill(label = "HOME")
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
            }

            // ── ERNÄHRUNG ──
            item(key = "nutrition") {
                NeoSectionLabel("Ernährung", Modifier.padding(horizontal = 20.dp))
                NeoCard(Modifier.padding(horizontal = 20.dp)) {
                    PinnedNutrientCard(
                        entries = homeState.pinnedKeys.filter { it != "water" }.map { k ->
                            val tr = homeState.trendTotals.entries.sortedBy { it.key }.map { (_, t) -> trend(k, t) }
                            when (k) {
                                "kcal"    -> PinnedNutrientEntry(k, homeState.totals.kcal.toDouble(), homeState.targets.kcal.toDouble(), tr)
                                "protein" -> PinnedNutrientEntry(k, homeState.totals.proteinG, homeState.targets.proteinG.toDouble(), tr)
                                "carbs"   -> PinnedNutrientEntry(k, homeState.totals.carbsG, homeState.targets.carbsG.toDouble(), tr)
                                "fat"     -> PinnedNutrientEntry(k, homeState.totals.fatG, homeState.targets.fatG.toDouble(), tr)
                                else      -> PinnedNutrientEntry(k, 0.0,
                                    de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(k)?.defaultPerDay ?: 1.0, tr)
                            }
                        },
                        pinnedKeys = homeState.pinnedKeys,
                        expanded = homeState.pinsExpanded,
                        onToggleExpanded = homeVm::togglePinsExpanded,
                        onTogglePin = homeVm::togglePin,
                    )
                }
            }

            // ── WASSER ──
            if (homeState.pinnedKeys.contains("water")) {
                item(key = "water") {
                    NeoSectionLabel("Wasser", Modifier.padding(horizontal = 20.dp))
                    NeoCard(Modifier.padding(horizontal = 20.dp)) {
                        Column {
                            WaterStageSlider(
                                homeState.waterMl, homeState.waterGhostMl, homeState.targets.waterMl,
                                homeState.waterReminderEnabled, homeVm::setWaterMl, homeVm::setWaterReminderEnabled,
                            )
                            val wv = homeState.waterTrend.entries.sortedBy { it.key }.map { it.value.toDouble() }
                            if (wv.size >= 2) {
                                val s = (homeState.waterMl / homeState.targets.waterMl.coerceAtLeast(1))
                                Sparkline(wv, hm.ambientCyan, Modifier.fillMaxWidth().padding(top = 4.dp).height(22.dp), homeState.targets.waterMl.toDouble(), s)
                            }
                        }
                    }
                }
            }

            // ── SUPPLEMENTE ──
            if (homeState.supplementChecklist.isNotEmpty()) {
                item(key = "supplements") {
                    NeoSectionLabel("Supplemente", Modifier.padding(horizontal = 20.dp))
                    NeoCard(Modifier.padding(horizontal = 20.dp), contentPadding = PaddingValues(0.dp)) {
                        SupplementChecklist(homeState.supplementChecklist, homeVm::markSupplementTaken)
                    }
                }
            }

            // ── TAGSPLAN ──
            item(key = "plan_label") {
                NeoSectionLabel("Tagesplan", Modifier.padding(horizontal = 20.dp))
            }

            item(key = "day_strip") {
                DayStrip(selected = state.selectedDay, onPick = { d -> vm.selectDay(d); homeVm.setDate(d) })
            }

            if (state.slots.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
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
                }
            } else {
                item(key = "day_header") {
                    DayHeader(date = state.selectedDay)
                }
                item(key = "day_summary") {
                    DaySummary(slots = state.slots)
                }
                items(state.slots, key = { it.slot.id }) { sw ->
                    SlotCard(
                        slotType = sw.slot.slotType,
                        consumed = sw.slot.consumed,
                        items = sw.items,
                        recipeDtos = state.recipeDtos,
                        onOpenRecipe = onOpenRecipe,
                        onOpenIngredient = { id ->
                            state.ingredientDtos[id]?.let { detailTarget = it }
                        },
                        onAddItem = { pickerForSlot = sw.slot.id },
                        onMarkConsumed = { vm.markConsumed(sw.slot.id) },
                        onDeleteSlot = { vm.deleteSlot(sw.slot.id) },
                        onDeleteItem = { id -> vm.deleteItem(id) },
                    )
                }
            }
        }

        // Bottom-right GradientFab — 48dp einheitlich mit allen Screens
        GradientFab(
            onClick = { addSlotDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            size = 48.dp,
        ) { Icon(Icons.Filled.Add, contentDescription = "Mahlzeit hinzufügen", tint = Color.White, modifier = Modifier.size(20.dp)) }

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
        val picker by vm.picker.collectAsState()
        PlanItemPicker(
            show = true,
            onDismiss = { pickerForSlot = null; vm.clearPicker() },
            pickerData = PickerData(recipes = picker.recipes, ingredients = picker.ingredients),
            onSearchRecipes = vm::searchRecipes,
            onSearchIngredients = vm::searchIngredients,
            onSelectRecipe = { vm.addRecipeItem(slotId, it); pickerForSlot = null; vm.clearPicker() },
            onSelectIngredient = { vm.addIngredientItem(slotId, it); pickerForSlot = null; vm.clearPicker() },
            onSelectSupplement = { vm.addSupplementItem(slotId, it); pickerForSlot = null; vm.clearPicker() },
            onClearPicker = vm::clearPicker,
            supplementList = vm.supplementList,
        )
    }

    if (autoState.visible && autoState.preview == null) {
        AutoPlanGenerateDialog(
            onDismiss = { autoVm.dismiss() },
            onSubmit = { req -> autoVm.generate(req) },
            myGroups = autoState.myGroups,
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

    detailTarget?.let { target ->
        IngredientDetailSheet(item = target, onDismiss = { detailTarget = null })
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

@Composable
private fun SlotCard(
    slotType: String,
    consumed: Boolean,
    items: List<MealPlanItemEntity>,
    recipeDtos: Map<String, RecipeListItemDto>,
    onOpenRecipe: (String) -> Unit = {},
    onOpenIngredient: (String) -> Unit = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    val isRecipe = item.sourceType == IntakeSourceType.RECIPE
                    if (isRecipe) {
                        val dto = recipeDtos[item.sourceId] ?: item.toRecipeListItemDto(slotType)
                        SwipeDeletePlanItem(onDelete = { onDeleteItem(item.id) }) {
                            RecipeCard(recipe = dto, onClick = { onOpenRecipe(dto.id) })
                        }
                    } else {
                        SwipeDeletePlanItem(onDelete = { onDeleteItem(item.id) }) {
                            IngredientPlanCard(
                                item = item,
                                onClick = { onOpenIngredient(item.sourceId) },
                            )
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

/**
 * Wraps a RecipeCard in right-swipe-to-delete for PlanScreen items.
 * Kein X-Button — nur Swipe (konsistent mit HomeScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeletePlanItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                try { onDelete() } catch (_: Exception) { }
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = StatusOverUl, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
    ) { content() }
}

/**
 * Card for ingredient/food items in Plan slots. Renders a GlassCard with
 * name, amount, kcal — tap opens [IngredientDetailSheet].
 */
@Composable
private fun IngredientPlanCard(
    item: MealPlanItemEntity,
    onClick: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val kcal = (item.snapshotKcalPer100g ?: 0.0) * item.amount / 100.0
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom)))
            .border(1.dp, hm.glassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.snapshotName,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = hm.fgPrimary,
            )
            Text(
                "${"%.0f".format(item.amount)} g · ${kcal.toInt()} kcal",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = hm.fgSecondary,
            )
        }
    }
}

private fun MealPlanItemEntity.toRecipeListItemDto(slotType: String): RecipeListItemDto {
    val unit = if (sourceType.name == "RECIPE") "Portion(en)" else "g"
    val kcal = snapshotKcalPer100g?.let { (it * amount / 100.0).toInt() }
    val desc = buildString {
        append("%.0f".format(amount))
        append(" $unit")
        if (kcal != null) append(" · $kcal kcal")
    }
    return RecipeListItemDto(
        id = sourceId,
        title = snapshotName,
        description = desc,
        image_key = null,
        servings = 1,
        prep_minutes = 0,
        slot_tags = listOf(slotType),
        visibility = "",
        author_id = "",
        created_at = "",
        like_count = 0,
        community_recommend_count = 0,
        community_not_recommend_count = 0,
    )
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

private fun trend(k: String, t: de.healthforge.data.repository.DayNutrientTotals) = when (k) {
    "kcal" -> t.kcal; "protein" -> t.proteinG; "carbs" -> t.carbsG; "fat" -> t.fatG; else -> 0.0
}
