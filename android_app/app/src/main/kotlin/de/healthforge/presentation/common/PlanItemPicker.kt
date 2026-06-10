package de.healthforge.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.data.db.entities.SupplementEntity
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.presentation.theme.GlassCard
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.SegmentedTabs

/** Data class holding picker search results. Mirrors PlanViewModel.PickerState. */
data class PickerData(
    val recipes: List<RecipeListItemDto> = emptyList(),
    val ingredients: List<IngredientDto> = emptyList(),
)

/**
 * Shared item picker used by both PlanScreen (via ModalBottomSheet) and
 * HomeScreen (via ModalBottomSheet). Supports 3 tabs: Rezepte, Lebensmittel, Supplements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanItemPicker(
    show: Boolean,
    onDismiss: () -> Unit,
    pickerData: PickerData,
    onSearchRecipes: (String) -> Unit,
    onSearchIngredients: (String) -> Unit,
    onSelectRecipe: (RecipeListItemDto) -> Unit,
    onSelectIngredient: (IngredientDto) -> Unit,
    onSelectSupplement: ((SupplementEntity) -> Unit)? = null,
    onClearPicker: () -> Unit,
    supplementList: List<SupplementEntity> = emptyList(),
) {
    if (!show) return
    val sheetState = rememberModalBottomSheetState()
    val hm = LocalHmTokens.current
    var tab by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Rezept oder Lebensmittel", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = hm.fgPrimary)
            Spacer(Modifier.height(10.dp))
            SegmentedTabs(
                options = if (onSelectSupplement != null) listOf("Rezepte", "Lebensmittel", "Supplements")
                          else listOf("Rezepte", "Lebensmittel"),
                selectedIndex = tab,
                onSelect = { tab = it; q = ""; onClearPicker() },
            )
            Spacer(Modifier.height(12.dp))
            if (tab != 2) {
                OutlinedTextField(value = q, onValueChange = {
                    q = it
                    if (tab == 0) onSearchRecipes(it) else onSearchIngredients(it)
                }, label = { Text("Suchen…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> items(pickerData.recipes, key = { it.id }) { r ->
                        GlassCard(Modifier.fillMaxWidth().clickable { onSelectRecipe(r) }, padding = PaddingValues(12.dp)) {
                            Text(r.title, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                            Text("${r.prep_minutes} min", style = MaterialTheme.typography.labelSmall, color = hm.fgSecondary)
                        }
                    }
                    1 -> items(pickerData.ingredients, key = { it.id }) { ing ->
                        GlassCard(Modifier.fillMaxWidth().clickable { onSelectIngredient(ing) }, padding = PaddingValues(12.dp)) {
                            Text(ing.name_de, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                            ing.energy_kcal_per_100g?.let { Text("${it.toInt()} kcal / 100g", style = MaterialTheme.typography.labelSmall, color = hm.fgSecondary) }
                        }
                    }
                    2 -> {
                        val sel = onSelectSupplement
                        if (sel != null) {
                            items(supplementList, key = { it.id }) { s ->
                                GlassCard(Modifier.fillMaxWidth().clickable { sel(s) }, padding = PaddingValues(12.dp)) {
                                    Text(s.nameDe, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                                    Text("${s.defaultDose} ${s.unitLabel}", style = MaterialTheme.typography.labelSmall, color = hm.fgSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
