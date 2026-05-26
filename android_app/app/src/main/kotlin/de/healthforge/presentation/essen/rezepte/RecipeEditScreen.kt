package de.healthforge.presentation.essen.rezepte

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.data.network.IngredientDto

private val SLOT_OPTS = listOf("BREAKFAST" to "Frühstück", "LUNCH" to "Mittag", "DINNER" to "Abend", "SNACK" to "Snack")
private val VISIBILITY_OPTS = listOf("PUBLIC" to "Öffentlich", "PRIVATE" to "Privat", "GROUP" to "Gruppe")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    vm: RecipeEditViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.savedId) {
        state.savedId?.let { onSaved(it) }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.pickImage(ctx, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Rezept bearbeiten" else "Neues Rezept") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                    } else {
                        TextButton(onClick = vm::save) { Text("Speichern") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                label = { Text("Titel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.prepMinutes,
                    onValueChange = vm::setPrep,
                    label = { Text("Prep (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.cookMinutes,
                    onValueChange = vm::setCook,
                    label = { Text("Cook (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Portionen", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.setServings(state.servings - 1) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "-")
                        }
                        Text(state.servings.toString(), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { vm.setServings(state.servings + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "+")
                        }
                    }
                }
            }

            // Slot-Tags
            Text("Mahlzeit (mindestens 1)", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SLOT_OPTS) { (code, label) ->
                    FilterChip(
                        selected = code in state.slotTags,
                        onClick = { vm.toggleSlot(code) },
                        label = { Text(label) },
                    )
                }
            }

            // Visibility
            Text("Sichtbarkeit", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(VISIBILITY_OPTS) { (code, label) ->
                    FilterChip(
                        selected = state.visibility == code,
                        onClick = { vm.setVisibility(code) },
                        label = { Text(label) },
                    )
                }
            }
            if (state.visibility == "GROUP") {
                GroupPickerSection(
                    selectedId = state.groupId,
                    groups = state.myGroups,
                    onSelect = vm::setGroupId,
                )
            }

            // Image
            OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.imageKey != null) "Bild ersetzen (gespeichert)" else "Bild auswählen")
            }

            IngredientPicker(state = state, vm = vm)
            StepsEditor(state = state, vm = vm)
        }
    }
}

@Composable
private fun IngredientPicker(state: RecipeEditUiState, vm: RecipeEditViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Zutaten", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.ingredientSearchQuery,
                onValueChange = vm::setIngredientQuery,
                label = { Text("Zutat suchen…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.ingredientSuggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(state.ingredientSuggestions) { ing ->
                        SuggestionRow(ing) { vm.addIngredient(ing) }
                    }
                }
            }
            state.ingredients.forEachIndexed { idx, line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(line.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = line.quantity,
                        onValueChange = { vm.updateIngredientQuantity(idx, it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(
                        value = line.unit,
                        onValueChange = { vm.updateIngredientUnit(idx, it) },
                        singleLine = true,
                        modifier = Modifier.width(64.dp),
                    )
                    IconButton(onClick = { vm.removeIngredient(idx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Entfernen")
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(ing: IngredientDto, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(ing.name_de, style = MaterialTheme.typography.bodyMedium)
            val k = ing.energy_kcal_per_100g
            if (k != null) Text("${k.toInt()} kcal / 100g", style = MaterialTheme.typography.labelSmall)
        }
        Button(onClick = onPick) { Text("+") }
    }
}

@Composable
private fun StepsEditor(state: RecipeEditUiState, vm: RecipeEditViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Schritte", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            state.steps.forEachIndexed { idx, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${idx + 1}.",
                        modifier = Modifier.padding(top = 16.dp).width(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = step.text,
                        onValueChange = { vm.updateStep(idx, it) },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                    )
                    IconButton(onClick = { vm.removeStep(idx) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Schritt löschen")
                    }
                }
            }
            TextButton(onClick = vm::addStep) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Schritt hinzufügen")
            }
        }
    }
}

@Composable
private fun GroupPickerSection(
    selectedId: String?,
    groups: List<de.healthforge.data.network.GroupSummaryDto>,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Gruppe", style = MaterialTheme.typography.titleSmall)
        if (groups.isEmpty()) {
            Text(
                "Du bist in keiner Gruppe. Tritt einer Gruppe bei (Profil → Meine Gruppen), um GROUP-Rezepte zu posten.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(groups) { g ->
                    FilterChip(
                        selected = selectedId == g.id,
                        onClick = { onSelect(if (selectedId == g.id) null else g.id) },
                        label = { Text(g.name) },
                    )
                }
            }
        }
    }
}
