package de.healthforge.presentation.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.domain.IsIntakeEditableUseCase
import de.healthforge.presentation.home.PlannedMealInfo
import de.healthforge.presentation.essen.rezepte.RecipeCard
import de.healthforge.presentation.home.components.DateNavigator
import de.healthforge.presentation.home.components.PinnedNutrientCard
import de.healthforge.presentation.home.components.PinnedNutrientEntry
import de.healthforge.presentation.home.components.QuickAddDialog
import de.healthforge.presentation.home.components.Sparkline
import de.healthforge.presentation.home.components.SupplementChecklist
import de.healthforge.presentation.home.components.WaterStageSlider
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.LocalSemanticColors
import de.healthforge.presentation.theme.NeoCard
import de.healthforge.presentation.theme.NeoSectionLabel
import de.healthforge.presentation.theme.StatusOverUl
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    onOpenRecipe: (String) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val editableUseCase = IsIntakeEditableUseCase()
    val hm = LocalHmTokens.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(hm.background),
    ) {
        AmbientBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // REQ-HOME-HEADER-001: Clean DateNavigator only (no greeting, no history)
            DateNavigator(
                date = s.date,
                onChange = vm::setDate,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )

            // P7.S3 / REQ-HOME-NUTRIENT-LIST-001 \u2014 Pinned Nutrients
            NeoSectionLabel(text = "Ern\u00e4hrung")
            NeoCard {
                PinnedNutrientCard(
                    entries = s.pinnedKeys.filter { it != "water" }.map { key ->
                        val trend = s.trendTotals.entries.sortedBy { it.key }.map { (_, totals) ->
                            extractTrendValue(key, totals)
                        }
                        when (key) {
                            "kcal" -> PinnedNutrientEntry(key, s.totals.kcal.toDouble(), s.targets.kcal.toDouble(), trendValues = trend)
                            "protein" -> PinnedNutrientEntry(key, s.totals.proteinG, s.targets.proteinG.toDouble(), trendValues = trend)
                            "carbs" -> PinnedNutrientEntry(key, s.totals.carbsG, s.targets.carbsG.toDouble(), trendValues = trend)
                            "fat" -> PinnedNutrientEntry(key, s.totals.fatG, s.targets.fatG.toDouble(), trendValues = trend)
                            else -> {
                                val def = de.healthforge.domain.nutrition.NutrientCatalog
                                    .byKeyOrNull(key)?.defaultPerDay ?: 1.0
                                PinnedNutrientEntry(key, 0.0, def, trendValues = trend)
                            }
                        }
                    },
                    trailingSlot = if (s.pinnedKeys.contains("water")) {
                        {
                            val waterTrendVals = s.waterTrend.entries.sortedBy { it.key }.map { it.value.toDouble() }
                            Column {
                                // P7.S3a v2 / REQ-HOME-WATER-BAR-001 — Stufen-Slider
                                WaterStageSlider(
                                    currentMl = s.waterMl,
                                    ghostMl = s.waterGhostMl,
                                    goalMl = s.targets.waterMl,
                                    reminderEnabled = s.waterReminderEnabled,
                                    onCommit = vm::setWaterMl,
                                    onToggleReminder = vm::setWaterReminderEnabled,
                                )
                                // REQ-HOME-TREND-001: 7-day water sparkline — 18dp for level-line visibility
                                if (waterTrendVals.size >= 2) {
                                    val waterStage = kotlin.math.floor(s.waterMl.toDouble() / s.targets.waterMl.coerceAtLeast(1).toDouble()).toInt().coerceAtLeast(0)
                                    Sparkline(
                                        values = waterTrendVals,
                                        accent = hm.ambientCyan,
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(18.dp),
                                        stageTarget = s.targets.waterMl.toDouble(),
                                        stage = waterStage,
                                    )
                                }
                            }
                        }
                    } else null,
                    pinnedKeys = s.pinnedKeys,
                    expanded = s.pinsExpanded,
                    onToggleExpanded = vm::togglePinsExpanded,
                    onTogglePin = vm::togglePin,
                )
            }


            if (s.supplementChecklist.isNotEmpty()) {
                NeoSectionLabel(text = "Supplemente")
                NeoCard(contentPadding = PaddingValues(0.dp)) {
                    SupplementChecklist(
                        items = s.supplementChecklist,
                        onMarkTaken = vm::markSupplementTaken,
                    )
                }
            }

            // REQ-HOME-PLAN-001: Planned meals (from Plan tab) + already eaten entries
            NeoSectionLabel(text = "Heute")
            if (s.plannedMeals.isEmpty() && s.entries.isEmpty()) {
                GlassCard {
                    Text(
                        "Noch nichts geplant. Tippe auf das Plus, um zu starten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = hm.fgSecondary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Build set of ALL planned meal source keys (consumed + not consumed)
                    // to prevent any duplicate display when an intake entry also exists.
                    val allPlannedKeys = remember(s.plannedMeals) {
                        s.plannedMeals.map { it.item.sourceType to it.item.sourceId }.toSet()
                    }
                    // Planned meals with toggle (consumed + not consumed)
                    s.plannedMeals.forEach { planned ->
                        HomeRecipeCard(
                            planned = planned,
                            recipeDtos = s.recipeDtos,
                            onOpenRecipe = onOpenRecipe,
                            onToggleEaten = {
                                if (planned.slotConsumed) vm.markAsNotEaten(planned.slotId)
                                else vm.markAsEaten(planned.slotId)
                            },
                            onDelete = { vm.deletePlannedSlot(planned.slotId) },
                        )
                    }
                    // Intake entries not already shown by any planned meal (prevents duplicate flash)
                    s.entries.take(5).filter { (it.sourceType to it.sourceId) !in allPlannedKeys }.forEach { e ->
                        HomeRecipeCard(
                            intakeEntry = e,
                            recipeDtos = s.recipeDtos,
                            onOpenRecipe = onOpenRecipe,
                            onDelete = { vm.deleteIntakeEntry(e.id) },
                        )
                    }
                }
                if (s.entries.size > 5) {
                    TextButton(onClick = onOpenHistory) {
                        Text("Alle ${s.entries.size} Eintr\u00e4ge anzeigen \u2192")
                    }
                }
            }

            // Quick-Add inline button

            Spacer(Modifier.height(96.dp).navigationBarsPadding())
        }

        // GradientFab overlay (REQ-HOME-003) — P7.S4 4b rev2: 48dp konsistent mit allen Screens
        GradientFab(
            onClick = vm::openQuickAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            size = 48.dp,
            icon = { Icon(Icons.Filled.Add, contentDescription = "Hinzuf\u00fcgen", tint = hm.fgPrimary, modifier = Modifier.size(20.dp)) },
        )

        // P6.S7 F-005: Snackbar-Host f\u00fcr Wasser-Undo (Long-Press-Hint).
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp),
        )
    }

    if (s.showQuickAdd) {
        QuickAddDialog(
            query = s.quickAddQuery,
            results = s.quickAddResults,
            portionGrams = s.quickAddPortion,
            selected = s.quickAddSelected,
            loading = s.quickAddLoading,
            onQueryChange = vm::onQuickAddQuery,
            onSelect = vm::onQuickAddSelect,
            onClearSelection = vm::onQuickAddClearSelection,
            onPortionChange = vm::onQuickAddPortion,
            onConfirm = vm::confirmQuickAdd,
            onDismiss = vm::closeQuickAdd,
        )
    }

    // P7.S4 4e — Picker-Sheet entfernt; Pinnen erfolgt inline im
    // PinnedNutrientCard (Expanded-View).
}

