package de.healthforge.presentation.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.network.GroupMemberDto
import de.healthforge.data.network.GroupUpdateRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onAddRecipe: () -> Unit = {},
    onOpenRecipe: (String) -> Unit = {},
    vm: GroupDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    var memberAction by remember { mutableStateOf<MemberActionTarget?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editVisibility by remember { mutableStateOf("PRIVATE") }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }
    LaunchedEffect(state.leftOrRemoved) {
        if (state.leftOrRemoved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.group?.name ?: "Gruppe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val g = state.group
        if (state.isLoading || g == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val isOwner = g.myRole == "OWNER"
        val canManage = g.myRole == "OWNER" || g.myRole == "ADMIN"
        val isMember = g.myRole != null

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header card
            Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(g.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        AssistChip(
                            onClick = {},
                            label = { Text(if (g.visibility == "PUBLIC") "öffentlich" else "privat") },
                        )
                    }
                    g.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "${g.memberCount} ${if (g.memberCount == 1) "Mitglied" else "Mitglieder"} · " +
                            (g.myRole?.let { roleLabel(it) } ?: "kein Mitglied"),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    // Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (isMember) {
                            OutlinedButton(onClick = { showShareDialog = true }, modifier = Modifier.weight(1f)) {
                                Text("Gruppe teilen")
                            }
                        }
                        if (canManage) {
                            OutlinedButton(onClick = {
                                editName = g.name; editDesc = g.description ?: ""; editVisibility = g.visibility
                                showSettingsDialog = true
                            }, modifier = Modifier.weight(1f)) {
                                Text("Einstellungen")
                            }
                        }
                    }
                }
            }

            // Tab-Navigation: Rezepte | Mitglieder
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Rezepte") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Mitglieder") })
            }

            when (tabIndex) {
                0 -> {
                    // ───── Rezepte-Tab ─────
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Rezepte der Gruppe", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (isMember) {
                                OutlinedButton(onClick = onAddRecipe) { Text("+ Rezept") }
                            }
                        }
                        if (state.recipes.isEmpty()) {
                            Text(
                                "Noch keine Rezepte in dieser Gruppe.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                items(state.recipes, key = { it.id }) { r ->
                                    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenRecipe(r.id) }) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(r.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    r.slot_tags.joinToString(", ") + " · ${r.author_id.take(8)}…",
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // ───── Mitglieder-Tab ─────
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Management-Buttons
                        if (isMember && !isOwner) {
                            OutlinedButton(onClick = { confirmLeave = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Gruppe verlassen")
                            }
                        }
                        if (isOwner) {
                            Text(
                                "Als Eigentümer musst du Ownership übertragen, bevor du verlassen kannst.",
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { confirmDelete = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Gruppe löschen") }
                            }
                        }

                        HorizontalDivider()
                        Text("Mitglieder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(state.members, key = { it.userId }) { m ->
                                MemberRow(
                                    member = m,
                                    isOwnerViewer = isOwner,
                                    canManageViewer = canManage,
                                    onAction = { action -> memberAction = MemberActionTarget(m, action) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Share dialog
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Gruppe teilen") },
            text = {
                Column {
                    val sg = state.group ?: return@Column
                    if (sg.visibility == "PRIVATE") {
                        Text("Einladungscode:", style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(sg.inviteCode ?: "-", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                copyToClipboard(ctx, sg.inviteCode ?: "")
                                vm.clearMessage()
                            }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Kopieren") }
                        }
                        Text("Teile diesen Code mit Freunden, damit sie der Gruppe beitreten können.",
                            style = MaterialTheme.typography.bodySmall)
                        if (sg.myRole == "OWNER") {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { vm.regenerateInvite() }) { Text("Neuen Code generieren") }
                        }
                    } else {
                        Text("Diese Gruppe ist öffentlich – jeder kann beitreten.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShareDialog = false }) { Text("Schließen") } },
        )
    }

    // Settings dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Gruppen-Einstellungen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Beschreibung") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Text("Sichtbarkeit", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = editVisibility == "PUBLIC", onClick = { editVisibility = "PUBLIC" }, label = { Text("Öffentlich") })
                        FilterChip(selected = editVisibility == "PRIVATE", onClick = { editVisibility = "PRIVATE" }, label = { Text("Privat") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateGroup(GroupUpdateRequest(name = editName.trim(), description = editDesc.trim().ifEmpty { null }, visibility = editVisibility))
                    showSettingsDialog = false
                }) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Abbrechen") } },
        )
    }

    // Leave confirm dialog
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Gruppe verlassen?") },
            text = { Text("Du kannst später wieder beitreten, sofern die Gruppe öffentlich ist oder du den Code hast.") },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; vm.leave() }) { Text("Verlassen") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Abbrechen") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Gruppe wirklich löschen?") },
            text = { Text("Das löscht die Gruppe und alle Mitgliedschaften endgültig. Rezepte der Gruppe bleiben erhalten.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.deleteGroup(onDeleted = onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
        )
    }
    memberAction?.let { target ->
        val text = when (target.action) {
            MemberAction.REMOVE -> "Mitglied wirklich entfernen?"
            MemberAction.TRANSFER -> "Ownership wirklich übertragen? Du wirst danach Mitglied."
            MemberAction.TOGGLE_ROLE -> {
                val newRole = if (target.member.role == "CONTRIBUTOR") "MEMBER" else "CONTRIBUTOR"
                "Rolle von ${target.member.userId.take(8)}… auf ${roleLabel(newRole)} ändern?"
            }
        }
        AlertDialog(
            onDismissRequest = { memberAction = null },
            title = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    when (target.action) {
                        MemberAction.REMOVE -> vm.removeMember(target.member.userId)
                        MemberAction.TRANSFER -> vm.transferOwnership(target.member.userId)
                        MemberAction.TOGGLE_ROLE -> {
                            val newRole = if (target.member.role == "CONTRIBUTOR") "MEMBER" else "CONTRIBUTOR"
                            vm.setMemberRole(target.member.userId, newRole)
                        }
                    }
                    memberAction = null
                }) { Text("Bestätigen") }
            },
            dismissButton = { TextButton(onClick = { memberAction = null }) { Text("Abbrechen") } },
        )
    }
}

private enum class MemberAction { REMOVE, TRANSFER, TOGGLE_ROLE }
private data class MemberActionTarget(val member: GroupMemberDto, val action: MemberAction)

@Composable
private fun MemberRow(
    member: GroupMemberDto,
    isOwnerViewer: Boolean,
    canManageViewer: Boolean,
    onAction: (MemberAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                val displayName = member.displayName ?: (member.userId.take(8) + "…")
                Text(displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(roleLabel(member.role), style = MaterialTheme.typography.labelSmall)
            }
            if (canManageViewer && member.role != "OWNER") {
                // Toggle zwischen MEMBER und CONTRIBUTOR
                val toggleLabel = if (member.role == "CONTRIBUTOR") "Zum Mitglied" else "Zum Beitragenden"
                IconButton(onClick = { onAction(MemberAction.TOGGLE_ROLE) }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = toggleLabel)
                }
                // Nur OWNER darf Ownership übertragen
                if (isOwnerViewer) {
                    IconButton(onClick = { onAction(MemberAction.TRANSFER) }) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Ownership übertragen")
                    }
                }
                IconButton(onClick = { onAction(MemberAction.REMOVE) }) {
                    Icon(Icons.Filled.PersonRemove, contentDescription = "Entfernen")
                }
            }
        }
    }
}

private fun copyToClipboard(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Invite-Code", text))
}
