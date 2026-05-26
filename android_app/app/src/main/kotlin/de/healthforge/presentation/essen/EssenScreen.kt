package de.healthforge.presentation.essen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.healthforge.presentation.lebensmittel.LebensmittelScreen
import de.healthforge.presentation.supplements.SupplementsScreen

/**
 * Essen-Tab mit drei Top-Sub-Tabs (REQ-NAV-002).
 * P1: nur Lebensmittel funktional, Rezepte/Supplements Placeholder (REQ-NAV-003).
 */
@Composable
fun EssenScreen(
    onOpenSupplementEdit: (id: Long) -> Unit = {},
) {
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf("Lebensmittel", "Rezepte", "Supplements")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selected) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selected == i,
                    onClick = { selected = i },
                    text = { Text(label) },
                )
            }
        }
        when (selected) {
            0 -> LebensmittelScreen()
            1 -> SubTabPlaceholder("Rezepte", "Rezept-Engine kommt in P2.")
            2 -> SupplementsScreen(onOpenEdit = onOpenSupplementEdit)
        }
    }
}

@Composable
private fun SubTabPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
