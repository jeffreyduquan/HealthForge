package de.healthforge.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionsScreen(
    onBack: () -> Unit,
    vm: SubmissionsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val hm = LocalHmTokens.current
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Zutaten", "Rezepte", "Supplements")

    LaunchedEffect(Unit) { vm.load() }

    Box(
        modifier = Modifier.fillMaxSize().background(hm.background),
    ) {
        AmbientBackdrop(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück", tint = hm.fgPrimary)
                }
                Spacer(Modifier.width(8.dp))
                GradientText("Meine Vorschläge", style = MaterialTheme.typography.titleLarge)
            }

            // Tabs
            TabRow(selectedTabIndex = tabIndex, containerColor = hm.background) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = {
                        Text(label, color = if (tabIndex == i) hm.ambientViolet else hm.fgSecondary)
                    })
                }
            }

            // Content
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Fehler: ${state.error}", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    val list = when (tabIndex) {
                        0 -> state.ingredients
                        1 -> state.recipes
                        else -> state.supplements
                    }
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Keine Vorschläge", color = hm.fgSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(list, key = { "${tabIndex}-${it.id}" }) { item ->
                                SubmissionCard(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionCard(item: de.healthforge.data.network.SubmissionDto) {
    val hm = LocalHmTokens.current

    GlassCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name_de, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = hm.fgPrimary)
                StatusBadge(item.status)
            }
            Text(item.created_at.take(10), style = MaterialTheme.typography.bodySmall, color = hm.fgTertiary)
            item.review_note?.takeIf { it.isNotBlank() }?.let {
                Text("Anmerkung: $it", style = MaterialTheme.typography.bodySmall, color = hm.fgSecondary)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "PENDING" -> "Ausstehend" to androidx.compose.ui.graphics.Color(0xFFFFA726)
        "APPROVED", "PUBLISHED" -> "Genehmigt" to androidx.compose.ui.graphics.Color(0xFF66BB6A)
        "REJECTED" -> "Abgelehnt" to androidx.compose.ui.graphics.Color(0xFFEF5350)
        else -> status to androidx.compose.ui.graphics.Color.Gray
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
