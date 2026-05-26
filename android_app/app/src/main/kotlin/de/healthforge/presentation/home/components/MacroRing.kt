package de.healthforge.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Single ring showing `current / target` as an arc.
 *
 * REQ-HOME-001 / REQ-HOME-004.
 */
@Composable
fun MacroRing(
    label: String,
    current: Double,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val ratio = if (target <= 0) 0f else (current / target.toDouble()).coerceIn(0.0, 1.0).toFloat()
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(96.dp)) {
                val stroke = Stroke(width = 10.dp.toPx())
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width, size.height),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * ratio,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.width, size.height),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${current.toInt()}", style = MaterialTheme.typography.titleMedium)
                Text("/ $target $unit", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Row showing four [MacroRing]s side-by-side. */
@Composable
fun MacroRingRow(
    kcal: Double, kcalTarget: Int,
    proteinG: Double, proteinTarget: Int,
    carbsG: Double, carbsTarget: Int,
    fatG: Double, fatTarget: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MacroRing("kcal", kcal, kcalTarget, "kcal", cs.primary)
        MacroRing("Protein", proteinG, proteinTarget, "g", cs.tertiary)
        MacroRing("Kohlenh.", carbsG, carbsTarget, "g", cs.secondary)
        MacroRing("Fett", fatG, fatTarget, "g", cs.error)
    }
}
