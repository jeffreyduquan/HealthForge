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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.text.style.TextOverflow
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
import de.healthforge.presentation.theme.NeoCard
import de.healthforge.presentation.theme.NeoSectionLabel
import de.healthforge.presentation.theme.StatusOverUl
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
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
                                // REQ-HOME-TREND-001: 7-day water sparkline with P7.S4 4b level lines
                                if (waterTrendVals.size >= 2) {
                                    val waterStage = kotlin.math.floor(s.waterMl.toDouble() / s.targets.waterMl.coerceAtLeast(1).toDouble()).toInt().coerceAtLeast(0)
                                    Sparkline(
                                        values = waterTrendVals,
                                        accent = hm.ambientCyan,
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(14.dp),
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
                    // Build set of source keys already shown as consumed planned items
                    val consumedKeys = remember(s.plannedMeals) {
                        s.plannedMeals.filter { it.slotConsumed }
                            .map { it.item.sourceType to it.item.sourceId }
                            .toSet()
                    }
                    // Planned meals with toggle (consumed + not consumed)
                    s.plannedMeals.forEach { planned ->
                        HomeRecipeCard(
                            planned = planned,
                            recipeDtos = s.recipeDtos,
                            onToggleEaten = {
                                if (planned.slotConsumed) vm.markAsNotEaten(planned.slotId)
                                else vm.markAsEaten(planned.slotId)
                            },
                            onDelete = { vm.deletePlannedSlot(planned.slotId) },
                        )
                    }
                    // Intake entries not already shown by planned meals
                    s.entries.take(5).filter { (it.sourceType to it.sourceId) !in consumedKeys }.forEach { e ->
                        HomeRecipeCard(
                            intakeEntry = e,
                            recipeDtos = s.recipeDtos,
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

        // GradientFab overlay (REQ-HOME-003) — P7.S4 4b reduced from 56dp to 44dp
        GradientFab(
            onClick = vm::openQuickAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            size = 44.dp,
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
    onToggleEaten: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    val hm = LocalHmTokens.current

    // ── Planned meal path ──
    if (planned != null) {
        val item = planned.item
        val isConsumed = planned.slotConsumed
        val serverDto = if (item.sourceType == IntakeSourceType.RECIPE) recipeDtos[item.sourceId] else null

        if (serverDto != null) {
            // P7.S4 4b: Proper RecipeCard with integrated toggle — the ONLY card style
            SwipeableRecipeCard(
                dto = serverDto,
                isConsumed = isConsumed,
                onToggleEaten = onToggleEaten,
                onDelete = onDelete,
            )
        } else {
            // P7.S4 4b: No cached recipe DTO → compact row (NOT a RecipeCard without image/description)
            CompactPlannedRow(
                name = item.snapshotName,
                amount = item.amount,
                sourceType = item.sourceType,
                kcalPer100g = item.snapshotKcalPer100g,
                isConsumed = isConsumed,
                onToggleEaten = onToggleEaten,
                onDelete = onDelete,
            )
        }
        return
    }

    // ── Intake entry path ──
    if (intakeEntry != null) {
        val entry = intakeEntry
        val timeLabel = entry.loggedAt.let { ts ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
            "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
        }
        val serverDto = if (entry.sourceType == IntakeSourceType.RECIPE) recipeDtos[entry.sourceId] else null

        if (serverDto != null) {
            // Proper RecipeCard for already-logged recipe entries
            SwipeableRecipeCard(
                dto = serverDto,
                isConsumed = true,
                onToggleEaten = null,
                onDelete = onDelete,
                timeLabel = timeLabel,
            )
        } else {
            // Compact row for logged non-recipe entries
            CompactIntakeRow(
                name = entry.snapshotName,
                portionGrams = entry.portionGrams,
                kcalPer100g = entry.snapshotKcalPer100g,
                timeLabel = timeLabel,
                onDelete = onDelete,
            )
        }
        return
    }
}

/**
 * P7.S4 4b — RecipeCard with integrated toggle and swipe-to-delete.
 * The toggle button is an inline chip (not a separate GradientFab) for better
 * visual integration into the trailing-actions area.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRecipeCard(
    dto: RecipeListItemDto,
    isConsumed: Boolean,
    onToggleEaten: (() -> Unit)?,
    onDelete: () -> Unit,
    timeLabel: String? = null,
) {
    val hm = LocalHmTokens.current

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = StatusOverUl, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
    ) {
        RecipeCard(
            recipe = dto,
            onClick = { },
            trailingActions = {
                if (onToggleEaten != null) {
                    InlineToggleChip(
                        isConsumed = isConsumed,
                        onToggle = onToggleEaten,
                    )
                }
                if (timeLabel != null) {
                    Text(
                        timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = hm.fgTertiary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            },
        )
    }
}

/**
 * P7.S4 4b — Inline toggle chip replacing the GradientFab for eaten/not-eaten.
 * Compact, better integrated into the card's trailing area.
 *
 * Not eaten: outlined pill with "✓ Gegessen" label
 * Eaten: filled accent pill with "✓" checkmark + subtle background
 */
@Composable
private fun InlineToggleChip(
    isConsumed: Boolean,
    onToggle: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(12.dp)

    if (isConsumed) {
        // Already eaten — show subtle "undo" indicator
        IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Nicht gegessen",
                tint = hm.fgSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    } else {
        // Not eaten — compact "mark as eaten" chip
        Row(
            modifier = Modifier
                .clip(shape)
                .background(hm.accentGradient)
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Als gegessen markieren",
                tint = Color.White,
                modifier = Modifier
                    .padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                    .size(16.dp),
            )
            Text(
                text = "Gegessen",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                modifier = Modifier.padding(start = 2.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            )
        }
    }
}

/**
 * P7.S4 4b — Compact row for planned meals without a cached recipe DTO.
 * Replaces the old fallback RecipeCard that had no image/description.
 */
@Composable
private fun CompactPlannedRow(
    name: String,
    amount: Double,
    sourceType: IntakeSourceType,
    kcalPer100g: Double?,
    isConsumed: Boolean,
    onToggleEaten: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(14.dp)
    val desc = buildString {
        if (sourceType == IntakeSourceType.RECIPE) append("%.0f Portion(en)".format(amount))
        else append("%.0f g".format(amount))
        kcalPer100g?.let { kcal ->
            val total = (kcal * amount / 100.0).toInt()
            append(" · $total kcal")
        }
    }

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
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = StatusOverUl, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(hm.cardSurface)
                .border(1.dp, hm.cardBorder, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = hm.fgPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = hm.fgSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onToggleEaten != null) {
                InlineToggleChip(isConsumed = isConsumed, onToggle = onToggleEaten)
            }
        }
    }
}

/**
 * P7.S4 4b — Compact row for logged intake entries without a cached recipe DTO.
 */
@Composable
private fun CompactIntakeRow(
    name: String,
    portionGrams: Double,
    kcalPer100g: Double?,
    timeLabel: String,
    onDelete: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(14.dp)
    val kcal = kcalPer100g?.let { (it * portionGrams / 100.0).toInt() }
    val desc = buildString {
        append("${portionGrams.toInt()} g")
        if (kcal != null) append(" · $kcal kcal")
    }

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
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = StatusOverUl, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(hm.cardSurface)
                .border(1.dp, hm.cardBorder, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = hm.fgPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = hm.fgSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = hm.fgTertiary,
            )
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
