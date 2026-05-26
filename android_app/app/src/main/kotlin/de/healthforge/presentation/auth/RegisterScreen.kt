package de.healthforge.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var inviteCode by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    LaunchedEffect(state.success) { if (state.success) onRegistered() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konto erstellen") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Zurück") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "HealthForge ist invite-only. Bitte gib deinen Einladungscode ein.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase() },
                label = { Text("Einladungscode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Anzeigename") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-Mail") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Passwort (min. 10 Zeichen)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = { passwordConfirm = it },
                label = { Text("Passwort wiederholen") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordConfirm.isNotEmpty() && passwordConfirm != password,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val canSubmit = inviteCode.isNotBlank() && email.contains("@") && displayName.isNotBlank() &&
                password.length >= 10 && password == passwordConfirm
            Button(
                onClick = { vm.register(inviteCode, email, displayName, password) },
                enabled = canSubmit && !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Konto erstellen")
            }
        }
    }
}
