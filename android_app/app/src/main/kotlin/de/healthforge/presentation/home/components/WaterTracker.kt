package de.healthforge.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Wasser-Tracker: zeigt Tagesfortschritt + Quick-Add 250/500ml + Custom-Button.
 *
 * REQ-WATER-001/002.
 */
@Composable
fun WaterTracker(
    currentMl: Int,
    goalMl: Int,
    onAdd: (Int) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = if (goalMl <= 0) 0f else (currentMl.toFloat() / goalMl).coerceIn(0f, 1f)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Wasser", style = MaterialTheme.typography.titleMedium)
                Text("$currentMl / $goalMl ml", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { onAdd(250) }, modifier = Modifier.weight(1f)) { Text("+250 ml") }
                Button(onClick = { onAdd(500) }, modifier = Modifier.weight(1f)) { Text("+500 ml") }
                OutlinedButton(onClick = onCustom, modifier = Modifier.weight(1f)) { Text("Anders") }
            }
        }
    }
}
