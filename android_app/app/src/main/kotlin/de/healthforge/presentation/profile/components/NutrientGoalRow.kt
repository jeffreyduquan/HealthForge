package de.healthforge.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * REQ-PROFILE-LAYOUT-001 / Profil-Redesign (post-Slice-4b-revert) — Tageszielzeile mit
 * Lock-Slider. Range = 0 … 2 × Profil-Default (=0–200 %). Lock-Button steuert
 * Schreibzugriff: gelockt = read-only Anzeige, entsperrt = Slider-Drag möglich,
 * Re-Lock = Commit. Reset-Icon (nur bei aktivem Override) entfernt den Override
 * und kehrt zum berechneten Default zurück. Lock-State ist ephemeral (UI-only).
 */
@Composable
fun NutrientGoalRow(
    nutrient: NutrientCatalog.Nutrient,
    effectiveDefault: Double,
    override: Double?,
    onChange: (Double) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val safeDefault = effectiveDefault.coerceAtLeast(1.0)
    val maxValue = (safeDefault * 2.0).toFloat()
    val committedValue = override ?: effectiveDefault

    var locked by remember(nutrient.key) { mutableStateOf(true) }
    var sliderPos by remember(nutrient.key, committedValue, locked) {
        mutableStateOf(committedValue.toFloat().coerceIn(0f, maxValue))
    }

    val displayValue = if (locked) committedValue else sliderPos.toDouble()
    val percent = ((displayValue / safeDefault) * 100.0).toInt().coerceIn(0, 999)
    val hasOverride = override != null

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nutrient.displayDe,
                    color = hm.fgPrimary,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${formatValue(displayValue)} ${nutrient.unit.label}  \u00B7  $percent %",
                    color = if (hasOverride || !locked) hm.fgPrimary else hm.fgTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (hasOverride || !locked) {
                    Text(
                        text = "Default: ${formatValue(effectiveDefault)} ${nutrient.unit.label}",
                        color = hm.fgTertiary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            IconButton(onClick = {
                if (locked) {
                    locked = false
                } else {
                    val committed = sliderPos.toDouble()
                    if (kotlin.math.abs(committed - committedValue) > 0.001) {
                        onChange(committed)
                    }
                    locked = true
                }
            }) {
                Icon(
                    imageVector = if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    contentDescription = if (locked) "${nutrient.displayDe} entsperren" else "${nutrient.displayDe} sperren & speichern",
                    tint = if (locked) hm.fgSecondary else hm.fgPrimary,
                )
            }
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (hasOverride) {
                    IconButton(onClick = {
                        onReset()
                        locked = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset ${nutrient.displayDe}",
                            tint = hm.fgSecondary,
                        )
                    }
                }
            }
        }
        Slider(
            value = sliderPos,
            onValueChange = { v -> sliderPos = v.coerceIn(0f, maxValue) },
            valueRange = 0f..maxValue,
            enabled = !locked,
            colors = SliderDefaults.colors(
                disabledThumbColor = hm.fgTertiary,
                disabledActiveTrackColor = hm.fgTertiary,
                disabledInactiveTrackColor = hm.fgTertiary,
            ),
        )
    }
}

private fun formatValue(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString()
    else "%.1f".format(v)
