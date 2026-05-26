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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import de.healthforge.data.network.RecipeDetailDto
import de.healthforge.data.network.RecipeIngredientDto
import de.healthforge.data.network.RecipeNutritionDto
import de.healthforge.data.network.RecipeStepDto
import de.healthforge.data.repository.MediaRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit = {},
    vm: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var reportOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.recipe?.title ?: "Rezept") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    state.recipe?.let { r ->
                        if (!state.reportSubmitted) {
                            IconButton(onClick = { reportOpen = true }, enabled = !state.reportBusy) {
                                Icon(Icons.Filled.Flag, contentDescription = "Melden")
                            }
                        }
                        IconButton(onClick = { onEdit(r.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null && state.recipe == null -> Box(
                    Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center,
                ) { Text("Fehler: ${state.error}") }
                state.recipe != null -> DetailContent(
                    recipe = state.recipe!!,
                    likeBusy = state.likeBusy,
                    onToggleLike = vm::toggleLike,
                    onRate = vm::rate,
                )
            }
        }
    }
    if (reportOpen) {
        ReportRecipeDialog(
            busy = state.reportBusy,
            onSubmit = { reason ->
                vm.report(reason)
                reportOpen = false
            },
            onDismiss = { reportOpen = false },
        )
    }
}

@Composable
private fun ReportRecipeDialog(
    busy: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rezept melden") },
        text = {
            Column {
                Text(
                    "Bitte beschreibe kurz das Problem (z.B. Spam, falsche Angaben, gefährlich).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 500) reason = it },
                    label = { Text("Grund") },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason) },
                enabled = !busy && reason.trim().length >= 3,
            ) { Text("Melden") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun DetailContent(
    recipe: RecipeDetailDto,
    likeBusy: Boolean,
    onToggleLike: () -> Unit,
    onRate: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MediaRepository.imageUrl("recipes", recipe.image_key, variant = "medium")?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
        // Meta-Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            recipe.slot_tags.firstOrNull()?.let {
                AssistChip(onClick = {}, label = { Text(humanSlot(it)) })
                Spacer(Modifier.width(8.dp))
            }
            Text("${recipe.prep_minutes} min Prep", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Text("· ${recipe.servings} Portionen", style = MaterialTheme.typography.bodyMedium)
        }
        // Visibility / Group-Origin (REQ-GROUP-006)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val visLabel = when (recipe.visibility) {
                "PUBLIC" -> "Allgemein"
                "PRIVATE" -> "Privat"
                "GROUP" -> "Gruppe"
                else -> recipe.visibility
            }
            AssistChip(onClick = {}, label = { Text(visLabel) })
        }

        recipe.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        // Like + Community-Rating
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = recipe.liked_by_me,
                onClick = { if (!likeBusy) onToggleLike() },
                leadingIcon = {
                    Icon(
                        if (recipe.liked_by_me) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                    )
                },
                label = { Text("Gefällt mir · ${recipe.like_count}") },
            )
            Spacer(Modifier.width(8.dp))
            CommunityRatingPill(
                myValue = recipe.my_community_rating,
                recommendCount = recipe.community_recommend_count,
                notRecommendCount = recipe.community_not_recommend_count,
                onRate = onRate,
            )
        }

        NutritionCard(recipe.nutrition, recipe.servings)
        IngredientsCard(recipe.ingredients)
        StepsCard(recipe.steps)
    }
}

@Composable
private fun CommunityRatingPill(
    myValue: String?,
    recommendCount: Long,
    notRecommendCount: Long,
    onRate: (String?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = myValue == "RECOMMEND",
            onClick = { onRate(if (myValue == "RECOMMEND") null else "RECOMMEND") },
            leadingIcon = { Icon(Icons.Filled.ThumbUp, contentDescription = null) },
            label = { Text(recommendCount.toString()) },
        )
        FilterChip(
            selected = myValue == "NOT_RECOMMEND",
            onClick = { onRate(if (myValue == "NOT_RECOMMEND") null else "NOT_RECOMMEND") },
            leadingIcon = { Icon(Icons.Filled.ThumbDown, contentDescription = null) },
            label = { Text(notRecommendCount.toString()) },
        )
    }
}

@Composable
private fun NutritionCard(n: RecipeNutritionDto, servings: Int) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Nährwerte (gesamt)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            NutriRow("Kalorien", "${n.energy_kcal.toInt()} kcal")
            NutriRow("Protein", "${"%.1f".format(n.protein_g)} g")
            NutriRow("Kohlenhydrate", "${"%.1f".format(n.carbs_g)} g")
            NutriRow("Fett", "${"%.1f".format(n.fat_g)} g")
            NutriRow("Ballaststoffe", "${"%.1f".format(n.fiber_g)} g")
            if (n.missing_ingredients.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${n.missing_ingredients.size} Zutat(en) ohne Nährwert-Daten",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (servings > 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pro Portion: ${(n.energy_kcal / servings).toInt()} kcal",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun NutriRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IngredientsCard(items: List<RecipeIngredientDto>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Zutaten", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            items.sortedBy { it.position }.forEach { ing ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"%g".format(ing.quantity)} ${ing.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(96.dp),
                    )
                    Text(
                        text = ing.ingredient_name ?: ing.ingredient_id.take(8),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (ing.is_optional) {
                        Spacer(Modifier.width(4.dp))
                        Text("(optional)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsCard(steps: List<RecipeStepDto>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Schritte", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            steps.sortedBy { it.position }.forEachIndexed { idx, step ->
                Row {
                    Text(
                        "${idx + 1}.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(step.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
