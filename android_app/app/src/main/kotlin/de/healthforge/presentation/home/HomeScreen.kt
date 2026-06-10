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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.presentation.essen.rezepte.RecipeCard
import de.healthforge.presentation.common.PickerData
import de.healthforge.presentation.common.PlanItemPicker
import de.healthforge.presentation.home.components.DateNavigator
import de.healthforge.presentation.home.components.PinnedNutrientCard
import de.healthforge.presentation.home.components.PinnedNutrientEntry
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHistory: () -> Unit,
    onOpenRecipe: (String) -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    val s = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize().background(hm.background)) {
        AmbientBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding()
                .padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(state, vm)
            NutritionCard(state, vm)
            if (state.pinnedKeys.contains("water")) WaterCard(state, vm, hm)
            if (state.supplementChecklist.isNotEmpty()) SupplementsCard(state, vm)
            OverviewCard(state, vm, hm, onOpenRecipe)
            Spacer(Modifier.height(80.dp).navigationBarsPadding())
        }
        // FAB
        GradientFab(
            vm::openQuickAdd,
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 24.dp, bottom = 24.dp),
            size = 48.dp,
            icon = { Icon(Icons.Filled.Add, "Hinzufügen", tint = hm.fgPrimary, modifier = Modifier.size(20.dp)) },
        )
        SnackbarHost(s, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp))
    }

    PlanItemPicker(
        show = state.showQuickAdd,
        onDismiss = vm::closeQuickAdd,
        pickerData = PickerData(
            recipes = state.quickAddRecipes,
            ingredients = state.quickAddResults,
        ),
        onSearchRecipes = vm::searchAddRecipes,
        onSearchIngredients = vm::onQuickAddQuery,
        onSelectRecipe = vm::selectAddRecipe,
        onSelectIngredient = vm::onQuickAddSelect,
        onSelectSupplement = vm::selectAddSupplement,
        onClearPicker = vm::onQuickAddClearSelection,
        supplementList = state.quickAddSupplements,
    )
}

// ═══ 1. HEADER ═══
@Composable
private fun Header(state: HomeState, vm: HomeViewModel) {
    DateNavigator(state.date, vm::setDate, Modifier.padding(top = 12.dp, bottom = 4.dp))
}

// ═══ 2. ERNÄHRUNG ═══
@Composable
private fun NutritionCard(state: HomeState, vm: HomeViewModel) {
    NeoSectionLabel("Ernährung")
    NeoCard {
        PinnedNutrientCard(
            entries = state.pinnedKeys.filter { it != "water" }.map { k ->
                val tr = state.trendTotals.entries.sortedBy { it.key }.map { (_, t) -> trend(k, t) }
                when (k) {
                    "kcal"    -> PinnedNutrientEntry(k, state.totals.kcal.toDouble(), state.targets.kcal.toDouble(), tr)
                    "protein" -> PinnedNutrientEntry(k, state.totals.proteinG, state.targets.proteinG.toDouble(), tr)
                    "carbs"   -> PinnedNutrientEntry(k, state.totals.carbsG, state.targets.carbsG.toDouble(), tr)
                    "fat"     -> PinnedNutrientEntry(k, state.totals.fatG, state.targets.fatG.toDouble(), tr)
                    else      -> PinnedNutrientEntry(k, 0.0,
                        de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(k)?.defaultPerDay ?: 1.0, tr)
                }
            },
            pinnedKeys = state.pinnedKeys,
            expanded = state.pinsExpanded,
            onToggleExpanded = vm::togglePinsExpanded,
            onTogglePin = vm::togglePin,
        )
    }
}

// ═══ 3. WASSER ═══
@Composable
private fun WaterCard(state: HomeState, vm: HomeViewModel, hm: de.healthforge.presentation.theme.HmTokens) {
    NeoSectionLabel("Wasser")
    NeoCard {
        Column {
            WaterStageSlider(
                state.waterMl, state.waterGhostMl, state.targets.waterMl,
                state.waterReminderEnabled, vm::setWaterMl, vm::setWaterReminderEnabled,
            )
            val wv = state.waterTrend.entries.sortedBy { it.key }.map { it.value.toDouble() }
            if (wv.size >= 2) {
                val s = (state.waterMl / state.targets.waterMl.coerceAtLeast(1))
                Sparkline(wv, hm.ambientCyan, Modifier.fillMaxWidth().padding(top = 4.dp).height(22.dp), state.targets.waterMl.toDouble(), s)
            }
        }
    }
}

// ═══ 4. SUPPLEMENTE ═══
@Composable
private fun SupplementsCard(state: HomeState, vm: HomeViewModel) {
    NeoSectionLabel("Supplemente")
    NeoCard(contentPadding = PaddingValues(0.dp)) {
        SupplementChecklist(state.supplementChecklist, vm::markSupplementTaken)
    }
}

// ═══ 5. ÜBERSICHT ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewCard(
    state: HomeState, vm: HomeViewModel,
    hm: de.healthforge.presentation.theme.HmTokens, onOpenRecipe: (String) -> Unit,
) {
    NeoSectionLabel("Übersicht")
    if (state.plannedMeals.isEmpty()) {
        GlassCard { Text("Noch nichts geplant.", style = MaterialTheme.typography.bodyMedium, color = hm.fgSecondary) }
        return
    }
    state.plannedMeals.forEach { m ->
        PlannedEntry(m, state.recipeDtos, vm, hm, onOpenRecipe)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannedEntry(
    meal: PlannedMealInfo,
    recipeDtos: Map<String, RecipeListItemDto>,
    vm: HomeViewModel,
    hm: de.healthforge.presentation.theme.HmTokens,
    onOpenRecipe: (String) -> Unit,
) {
    val item = meal.item
    val isRecipe = item.sourceType == IntakeSourceType.RECIPE
    val dto = if (isRecipe) recipeDtos[item.sourceId] else null
    val sem = LocalSemanticColors.current

    val ds = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.StartToEnd) { vm.deletePlannedSlot(meal.slotId); true } else false
    })

    SwipeToDismissBox(
        state = ds,
        enableDismissFromStartToEnd = true, enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
                Icon(Icons.Filled.Delete, "Löschen", tint = StatusOverUl, modifier = Modifier.size(24.dp))
            }
        },
    ) {
        val toggle = @Composable {
            val label = "Gegessen"
            val on = meal.slotConsumed
            val txt = if (on) "✓ $label" else label
            val clr = if (on) sem.statusGood else hm.fgSecondary
            val w = if (on) FontWeight.SemiBold else FontWeight.Medium
            val mod = if (on) Modifier.background(sem.statusGood.copy(alpha = 0.12f)) else Modifier
            Text(txt, style = MaterialTheme.typography.labelSmall.copy(fontWeight = w), color = clr,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).then(mod))
        }

        if (dto != null) {
            RecipeCard(dto, { onOpenRecipe(dto.id) }, trailingActions = { toggle() })
        } else {
            val amount = "%.0f".format(item.amount)
            val unit = if (isRecipe) "Portion(en)" else "g"
            val kcal = ((item.snapshotKcalPer100g ?: 0.0) * item.amount / 100).toInt()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.snapshotName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                    Text("$amount $unit · $kcal kcal", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
                }
                toggle()
            }
        }
    }
}

private fun trend(k: String, t: de.healthforge.data.repository.DayNutrientTotals) = when (k) {
    "kcal" -> t.kcal; "protein" -> t.proteinG; "carbs" -> t.carbsG; "fat" -> t.fatG; else -> 0.0
}
