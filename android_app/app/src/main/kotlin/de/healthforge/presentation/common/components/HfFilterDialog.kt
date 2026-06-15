package de.healthforge.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * P7.S5 — Unified filter dialog for ALL Essen tabs.
 * Used by LebensmittelScreen and RecipesScreen.
 *
 * Contains:
 * - Allergen exclusion (EU-14)
 * - FODMAP exclusion
 * - Profile filter toggle
 * - Optional: slot tag filters (for recipes)
 * - Optional: "Nur eigene" toggle (for recipes)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HfFilterDialog(
    // Allergen/FODMAP state
    excludedAllergens: Set<AllergenType>,
    excludedFodmap: Set<FodmapType>,
    onToggleAllergen: (AllergenType) -> Unit,
    onToggleFodmap: (FodmapType) -> Unit,
    onDismiss: () -> Unit,
    // Profile filter
    applyProfileFilters: Boolean = false,
    onToggleProfileFilters: (() -> Unit)? = null,
    // Optional: recipe-specific slot tags
    slotOptions: List<Pair<String, String>> = emptyList(),
    selectedSlots: Set<String> = emptySet(),
    onToggleSlot: ((String) -> Unit)? = null,
) {
    val hm = LocalHmTokens.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter", color = hm.fgPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Profile filter toggle
                if (onToggleProfileFilters != null) {
                    FilterChip(
                        selected = applyProfileFilters,
                        onClick = onToggleProfileFilters,
                        label = {
                            val n = excludedAllergens.size + excludedFodmap.size
                            Text(if (applyProfileFilters) "Profil-Filter an ($n)" else "Profil-Filter aus")
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Text("Allergene ausschliessen", style = MaterialTheme.typography.labelMedium,
                    color = hm.fgSecondary, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AllergenType.entries.forEach { a ->
                        FilterChip(
                            selected = a in excludedAllergens,
                            onClick = { onToggleAllergen(a) },
                            label = { Text(a.germanLabel, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(),
                        )
                    }
                }

                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.2f))

                Text("FODMAP ausschliessen", style = MaterialTheme.typography.labelMedium,
                    color = hm.fgSecondary, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FodmapType.entries.forEach { f ->
                        FilterChip(
                            selected = f in excludedFodmap,
                            onClick = { onToggleFodmap(f) },
                            label = { Text(f.germanLabel, style = MaterialTheme.typography.bodySmall) },
                        )
                    }
                }

                // Slot tags (recipes only)
                if (slotOptions.isNotEmpty() && onToggleSlot != null) {
                    HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.2f))
                    Text("Mahlzeit", style = MaterialTheme.typography.labelMedium,
                        color = hm.fgSecondary, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        slotOptions.forEach { (code, label) ->
                            FilterChip(
                                selected = code in selectedSlots,
                                onClick = { onToggleSlot(code) },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fertig") }
        },
    )
}
