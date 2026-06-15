package de.healthforge.presentation.lebensmittel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientDto
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfRatingBar
import de.healthforge.presentation.common.components.HfFilterDialog
import de.healthforge.presentation.common.components.HfMasterTile
import de.healthforge.presentation.common.components.HfSearchBar
import de.healthforge.presentation.common.components.MasterTileNutrient
import de.healthforge.presentation.common.components.formatNutrientValue
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Tab-Inhalt für den Essen → Lebensmittel-Sub-Tab. Kein eigenes Scaffold/TopAppBar
 * (kommt vom MainShell + EssenScreen). REQ-NAV-002.
 *
 * Mehrere Modi:
 *  • Standard (preselect=false): Stöbern + Profil-Filter + „Korrektur vorschlagen".
 *  • Picker (preselect=true): Tippen auf Karte → [onPick]. Keine Korrektur-CTA.
 *
 * REQ-LIST-PRELOAD-001 (F-009): nach Filter-Hydration wird automatisch eine
 * alphabetische Voransicht geladen.
 * REQ-INGREDIENT-CREATE-WIZARD-001: „Neues Lebensmittel vorschlagen" navigiert
 * jetzt zum 4-Step-Wizard ([onSuggestIngredient]) — kein In-Screen-Dialog mehr.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LebensmittelScreen(
    preselect: Boolean = false,
    onPick: (IngredientDto) -> Unit = {},
    onSuggestIngredient: (initialName: String) -> Unit = {},
    onOpenIngredientDetail: (String) -> Unit = {},
    vm: LebensmittelViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pinnedKeys by vm.pinnedKeys.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    var showFilters by remember { mutableStateOf(false) }
    var fieldPrTarget by remember { mutableStateOf<IngredientDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearToast()
        }
    }

    // Load ingredient ratings on first composition
    LaunchedEffect(Unit) { vm.refreshRatings() }

    Scaffold(
        floatingActionButton = {
            GradientFab(
                onClick = { onSuggestIngredient(state.query) },
                size = 56.dp,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Lebensmittel vorschlagen", tint = hm.fgPrimary)
            }
        },
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
    ) {
        HfSearchBar(
            query = state.query,
            onQueryChange = vm::onQueryChanged,
            placeholder = "Apfel, Brot, Tomate…",
            showFilterIcon = true,
            onFilterClick = { showFilters = true },
        )

        // Filter row removed — everything in filter dialog
        Spacer(Modifier.height(4.dp))

        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Text("Fehler: ${state.error}", modifier = Modifier.padding(16.dp))
            state.results.isEmpty() -> Text(
                if (state.query.isBlank()) "Keine Lebensmittel verfügbar." else "Keine Treffer für \"${state.query}\".",
                modifier = Modifier.padding(16.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.results, key = { it.id }) { item ->
                    IngredientRow(
                        item = item, preselect = preselect,
                        isLiked = state.likedIngredientIds.contains(item.id),
                        isDisliked = state.dislikedIngredientIds.contains(item.id),
                        onPick = { onPick(item) },
                        onOpenDetail = { onOpenIngredientDetail(item.id) },
                        onCorrect = { fieldPrTarget = item },
                        onToggleLike = { vm.toggleLikeIngredient(item.id) },
                        onToggleDislike = { vm.toggleDislikeIngredient(item.id) },
                        pinnedKeys = pinnedKeys,
                    )
                }
            }
        }
    }
    } // Scaffold

    if (showFilters) {
        HfFilterDialog(
            excludedAllergens = state.excludedAllergens, excludedFodmap = state.excludedFodmap,
            onToggleAllergen = vm::toggleAllergen, onToggleFodmap = vm::toggleFodmap,
            onDismiss = { showFilters = false },
            applyProfileFilters = state.applyProfileFilters,
            onToggleProfileFilters = vm::toggleApplyProfileFilters,
        )
    }
    fieldPrTarget?.let { target ->
        FieldPrDialog(
            ingredientId = target.id, ingredientName = target.name_de,
            onDismiss = { fieldPrTarget = null },
            onSubmit = { id, req -> vm.submitFieldPr(id, req); fieldPrTarget = null },
        )
    }
    SnackbarHost(hostState = snackbarHostState)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun IngredientRow(
    item: IngredientDto,
    preselect: Boolean,
    isLiked: Boolean,
    isDisliked: Boolean,
    onPick: () -> Unit,
    onOpenDetail: () -> Unit,
    onCorrect: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    pinnedKeys: List<String> = emptyList(),
) {
    val hm = LocalHmTokens.current
    val subtitle = buildString {
        item.brand?.takeIf { it.isNotBlank() }?.let { append(it); append(" · ") }
        item.energy_kcal_per_100g?.let { append("${it.toInt()} kcal/100g") }
    }

    val nutrients = buildIngredientNutrientRows(
        item = item,
        pinnedKeys = pinnedKeys,
    )

    HfMasterTile(
        title = item.name_de,
        subtitle = subtitle.ifBlank { item.source },
        sourceBadge = item.source,
        nutrients = nutrients,
        nutrientLabel = "PRO 100 G",
        liked = isLiked,
        onToggleLike = if (!preselect) onToggleLike else null,
        onClick = if (preselect) onPick else onOpenDetail,
        trailingSlot = if (!preselect) {
            { TextButton(onClick = onCorrect) { Text("Korrektur") } }
        } else null,
    )
}

/** Build MasterTileNutrient list from IngredientDto, filtered to pinned keys. */
private fun buildIngredientNutrientRows(
    item: IngredientDto,
    pinnedKeys: List<String>,
): List<MasterTileNutrient> {
    val rows = mutableListOf<MasterTileNutrient>()

    fun add(key: String, value: Double, unit: String, dgeDefault: Double) {
        if (pinnedKeys.isNotEmpty() && key !in pinnedKeys) return
        val pct = (value / dgeDefault) * 100.0
        val label = when (key) {
            "kcal" -> "Kalorien"
            "protein" -> "Eiweiß"
            "carbs" -> "Kohlenhydrate"
            "fat" -> "Fett"
            "fiber" -> "Ballaststoffe"
            else -> key
        }
        rows.add(MasterTileNutrient(key, label, "${formatNutrientValue(value)} $unit", pct))
    }

    item.energy_kcal_per_100g?.let { add("kcal", it, "kcal", 2000.0) }
    item.protein_g_per_100g?.let { add("protein", it, "g", 50.0) }
    item.carbs_g_per_100g?.let { add("carbs", it, "g", 260.0) }
    item.fat_g_per_100g?.let { add("fat", it, "g", 65.0) }
    item.fiber_g_per_100g?.let { add("fiber", it, "g", 30.0) }

    // Micronutrients with DGE
    item.micronutrients.entries.forEach { (key, value) ->
        val nutrient = de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(key) ?: return@forEach
        val dge = nutrient.defaultPerDay
        if (dge > 0) {
            add(key, value, nutrient.unit.label, dge)
        }
    }

    rows.sortByDescending { it.percentDge }
    return rows
}
