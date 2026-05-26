package de.healthforge.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.GroupSummaryDto
import de.healthforge.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** REQ-GROUP-001..006 (Client). Tab=0 → Meine Gruppen; Tab=1 → Discover. */
data class GroupsUiState(
    val mine: List<GroupSummaryDto> = emptyList(),
    val discover: List<GroupSummaryDto> = emptyList(),
    val tab: Int = 0,
    val discoverQuery: String = "",
    val isLoadingMine: Boolean = false,
    val isLoadingDiscover: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val repo: GroupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state.asStateFlow()

    init { refreshMine() }

    fun setTab(idx: Int) {
        _state.update { it.copy(tab = idx) }
        if (idx == 1 && _state.value.discover.isEmpty()) refreshDiscover()
    }

    fun setDiscoverQuery(q: String) {
        _state.update { it.copy(discoverQuery = q) }
    }

    fun clearMessage() { _state.update { it.copy(message = null) } }

    fun refreshMine() {
        _state.update { it.copy(isLoadingMine = true) }
        viewModelScope.launch {
            repo.myGroups().fold(
                onSuccess = { list -> _state.update { it.copy(mine = list, isLoadingMine = false) } },
                onFailure = { e -> _state.update { it.copy(isLoadingMine = false, message = e.message ?: "Fehler") } },
            )
        }
    }

    fun refreshDiscover() {
        val q = _state.value.discoverQuery
        _state.update { it.copy(isLoadingDiscover = true) }
        viewModelScope.launch {
            repo.discover(q = q).fold(
                onSuccess = { list -> _state.update { it.copy(discover = list, isLoadingDiscover = false) } },
                onFailure = { e -> _state.update { it.copy(isLoadingDiscover = false, message = e.message ?: "Fehler") } },
            )
        }
    }

    fun createGroup(name: String, description: String?, visibility: String, onCreated: (GroupSummaryDto) -> Unit) {
        viewModelScope.launch {
            repo.create(name = name.trim(), description = description?.takeIf { it.isNotBlank() }, visibility = visibility).fold(
                onSuccess = { g ->
                    _state.update { it.copy(mine = listOf(g) + it.mine, message = "Gruppe erstellt") }
                    onCreated(g)
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Erstellen fehlgeschlagen") } },
            )
        }
    }

    fun joinByCode(code: String, onJoined: (GroupSummaryDto) -> Unit) {
        viewModelScope.launch {
            repo.joinByCode(code).fold(
                onSuccess = { g ->
                    _state.update { it.copy(mine = listOf(g) + it.mine.filterNot { it.id == g.id }, message = "Beigetreten: ${g.name}") }
                    onJoined(g)
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Code ungültig") } },
            )
        }
    }

    fun joinPublic(id: String) {
        viewModelScope.launch {
            repo.joinPublic(id).fold(
                onSuccess = { g ->
                    _state.update {
                        it.copy(
                            mine = listOf(g) + it.mine.filterNot { m -> m.id == g.id },
                            discover = it.discover.map { d -> if (d.id == g.id) g else d },
                            message = "Beigetreten: ${g.name}",
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(message = e.message ?: "Beitritt fehlgeschlagen") } },
            )
        }
    }
}
