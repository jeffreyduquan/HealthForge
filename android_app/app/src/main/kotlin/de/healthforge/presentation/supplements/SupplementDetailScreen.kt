package de.healthforge.presentation.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.presentation.essen.rezepte.PortionInputDialog
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.NeoCard
import de.healthforge.presentation.theme.NeoSectionLabel
import de.healthforge.presentation.theme.SectionPill

/**
 * Full-screen supplement detail view — styled like IngredientDetailScreen.
 * Shows supplement name, brand, default dose, and nutrient contribution per dose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementDetailScreen(
    supplementId: String,
    onBack: () -> Unit,
    onAddedToPlan: () -> Unit = {},
    vm: SupplementDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val hm = LocalHmTokens.current

    LaunchedEffect(supplementId) { vm.load(supplementId) }

    LaunchedEffect(state.navigateHome) {
        if (state.navigateHome) onAddedToPlan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.supplement?.nameDe ?: "Supplement", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.openAddToPlanDialog() }) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = "Zum Plan hinzufügen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = hm.background,
                    titleContentColor = hm.fgPrimary,
                ),
            )
        },
        containerColor = hm.background,
    ) { padding ->
        val sup = state.supplement
        if (sup != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Brand
                sup.brand?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = hm.fgSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionPill(label = "Supplement")
                }

                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.3f))

                // Dose info
                NeoSectionLabel("Dosierung")
                NeoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MacroRow("Empfohlene Dosis", "${formatNum(sup.defaultDose)} g")
                        sup.kcalPerDose?.let {
                            MacroRow("Kalorien pro Dosis", "${formatNum(it)} kcal")
                        }
                    }
                }

                // Nutrient contributions per dose
                val hasNutrients = sup.proteinPerDose != null || sup.carbsPerDose != null || sup.fatPerDose != null
                if (hasNutrients) {
                    NeoSectionLabel("Nährwerte pro Dosis")
                    NeoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sup.proteinPerDose?.let {
                                MacroRow("Eiweiß", "${formatNum(it)} g")
                            }
                            sup.carbsPerDose?.let {
                                MacroRow("Kohlenhydrate", "${formatNum(it)} g")
                            }
                            sup.fatPerDose?.let {
                                MacroRow("Fett", "${formatNum(it)} g")
                            }
                        }
                    }
                }
            }
        }

        if (state.showAddToPlan) {
            val dose = state.supplement?.defaultDose?.let { "%.0f".format(it) } ?: "1"
            PortionInputDialog(
                onConfirm = { grams -> vm.addToPlan(grams) },
                onDismiss = { vm.dismissAddToPlanDialog() },
                defaultValue = dose,
                unitLabel = "Dosis",
                title = "Supplement",
            )
        }
    }
}

@Composable
private fun MacroRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalHmTokens.current.fgSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalHmTokens.current.fgPrimary)
    }
}

private fun formatNum(d: Double): String = if (d == d.toLong().toDouble()) "%.0f".format(d) else "%.1f".format(d)
