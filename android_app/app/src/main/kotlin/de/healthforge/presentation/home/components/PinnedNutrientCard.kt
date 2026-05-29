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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * **P7.S4 4e (Redesign):** Card hat 2 Modi, gesteuert via Chevron im Header:
 *  - `expanded = false` (default, steady-state): zeigt nur die gepinnten
 *    Nährstoffe als Progress-Rows + optional `trailingSlot` (Wasser-Slider).
 *  - `expanded = true` (Management-Modus): zeigt **alle** Nährstoffe gruppiert
 *    nach Kategorie (Makros / Vitamine / Mineralien / Sonstiges) als kompakte
 *    Toggle-Rows mit trailing PushPin-Icon (Filled = pinned, Outline = nicht).
 *    Tap auf das Icon → `onTogglePin(key)` (sofort persistiert, min. 1 Pin).
 *
 * Wasser wird genauso behandelt wie andere Nährstoffe: standardmäßig gepinnt
 * (siehe `NutrientCatalog.defaultPinnedKeys`), aber im Expanded-View normal
 * entpinnbar (Min-1-Invariant gilt in `HomeViewModel.togglePin`).
 *
 * @param entries Gepinnte Nährstoff-Entries in Anzeigereihenfolge (für
 *                Collapsed-View). HomeScreen filtert "water" raus, weil Wasser
 *                via `trailingSlot` als interaktiver Slider gerendert wird.
 * @param pinnedKeys Vollständige Liste der gepinnten Keys (inkl. "water").
 *                   Wird für die Filled/Outline-Anzeige des PushPin-Icons im
 *                   Expanded-View gebraucht.
 * @param expanded Aktueller Modus (siehe oben).
 * @param onToggleExpanded Callback für Chevron-Tap; `null` ⇒ Header wird nicht
 *                         gerendert (Backwards-Compat für Test-Previews).
 * @param onTogglePin Callback für PushPin-Tap im Expanded-View; `null` ⇒
 *                    Pin-Icons sind nicht klickbar (Read-only).
 * @param trailingSlot Optionaler Composable, der als **letzte Zeile** unter
 *                     allen Collapsed-Entries gerendert wird. Wird für die
 *                     interaktive Wasser-Zeile genutzt (REQ-HOME-WATER-BAR-001
 *                     v2: `WaterStageSlider`).
 */
@Composable
fun PinnedNutrientCard(
    entries: List<PinnedNutrientEntry>,
    pinnedKeys: List<String>,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
    onTogglePin: ((String) -> Unit)? = null,
    trailingSlot: (@Composable () -> Unit)? = null,
) {
    if (entries.isEmpty() && trailingSlot == null && onToggleExpanded == null) return
    val hm = LocalHmTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // P7.S4 4e — Header: Titel + Chevron (Expand-Toggle).
        if (onToggleExpanded != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (expanded) "Nährstoffe verwalten" else "Angepinnt",
                    style = MaterialTheme.typography.titleSmall,
                    color = hm.fgPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleExpanded, modifier = Modifier.size(40.dp)) {
                    // P7.S4 4e Revision 2: Chevron erh\u00e4lt sichtbare Pill-Aff. analog zum
                    // aktiven Pin-Icon (violet-tinted Background + Border). Im Expanded-Modus
                    // st\u00e4rker akzentuiert (Alpha 0.28), im Collapsed dezenter (Alpha 0.12).
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                hm.ambientViolet.copy(alpha = if (expanded) 0.28f else 0.12f)
                            )
                            .border(
                                width = 1.dp,
                                color = hm.ambientViolet.copy(alpha = if (expanded) 0.7f else 0.35f),
                                shape = RoundedCornerShape(50),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Verwaltung schlie\u00dfen" else "Alle N\u00e4hrstoffe anzeigen",
                            tint = if (expanded) hm.ambientViolet else hm.fgPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        if (!expanded) {
            // Steady-State: gepinnte Progress-Rows + Wasser-Slot.
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                entries.forEach { entry -> PinnedNutrientRow(entry = entry) }
                trailingSlot?.invoke()
            }
        } else {
            // Management-Modus: Kategorie-Sections mit Pin-Toggle pro Nährstoff.
            CategoryPinList(
                pinnedKeys = pinnedKeys,
                onTogglePin = onTogglePin,
            )
        }
    }
}

@Composable
private fun CategoryPinList(
    pinnedKeys: List<String>,
    onTogglePin: ((String) -> Unit)?,
) {
    val sections = listOf(
        "Makronährstoffe" to NutrientCatalog.ofCategory(NutrientCatalog.Category.MACRO),
        "Vitamine" to NutrientCatalog.ofCategory(NutrientCatalog.Category.VITAMIN),
        "Mineralien" to NutrientCatalog.ofCategory(NutrientCatalog.Category.MINERAL),
        "Sonstiges" to NutrientCatalog.ofCategory(NutrientCatalog.Category.WATER),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEach { (title, items) ->
            if (items.isEmpty()) return@forEach
            CategorySection(
                title = title,
                items = items,
                pinnedKeys = pinnedKeys,
                onTogglePin = onTogglePin,
            )
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    items: List<NutrientCatalog.Nutrient>,
    pinnedKeys: List<String>,
    onTogglePin: ((String) -> Unit)?,
) {
    val hm = LocalHmTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = hm.fgTertiary,
            fontWeight = FontWeight.SemiBold,
        )
        items.forEach { n ->
            val pinned = n.key in pinnedKeys
            CategoryPinRow(
                nutrient = n,
                pinned = pinned,
                onTogglePin = onTogglePin,
            )
        }
    }
}

@Composable
private fun CategoryPinRow(
    nutrient: NutrientCatalog.Nutrient,
    pinned: Boolean,
    onTogglePin: ((String) -> Unit)?,
) {
    val hm = LocalHmTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nutrient.displayDe,
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatNumber(nutrient.defaultPerDay)} ${nutrient.unit.label} / Tag",
                style = MaterialTheme.typography.bodySmall,
                color = hm.fgTertiary,
            )
        }
        IconButton(
            onClick = { onTogglePin?.invoke(nutrient.key) },
            modifier = Modifier.size(36.dp),
            enabled = onTogglePin != null,
        ) {
            // P7.S4 4e Revision 2: stärkerer visueller Unterschied
            //   aktiv   = filled PushPin, violetter Akzent, runder Glow-Hintergrund
            //   inaktiv = outlined PushPin, fgTertiary (gedämpft), kein Background
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (pinned) hm.ambientViolet.copy(alpha = 0.22f)
                        else androidx.compose.ui.graphics.Color.Transparent
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (pinned) "${nutrient.displayDe} entpinnen" else "${nutrient.displayDe} anpinnen",
                    tint = if (pinned) hm.ambientViolet else hm.fgTertiary,
                    modifier = Modifier.size(if (pinned) 18.dp else 16.dp),
                )
            }
        }
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
