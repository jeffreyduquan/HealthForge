package de.healthforge.presentation.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.presentation.home.components.waterStageAccent
import de.healthforge.presentation.home.components.waterStageGradient
import de.healthforge.presentation.home.components.waterStageTrackColor
import de.healthforge.presentation.theme.LocalHmTokens
import kotlin.math.roundToInt

/**
 * P7.S5 — Unified nutrient slider bar.
 *
 * Looks like [HfNutrientProgressRow] (8dp Canvas, stage-colored gradient,
 * Lv-Badge, %) but has a draggable [Slider] overlay — combines display + input.
 *
 * Layout:
 *   Eiweiß                    50 / 120 g  Lv 0  42%
 *   ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░  ← Canvas bar + Slider thumb
 *
 * Without [dgeTarget]:
 *   Eiweiß                    50.0 g
 *   ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░
 *
 * @param label      Display name
 * @param value      Current value in unit
 * @param unit       Unit label (e.g. "g", "mg", "µg")
 * @param min        Slider minimum
 * @param max        Slider maximum
 * @param onChange   Called on drag release with new value
 * @param dgeTarget  Optional DGE reference for % and Lv-Badge
 * @param nonLinear  If true, scale is quadratic (finer at low end). Slider goes 0..1 internally.
 */
@Composable
fun NutrientSliderBar(
    label: String,
    value: Float,
    unit: String,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    dgeTarget: Float? = null,
    nonLinear: Boolean = false,
    onChange: (Float) -> Unit,
) {
    val hm = LocalHmTokens.current

    // For non-linear: internal slider is 0..1, display = min + position² * range
    val range = max - min
    val internalMax = if (nonLinear) 1f else max
    val sliderPos = if (nonLinear && range > 0f) {
        kotlin.math.sqrt(((value - min) / range).coerceIn(0f, 1f))
    } else value
    var sliderVal by remember(value) { mutableFloatStateOf(sliderPos) }

    val displayValue = if (nonLinear && range > 0f) {
        min + (sliderVal * sliderVal) * range
    } else sliderVal

    val frac = if (nonLinear) sliderVal.coerceIn(0f, 1f)
               else if (range > 0f) ((sliderVal - min) / range).coerceIn(0f, 1f)
               else 0f

    val stage: Int
    val pctInt: Int?
    val accent: Color?
    val gradient: Any? // Brush
    val trackTint: Color

    if (dgeTarget != null && dgeTarget > 0f) {
        val pct = (displayValue / dgeTarget * 100.0).coerceIn(0.0, 999.0)
        stage = (pct / 100.0).toInt()
        val withinStage = (pct % 100.0) / 100.0
        pctInt = (withinStage * 100).roundToInt()
        accent = waterStageAccent(stage)
        gradient = waterStageGradient(stage)
        trackTint = waterStageTrackColor(stage) ?: hm.barTrack
    } else {
        stage = 0
        pctInt = null
        accent = null
        gradient = null
        trackTint = hm.barTrack
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (displayValue > 0f) "${"%.0f".format(displayValue)} $unit" else "— $unit",
                style = MaterialTheme.typography.bodySmall,
                color = hm.fgSecondary,
            )
            if (stage >= 1 && accent != null) {
                Spacer(Modifier.width(6.dp))
                NutrientStageBadge(stage = stage, color = accent)
            }
            if (pctInt != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$pctInt%",
                    style = MaterialTheme.typography.labelSmall,
                    color = hm.fgTertiary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Canvas bar + Slider overlay
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center),
            ) {
                val w = size.width
                val h = size.height
                val corner = CornerRadius(h / 2f, h / 2f)
                drawRoundRect(color = trackTint, size = Size(w, h), cornerRadius = corner)
                val fillW = (w * frac).coerceAtMost(w)
                if (fillW > 0f && gradient != null) {
                    @Suppress("UNCHECKED_CAST")
                    drawRoundRect(
                        brush = gradient as androidx.compose.ui.graphics.Brush,
                        size = Size(fillW, h),
                        cornerRadius = corner,
                    )
                } else if (fillW > 0f) {
                    drawRoundRect(
                        color = hm.ambientViolet,
                        size = Size(fillW, h),
                        cornerRadius = corner,
                    )
                }
            }

            Slider(
                value = sliderVal,
                onValueChange = { sliderVal = it },
                onValueChangeFinished = {
                    val result = if (nonLinear && range > 0f) {
                        min + (sliderVal * sliderVal) * range
                    } else sliderVal
                    onChange(result)
                },
                valueRange = 0f..internalMax,
                colors = SliderDefaults.colors(
                    thumbColor = accent ?: hm.ambientViolet,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            )
        }
    }
}
