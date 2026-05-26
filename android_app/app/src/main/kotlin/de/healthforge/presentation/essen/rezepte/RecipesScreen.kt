package de.healthforge.presentation.essen.rezepte

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
import androidx.compose.material3.FloatingActionButton
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
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = "Rezept anlegen")
            }
        },
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Rezepte suchen") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
private fun RecipeCard(recipe: RecipeListItemDto, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumbUrl = MediaRepository.imageUrl("recipes", recipe.image_key, variant = "thumb")
            if (thumbUrl != null) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            recipe.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${recipe.prep_minutes} min", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(16.dp))
                recipe.slot_tags.firstOrNull()?.let {
                    Text(
                        text = humanSlot(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(recipe.like_count.toString(), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(recipe.community_recommend_count.toString(), style = MaterialTheme.typography.labelMedium)
            }
            }
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
