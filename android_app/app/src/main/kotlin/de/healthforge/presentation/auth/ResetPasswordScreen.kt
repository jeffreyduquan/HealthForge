package de.healthforge.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(state.success) { if (state.success) submitted = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passwort zurücksetzen") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Zurück") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (submitted) {
                Text("Falls die E-Mail registriert ist, hast du eine Nachricht mit einem Reset-Link erhalten. Bitte prüfe dein Postfach.")
                Button(onClick = onBack) { Text("Zurück zur Anmeldung") }
            } else {
                Text("Gib deine E-Mail ein, wir senden dir einen Link zum Zurücksetzen.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-Mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { vm.requestPasswordReset(email) },
                    enabled = !state.loading && email.contains("@"),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Link anfordern")
                }
            }
        }
    }
}
