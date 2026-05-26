package de.healthforge.presentation.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.GroupMemberDto
import de.healthforge.data.network.GroupSummaryDto
import de.healthforge.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailUiState(
    val group: GroupSummaryDto? = null,
    val members: List<GroupMemberDto> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val leftOrRemoved: Boolean = false,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val repo: GroupRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedState["id"]) { "missing nav arg `id`" }

    private val _state = MutableStateFlow(GroupDetailUiState())
    val state: StateFlow<GroupDetailUiState> = _state.asStateFlow()

    init { load() }

    fun clearMessage() { _state.update { it.copy(message = null) } }

    fun load() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val gRes = repo.detail(groupId)
            val mRes = repo.members(groupId)
            _state.update {
                it.copy(
                    isLoading = false,
                    group = gRes.getOrNull() ?: it.group,
                    members = mRes.getOrNull() ?: emptyList(),
                    message = gRes.exceptionOrNull()?.message ?: mRes.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            repo.leave(groupId).fold(
                onSuccess = { _state.update { it.copy(leftOrRemoved = true, message = "Gruppe verlassen") } },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Verlassen fehlgeschlagen") } },
            )
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            repo.removeMember(groupId, userId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(members = it.members.filterNot { m -> m.userId == userId }, message = "Mitglied entfernt")
                    }
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Entfernen fehlgeschlagen") } },
            )
        }
    }

    fun transferOwnership(newOwnerId: String) {
        viewModelScope.launch {
            repo.transferOwnership(groupId, newOwnerId).fold(
                onSuccess = { g ->
                    _state.update { it.copy(group = g, message = "Ownership übertragen") }
                    load()
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Übertragung fehlgeschlagen") } },
            )
        }
    }
}
