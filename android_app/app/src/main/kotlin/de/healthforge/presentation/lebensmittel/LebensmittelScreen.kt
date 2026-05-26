package de.healthforge.presentation.lebensmittel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientDto

/**
 * Tab-Inhalt für den Essen → Lebensmittel-Sub-Tab. Kein eigenes Scaffold/TopAppBar
 * (kommt vom MainShell + EssenScreen). REQ-NAV-002.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LebensmittelScreen(
    vm: LebensmittelViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChanged,
                label = { Text("Suche (z. B. Apfel, Brot, Tomate)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showFilters = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }

            FilterChip(
                selected = state.applyProfileFilters,
                onClick = vm::toggleApplyProfileFilters,
                label = {
                    val n = state.excludedAllergens.size + state.excludedFodmap.size
                    Text(
                        if (state.applyProfileFilters)
                            "Profil-Filter aktiv ($n)"
                        else
                            "Profil-Filter aus",
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            when {
                state.loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.error != null -> Text(
                    text = "Fehler: ${state.error}",
                    modifier = Modifier.padding(16.dp),
                )

                state.query.isBlank() -> Text(
                    text = "Tippe einen Begriff ein, um Lebensmittel zu suchen.",
                    modifier = Modifier.padding(16.dp),
                )

                state.results.isEmpty() -> Text(
                    text = "Keine Treffer für \"${state.query}\".",
                    modifier = Modifier.padding(16.dp),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items = state.results, key = { it.id }) { item ->
                        IngredientRow(item)
                    }
                }
            }
    }

    if (showFilters) {
        FilterDialog(
            allergens = state.excludedAllergens,
            fodmap = state.excludedFodmap,
            onToggleAllergen = vm::toggleAllergen,
            onToggleFodmap = vm::toggleFodmap,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun IngredientRow(item: IngredientDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.name_de, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            item.brand?.let {
                Text(text = it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item.energy_kcal_per_100g?.let {
                    Text("${it.toInt()} kcal/100g", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                item.histamine_score?.let {
                    Text("Histamin: $it/3", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                Text("Quelle: ${item.source}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            if (item.allergens.isNotEmpty()) {
                Text(
                    text = "Enthält: " + item.allergens.joinToString(", "),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FilterDialog(
    allergens: Set<AllergenType>,
    fodmap: Set<FodmapType>,
    onToggleAllergen: (AllergenType) -> Unit,
    onToggleFodmap: (FodmapType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        title = { Text("Ausschluss-Filter") },
        text = {
            Column {
                Text("Allergene", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp),
                ) {
                    AllergenType.values().forEach { a ->
                        FilterChip(
                            selected = a in allergens,
                            onClick = { onToggleAllergen(a) },
                            label = { Text(a.germanLabel) },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }
                HorizontalDivider()
                Text(
                    "FODMAP",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp),
                ) {
                    FodmapType.values().forEach { f ->
                        FilterChip(
                            selected = f in fodmap,
                            onClick = { onToggleFodmap(f) },
                            label = { Text(f.germanLabel) },
                        )
                    }
                }
            }
        },
    )
}
