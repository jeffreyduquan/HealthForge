package de.healthforge.presentation.log

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.Composable
import de.healthforge.presentation.common.PhasePlaceholder

/** P1-Placeholder per REQ-NAV-003/004. Symptom-Tagebuch in P3. */
@Composable
fun LogScreen() {
    PhasePlaceholder(
        title = "Log",
        description = "Symptom-Tagebuch (Mood / Schlaf / Symptome) \u2014 bald verf\u00fcgbar.",
        icon = Icons.Filled.BookmarkBorder,
        phaseLabel = "P3",
    )
}
