package de.healthforge.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * REQ-PROFILE-LAYOUT-001 / Profil-Redesign — Tagesziel-Zeile mit Lock-Slider.
 * Range = 0 … 2 × Profil-Default (=0–200 %). Lock-Toggle (ephemeral) sperrt
 * Slider-Drag. Re-Lock committed den Wert via [onChange]. Reset-Icon (nur bei
 * aktivem Override) entfernt den Override.
 *
 * Persistenz-Fix: [sliderPos] ist die Single-Source-of-Truth für die Anzeige.
 * Externe Updates (DB-Flow, Reset) werden via [LaunchedEffect] in sliderPos
 * gespiegelt, aber NUR wenn locked = true (kein Sync während Drag).
 *
 * Kompakter Stil: IconButton 32 dp, Icons 16 dp, Slider-Track 2 dp, Thumb 12 dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var sliderPos by remember(nutrient.key) {
        mutableStateOf(committedValue.toFloat().coerceIn(0f, maxValue))
    }

    LaunchedEffect(committedValue) {
        // Sync nur wenn nicht gerade aktiv gedraggt wird UND der Wert sich wirklich
        // unterscheidet. Triggert NICHT beim Lock-Flip — dadurch bleibt der gerade
        // committed-te Wert sichtbar, bis das DB-Flow-Echo mit demselben Wert kommt.
        if (locked) {
            val target = committedValue.toFloat().coerceIn(0f, maxValue)
            if (kotlin.math.abs(target - sliderPos) > 0.001f) {
                sliderPos = target
            }
        }
    }

    val displayValue = sliderPos.toDouble()
    val percent = ((displayValue / safeDefault) * 100.0).toInt().coerceIn(0, 999)
    val hasOverride = override != null

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nutrient.displayDe,
                    color = hm.fgPrimary,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "${formatValue(displayValue)} ${nutrient.unit.label}  \u00B7  $percent %",
                    color = if (hasOverride || !locked) hm.fgPrimary else hm.fgTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(
                onClick = {
                    if (locked) {
                        locked = false
                    } else {
                        val committed = sliderPos.toDouble()
                        if (kotlin.math.abs(committed - committedValue) > 0.001) {
                            onChange(committed)
                        }
                        locked = true
                    }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    contentDescription = if (locked) "${nutrient.displayDe} entsperren" else "${nutrient.displayDe} sperren & speichern",
                    tint = if (locked) hm.fgSecondary else hm.fgPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                if (hasOverride) {
                    IconButton(
                        onClick = {
                            onReset()
                            locked = true
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset ${nutrient.displayDe}",
                            tint = hm.fgSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        val interactionSource = remember(nutrient.key) { MutableInteractionSource() }
        val sliderColors = SliderDefaults.colors(
            disabledThumbColor = hm.fgTertiary,
            disabledActiveTrackColor = hm.fgTertiary,
            disabledInactiveTrackColor = hm.fgTertiary.copy(alpha = 0.4f),
        )
        Slider(
            value = sliderPos,
            onValueChange = { v -> sliderPos = v.coerceIn(0f, maxValue) },
            valueRange = 0f..maxValue,
            enabled = !locked,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            colors = sliderColors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = sliderColors,
                    enabled = !locked,
                    thumbSize = DpSize(12.dp, 12.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    enabled = !locked,
                    colors = sliderColors,
                    modifier = Modifier.height(2.dp),
                    drawStopIndicator = null,
                )
            },
        )
    }
}

private fun formatValue(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString()
    else "%.1f".format(v)
