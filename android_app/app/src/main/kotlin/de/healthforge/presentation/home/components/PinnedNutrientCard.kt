package de.healthforge.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.theme.LocalHmTokens
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * P7.S3 / REQ-HOME-NUTRIENT-LIST-001 — Wert + Ziel pro angepinntem Nährstoff,
 * eine Zeile pro Nutrient, Layout:
 *
 *   Eiweiß                      72 / 120 g · Lv 0
 *   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 60%
 *
 * Stufen-Logik (P7.S3.b, einheitlich mit Wasser):
 *  - Stufe N = `N×goal..(N+1)×goal`
 *  - Bar zeigt 0..100 % der **aktuellen Stufe** (Roll-over bei Stufen-Übergang)
 *  - Farbe = `waterStageGradient(stage)` (Single Source of Truth)
 *  - Track = Vorgängerstufen-Akzent × 0.25 Alpha (Stufe 0 → `hm.barTrack`)
 *  - Lv-Badge rechts ab Stufe ≥ 1.
 *
 * @param entries Liste von [PinnedNutrientEntry] in Anzeigereihenfolge
 *                (= persistierte Pin-Reihenfolge, P7.S5 Drag-Reorder).
 * @param trailingSlot Optionaler Composable, der als **letzte Zeile** unter
 *                     allen Entries gerendert wird. Wird für die interaktive
 *                     Wasser-Zeile genutzt (REQ-HOME-WATER-BAR-001 v2:
 *                     `WaterStageSlider`), die zwar wie ein Pin aussieht, aber
 *                     einen Slider mit Stufen-Logik enthält.
 */
@Composable
fun PinnedNutrientCard(
    entries: List<PinnedNutrientEntry>,
    modifier: Modifier = Modifier,
    trailingSlot: (@Composable () -> Unit)? = null,
) {
    if (entries.isEmpty() && trailingSlot == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        entries.forEach { entry -> PinnedNutrientRow(entry) }
        trailingSlot?.invoke()
    }
}

@Composable
private fun PinnedNutrientRow(entry: PinnedNutrientEntry) {
    val hm = LocalHmTokens.current
    val n = NutrientCatalog.byKeyOrNull(entry.key)
    val displayName = n?.displayDe ?: entry.key
    val unit = n?.unit?.label ?: ""
    val target = entry.targetPerDay.coerceAtLeast(0.001)

    // Stufenmechanik analog WaterStageSlider/LeveledPowerBar.
    val stage = floor(entry.current / target).toInt().coerceAtLeast(0)
    val withinStage = (entry.current - stage * target).coerceAtLeast(0.0)
    val frac = (withinStage / target).coerceIn(0.0, 1.0)
    // Exakter Stufen-Treffer (current == N*goal) → Bar voll in Stufe N-1 ist
    // unintuitiv; wir interpretieren als "gerade Stufe N erreicht, 0 % drin".
    val pct = (frac * 100).roundToInt()

    val accent = waterStageAccent(stage)
    val gradient = waterStageGradient(stage)
    val trackTint = waterStageTrackColor(stage) ?: hm.barTrack

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatNumber(entry.current)} / ${formatNumber(entry.targetPerDay)} $unit",
                style = MaterialTheme.typography.bodySmall,
                color = hm.fgSecondary,
            )
            if (stage >= 1) {
                Spacer(Modifier.width(6.dp))
                StageBadge(stage = stage, color = accent)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall,
                color = hm.fgTertiary,
            )
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(h / 2f, h / 2f)
            drawRoundRect(color = trackTint, size = Size(w, h), cornerRadius = corner)
            val fillW = (w * frac.toFloat()).coerceAtMost(w)
            if (fillW > 0f) {
                drawRoundRect(
                    brush = gradient,
                    size = Size(fillW, h),
                    cornerRadius = corner,
                )
            }
        }
    }
}

/**
 * Lv-Badge — kleiner Pill rechts neben dem Wert/Ziel-Text. Nur für Stufen ≥ 1.
 * Farbe folgt der aktuellen Stufenfarbe.
 */
@Composable
private fun StageBadge(stage: Int, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = "Lv $stage",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            ),
            color = color,
        )
    }
}

/**
 * P7.S3 — UI-Daten für eine gepinnte Nährstoff-Zeile.
 * `key` ist ein [NutrientCatalog]-Key. `current` und `targetPerDay` in der
 * Einheit des Nutrients (kcal/g/mg/µg/ml).
 */
data class PinnedNutrientEntry(
    val key: String,
    val current: Double,
    val targetPerDay: Double,
)

/** Formatiert klein/groß abhängig vom Range — kompakte Anzeige im Header. */
private fun formatNumber(v: Double): String = when {
    v >= 100.0 -> v.roundToInt().toString()
    v >= 10.0 -> "%.1f".format(v)
    else -> "%.2f".format(v).trimEnd('0').trimEnd('.', ',')
}
