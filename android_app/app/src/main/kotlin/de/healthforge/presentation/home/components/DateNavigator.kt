package de.healthforge.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Vor/Zurück-Navigation für den Home-Datums-Kontext (REQ-HOME-004).
 * "Heute" wird statt Datum angezeigt; gestern/morgen analog.
 */
@Composable
fun DateNavigator(
    date: LocalDate,
    onChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Heute"
        today.minusDays(1) -> "Gestern"
        today.plusDays(1) -> "Morgen"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onChange(date.minusDays(1)) }) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Vorheriger Tag")
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        IconButton(
            onClick = { onChange(date.plusDays(1)) },
            enabled = date < today.plusDays(1),
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "N\u00e4chster Tag")
        }
    }
}
