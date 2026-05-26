package de.healthforge.presentation.essen.rezepte

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.healthforge.data.network.GroupSummaryDto
import de.healthforge.data.network.IngredientDto
import de.healthforge.data.network.RecipeIngredientInput
import de.healthforge.data.network.RecipeStepInput
import de.healthforge.data.network.RecipeUpsertRequest
import de.healthforge.data.repository.GroupRepository
import de.healthforge.data.repository.IngredientRepository
import de.healthforge.data.repository.MediaRepository
import de.healthforge.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientLine(
    val ingredientId: String,
    val name: String,
    val quantity: String,
    val unit: String = "g",
    val isOptional: Boolean = false,
)

data class StepLine(val text: String)

data class RecipeEditUiState(
    val isEditing: Boolean = false,
    val originalId: String? = null,
    val title: String = "",
    val description: String = "",
    val servings: Int = 1,
    val prepMinutes: String = "",
    val cookMinutes: String = "",
    val slotTags: Set<String> = emptySet(),
    val visibility: String = "PUBLIC",
    val groupId: String? = null,
    val myGroups: List<GroupSummaryDto> = emptyList(),
    val ingredients: List<IngredientLine> = emptyList(),
    val steps: List<StepLine> = listOf(StepLine("")),
    val imageKey: String? = null,
    val ingredientSearchQuery: String = "",
    val ingredientSuggestions: List<IngredientDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val savedId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository,
    private val ingredientRepo: IngredientRepository,
    private val mediaRepo: MediaRepository,
    private val groupRepo: GroupRepository,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val editingId: String? = savedState["id"]
    private val _state = MutableStateFlow(RecipeEditUiState(isEditing = editingId != null, originalId = editingId))
    val state: StateFlow<RecipeEditUiState> = _state.asStateFlow()

    init {
        if (editingId != null) loadExisting(editingId)
        loadMyGroups()
    }

    private fun loadMyGroups() {
        viewModelScope.launch {
            groupRepo.myGroups().onSuccess { list -> _state.update { it.copy(myGroups = list) } }
        }
    }

    private fun loadExisting(id: String) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            recipeRepo.detail(id).fold(
                onSuccess = { d ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            title = d.title,
                            description = d.description.orEmpty(),
                            servings = d.servings,
                            prepMinutes = d.prep_minutes.toString(),
                            cookMinutes = d.cook_minutes?.toString().orEmpty(),
                            slotTags = d.slot_tags.toSet(),
                            visibility = d.visibility,
                            groupId = d.group_id,
                            imageKey = d.image_key,
                            ingredients = d.ingredients.sortedBy { ing -> ing.position }.map { ing ->
                                IngredientLine(
                                    ingredientId = ing.ingredient_id,
                                    name = ing.ingredient_name ?: ing.ingredient_id.take(8),
                                    quantity = "%g".format(ing.quantity),
                                    unit = ing.unit,
                                    isOptional = ing.is_optional,
                                )
                            },
                            steps = d.steps.sortedBy { s -> s.position }.map { StepLine(it.text) }
                                .ifEmpty { listOf(StepLine("")) },
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message) } },
            )
        }
    }

    fun setTitle(v: String) = _state.update { it.copy(title = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun setServings(v: Int) = _state.update { it.copy(servings = v.coerceAtLeast(1)) }
    fun setPrep(v: String) = _state.update { it.copy(prepMinutes = v.filter { c -> c.isDigit() }) }
    fun setCook(v: String) = _state.update { it.copy(cookMinutes = v.filter { c -> c.isDigit() }) }
    fun setVisibility(v: String) = _state.update {
        if (v != "GROUP") it.copy(visibility = v, groupId = null) else it.copy(visibility = v)
    }
    fun setGroupId(id: String?) = _state.update { it.copy(groupId = id) }
    fun toggleSlot(slot: String) = _state.update {
        val next = it.slotTags.toMutableSet().apply { if (!add(slot)) remove(slot) }
        it.copy(slotTags = next)
    }

    fun setIngredientQuery(q: String) {
        _state.update { it.copy(ingredientSearchQuery = q) }
        if (q.isBlank()) {
            _state.update { it.copy(ingredientSuggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            ingredientRepo.search(q, limit = 8).onSuccess { list ->
                _state.update { it.copy(ingredientSuggestions = list) }
            }
        }
    }

    fun addIngredient(d: IngredientDto) {
        _state.update {
            it.copy(
                ingredients = it.ingredients + IngredientLine(d.id, d.name_de, quantity = "100"),
                ingredientSearchQuery = "",
                ingredientSuggestions = emptyList(),
            )
        }
    }

    fun updateIngredientQuantity(idx: Int, q: String) = _state.update {
        val list = it.ingredients.toMutableList()
        if (idx in list.indices) list[idx] = list[idx].copy(quantity = q.filter { c -> c.isDigit() || c == '.' || c == ',' })
        it.copy(ingredients = list)
    }

    fun updateIngredientUnit(idx: Int, u: String) = _state.update {
        val list = it.ingredients.toMutableList()
        if (idx in list.indices) list[idx] = list[idx].copy(unit = u)
        it.copy(ingredients = list)
    }

    fun removeIngredient(idx: Int) = _state.update {
        val list = it.ingredients.toMutableList()
        if (idx in list.indices) list.removeAt(idx)
        it.copy(ingredients = list)
    }

    fun updateStep(idx: Int, text: String) = _state.update {
        val list = it.steps.toMutableList()
        if (idx in list.indices) list[idx] = StepLine(text)
        it.copy(steps = list)
    }

    fun addStep() = _state.update { it.copy(steps = it.steps + StepLine("")) }

    fun removeStep(idx: Int) = _state.update {
        val list = it.steps.toMutableList()
        if (idx in list.indices && list.size > 1) list.removeAt(idx)
        it.copy(steps = list)
    }

    fun pickImage(ctx: Context, uri: Uri) {
        viewModelScope.launch {
            mediaRepo.uploadImage(ctx, bucket = "recipes", uri).fold(
                onSuccess = { key -> _state.update { it.copy(imageKey = key) } },
                onFailure = { e -> _state.update { it.copy(error = "Bild-Upload fehlgeschlagen: ${e.message}") } },
            )
        }
    }

    fun save() {
        val s = _state.value
        val err = validate(s)
        if (err != null) {
            _state.update { it.copy(error = err) }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }
        val req = RecipeUpsertRequest(
            title = s.title.trim(),
            description = s.description.trim().ifEmpty { null },
            image_key = s.imageKey,
            servings = s.servings,
            prep_minutes = s.prepMinutes.toIntOrNull() ?: 0,
            cook_minutes = s.cookMinutes.toIntOrNull(),
            slot_tags = s.slotTags.toList(),
            visibility = s.visibility,
            group_id = s.groupId,
            ingredients = s.ingredients.map { ing ->
                RecipeIngredientInput(
                    ingredient_id = ing.ingredientId,
                    quantity = (ing.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0),
                    unit = ing.unit,
                    is_optional = ing.isOptional,
                )
            },
            steps = s.steps.filter { it.text.isNotBlank() }.map { RecipeStepInput(text = it.text.trim()) },
        )
        viewModelScope.launch {
            val res = if (s.originalId != null) {
                recipeRepo.update(s.originalId, req).map { s.originalId }
            } else {
                recipeRepo.create(req)
            }
            res.fold(
                onSuccess = { id -> _state.update { it.copy(isSaving = false, savedId = id) } },
                onFailure = { e -> _state.update { it.copy(isSaving = false, error = e.message) } },
            )
        }
    }

    private fun validate(s: RecipeEditUiState): String? {
        if (s.title.isBlank()) return "Titel fehlt"
        if (s.slotTags.isEmpty()) return "Mindestens 1 Mahlzeitenslot wählen"
        if ((s.prepMinutes.toIntOrNull() ?: -1) < 0) return "Prep-Zeit ungültig"
        if (s.visibility == "GROUP" && s.groupId.isNullOrBlank()) return "Bitte Gruppe wählen"
        if (s.ingredients.isEmpty()) return "Mindestens 1 Zutat"
        if (s.ingredients.any { (it.quantity.replace(',', '.').toDoubleOrNull() ?: 0.0) <= 0.0 }) return "Zutat-Menge muss > 0 sein"
        if (s.steps.none { it.text.isNotBlank() }) return "Mindestens 1 Schritt"
        return null
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
