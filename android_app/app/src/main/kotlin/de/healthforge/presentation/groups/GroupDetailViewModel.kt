package de.healthforge.presentation.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.GroupMemberDto
import de.healthforge.data.network.GroupSummaryDto
import de.healthforge.data.network.RecipeListItemDto
import de.healthforge.data.repository.GroupRepository
import de.healthforge.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupDetailUiState(
    val group: GroupSummaryDto? = null,
    val members: List<GroupMemberDto> = emptyList(),
    val recipes: List<RecipeListItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val leftOrRemoved: Boolean = false,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val repo: GroupRepository,
    private val recipeRepo: RecipeRepository,
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
            val rRes = recipeRepo.browse(scope = "PUBLIC_OR_MINE", groupId = groupId)
            _state.update {
                it.copy(
                    isLoading = false,
                    group = gRes.getOrNull() ?: it.group,
                    members = mRes.getOrNull() ?: emptyList(),
                    recipes = rRes.getOrNull() ?: emptyList(),
                    message = gRes.exceptionOrNull()?.message
                        ?: mRes.exceptionOrNull()?.message,
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

    fun updateGroup(req: de.healthforge.data.network.GroupUpdateRequest) {
        viewModelScope.launch {
            repo.updateGroup(groupId, req).fold(
                onSuccess = { g ->
                    _state.update { it.copy(group = g, message = "Gruppe aktualisiert") }
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Update fehlgeschlagen") } },
            )
        }
    }

    fun regenerateInvite() {
        viewModelScope.launch {
            repo.regenerateInvite(groupId).fold(
                onSuccess = { code ->
                    _state.update { it.copy(message = "Neuer Code: $code") }
                    load()
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Fehler") } },
            )
        }
    }

    /** OWNER/ADMIN: Setzt die Rolle eines Mitglieds (z.B. CONTRIBUTOR ↔ MEMBER). */
    fun setMemberRole(userId: String, role: String) {
        viewModelScope.launch {
            repo.setMemberRole(groupId, userId, role).fold(
                onSuccess = {
                    _state.update { it.copy(message = "Rolle geändert") }
                    load()
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Rollenänderung fehlgeschlagen") } },
            )
        }
    }

    /** Nur OWNER: löscht die gesamte Gruppe. */
    fun deleteGroup(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repo.deleteGroup(groupId).fold(
                onSuccess = {
                    _state.update { it.copy(leftOrRemoved = true, message = "Gruppe gelöscht") }
                    onDeleted()
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Löschen fehlgeschlagen") } },
            )
        }
    }
}
