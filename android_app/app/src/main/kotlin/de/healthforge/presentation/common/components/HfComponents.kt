package de.healthforge.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.ManropeFamily
import kotlin.math.roundToInt

// =============================================================================
// HealthForge Unified Components — P7.S5 Consistency Refactor
// =============================================================================
// MASTER SINGLETONS for ALL screens. Replaces scattered GlassCard/NeoCard/
// ElevatedCard/Card, SectionPill/NeoSectionLabel, MacroRow/NutriRow, etc.
// =============================================================================

// ─────────────────────────────────────────────────────────────────────────────
// HfCard — Unified Master Card
// ─────────────────────────────────────────────────────────────────────────────
/** Corner radius for all HfCards (16dp — matches IntakeCard master design). */
private val HfCardRadius = RoundedCornerShape(16.dp)

/**
 * Unified card component — THE single card design for the entire app.
 * Uses HmTokens for consistent theming in light + dark mode.
 * Replaces: GlassCard, NeoCard, ElevatedCard, Card, IntakeCard (wrapper).
 */
@Composable
fun HfCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val hm = LocalHmTokens.current
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(HfCardRadius)
        .background(hm.cardSurface)
        .border(1.dp, hm.cardBorder, HfCardRadius)

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        baseModifier
    }

    Box(modifier = finalModifier.padding(contentPadding)) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfSectionHeader — Unified Section Label
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Unified section header. Replaces both SectionPill (gradient stripe)
 * and NeoSectionLabel (uppercase grey).
 *
 * Style: uppercase, semi-bold, hm.fgTertiary, letterSpacing 1.2sp.
 * Compact — no accent stripe to keep it clean across all contexts.
 */
@Composable
fun HfSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
        color = hm.fgTertiary,
        modifier = modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HfValueRow — Unified Label-Value Row
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Single source of truth for label-value rows.
 * Replaces: MacroRow (×2 variants), NutriRow.
 *
 * Layout: [Label] ──flex── [Value]  [optional trailing slot]
 */
@Composable
fun HfValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val hm = LocalHmTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = hm.fgSecondary,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = hm.fgPrimary,
        )
        trailing?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfNutrientProgressRow — Value + DGE Progress Bar
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Nutrient row with a compact horizontal progress bar showing % of daily goal
 * (DGE Referenzwert). Used in ALL detail screens (ingredient, recipe, supplement).
 *
 * Layout:
 *   Eiweiß                    18.5 g   25%
 *   ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░
 *
 * Bar color uses [waterStageGradient] from HmTokens for consistency with Home.
 * Bar height: 4dp. Track: hm.barTrack. Corner radius: 2dp.
 *
 * @param label  Nutrient display name (e.g. "Eiweiß")
 * @param value  Formatted value string (e.g. "18.5 g")
 * @param percentDge 0..100+ percentage of daily goal
 */
@Composable
fun HfNutrientProgressRow(
    label: String,
    value: String,
    percentDge: Double,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val pct = percentDge.coerceIn(0.0, 999.0)
    val stage = (pct / 100.0).toInt() // 0=0-99%, 1=100-199%, etc.
    val stageProgress = (pct % 100.0).coerceIn(0.0, 100.0) / 100.0
    val barColor = waterStageColor(stage)
    val trackColor = hm.barTrack

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgSecondary,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = hm.fgPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${pct.roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = barColor,
            )
        }
        Spacer(Modifier.height(4.dp))
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(stageProgress.toFloat().coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor),
            )
        }
    }
}

/**
 * Stage-based bar color — mirrors [waterStageGradient] logic from Home.
 * Stage 0: green (StatusGood), Stage 1: yellow (StatusRelax), Stage 2+: red (StatusOverUl).
 */
@Composable
private fun waterStageColor(stage: Int): Color {
    val hm = LocalHmTokens.current
    return when {
        stage <= 0 -> de.healthforge.presentation.theme.StatusGood
        stage == 1 -> de.healthforge.presentation.theme.StatusRelax
        else -> de.healthforge.presentation.theme.StatusOverUl
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfAddToHomeButton — Sticky Lilac Bottom CTA
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Prominent sticky bottom bar with lilac gradient. "ZUM HOME-SCREEN HINZUFÜGEN".
 * Used in ALL detail screens. Positioned OUTSIDE the scroll container,
 * fixed at the bottom of the screen.
 */
@Composable
fun HfAddToHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    val bg: Brush = if (enabled) hm.accentGradient
        else Brush.linearGradient(listOf(hm.fgTertiary, hm.fgTertiary))
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "🏠",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "ZUM HOME-SCREEN HINZUFÜGEN",
                color = hm.fgPrimary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfEmptyState — Unified Empty State
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HfEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val hm = LocalHmTokens.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = hm.fgSecondary,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfLoadingState — Unified Centered Loader
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HfLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfSourceBadge — Source + FDC-ID Badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HfSourceBadge(
    source: String,
    fdcId: String? = null,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(hm.ambientViolet.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = hm.ambientViolet,
                fontWeight = FontWeight.SemiBold,
            )
        }
        fdcId?.let { fdc ->
            Spacer(Modifier.width(8.dp))
            Text(
                text = "#$fdc",
                style = MaterialTheme.typography.labelSmall,
                color = hm.fgTertiary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HfThumbnail — Unified Image Thumbnail
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HfThumbnail(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val hm = LocalHmTokens.current
    if (imageUrl != null) {
        coil.compose.AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, hm.cardBorder, RoundedCornerShape(8.dp)),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Format helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Smart rounding: >=100 → int, >=10 → 1 decimal, <10 → 2 decimals. */
fun formatNutrientValue(v: Double): String = when {
    v >= 100 -> v.toInt().toString()
    v >= 10 -> "%.1f".format(v)
    else -> "%.2f".format(v)
}
