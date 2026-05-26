package de.healthforge.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// LOCKED corner-radius tokens — source: docs/GUI.md §5
// Cards: 8dp · Buttons: 12dp · Chips: 8dp · TextFields: 8dp · Dialogs/Sheets: 16dp

val HealthForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),   // Cards, Chips, TextFields, Snackbar, Thumbnails
    medium     = RoundedCornerShape(12.dp),  // Buttons
    large      = RoundedCornerShape(16.dp),  // Dialogs, Bottom-Sheets, FAB
    extraLarge = RoundedCornerShape(28.dp),  // FAB Extended
)
