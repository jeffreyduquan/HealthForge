package de.healthforge.presentation.lebensmittel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.data.network.IngredientDto
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.common.components.HfAddToHomeButton
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfDetailTopBar
import de.healthforge.presentation.common.components.HfNutrientProgressRow
import de.healthforge.presentation.common.components.HfRatingBar
import de.healthforge.presentation.common.components.HfSectionHeader
import de.healthforge.presentation.common.components.HfSourceBadge
import de.healthforge.presentation.common.components.formatNutrientValue
import de.healthforge.presentation.essen.rezepte.PortionInputDialog
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Full-screen ingredient detail — P7.S5 Consistency Refactor.
 * Uses unified Hf* components. Shows DGE progress bars for all nutrients.
 * Like/Dislike managed by ViewModel via IngredientRatingStore.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientDetailScreen(
    ingredientId: String,
    onBack: () -> Unit,
    onAddedToPlan: () -> Unit = {},
    onToggleLike: (String) -> Unit = {},
    onToggleDislike: (String) -> Unit = {},
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    vm: IngredientDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val hm = LocalHmTokens.current

    LaunchedEffect(ingredientId) { vm.load(ingredientId) }

    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) onAddedToPlan()
    }

    Scaffold(
        topBar = {
            HfDetailTopBar(
                title = state.item?.name_de ?: "Lebensmittel",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { vm.openAddToPlanDialog() }) {
                        Icon(
                            Icons.Filled.PlaylistAdd,
                            contentDescription = "Zum Plan hinzufügen",
                            tint = hm.fgPrimary,
                        )
                    }
                },
            )
        },
        containerColor = hm.background,
        bottomBar = {
            HfAddToHomeButton(onClick = { vm.openAddToPlanDialog() })
        },
    ) { padding ->
        val item = state.item
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        if (item != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Brand
                item.brand?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = hm.fgSecondary)
                }

                // Source badge
                HfSourceBadge(source = item.source, fdcId = item.fdc_id)

                // Rating bar (Like/Dislike — managed by ViewModel)
                HfRatingBar(
                    liked = state.isLiked,
                    onToggleLike = { vm.toggleLike() },
                )

                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.3f))

                // ── TAGESBEDARF-ABDECKUNG (DGE Progress Bars) ──
                val dgeRows = buildDgeRows(item)
                if (dgeRows.isNotEmpty()) {
                    HfSectionHeader("Tagesbedarf-Abdeckung")
                    HfCard {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            dgeRows.forEach { (label, value, pct) ->
                                HfNutrientProgressRow(
                                    label = label,
                                    value = value,
                                    percentDge = pct,
                                )
                            }
                        }
                    }
                }

                // Allergens
                if (item.allergens.isNotEmpty()) {
                    HfSectionHeader("Allergene")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.allergens.forEach { allergen ->
                            AssistChip(
                                onClick = {},
                                label = { Text(allergen) },
                                colors = AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }

                // FODMAP
                if (item.fodmap_flags.isNotEmpty()) {
                    HfSectionHeader("FODMAP")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.fodmap_flags.forEach { flag ->
                            val label = runCatching {
                                de.healthforge.data.db.entities.FodmapType.valueOf(flag).germanLabel
                            }.getOrDefault(flag)
                            AssistChip(
                                onClick = {},
                                label = { Text(label) },
                                colors = AssistChipDefaults.assistChipColors(),
                            )
                        }
                    }
                }

                // Histamin
                item.histamine_score?.let { score ->
                    HfSectionHeader("Histamin (SIGHI)")
                    Text(
                        "$score / 3",
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            score <= 1 -> de.healthforge.presentation.theme.StatusGood
                            score == 2 -> hm.fgSecondary
                            else -> de.healthforge.presentation.theme.StatusOverUl
                        },
                    )
                }

                Spacer(Modifier.height(80.dp)) // leave room for sticky bottom button
            }
        }

        if (state.error != null) {
            Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }
    }

    if (state.showAddToPlan) {
        PortionInputDialog(
            onConfirm = { grams -> vm.addToPlan(grams) },
            onDismiss = { vm.dismissAddToPlanDialog() },
        )
    }
}

/**
 * Build list of (label, value, percentDge) for all nutrients that have DGE references.
 * Sorted by % DGE descending so most impactful nutrients appear first.
 */
private fun buildDgeRows(item: IngredientDto): List<Triple<String, String, Double>> {
    val rows = mutableListOf<Triple<String, String, Double>>()

    fun add(key: String, value: Double, unit: String) {
        val nutrient = NutrientCatalog.byKeyOrNull(key) ?: return
        val dge = nutrient.defaultPerDay
        if (dge <= 0) return
        val pct = (value / dge) * 100.0
        rows.add(Triple(nutrient.displayDe, "${formatNutrientValue(value)} $unit", pct))
    }

    item.energy_kcal_per_100g?.let { add("kcal", it, "kcal") }
    item.protein_g_per_100g?.let { add("protein", it, "g") }
    item.carbs_g_per_100g?.let { add("carbs", it, "g") }
    item.fat_g_per_100g?.let { add("fat", it, "g") }
    item.fiber_g_per_100g?.let { add("fiber", it, "g") }

    item.micronutrients.entries.forEach { (key, value) ->
        val nutrient = NutrientCatalog.byKeyOrNull(key) ?: return@forEach
        val dge = nutrient.defaultPerDay
        if (dge > 0) {
            val pct = (value / dge) * 100.0
            rows.add(Triple(nutrient.displayDe, "${formatNutrientValue(value)} ${nutrient.unit.label}", pct))
        }
    }

    // Sort: highest % DGE first
    rows.sortByDescending { it.third }
    return rows
}
