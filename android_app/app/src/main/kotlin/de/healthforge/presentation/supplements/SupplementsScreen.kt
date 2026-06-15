package de.healthforge.presentation.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfMasterTile
import de.healthforge.presentation.common.components.HfSearchBar
import de.healthforge.presentation.common.components.MasterTileNutrient
import de.healthforge.presentation.common.components.formatNutrientValue
import de.healthforge.presentation.theme.GradientFab
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Supplements-Liste. Zeigt lokale + öffentliche Supplements (REQ-SUPP-001/002/004).
 * Öffentliche Supplements werden vom Server geladen und können lokal übernommen werden.
 */
@Composable
fun SupplementsScreen(
    onOpenEdit: (id: Long) -> Unit,
    onOpenDetail: (id: String) -> Unit = {},
    vm: SupplementsListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val filtered = remember(s.items, query) {
        if (query.isBlank()) s.items
        else s.items.filter { it.nameDe.contains(query, ignoreCase = true) || (it.brand?.contains(query, ignoreCase = true) == true) }
    }

    Scaffold(
        floatingActionButton = {
            GradientFab(
                onClick = { onOpenEdit(0L) },
                size = 56.dp,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Supplement anlegen", tint = LocalHmTokens.current.fgPrimary)
            }
        },
    ) { padding ->
        if (s.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (s.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Noch keine Supplements", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tippe \u201eNeu\u201c, um dein erstes Supplement anzulegen.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                HfSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Supplements suchen…",
                )
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (query.isNotBlank()) "Keine Treffer für \"$query\""
                            else "Noch keine Supplements",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = filtered, key = { it.id }) { sup ->
                            SupplementRow(
                                sup = sup,
                                onClick = {
                                    if (sup.isLocal) {
                                        onOpenDetail(sup.localId.toString())
                                    } else {
                                        sup.publicServerId?.let { vm.adoptPublic(it) }
                                    }
                                },
                                onEdit = {
                                    if (sup.isLocal) onOpenEdit(sup.localId)
                                },
                                onDelete = {
                                    if (sup.isLocal) vm.delete(sup.localId)
                                },
                                onAdopt = {
                                    sup.publicServerId?.let { vm.adoptPublic(it) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplementRow(
    sup: SupplementDisplayItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdopt: () -> Unit,
) {
    val subtitle = buildString {
        append("${sup.defaultDose} ${sup.unitLabel}")
        sup.kcalPerDose?.let { append(" · ${it.toInt()} kcal") }
    }

    val pinnedKeys = de.healthforge.domain.nutrition.NutrientCatalog.defaultPinnedKeys
    val nutrients = buildList {
        if ("protein" in pinnedKeys) sup.proteinPerDose?.let { add(MasterTileNutrient("protein", "Eiweiß", "${formatNutrientValue(it)} g", it / 50.0 * 100)) }
        if ("carbs" in pinnedKeys) sup.carbsPerDose?.let { add(MasterTileNutrient("carbs", "Kohlenhydrate", "${formatNutrientValue(it)} g", it / 260.0 * 100)) }
        if ("fat" in pinnedKeys) sup.fatPerDose?.let { add(MasterTileNutrient("fat", "Fett", "${formatNutrientValue(it)} g", it / 65.0 * 100)) }
    }

    HfMasterTile(
        title = sup.nameDe,
        subtitle = subtitle,
        sourceBadge = if (!sup.isLocal) "ÖFFENTLICH" else null,
        nutrients = nutrients,
        nutrientLabel = "PRO DOSIS",
        onClick = if (sup.isLocal) onClick else null,
        trailingSlot = {
            if (sup.isLocal) {
                TextButton(onClick = onEdit) { Text("Bearb.") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = LocalHmTokens.current.fgTertiary)
                }
            } else {
                IconButton(onClick = onAdopt) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = "Übernehmen", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
    )
}
