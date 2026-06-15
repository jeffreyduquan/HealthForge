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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
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
import de.healthforge.presentation.common.components.HfAddToHomeButton
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfDetailTopBar
import de.healthforge.presentation.common.components.HfNutrientProgressRow
import de.healthforge.presentation.common.components.HfRatingBar
import de.healthforge.presentation.common.components.HfSectionHeader
import de.healthforge.presentation.common.components.HfValueRow
import de.healthforge.presentation.common.components.formatNutrientValue
import de.healthforge.presentation.theme.LocalHmTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit = {},
    onAddedToPlan: () -> Unit = {},
    vm: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val currentUserId = vm.currentUserId
    var reportOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) onAddedToPlan()
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HfDetailTopBar(
                title = state.recipe?.title ?: "Rezept",
                onBack = onBack,
                actions = {
                    state.recipe?.let { r ->
                        if (!state.reportSubmitted) {
                            IconButton(onClick = { reportOpen = true }, enabled = !state.reportBusy) {
                                Icon(Icons.Filled.Flag, contentDescription = "Melden")
                            }
                        }
                        if (currentUserId != null && r.author_id == currentUserId) {
                            IconButton(onClick = { onEdit(r.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten")
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            HfAddToHomeButton(onClick = { vm.openAddToPlanDialog() })
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
                    onAddToGroup = vm::openAddToGroupDialog,
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
    // Group-Picker-Dialog: Rezept zu Gruppe hinzufügen
    state.myGroups?.let { groups ->
        AlertDialog(
            onDismissRequest = { vm.closeAddToGroupDialog() },
            title = { Text("Zu welcher Gruppe?") },
            text = {
                if (groups.isEmpty()) {
                    Text("Du bist in keiner Gruppe mit Verwaltungs-Rechten.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groups.forEach { g ->
                            OutlinedButton(
                                onClick = { vm.assignToGroup(g.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(g.name)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.closeAddToGroupDialog() }) { Text("Abbrechen") } },
        )
    }
    // Add-to-Plan Portion-Dialog
    if (state.showAddToPlan) {
        PortionInputDialog(
            onConfirm = { grams -> vm.addToPlan(grams) },
            onDismiss = { vm.dismissAddToPlanDialog() },
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
    onAddToGroup: () -> Unit = {},
) {
    val hm = LocalHmTokens.current
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
        // Visibility
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

        // ── Unified Rating Bar ──
        HfRatingBar(
            liked = recipe.liked_by_me,
            likeCount = recipe.like_count,
            likeBusy = likeBusy,
            onToggleLike = { if (!likeBusy) onToggleLike() },
            myCommunityRating = recipe.my_community_rating,
            recommendCount = recipe.community_recommend_count,
            notRecommendCount = recipe.community_not_recommend_count,
            onRate = onRate,
        )

        // ── DGE Progress Bars (per portion) ──
        val n = recipe.nutrition
        val servings = recipe.servings.coerceAtLeast(1)
        val perServing = { v: Double -> v / servings }
        HfSectionHeader("Tagesbedarf-Abdeckung (pro Portion)")
        HfCard {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HfNutrientProgressRow("Kalorien", "${perServing(n.energy_kcal).toInt()} kcal",
                    perServing(n.energy_kcal) / 2000.0 * 100)
                HfNutrientProgressRow("Eiweiß", "${formatNutrientValue(perServing(n.protein_g))} g",
                    perServing(n.protein_g) / 50.0 * 100)
                HfNutrientProgressRow("Kohlenhydrate", "${formatNutrientValue(perServing(n.carbs_g))} g",
                    perServing(n.carbs_g) / 260.0 * 100)
                HfNutrientProgressRow("Fett", "${formatNutrientValue(perServing(n.fat_g))} g",
                    perServing(n.fat_g) / 65.0 * 100)
                HfNutrientProgressRow("Ballaststoffe", "${formatNutrientValue(perServing(n.fiber_g))} g",
                    perServing(n.fiber_g) / 30.0 * 100)
            }
        }

        if (n.missing_ingredients.isNotEmpty()) {
            Text(
                "${n.missing_ingredients.size} Zutat(en) ohne Nährwert-Daten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // ── Allergens & FODMAP (aggregated from ingredients) ──
        if (recipe.allergens.isNotEmpty()) {
            HfSectionHeader("Enthält Allergene")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.allergens.forEach { allergen ->
                    AssistChip(onClick = {}, label = { Text(allergen) })
                }
            }
        }
        if (recipe.fodmap_flags.isNotEmpty()) {
            HfSectionHeader("FODMAP-Hinweise")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                recipe.fodmap_flags.forEach { flag ->
                    val label = runCatching {
                        de.healthforge.data.db.entities.FodmapType.valueOf(flag).germanLabel
                    }.getOrDefault(flag)
                    AssistChip(onClick = {}, label = { Text(label) })
                }
            }
        }

        // Zutaten
        HfSectionHeader("Zutaten")
        HfCard {
            Column(Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                recipe.ingredients.sortedBy { it.position }.forEach { ing ->
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

        // Schritte
        HfSectionHeader("Schritte")
        HfCard {
            Column(Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recipe.steps.sortedBy { it.position }.forEachIndexed { idx, step ->
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

        // "Zu Gruppe"-Button
        OutlinedButton(
            onClick = onAddToGroup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Filled.GroupAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Zu Gruppe hinzufügen")
        }

        Spacer(Modifier.height(80.dp)) // leave room for sticky bottom button
    }
}

@Composable
fun PortionInputDialog(
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    defaultValue: String = "100",
    unitLabel: String = "Gramm",
    title: String = "Portion",
) {
    var text by remember { mutableStateOf(defaultValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title ($unitLabel)") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) text = it },
                label = { Text(unitLabel) },
                singleLine = true,
            )
        },
        confirmButton = {
            val value = text.toDoubleOrNull()
            TextButton(
                onClick = { value?.let { onConfirm(it) } },
                enabled = value != null && value > 0,
            ) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
