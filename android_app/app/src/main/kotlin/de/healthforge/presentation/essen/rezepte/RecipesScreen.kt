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
import de.healthforge.presentation.common.components.HfSearchBar

private val SLOT_OPTIONS = listOf("BREAKFAST" to "Frühstück", "LUNCH" to "Mittag", "DINNER" to "Abend", "SNACK" to "Snack")

@Composable
fun RecipesScreen(
    onOpenDetail: (String) -> Unit,
    onCreate: () -> Unit = {},
    vm: RecipeBrowseViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        floatingActionButton = {
            GradientFab(onClick = onCreate, size = 48.dp) {
                Icon(Icons.Filled.Add, contentDescription = "Rezept anlegen", tint = LocalHmTokens.current.fgPrimary)
            }
        },
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        HfSearchBar(
            query = state.query,
            onQueryChange = vm::setQuery,
            placeholder = "Rezepte suchen…",
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(SLOT_OPTIONS) { (code, label) ->
                FilterChip(
                    selected = code in state.slotFilter,
                    onClick = { vm.toggleSlot(code) },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

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
                    RecipeCard(recipe = recipe, onClick = { onOpenDetail(recipe.id) })
                }
            }
        }
    }
    }
}

@Composable
fun RecipeCard(
    recipe: RecipeListItemDto,
    onClick: () -> Unit,
    trailingActions: @Composable RowScope.() -> Unit = {},
) {
    val hm = LocalHmTokens.current
    de.healthforge.presentation.common.components.HfCard(
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val thumbUrl = MediaRepository.imageUrl("recipes", recipe.image_key, variant = "thumb")
            if (thumbUrl != null) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = hm.fgPrimary,
                )
                recipe.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = hm.fgSecondary)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = hm.fgTertiary)
                    Spacer(Modifier.width(4.dp))
                    Text("${recipe.prep_minutes} min", style = MaterialTheme.typography.labelMedium, color = hm.fgSecondary)
                    Spacer(Modifier.width(12.dp))
                    recipe.slot_tags.firstOrNull()?.let {
                        Text(
                            text = humanSlot(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = hm.ambientViolet,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = hm.fgTertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(recipe.like_count.toString(), style = MaterialTheme.typography.labelMedium, color = hm.fgSecondary)
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = hm.fgTertiary)
                    Spacer(Modifier.width(4.dp))
                    Text(recipe.community_recommend_count.toString(), style = MaterialTheme.typography.labelMedium, color = hm.fgSecondary)
                }
            }
            trailingActions()
        }
    }
}

internal fun humanSlot(code: String): String = when (code) {
    "BREAKFAST" -> "Frühstück"
    "LUNCH" -> "Mittag"
    "DINNER" -> "Abend"
    "SNACK" -> "Snack"
    else -> code
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
