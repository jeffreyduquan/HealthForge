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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Supplements-Liste. Zeigt lokale + öffentliche Supplements (REQ-SUPP-001/002/004).
 * Öffentliche Supplements werden vom Server geladen und können lokal übernommen werden.
 */
@Composable
fun SupplementsScreen(
    onOpenEdit: (id: Long) -> Unit,
    vm: SupplementsListViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenEdit(0L) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Neu") },
            )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = s.items, key = { it.id }) { sup ->
                    SupplementRow(
                        sup = sup,
                        onClick = {
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

@Composable
private fun SupplementRow(
    sup: SupplementDisplayItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAdopt: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sup.nameDe, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    if (!sup.isLocal) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Öffentlich", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
                sup.brand?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${sup.defaultDose} ${sup.unitLabel}" +
                        (sup.kcalPerDose?.let { " · ${it.toInt()} kcal" }.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (sup.isLocal) {
                IconButton(onClick = onClick) {
                    Text("Bearb.", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Löschen")
                }
            } else {
                IconButton(onClick = onAdopt) {
                    Icon(
                        Icons.Filled.CloudDownload,
                        contentDescription = "Übernehmen",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
