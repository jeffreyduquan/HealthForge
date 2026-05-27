package de.healthforge.presentation.home.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * P7.S3a / REQ-HOME-WATER-BAR-001 — Farb-Cycle pro Wasser-Stufe.
 *
 * Eine "Stufe" entspricht 1×Tagesziel. Stufe 0 = 0..goal (Normalbedarf),
 * Stufe 1 = goal..2×goal (1. Überschuss-Lap), Stufe N = N×goal..(N+1)×goal.
 *
 * Stufen 0..9 erhalten je eine eigene Farbe innerhalb der Histamind-Palette.
 * Ab Stufe 10+ bleibt die Farbe identisch zu Stufe 9 (Endless-Stages).
 *
 * Farben sind so geordnet, dass sie semantisch eine Eskalation andeuten:
 * cool (cyan/violet/blue) → warm (teal/lime/amber) → alert (orange/red/magenta) → deep.
 */
private val StagePalette: List<Pair<Color, Color>> = listOf(
    // Stufe 0 — Standard Violet→Cyan (Default Histamind Accent)
    Color(0xFF7C5CFF) to Color(0xFF4DD0E1),
    // Stufe 1 — Cyan → Türkis
    Color(0xFF4DD0E1) to Color(0xFF22D3A6),
    // Stufe 2 — Türkis → Lime
    Color(0xFF22D3A6) to Color(0xFFA8E063),
    // Stufe 3 — Lime → Gold
    Color(0xFFA8E063) to Color(0xFFFFD166),
    // Stufe 4 — Gold → Amber
    Color(0xFFFFD166) to Color(0xFFFFB454),
    // Stufe 5 — Amber → Orange
    Color(0xFFFFB454) to Color(0xFFFF8A4C),
    // Stufe 6 — Orange → Coral
    Color(0xFFFF8A4C) to Color(0xFFFF5470),
    // Stufe 7 — Coral → Magenta
    Color(0xFFFF5470) to Color(0xFFD946EF),
    // Stufe 8 — Magenta → Deep Purple
    Color(0xFFD946EF) to Color(0xFF9333EA),
    // Stufe 9 — Deep Purple → Indigo (Maximum)
    Color(0xFF9333EA) to Color(0xFF4338CA),
)

/** Anzahl distinct definierter Stufen-Farben. */
const val WaterStageColorCount: Int = 10

/** Gibt das Farb-Paar (start, end) für die angegebene Stufe zurück. Stufen ≥ 9 → Stufe 9. */
fun waterStageGradient(stage: Int): Brush {
    val clamped = stage.coerceAtLeast(0).coerceAtMost(StagePalette.lastIndex)
    val (start, end) = StagePalette[clamped]
    return Brush.horizontalGradient(listOf(start, end))
}

/** Akzent-Farbe der Stufe (für Marker / Text / Thumb-Tint). */
fun waterStageAccent(stage: Int): Color {
    val clamped = stage.coerceAtLeast(0).coerceAtMost(StagePalette.lastIndex)
    return StagePalette[clamped].second
}

/**
 * Track-Farbe für die aktuelle Stufe: Akzentfarbe der **vorherigen** Stufe
 * mit 25 % Alpha. Zeigt "wo komme ich her" als gedimmter Hintergrund.
 *
 * Stufe 0 → `null` (kein Vorgänger), Aufrufer fällt dann auf
 * `LocalHmTokens.barTrack` zurück.
 */
fun waterStageTrackColor(stage: Int): Color? {
    if (stage <= 0) return null
    val prev = (stage - 1).coerceAtMost(StagePalette.lastIndex)
    return StagePalette[prev].second.copy(alpha = 0.25f)
}
