package de.healthforge.presentation.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.presentation.theme.LocalHmTokens

/**
 * Wasser-Tracker: zeigt Tagesfortschritt + Quick-Add 250/500ml + Custom-Button.
 *
 * REQ-WATER-001/002, REQ-REMIND-001 (Reminder-Toggle).
 * P6.S7 F-005: Long-Press auf Quick-Add → Undo-Snackbar (siehe HomeScreen-Host).
 * P6.S7 F-006: Helper-Text unter Reminder-Toggle erklärt Intervall + Zeitfenster.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaterTracker(
    currentMl: Int,
    goalMl: Int,
    onAdd: (Int) -> Unit,
    onCustom: () -> Unit,
    reminderEnabled: Boolean,
    onReminderToggle: (Boolean) -> Unit,
    onUndoLast: () -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val ratio = if (goalMl <= 0) 0f else (currentMl.toFloat() / goalMl).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Wasser",
                style = MaterialTheme.typography.titleMedium,
                color = hm.fgPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$currentMl / $goalMl ml",
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgSecondary,
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickWaterButton(
                label = "+250 ml",
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = { onAdd(250) },
                onLongClick = if (canUndo) onUndoLast else null,
            )
            QuickWaterButton(
                label = "+500 ml",
                filled = true,
                modifier = Modifier.weight(1f),
                onClick = { onAdd(500) },
                onLongClick = if (canUndo) onUndoLast else null,
            )
            QuickWaterButton(
                label = "Anders",
                filled = false,
                modifier = Modifier.weight(1f),
                onClick = onCustom,
                onLongClick = null,
            )
        }
        Text(
            "Tipp: lange tippen \u2192 letzten Eintrag r\u00fcckg\u00e4ngig.",
            style = MaterialTheme.typography.labelSmall,
            color = hm.fgTertiary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Erinnerungen",
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgPrimary,
            )
            Switch(checked = reminderEnabled, onCheckedChange = onReminderToggle)
        }
        // F-006: Helper-Text — erkl\u00e4rt Reminder-Intervall + Zeitfenster.
        Text(
            "Erinnerung alle 2 Stunden zwischen 08:00 und 22:00 Uhr.",
            style = MaterialTheme.typography.labelSmall,
            color = hm.fgTertiary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickWaterButton(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(14.dp)
    val base = modifier
        .heightIn(min = 44.dp)
        .clip(shape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    val styled = if (filled) {
        base.background(hm.accentGradient)
    } else {
        base.border(1.dp, hm.glassBorder, shape)
    }
    Box(modifier = styled, contentAlignment = Alignment.Center) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) Color.White else hm.fgPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
