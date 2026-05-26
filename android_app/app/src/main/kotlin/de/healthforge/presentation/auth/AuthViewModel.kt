package de.healthforge.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun isLoggedIn(): Boolean = repo.isLoggedIn()

    fun login(email: String, password: String) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            repo.login(email.trim(), password)
                .onSuccess { _state.value = AuthUiState(success = true) }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "Login fehlgeschlagen") }
        }
    }

    fun register(inviteCode: String, email: String, displayName: String, password: String) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            repo.register(inviteCode.trim().uppercase(), email.trim(), displayName.trim(), password)
                .onSuccess { _state.value = AuthUiState(success = true) }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "Registrierung fehlgeschlagen") }
        }
    }

    fun requestPasswordReset(email: String) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            repo.requestPasswordReset(email.trim())
                .onSuccess { _state.value = AuthUiState(success = true) }
                .onFailure { _state.value = AuthUiState(error = it.message ?: "Anfrage fehlgeschlagen") }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reset() { _state.value = AuthUiState() }
}
