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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.LocalHmTokens
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            GradientFab(onClick = onCreate, size = 56.dp) {
                Icon(Icons.Filled.Add, contentDescription = "Rezept anlegen", tint = LocalHmTokens.current.fgPrimary)
            }
        },
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        HfSearchBar(
            query = state.query,
            onQueryChange = vm::setQuery,
            placeholder = "Rezepte suchen…",
            showFilterIcon = true,
            onFilterClick = { showFilters = true },
        )
        Spacer(Modifier.height(4.dp))

        when {
            state.isLoading && state.items.isEmpty() -> CenteredLoader()
            state.error != null && state.items.isEmpty() -> ErrorBlock(state.error!!) { vm.refresh() }
            state.items.isEmpty() -> EmptyBlock()
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onOpenDetail(recipe.id) },
                        pinnedNutrientKeys = de.healthforge.domain.nutrition.NutrientCatalog.defaultPinnedKeys)
                }
            }
        }
    }
    }

    if (showFilters) {
        HfFilterDialog(
            excludedAllergens = emptySet(),
            excludedFodmap = emptySet(),
            onToggleAllergen = {},
            onToggleFodmap = {},
            onDismiss = { showFilters = false },
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
        fat = recipe.fat_per_100g,
        fiber = recipe.fiber_per_100g,
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

/** Build MasterTileNutrient list from per-100g recipe values, filtered to pinned keys. */
internal fun buildNutrientRows(
    pinnedKeys: List<String>,
    kcal: Double?,
    protein: Double?,
    carbs: Double?,
    fat: Double?,
    fiber: Double?,
): List<MasterTileNutrient> {
    val rows = mutableListOf<MasterTileNutrient>()

    fun add(key: String, value: Double, unit: String, dgeDefault: Double) {
        if (pinnedKeys.isNotEmpty() && key !in pinnedKeys) return
        val pct = (value / dgeDefault) * 100.0
        rows.add(MasterTileNutrient(key, nutrientLabel(key), "${formatNutrientValue(value)} $unit", pct))
    }

    kcal?.let { add("kcal", it, "kcal", 2000.0) }
    protein?.let { add("protein", it, "g", 50.0) }
    carbs?.let { add("carbs", it, "g", 260.0) }
    fat?.let { add("fat", it, "g", 65.0) }
    fiber?.let { add("fiber", it, "g", 30.0) }

    return rows
}

private fun nutrientLabel(key: String): String = when (key) {
    "kcal" -> "Kalorien"
    "protein" -> "Eiweiß"
    "carbs" -> "Kohlenhydrate"
    "fat" -> "Fett"
    "fiber" -> "Ballaststoffe"
    else -> key
}

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
