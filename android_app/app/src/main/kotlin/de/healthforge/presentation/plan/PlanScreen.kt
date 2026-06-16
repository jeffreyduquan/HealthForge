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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.presentation.essen.rezepte.RecipeCard
import de.healthforge.presentation.home.HomeViewModel
import de.healthforge.presentation.home.components.DottedAddButton
import de.healthforge.presentation.home.components.IntakeCard
import de.healthforge.presentation.home.components.PinnedNutrientCard
import de.healthforge.presentation.home.components.PinnedNutrientEntry
import de.healthforge.presentation.home.components.Sparkline
import de.healthforge.presentation.home.components.SupplementChecklist
import de.healthforge.presentation.home.components.WaterStageSlider
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onOpenHistory: () -> Unit = {},
    onOpenShoppingList: () -> Unit = {},
    onOpenRecipe: (String) -> Unit = {},
    onOpenIngredient: (String) -> Unit = {},
    onOpenSupplement: (String) -> Unit = {},
    onNavigateToEssen: () -> Unit = {},
    vm: PlanViewModel = hiltViewModel(),
    autoVm: AutoPlanViewModel = hiltViewModel(),
    homeVm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val autoState by autoVm.state.collectAsState()
    val homeState by homeVm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    val snackbar = remember { SnackbarHostState() }

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
            // ── AKTIONEN (ganz oben) ──
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionPill(label = "HOME")
                    // ── HEUTE-BUTTON (nur sichtbar wenn nicht auf heute) ──
                    if (state.selectedDay != LocalDate.now()) {
                        Spacer(Modifier.width(10.dp))
                        TodayPill(
                            onClick = {
                                val today = LocalDate.now()
                                vm.selectDay(today)
                                homeVm.setDate(today)
                            },
                        )
                    }
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

            // ── TAG & HEUTE-INTAKES ──
            item(key = "day_strip") {
                DayStrip(selected = state.selectedDay, onPick = { d -> vm.selectDay(d); homeVm.setDate(d) })
            }

            item(key = "day_header") {
                DayHeader(date = state.selectedDay)
            }

            // ── INTAKE CARDS (flat list, swipe-to-delete) ──
            val entries = homeState.entries
            if (entries.isEmpty()) {
                item(key = "empty_intake") {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Noch nichts gegessen heute",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = hm.fgTertiary,
                        )
                    }
                }
            } else {
                items(entries, key = { "intake-${it.id}" }) { entry ->
                    val recipeUrl = if (entry.sourceType == IntakeSourceType.RECIPE)
                        de.healthforge.data.repository.MediaRepository.imageUrl(
                            "recipes",
                            homeState.recipeDtos[entry.sourceId]?.image_key,
                            variant = "medium"
                        ) else null
                    IntakeCard(
                        entry = entry,
                        pinnedKeys = homeState.pinnedKeys,
                        recipeImageUrl = recipeUrl,
                        ingredientDto = homeState.ingredientDtos[entry.sourceId],
                        onDelete = { homeVm.deleteIntakeEntryCascade(entry) },
                        onToggleConsumed = { homeVm.toggleEntryConsumed(entry) },
                        onTap = {
                            when (entry.sourceType) {
                                IntakeSourceType.RECIPE -> onOpenRecipe(entry.sourceId)
                                IntakeSourceType.INGREDIENT -> onOpenIngredient(entry.sourceId)
                                IntakeSourceType.SUPPLEMENT -> onOpenSupplement(entry.sourceId)
                            }
                        },
                    )
                }
            }

            // ── GROSSES DOTTED + (öffnet Essen-Tab) ──
            item(key = "dotted_add") {
                DottedAddButton(onClick = onNavigateToEssen)
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
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }

    // ── AUTOPLAN ──
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
    val days = remember { (-365..365).map { today.plusDays(it.toLong()) } }
    val todayIndex = 365 // today is at position 365 in the range -365..365
    val fmt = remember { DateTimeFormatter.ofPattern("d.M.") }
    val listState = rememberLazyListState()

    // Auto-scroll to today on first composition
    LaunchedEffect(Unit) {
        listState.scrollToItem(todayIndex)
    }

    LazyRow(
        state = listState,
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
 * TodayPill — accent-gradient pill button that jumps back to today.
 * Only visible when viewing a past or future day.
 *
 * Design: matches DayStrip's selected pill (accentGradient + RoundedCornerShape(20.dp))
 * with bold "Heute" label in fgPrimary. Subtle violetGlow shadow on the pill.
 */
@Composable
private fun TodayPill(onClick: () -> Unit) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = Color.Transparent,
        contentColor = hm.fgPrimary,
    ) {
        Box(
            modifier = Modifier
                .background(hm.accentGradient, shape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Heute",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.4.sp,
                ),
                color = hm.fgPrimary,
            )
        }
    }
}

private fun trend(k: String, t: de.healthforge.data.repository.DayNutrientTotals) = when (k) {
    "kcal" -> t.kcal; "protein" -> t.proteinG; "carbs" -> t.carbsG; "fat" -> t.fatG; else -> 0.0
}
