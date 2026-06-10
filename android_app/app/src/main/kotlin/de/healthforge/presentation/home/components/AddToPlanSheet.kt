package de.healthforge.presentation.home.components

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

/**
 * P7.S4 4b — Add-to-Plan Bottom Sheet.
 * Replaces QuickAddDialog. 3 Tabs: Lebensmittel → Rezepte → Supplements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlanSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    ingredientQuery: String,
    ingredientResults: List<IngredientDto>,
    ingredientLoading: Boolean,
    recipeResults: List<RecipeListItemDto>,
    recipeLoading: Boolean,
    supplementList: List<SupplementEntity>,
    onIngredientQuery: (String) -> Unit,
    onSelectIngredient: (IngredientDto) -> Unit,
    onSelectRecipe: (RecipeListItemDto) -> Unit,
    onSelectSupplement: (SupplementEntity) -> Unit,
    onConfirmIngredient: () -> Unit,
    selectedIngredient: IngredientDto?,
    portionGrams: String,
    onPortionChange: (String) -> Unit,
    onClearIngredient: () -> Unit,
) {
    if (!show) return

    val sheetState = rememberModalBottomSheetState()
    val hm = LocalHmTokens.current
    var tab by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Zum Plan hinzufügen", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = hm.fgPrimary)
            Spacer(Modifier.height(12.dp))
            SegmentedTabs(options = listOf("Lebensmittel", "Rezepte", "Supplements"), selectedIndex = tab, onSelect = { tab = it; q = "" })

            Spacer(Modifier.height(12.dp))
            if (tab != 2) {
                OutlinedTextField(value = q, onValueChange = { q = it; if (tab == 0) onIngredientQuery(it) },
                    label = { Text("Suchen…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))

            // ── Tab 0: Lebensmittel ──
            if (tab == 0) {
                if (ingredientLoading) { Text("Suchen…", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary) }
                else if (selectedIngredient != null) {
                    Text("Gewählt: ${selectedIngredient.name_de}", style = MaterialTheme.typography.bodyMedium, color = hm.fgPrimary)
                    TextButton(onClick = onClearIngredient) { Text("Andere auswählen") }
                    OutlinedTextField(value = portionGrams, onValueChange = onPortionChange,
                        label = { Text("Portion (g)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onConfirmIngredient, modifier = Modifier.fillMaxWidth(),
                        enabled = portionGrams.toDoubleOrNull()?.let { it > 0 } == true) { Text("Hinzufügen") }
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(ingredientResults, key = { it.id }) { ing ->
                            GlassCard(Modifier.fillMaxWidth().clickable { onSelectIngredient(ing) }, padding = PaddingValues(10.dp)) {
                                Text(ing.name_de, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                                Text("${(ing.energy_kcal_per_100g ?: 0.0).toInt()} kcal/100g", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
                            }
                        }
                    }
                }
            }

            // ── Tab 1: Rezepte ──
            if (tab == 1) {
                if (recipeLoading) Text("Suchen…", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
                else {
                    LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(recipeResults, key = { it.id }) { r ->
                            GlassCard(Modifier.fillMaxWidth().clickable { onSelectRecipe(r) }, padding = PaddingValues(10.dp)) {
                                Text(r.title, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                                Text(r.description ?: "", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
                            }
                        }
                    }
                }
            }

            // ── Tab 2: Supplements ──
            if (tab == 2) {
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(supplementList, key = { it.id }) { s ->
                        GlassCard(Modifier.fillMaxWidth().clickable { onSelectSupplement(s) }, padding = PaddingValues(10.dp)) {
                            Text(s.nameDe, fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                            Text("${s.defaultDose} ${s.unitLabel}", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
                        }
                    }
                }
            }
        }
    }
}
