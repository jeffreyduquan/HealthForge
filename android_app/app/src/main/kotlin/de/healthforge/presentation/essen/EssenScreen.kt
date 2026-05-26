package de.healthforge.presentation.essen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.healthforge.presentation.essen.rezepte.RecipesScreen
import de.healthforge.presentation.lebensmittel.LebensmittelScreen
import de.healthforge.presentation.supplements.SupplementsScreen
import de.healthforge.presentation.theme.AmbientBackdrop
import de.healthforge.presentation.theme.SegmentedTabs

/**
 * Essen-Tab mit drei Top-Sub-Tabs (REQ-NAV-002).
 * P6.S5: Visual auf Histamind-Design — AmbientBackdrop + SegmentedTabs (P6.S3-Component).
 */
@Composable
fun EssenScreen(
    onOpenSupplementEdit: (id: Long) -> Unit = {},
    onOpenRecipeDetail: (String) -> Unit = {},
    onCreateRecipe: () -> Unit = {},
    onSuggestIngredient: (initialName: String) -> Unit = {},
) {
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf("Lebensmittel", "Rezepte", "Supplements")

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientBackdrop()
        Column(modifier = Modifier.fillMaxSize()) {
            SegmentedTabs(
                options = tabs,
                selectedIndex = selected,
                onSelect = { selected = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            when (selected) {
                0 -> LebensmittelScreen(onSuggestIngredient = onSuggestIngredient)
                1 -> RecipesScreen(onOpenDetail = onOpenRecipeDetail, onCreate = onCreateRecipe)
                2 -> SupplementsScreen(onOpenEdit = onOpenSupplementEdit)
            }
        }
    }
}
