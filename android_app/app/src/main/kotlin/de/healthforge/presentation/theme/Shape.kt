package de.healthforge.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// LOCKED corner-radius tokens — P6.S2 (source: docs/HistamindDesignReference.md §4)
// Chips: 14dp · Buttons: 18dp · Cards/Sheets: 24dp · FAB-Extended: 28dp
// Supersedes: 4/8/12/16/28 from GUI.md §5 (v0.1).

val HealthForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(14.dp),  // Chips, TextFields, Snackbar
    medium     = RoundedCornerShape(18.dp),  // Buttons, Segmented-Tabs
    large      = RoundedCornerShape(24.dp),  // Cards, Dialogs, Bottom-Sheets, FAB
    extraLarge = RoundedCornerShape(28.dp),  // FAB-Extended
)
