package de.healthforge.presentation.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.data.network.GroupMemberDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    vm: GroupDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    var memberAction by remember { mutableStateOf<MemberActionTarget?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }

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
                    // Invite-Code for PRIVATE group members
                    if (g.visibility == "PRIVATE" && isMember && !g.inviteCode.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Code: ${g.inviteCode}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            IconButton(onClick = { copyToClipboard(ctx, g.inviteCode) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Code kopieren")
                            }
                        }
                    }
                }
            }

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isMember && !isOwner) {
                    OutlinedButton(onClick = { confirmLeave = true }) { Text("Verlassen") }
                }
                if (isOwner) {
                    Text(
                        "Als Eigentümer musst du Ownership übertragen, bevor du verlassen kannst.",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            HorizontalDivider()
            Text("Mitglieder", style = MaterialTheme.typography.titleMedium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(state.members, key = { it.userId }) { m ->
                    MemberRow(
                        member = m,
                        isOwnerViewer = isOwner,
                        onAction = { action -> memberAction = MemberActionTarget(m, action) },
                    )
                }
            }
        }
    }

    // Confirmation dialogs
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
    memberAction?.let { target ->
        val text = when (target.action) {
            MemberAction.REMOVE -> "Mitglied wirklich entfernen?"
            MemberAction.TRANSFER -> "Ownership wirklich übertragen? Du wirst danach Mitglied."
        }
        AlertDialog(
            onDismissRequest = { memberAction = null },
            title = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    when (target.action) {
                        MemberAction.REMOVE -> vm.removeMember(target.member.userId)
                        MemberAction.TRANSFER -> vm.transferOwnership(target.member.userId)
                    }
                    memberAction = null
                }) { Text("Bestätigen") }
            },
            dismissButton = { TextButton(onClick = { memberAction = null }) { Text("Abbrechen") } },
        )
    }
}

private enum class MemberAction { REMOVE, TRANSFER }
private data class MemberActionTarget(val member: GroupMemberDto, val action: MemberAction)

@Composable
private fun MemberRow(
    member: GroupMemberDto,
    isOwnerViewer: Boolean,
    onAction: (MemberAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(member.userId.take(8) + "…", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(roleLabel(member.role), style = MaterialTheme.typography.labelSmall)
            }
            if (isOwnerViewer && member.role != "OWNER") {
                IconButton(onClick = { onAction(MemberAction.TRANSFER) }) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Ownership übertragen")
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
