package de.healthforge.presentation.lebensmittel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import de.healthforge.presentation.essen.rezepte.PortionInputDialog
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.NeoCard
import de.healthforge.presentation.theme.NeoSectionLabel
import de.healthforge.presentation.theme.SectionPill

/**
 * Full-screen ingredient detail — styled like RecipeDetailScreen.
 * Replaces the old [IngredientDetailSheet] ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientDetailScreen(
    ingredientId: String,
    onBack: () -> Unit,
    onToggleLike: (String) -> Unit = {},
    onToggleDislike: (String) -> Unit = {},
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    vm: IngredientDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val hm = LocalHmTokens.current

    LaunchedEffect(ingredientId) { vm.load(ingredientId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.name_de ?: "Lebensmittel", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.openAddToPlanDialog() }) {
                        Icon(
                            Icons.Filled.PlaylistAdd,
                            contentDescription = "Zum Plan hinzufügen",
                            tint = hm.fgPrimary,
                        )
                    }
                    IconButton(onClick = { onToggleLike(ingredientId) }) {
                        Icon(
                            Icons.Filled.ThumbUp,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else hm.fgTertiary,
                        )
                    }
                    IconButton(onClick = { onToggleDislike(ingredientId) }) {
                        Icon(
                            Icons.Filled.ThumbDown,
                            contentDescription = "Nicht empfehlen",
                            tint = if (isDisliked) MaterialTheme.colorScheme.error else hm.fgTertiary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hm.background,
                    titleContentColor = hm.fgPrimary,
                ),
            )
        },
        containerColor = hm.background,
    ) { padding ->
        val item = state.item
        if (state.loading) {
            // loading indicator handled by ViewModel state
        }
        if (item != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Brand + Source
                item.brand?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = hm.fgSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionPill(label = item.source)
                    item.fdc_id?.let { fdc ->
                        Spacer(Modifier.width(8.dp))
                        Text("#$fdc", style = MaterialTheme.typography.labelSmall, color = hm.fgTertiary)
                    }
                }

                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.3f))

                // Macros per 100g
                NeoSectionLabel("Nährwerte pro 100 g")
                NeoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.energy_kcal_per_100g?.let {
                            MacroRow("Kalorien", "${it.toInt()} kcal")
                        }
                        item.protein_g_per_100g?.let {
                            MacroRow("Eiweiß", "${formatNum(it)} g")
                        }
                        item.carbs_g_per_100g?.let {
                            MacroRow("Kohlenhydrate", "${formatNum(it)} g")
                        }
                        item.sugar_g_per_100g?.let {
                            MacroRow("  davon Zucker", "${formatNum(it)} g")
                        }
                        item.fat_g_per_100g?.let {
                            MacroRow("Fett", "${formatNum(it)} g")
                        }
                        item.satfat_g_per_100g?.let {
                            MacroRow("  davon gesättigt", "${formatNum(it)} g")
                        }
                        item.fiber_g_per_100g?.let {
                            MacroRow("Ballaststoffe", "${formatNum(it)} g")
                        }
                        item.salt_g_per_100g?.let {
                            MacroRow("Salz", "${formatNum(it)} g")
                        }
                    }
                }

                // Micronutrients
                if (item.micronutrients.isNotEmpty()) {
                    NeoSectionLabel("Mikronährstoffe")
                    NeoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            item.micronutrients.entries.forEach { (key, value) ->
                                val nutrient = de.healthforge.domain.nutrition.NutrientCatalog.byKeyOrNull(key)
                                val label = nutrient?.displayDe ?: key
                                val unitLabel = nutrient?.unit?.label ?: ""
                                val dge = nutrient?.defaultPerDay
                                val pct = if (dge != null && dge > 0) " ${(value / dge * 100).toInt()} %" else ""
                                MacroRow(label, "${formatNum(value)} $unitLabel$pct")
                            }
                        }
                    }
                }

                // Allergens
                if (item.allergens.isNotEmpty()) {
                    NeoSectionLabel("Allergene")
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
                    NeoSectionLabel("FODMAP")
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
                    NeoSectionLabel("Histamin (SIGHI)")
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

                Spacer(Modifier.height(32.dp))
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

@Composable
private fun MacroRow(label: String, value: String) {
    val hm = LocalHmTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = hm.fgSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
    }
}

/** Smart rounding: >=100 → int, >=10 → 1 decimal, <10 → 2 decimals. */
private fun formatNum(v: Double): String = when {
    v >= 100 -> v.toInt().toString()
    v >= 10 -> "%.1f".format(v)
    else -> "%.2f".format(v)
}
