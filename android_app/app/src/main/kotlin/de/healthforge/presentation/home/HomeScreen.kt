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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.network.RecipeListItemDto
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
import java.util.Locale

// ══════════════════════════════════════════════════════════════════════
// HomeScreen — P7.S4 4b Clean Rewrite
// Layout: DateNav → Ernährung → Wasser → Supplemente → Übersicht → FAB
// ══════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    onOpenRecipe: (String) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    val snackbarHostState = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize().background(hm.background)) {
        AmbientBackdrop(Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 1. Header ──
            DateNavigator(date = s.date, onChange = vm::setDate,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))

            // ── 2. ERNÄHRUNG ──
            NeoSectionLabel("Ernährung")
            NeoCard {
                PinnedNutrientCard(
                    entries = s.pinnedKeys.filter { it != "water" }.map { key ->
                        val trend = s.trendTotals.entries.sortedBy { it.key }
                            .map { (_, totals) -> extractTrendValue(key, totals) }
                        when (key) {
                            "kcal" -> PinnedNutrientEntry(key, s.totals.kcal.toDouble(),
                                s.targets.kcal.toDouble(), trend)
                            "protein" -> PinnedNutrientEntry(key, s.totals.proteinG,
                                s.targets.proteinG.toDouble(), trend)
                            "carbs" -> PinnedNutrientEntry(key, s.totals.carbsG,
                                s.targets.carbsG.toDouble(), trend)
                            "fat" -> PinnedNutrientEntry(key, s.totals.fatG,
                                s.targets.fatG.toDouble(), trend)
                            else -> PinnedNutrientEntry(key, 0.0,
                                de.healthforge.domain.nutrition.NutrientCatalog
                                    .byKeyOrNull(key)?.defaultPerDay ?: 1.0, trend)
                        }
                    },
                    pinnedKeys = s.pinnedKeys,
                    expanded = s.pinsExpanded,
                    onToggleExpanded = vm::togglePinsExpanded,
                    onTogglePin = vm::togglePin,
                )
            }

            // ── 3. WASSER ──
            if (s.pinnedKeys.contains("water")) {
                NeoSectionLabel("Wasser")
                NeoCard {
                    val wv = s.waterTrend.entries.sortedBy { it.key }.map { it.value.toDouble() }
                    WaterStageSlider(
                        currentMl = s.waterMl, ghostMl = s.waterGhostMl, goalMl = s.targets.waterMl,
                        reminderEnabled = s.waterReminderEnabled,
                        onCommit = vm::setWaterMl, onToggleReminder = vm::setWaterReminderEnabled,
                    )
                    if (wv.size >= 2) {
                        val ws = kotlin.math.floor(s.waterMl.toDouble() /
                            s.targets.waterMl.coerceAtLeast(1).toDouble()).toInt().coerceAtLeast(0)
                        Sparkline(wv, hm.ambientCyan,
                            Modifier.fillMaxWidth().padding(top = 4.dp).height(22.dp),
                            s.targets.waterMl.toDouble(), ws)
                    }
                }
            }

            // ── 4. SUPPLEMENTE (optional) ──
            if (s.supplementChecklist.isNotEmpty()) {
                NeoSectionLabel("Supplemente")
                NeoCard(contentPadding = PaddingValues(0.dp)) {
                    SupplementChecklist(items = s.supplementChecklist,
                        onMarkTaken = vm::markSupplementTaken)
                }
            }

            // ── 5. ÜBERSICHT ──
            NeoSectionLabel("Übersicht")
            if (s.plannedMeals.isEmpty()) {
                GlassCard {
                    Text("Noch nichts geplant.", style = MaterialTheme.typography.bodyMedium,
                        color = hm.fgSecondary)
                }
            } else {
                s.plannedMeals.forEach { planned ->
                    UebersichtCard(
                        planned = planned, recipeDtos = s.recipeDtos,
                        onOpenRecipe = onOpenRecipe,
                        onToggleEaten = {
                            if (planned.slotConsumed) vm.markAsNotEaten(planned.slotId)
                            else vm.markAsEaten(planned.slotId)
                        },
                        onDelete = { vm.deletePlannedSlot(planned.slotId) },
                    )
                }
            }

            Spacer(Modifier.height(80.dp).navigationBarsPadding())
        }

        // ── FAB ──
        GradientFab(
            onClick = vm::openQuickAdd,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(end = 24.dp, bottom = 24.dp),
            size = 48.dp,
            icon = { Icon(Icons.Filled.Add, "Hinzufügen", tint = hm.fgPrimary,
                modifier = Modifier.size(20.dp)) },
        )
        SnackbarHost(hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                .padding(bottom = 96.dp))
    }

    if (s.showQuickAdd) {
        QuickAddDialog(
            query = s.quickAddQuery, results = s.quickAddResults,
            portionGrams = s.quickAddPortion,
            selected = s.quickAddSelected, loading = s.quickAddLoading,
            onQueryChange = vm::onQuickAddQuery, onSelect = vm::onQuickAddSelect,
            onClearSelection = vm::onQuickAddClearSelection,
            onPortionChange = vm::onQuickAddPortion,
            onConfirm = vm::confirmQuickAdd, onDismiss = vm::closeQuickAdd,
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ÜBERSICHT Card — RecipeCard (mit DTO) oder Compact-Row + Toggle
// ═══════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UebersichtCard(
    planned: PlannedMealInfo,
    recipeDtos: Map<String, RecipeListItemDto>,
    onOpenRecipe: (String) -> Unit,
    onToggleEaten: () -> Unit,
    onDelete: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val item = planned.item
    val serverDto = if (item.sourceType == IntakeSourceType.RECIPE)
        recipeDtos[item.sourceId] else null

    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.StartToEnd)
        { try { onDelete() } catch (_: Exception) {}; true } else false
    })

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                Icon(Icons.Filled.Delete, "Löschen", tint = StatusOverUl, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromEndToStart = false, enableDismissFromStartToEnd = true,
    ) {
        if (serverDto != null) {
            RecipeCard(recipe = serverDto, onClick = { onOpenRecipe(serverDto.id) },
                trailingActions = {
                    GegessenToggle(isConsumed = planned.slotConsumed, onToggle = onToggleEaten)
                })
        } else {
            val kcal = ((item.snapshotKcalPer100g ?: 0.0) * item.amount / 100.0).toInt()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(hm.cardSurface)
                    .border(1.dp, hm.cardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.snapshotName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                    Text("%.0f g · %d kcal".format(Locale.US, item.amount, kcal),
                        style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
                }
                GegessenToggle(isConsumed = planned.slotConsumed, onToggle = onToggleEaten)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// GEGESSEN Toggle — Fixed 80dp width, no layout shift
// ═══════════════════════════════════════════════════════════════
@Composable
private fun GegessenToggle(isConsumed: Boolean, onToggle: () -> Unit) {
    val hm = LocalHmTokens.current
    val sem = LocalSemanticColors.current
    val shape = RoundedCornerShape(10.dp)
    val fixed = Modifier.width(80.dp).clip(shape).clickable(onClick = onToggle)
    val pad = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    if (isConsumed) {
        Row(fixed.background(sem.statusGood.copy(alpha = 0.15f))
            .border(1.dp, sem.statusGood.copy(alpha = 0.5f), shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Text("✓ Gegessen", style = MaterialTheme.typography.labelSmall
                .copy(fontWeight = FontWeight.SemiBold), color = sem.statusGood, modifier = pad)
        }
    } else {
        Row(fixed.border(1.dp, hm.fgTertiary.copy(alpha = 0.4f), shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            Text("Gegessen", style = MaterialTheme.typography.labelSmall
                .copy(fontWeight = FontWeight.Medium), color = hm.fgSecondary, modifier = pad)
        }
    }
}

private fun extractTrendValue(key: String, totals: de.healthforge.data.repository.DayNutrientTotals): Double =
    when (key) {
        "kcal" -> totals.kcal
        "protein" -> totals.proteinG
        "carbs" -> totals.carbsG
        "fat" -> totals.fatG
        else -> 0.0
    }
