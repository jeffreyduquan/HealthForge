package de.healthforge.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * REQ-EXPORT-001..-004 — Drei Buttons:
 *  - Server-Daten (JSON / PDF) via `/v1/export/full`
 *  - Lokale Daten (JSON) via Room+Moshi.
 * Dateien landen unter Downloads/HealthForge/.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    vm: ExportViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daten exportieren") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Lade deine Daten als Datei in den Download-Ordner. Server- und lokale " +
                    "Datenbank werden getrennt exportiert.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = vm::downloadServerJson,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Server-Daten als JSON") }
            Button(
                onClick = vm::downloadServerPdf,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Server-Daten als PDF") }
            OutlinedButton(
                onClick = vm::exportLocalJson,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Lokale Daten als JSON") }
        }
    }
}
