package de.healthforge.presentation.supplements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.healthforge.presentation.common.components.HfAddToHomeButton
import de.healthforge.presentation.common.components.HfCard
import de.healthforge.presentation.common.components.HfDetailTopBar
import de.healthforge.presentation.common.components.HfNutrientProgressRow
import de.healthforge.presentation.common.components.HfSectionHeader
import de.healthforge.presentation.common.components.HfValueRow
import de.healthforge.presentation.common.components.formatNutrientValue
import de.healthforge.presentation.essen.rezepte.PortionInputDialog
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Full-screen supplement detail view — P7.S5 Consistency Refactor.
 * Uses unified Hf* components. Shows DGE progress bars per dose.
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
            HfDetailTopBar(
                title = state.supplement?.nameDe ?: "Supplement",
                onBack = onBack,
            )
        },
        containerColor = hm.background,
        bottomBar = {
            HfAddToHomeButton(onClick = { vm.openAddToPlanDialog() })
        },
    ) { padding ->
        val sup = state.supplement
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
        if (sup != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Brand
                sup.brand?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = hm.fgSecondary)
                }

                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.3f))

                // Dose info
                HfSectionHeader("Dosierung")
                HfCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HfValueRow("Empfohlene Dosis", "${formatNum(sup.defaultDose)} g")
                        sup.kcalPerDose?.let {
                            HfValueRow("Kalorien pro Dosis", "${formatNum(it)} kcal")
                        }
                    }
                }

                // DGE-Abdeckung
                val hasProtein = sup.proteinPerDose != null
                val hasCarbs = sup.carbsPerDose != null
                val hasFat = sup.fatPerDose != null
                if (hasProtein || hasCarbs || hasFat) {
                    HfSectionHeader("Tagesbedarf-Abdeckung")
                    HfCard {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            sup.proteinPerDose?.let {
                                HfNutrientProgressRow("Eiweiß", "${formatNutrientValue(it)} g", it / 50.0 * 100)
                            }
                            sup.carbsPerDose?.let {
                                HfNutrientProgressRow("Kohlenhydrate", "${formatNutrientValue(it)} g", it / 260.0 * 100)
                            }
                            sup.fatPerDose?.let {
                                HfNutrientProgressRow("Fett", "${formatNutrientValue(it)} g", it / 65.0 * 100)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
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

private fun formatNum(d: Double): String = if (d == d.toLong().toDouble()) "%.0f".format(d) else "%.1f".format(d)
