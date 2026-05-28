package de.healthforge.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * REQ-PROFILE-LAYOUT-001 / GUI.md (P7) — Eine Zeile im Profil-Tagesziele-Block.
 *
 * Zeigt Naehrstoff-Label + read-only Default-Wert + Override-NumberField + Reset-Icon.
 * - `override` = null  → Field zeigt Default als Hint, Reset-Icon ausgeblendet.
 * - `override` != null → Field zeigt Override-Wert, Reset-Icon sichtbar.
 *
 * Persistenz erfolgt durch Caller (ProfileViewModel) via [onChange]/[onReset].
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
    var draft by remember(override) { mutableStateOf(override?.let { formatValue(it) } ?: "") }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                nutrient.displayDe,
                color = hm.fgPrimary,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Default: ${formatValue(effectiveDefault)} ${nutrient.unit.label}",
                color = hm.fgTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { new ->
                // Erlaubt Komma + Punkt; leer = Reset auf Default.
                val sanitized = new.replace(',', '.').filter { it.isDigit() || it == '.' }
                draft = sanitized
                val parsed = sanitized.toDoubleOrNull()
                if (parsed != null) {
                    val clamped = parsed.coerceIn(nutrient.min, nutrient.max)
                    onChange(clamped)
                }
            },
            modifier = Modifier.width(96.dp),
            placeholder = { Text(formatValue(effectiveDefault), style = MaterialTheme.typography.bodySmall) },
            suffix = { Text(nutrient.unit.label, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
        )
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (override != null) {
                IconButton(onClick = {
                    draft = ""
                    onReset()
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
}

/** Formatiert Doubles als Int wenn ganzzahlig, sonst mit max. 1 Nachkommastelle. */
private fun formatValue(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString()
    else "%.1f".format(v)
