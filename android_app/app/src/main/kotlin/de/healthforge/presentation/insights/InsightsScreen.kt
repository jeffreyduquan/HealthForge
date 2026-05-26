package de.healthforge.presentation.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.healthforge.domain.insights.CorrelationResult
import de.healthforge.domain.insights.INSIGHT_MIN_LOG_DAYS
import de.healthforge.domain.insights.InsightsReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    vm: InsightsViewModel = hiltViewModel(),
) {
    val ui by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Erkenntnisse") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zur\u00fcck")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Neu berechnen")
                    }
                },
            )
        },
    ) { padding ->
        when {
            ui.loading -> LoadingPane(padding)
            ui.error != null -> ErrorPane(padding, ui.error!!)
            ui.report != null -> ReportPane(padding, ui.report!!)
            else -> EmptyPane(padding)
        }
    }
}

@Composable
private fun LoadingPane(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text("Berechne Korrelationen \u2026", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ErrorPane(padding: PaddingValues, msg: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
    ) {
        Text("Fehler: $msg", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyPane(padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
        Text("Keine Daten.")
    }
}

@Composable
private fun ReportPane(padding: PaddingValues, report: InsightsReport) {
    if (!report.unlocked) {
        LockedPane(padding, report.distinctLogDays)
        return
    }
    if (report.topResults.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Keine signifikanten Korrelationen gefunden.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Bedingungen: Lift > 1.5, mindestens 3 Co-Vorkommen innerhalb 4\u201348h.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Daten-Basis: ${report.distinctLogDays} Log-Tage.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Top-${report.topResults.size.coerceAtMost(5)} Korrelationen",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "Basis: ${report.distinctLogDays} Log-Tage  \u2022  Schwelle Lift > 1.5, n \u2265 3",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        items(report.topResults.take(5)) { r ->
            CorrelationCard(r)
        }
        if (report.topResults.size > 5) {
            item {
                Text(
                    "(${report.topResults.size - 5} weitere unterhalb)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LockedPane(padding: PaddingValues, currentLogDays: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Erkenntnisse gesperrt", style = MaterialTheme.typography.titleLarge)
        Text(
            "Bitte protokolliere mindestens $INSIGHT_MIN_LOG_DAYS Tage Symptome, " +
                "bevor Korrelationen berechnet werden. Aktuell: $currentLogDays Tage.",
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { (currentLogDays.toFloat() / INSIGHT_MIN_LOG_DAYS).coerceIn(0f, 1f) },
        )
    }
}

@Composable
private fun CorrelationCard(r: CorrelationResult) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${r.foodName} \u2192 ${r.symptomName}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Lift ${"%.2f".format(r.lift)}  \u2022  n=${r.n}  \u2022  \u00d8 Schwere ${"%.1f".format(r.avgSeverity)}/5",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Score ${"%.2f".format(r.score)}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
