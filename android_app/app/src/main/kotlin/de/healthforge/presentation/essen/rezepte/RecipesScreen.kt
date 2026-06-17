package de.healthforge.presentation.essen.rezepte

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import de.healthforge.presentation.theme.LocalHmTokens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.repository.MediaRepository
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfFilterDialog
import de.healthforge.presentation.common.components.HfMasterTile
import de.healthforge.presentation.common.components.HfSearchBar
import de.healthforge.presentation.common.components.MasterTileNutrient
import de.healthforge.presentation.common.components.formatNutrientValue

private val SLOT_OPTIONS = listOf("BREAKFAST" to "Frühstück", "LUNCH" to "Mittag", "DINNER" to "Abend", "SNACK" to "Snack")

@Composable
fun RecipesScreen(
    onOpenDetail: (String) -> Unit,
    onCreate: () -> Unit = {},
    vm: RecipeBrowseViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val pinnedKeys by vm.pinnedKeys.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Search bar row: search field + filter + suggest button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HfSearchBar(
                query = state.query,
                onQueryChange = vm::setQuery,
                placeholder = "Rezepte suchen…",
                showFilterIcon = true,
                onFilterClick = { showFilters = true },
                filterCount = state.slotFilter.size + state.excludedAllergens.size + state.excludedFodmap.size + (if (state.applyProfileFilters) 1 else 0),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCreate) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "Rezept anlegen",
                    tint = LocalHmTokens.current.ambientViolet,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        when {
            state.isLoading && state.items.isEmpty() -> CenteredLoader()
            state.error != null && state.items.isEmpty() -> ErrorBlock(state.error!!) { vm.refresh() }
            state.items.isEmpty() -> EmptyBlock()
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onOpenDetail(recipe.id) },
                        pinnedNutrientKeys = pinnedKeys)
                }
            }
        }
    }

    if (showFilters) {
        HfFilterDialog(
            excludedAllergens = state.excludedAllergens,
            excludedFodmap = state.excludedFodmap,
            onToggleAllergen = vm::toggleAllergen,
            onToggleFodmap = vm::toggleFodmap,
            onDismiss = { showFilters = false },
            applyProfileFilters = state.applyProfileFilters,
            onToggleProfileFilters = vm::toggleApplyProfileFilters,
            slotOptions = SLOT_OPTIONS,
            selectedSlots = state.slotFilter,
            onToggleSlot = vm::toggleSlot,
        )
    }
}

@Composable
fun RecipeCard(
    recipe: RecipeListItemDto,
    onClick: () -> Unit,
    pinnedNutrientKeys: List<String> = emptyList(),
    trailingActions: (@Composable () -> Unit)? = null,
) {
    val thumbUrl = MediaRepository.imageUrl("recipes", recipe.image_key, variant = "thumb")
    val subtitle = buildString {
        append("${recipe.prep_minutes} min")
        append(" · ${recipe.servings} Portionen")
        recipe.slot_tags.firstOrNull()?.let { append(" · ${humanSlot(it)}") }
    }

    val nutrients = buildNutrientRows(
        pinnedKeys = pinnedNutrientKeys,
        kcal = recipe.kcal_per_100g,
        protein = recipe.protein_per_100g,
        carbs = recipe.carbs_per_100g,
        sugar = recipe.sugar_per_100g,
        fat = recipe.fat_per_100g,
        satfat = recipe.satfat_per_100g,
        fiber = recipe.fiber_per_100g,
        salt = recipe.salt_per_100g,
        micronutrients = recipe.micronutrients_per_100g,
    )

    HfMasterTile(
        title = recipe.title,
        subtitle = subtitle,
        imageUrl = thumbUrl,
        nutrients = nutrients,
        nutrientLabel = if (recipe.total_weight_grams != null) "PRO 100 G" else "PRO PORTION",
        likeCount = recipe.like_count,
        recommendCount = recipe.community_recommend_count,
        notRecommendCount = recipe.community_not_recommend_count,
        onClick = onClick,
        trailingSlot = trailingActions,
    )
}

internal fun humanSlot(code: String): String = when (code) {
    "BREAKFAST" -> "Frühstück"
    "LUNCH" -> "Mittag"
    "DINNER" -> "Abend"
    "SNACK" -> "Snack"
    else -> code
}

/** Build MasterTileNutrient list from per-100g recipe values, in pinnedKeys order. */
internal fun buildNutrientRows(
    pinnedKeys: List<String>,
    kcal: Double?,
    protein: Double?,
    carbs: Double?,
    sugar: Double?,
    fat: Double?,
    satfat: Double?,
    fiber: Double?,
    salt: Double?,
    micronutrients: Map<String, Double>?,
): List<MasterTileNutrient> {
    // Pre-compute value map
    val valueMap = mutableMapOf<String, Pair<Double, String>>()
    fun put(key: String, value: Double, unit: String) { valueMap[key] = value to unit }
    kcal?.let { put("kcal", it, "kcal") }
    protein?.let { put("protein", it, "g") }
    carbs?.let { put("carbs", it, "g") }
    sugar?.let { put("sugar", it, "g") }
    fat?.let { put("fat", it, "g") }
    satfat?.let { put("satfat", it, "g") }
    fiber?.let { put("fiber", it, "g") }
    salt?.let { put("salt", it, "g") }
    micronutrients?.entries?.forEach { (key, value) ->
        val nutrient = de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(key) ?: return@forEach
        put(key, value, nutrient.unit.label)
    }

    // Emit in pinnedKeys order
    return pinnedKeys.mapNotNull { key ->
        val (value, unit) = valueMap[key] ?: return@mapNotNull null
        val dge = de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(key)?.defaultPerDay ?: 1.0
        if (dge <= 0) return@mapNotNull null
        val pct = (value / dge) * 100.0
        val label = de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(key)?.displayDe ?: key
        MasterTileNutrient(key, label, "${formatNutrientValue(value)} $unit", pct)
    }
}

private fun nutrientLabel(key: String): String =
    de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(key)?.displayDe ?: key

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyBlock() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Noch keine Rezepte. Lege das erste an!", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorBlock(msg: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Fehler beim Laden", style = MaterialTheme.typography.titleSmall)
                Text(msg, style = MaterialTheme.typography.bodySmall)
                androidx.compose.material3.TextButton(onClick = onRetry) { Text("Erneut versuchen") }
            }
        }
    }
}