@Composable
private fun HomeRecipeCard(
    planned: PlannedMealInfo? = null,
    intakeEntry: IntakeEntryEntity? = null,
    recipeDtos: Map<String, RecipeListItemDto>,
    onOpenRecipe: (String) -> Unit = {},
    onToggleEaten: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    val hm = LocalHmTokens.current

    // ── Planned meal ── ONLY RecipeCard, NOTHING else
    if (planned != null) {
        val item = planned.item
        if (item.sourceType != IntakeSourceType.RECIPE) return
        val serverDto = recipeDtos[item.sourceId] ?: return
        SwipeDeleteWrapper(onDelete = onDelete) {
            RecipeCard(
                recipe = serverDto,
                onClick = { onOpenRecipe(serverDto.id) },
                trailingActions = {
                    GegessenToggle(
                        isConsumed = planned.slotConsumed,
                        onToggle = onToggleEaten ?: {},
                    )
                },
            )
        }
        return
    }

    // ── Intake entry ── ONLY RecipeCard, NOTHING else
    if (intakeEntry != null) {
        val entry = intakeEntry
        if (entry.sourceType != IntakeSourceType.RECIPE) return
        val serverDto = recipeDtos[entry.sourceId] ?: return
        val timeLabel = entry.loggedAt.let { ts ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
            "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        }
        SwipeDeleteWrapper(onDelete = onDelete) {
            RecipeCard(
                recipe = serverDto,
                onClick = { onOpenRecipe(serverDto.id) },
                trailingActions = {
                    Text(timeLabel, style = MaterialTheme.typography.labelSmall, color = hm.fgTertiary)
                },
            )
        }
    }
}

/**
 * Right-swipe-to-delete wrapper. Keine X-Buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteWrapper(
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
 * GEGESSEN-Toggle — rechts auf der RecipeCard.
 * Gegessen: grüner Mini-Chip. Nicht gegessen: outlined Label.
 */
@Composable
private fun GegessenToggle(isConsumed: Boolean, onToggle: () -> Unit) {
    val hm = LocalHmTokens.current
    val sem = LocalSemanticColors.current
    val shape = RoundedCornerShape(10.dp)
    // EXACT fixed width prevents SwipeToDismissBox reset on recomposition
    val fixedMod = Modifier.width(80.dp).clip(shape).clickable(onClick = onToggle)
    val labelMod = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)

    if (isConsumed) {
        Row(
            modifier = fixedMod.background(sem.statusGood.copy(alpha = 0.15f))
                .border(1.dp, sem.statusGood.copy(alpha = 0.5f), shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("✓ Gegessen", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = sem.statusGood, modifier = labelMod)
        }
    } else {
        Row(
            modifier = fixedMod.border(1.dp, hm.fgTertiary.copy(alpha = 0.4f), shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Gegessen", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = hm.fgSecondary, modifier = labelMod)
        }
    }
}

/** Extract nutrient value for sparkline trend from DayNutrientTotals. */
private fun extractTrendValue(key: String, totals: de.healthforge.data.repository.DayNutrientTotals): Double = when (key) {
    "kcal" -> totals.kcal
    "protein" -> totals.proteinG
    "carbs" -> totals.carbsG
    "fat" -> totals.fatG
    else -> 0.0
}
