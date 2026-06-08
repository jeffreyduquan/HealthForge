package de.healthforge.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.db.entities.MealPlanItemEntity
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.domain.IsIntakeEditableUseCase
import de.healthforge.presentation.home.PlannedMealInfo
import de.healthforge.presentation.essen.rezepte.RecipeCard
import de.healthforge.presentation.home.components.DateNavigator
import de.healthforge.presentation.home.components.PinnedNutrientCard
import de.healthforge.presentation.home.components.PinnedNutrientEntry
import de.healthforge.presentation.home.components.QuickAddDialog
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
                            // P7.S3a v2 / REQ-HOME-WATER-BAR-001 \u2014 Stufen-Slider als
                            // letzte Pin-Zeile. Range 0..goal (0\u2013100\u00a0% der aktuellen
                            // Stufe), 50-ml-Steps, eigene Farbe pro Stufe 0..9,
                            // Drag-Through-Zero entlocked Stufe-1.
                            WaterStageSlider(
                                currentMl = s.waterMl,
                                ghostMl = s.waterGhostMl,
                                goalMl = s.targets.waterMl,
                                reminderEnabled = s.waterReminderEnabled,
                                onCommit = vm::setWaterMl,
                                onToggleReminder = vm::setWaterReminderEnabled,
                            )
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
                    // Planned meals with GEGESSEN button
                    s.plannedMeals.forEach { planned ->
                        PlannedMealCard(
                            planned = planned,
                            recipeDtos = s.recipeDtos,
                            onMarkEaten = { vm.markAsEaten(planned.slotId) },
                        )
                    }
                    // Already eaten entries (informational, no delete)
                    s.entries.take(5).forEach { e ->
                        EatenEntryRow(
                            entry = e,
                            recipeDtos = s.recipeDtos,
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

        // GradientFab overlay (REQ-HOME-003)
        GradientFab(
            onClick = vm::openQuickAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = "Hinzuf\u00fcgen", tint = hm.fgPrimary) },
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
private fun PlannedMealCard(
    planned: PlannedMealInfo,
    recipeDtos: Map<String, RecipeListItemDto>,
    onMarkEaten: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val item = planned.item
    val dto = if (item.sourceType == IntakeSourceType.RECIPE) {
        recipeDtos[item.sourceId]
    } else null
    val fallback = RecipeListItemDto(
        id = item.sourceId,
        title = item.snapshotName,
        description = if (item.sourceType == IntakeSourceType.RECIPE) "%.0f Portion(en)".format(item.amount) else "%.0f g".format(item.amount),
        image_key = null,
        servings = 1,
        prep_minutes = 0,
        slot_tags = emptyList(),
        visibility = "",
        author_id = "",
        created_at = "",
        like_count = 0,
        community_recommend_count = 0,
        community_not_recommend_count = 0,
    )

    if (planned.slotConsumed) {
        // Already eaten — informational
        RecipeCard(
            recipe = dto ?: fallback,
            onClick = { },
            trailingActions = {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Gegessen",
                    tint = de.healthforge.presentation.theme.LocalSemanticColors.current.statusGood,
                    modifier = Modifier.size(20.dp).padding(end = 8.dp),
                )
            },
        )
    } else {
        // Not yet eaten — show GEGESSEN button
        RecipeCard(
            recipe = dto ?: fallback,
            onClick = { },
            trailingActions = {
                GradientFab(
                    onClick = onMarkEaten,
                    size = 40.dp,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Als gegessen markieren", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            },
        )
    }
}

@Composable
private fun EatenEntryRow(
    entry: IntakeEntryEntity,
    recipeDtos: Map<String, RecipeListItemDto>,
) {
    val hm = LocalHmTokens.current
    val dto = if (entry.sourceType == IntakeSourceType.RECIPE) {
        recipeDtos[entry.sourceId]
    } else null
    val fallback = entry.toRecipeListItemDto()

    RecipeCard(
        recipe = dto ?: fallback,
        onClick = { },
        trailingActions = {
            Text(
                entry.loggedAt.let { ts ->
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
                    "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                },
                style = MaterialTheme.typography.labelSmall,
                color = hm.fgTertiary,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
    )
}

/** Extract nutrient value for sparkline trend from DayNutrientTotals. */
private fun extractTrendValue(key: String, totals: de.healthforge.data.repository.DayNutrientTotals): Double = when (key) {
    "kcal" -> totals.kcal
    "protein" -> totals.proteinG
    "carbs" -> totals.carbsG
    "fat" -> totals.fatG
    else -> 0.0
}

private fun IntakeEntryEntity.toRecipeListItemDto(): RecipeListItemDto {
    val kcal = snapshotKcalPer100g?.let { (it * portionGrams / 100.0).toInt() }
    val desc = buildString {
        append("${portionGrams.toInt()} g")
        if (kcal != null) append(" · $kcal kcal")
    }
    return RecipeListItemDto(
        id = sourceId,
        title = snapshotName,
        description = desc,
        image_key = null,
        servings = 1,
        prep_minutes = 0,
        slot_tags = emptyList(),
        visibility = "",
        author_id = "",
        created_at = "",
        like_count = 0,
        community_recommend_count = 0,
        community_not_recommend_count = 0,
    )
}
