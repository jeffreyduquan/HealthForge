package de.healthforge.presentation.plan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import de.healthforge.presentation.common.PhasePlaceholder

/** P1-Placeholder per REQ-NAV-003. Volle Funktion in P2. */
@Composable
fun PlanScreen() {
    PhasePlaceholder(
        title = "Plan",
        description = "Mahlzeiten-Wochenplaner \u2014 bald verf\u00fcgbar.",
        icon = Icons.Filled.CalendarMonth,
        phaseLabel = "P2",
    )
}
