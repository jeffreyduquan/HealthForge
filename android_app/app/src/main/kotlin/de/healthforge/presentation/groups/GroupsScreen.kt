package de.healthforge.presentation.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.healthforge.data.network.GroupSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onOpenGroup: (String) -> Unit,
    vm: GroupsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var showJoinCode by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gruppen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Gruppe erstellen")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            TabRow(selectedTabIndex = state.tab) {
                Tab(selected = state.tab == 0, onClick = { vm.setTab(0) }, text = { Text("Meine") })
                Tab(selected = state.tab == 1, onClick = { vm.setTab(1) }, text = { Text("Entdecken") })
            }

            when (state.tab) {
                0 -> MyGroupsTab(
                    items = state.mine,
                    isLoading = state.isLoadingMine,
                    onOpenGroup = onOpenGroup,
                    onJoinByCode = { showJoinCode = true },
                )
                1 -> DiscoverTab(
                    items = state.discover,
                    query = state.discoverQuery,
                    isLoading = state.isLoadingDiscover,
                    onQueryChange = vm::setDiscoverQuery,
                    onSearch = vm::refreshDiscover,
                    onOpenGroup = onOpenGroup,
                    onJoinPublic = vm::joinPublic,
                )
            }
        }
    }

    if (showCreate) {
        CreateGroupDialog(
            onDismiss = { showCreate = false },
            onConfirm = { name, desc, vis ->
                vm.createGroup(name, desc, vis) { g ->
                    showCreate = false
                    onOpenGroup(g.id)
                }
            },
        )
    }
    if (showJoinCode) {
        JoinByCodeDialog(
            onDismiss = { showJoinCode = false },
            onConfirm = { code ->
                vm.joinByCode(code) { g ->
                    showJoinCode = false
                    onOpenGroup(g.id)
                }
            },
        )
    }
}

@Composable
private fun MyGroupsTab(
    items: List<GroupSummaryDto>,
    isLoading: Boolean,
    onOpenGroup: (String) -> Unit,
    onJoinByCode: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onJoinByCode, modifier = Modifier.fillMaxWidth()) {
            Text("Beitreten via Code")
        }
        HorizontalDivider()
        when {
            isLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            items.isEmpty() -> Text(
                "Du bist in keiner Gruppe. Tippe auf + um eine zu erstellen, oder nutze \"Beitreten via Code\".",
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { g -> GroupCard(g, onClick = { onOpenGroup(g.id) }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTab(
    items: List<GroupSummaryDto>,
    query: String,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onJoinPublic: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Suche") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onSearch) { Text("Suchen") }
        }
        HorizontalDivider()
        when {
            isLoading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            items.isEmpty() -> Text(
                "Keine öffentlichen Gruppen gefunden.",
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { g ->
                    GroupCard(g, onClick = { onOpenGroup(g.id) }, action = {
                        if (g.myRole == null) {
                            TextButton(onClick = { onJoinPublic(g.id) }) { Text("Beitreten") }
                        } else {
                            Text("Mitglied", style = MaterialTheme.typography.labelSmall)
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    g: GroupSummaryDto,
    onClick: () -> Unit,
    action: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(g.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    val visLabel = if (g.visibility == "PUBLIC") "öffentlich" else "privat"
                    Text("· $visLabel", style = MaterialTheme.typography.labelSmall)
                }
                g.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
                Text(
                    "${g.memberCount} ${if (g.memberCount == 1) "Mitglied" else "Mitglieder"}" +
                        (g.myRole?.let { " · ${roleLabel(it)}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            action?.invoke()
        }
    }
}

internal fun roleLabel(role: String): String = when (role) {
    "OWNER" -> "Eigentümer"
    "ADMIN" -> "Admin"
    "MEMBER" -> "Mitglied"
    else -> role
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, visibility: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("PRIVATE") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Gruppe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Beschreibung (optional)") })
                Spacer(Modifier.height(4.dp))
                Text("Sichtbarkeit", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = visibility == "PRIVATE", onClick = { visibility = "PRIVATE" }, label = { Text("Privat") })
                    FilterChip(selected = visibility == "PUBLIC", onClick = { visibility = "PUBLIC" }, label = { Text("Öffentlich") })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, desc, visibility) },
            ) { Text("Erstellen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinByCodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beitreten via Code") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = { Text("Einladungscode") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(enabled = code.trim().length >= 4, onClick = { onConfirm(code) }) { Text("Beitreten") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
