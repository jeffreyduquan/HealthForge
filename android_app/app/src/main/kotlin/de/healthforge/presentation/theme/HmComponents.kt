package de.healthforge.presentation.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// HealthForge Hm-Components — P6.S3 (LOCKED 2026-05-26)
// Source: docs/HistamindDesignReference.md §5
// Re-usable composables fed by LocalHmTokens + Hm color palette.
// =============================================================================

// -----------------------------------------------------------------------------
// 5.1 GlassCard
// -----------------------------------------------------------------------------

/**
 * Card with glass-morphic appearance in dark mode (vertical white-alpha gradient
 * + 1dp hairline border + 40dp ambient shadow) and clean solid surface in light mode.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(24.dp)
    val styled = if (hm.isGlassEnabled) {
        Modifier
            .shadow(elevation = 40.dp, shape = shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(hm.cardSurface)
            .background(Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom)))
            .border(1.dp, hm.glassBorder, shape)
    } else {
        Modifier
            .clip(shape)
            .background(hm.cardSurface)
            .border(1.dp, hm.glassBorder, shape)
    }
    Box(modifier.then(styled).padding(padding)) { content() }
}

// -----------------------------------------------------------------------------
// 5.2 SectionPill
// -----------------------------------------------------------------------------

/** Small uppercase section header preceded by a 3×14dp accent-gradient stripe. */
@Composable
fun SectionPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(hm.accentGradient),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = ManropeFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.W800,
                letterSpacing = 1.4.sp,
                color = hm.fgTertiary,
            ),
        )
    }
}

// -----------------------------------------------------------------------------
// 5.3 GradientFab
// -----------------------------------------------------------------------------

/** Circular FAB with accent-gradient fill and violet glow shadow. */
@Composable
fun GradientFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    icon: @Composable () -> Unit,
) {
    val hm = LocalHmTokens.current
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .shadow(elevation = 24.dp, shape = CircleShape, ambientColor = hm.violetGlow, spotColor = hm.violetGlow)
            .background(hm.accentGradient, CircleShape),
        containerColor = Color.Transparent,
        contentColor = hm.fgPrimary,
        shape = CircleShape,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
    ) { icon() }
}

// -----------------------------------------------------------------------------
// 5.4 GradientButton
// -----------------------------------------------------------------------------

/** Primary CTA button — 56dp tall, full-width by default, accent-gradient fill. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val hm = LocalHmTokens.current
    val shape = RoundedCornerShape(18.dp)
    val bg: Brush = if (enabled) hm.accentGradient
        else Brush.linearGradient(listOf(hm.fgTertiary, hm.fgTertiary))
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(bg)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = hm.fgPrimary,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = ManropeFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.W700,
                letterSpacing = 0.3.sp,
            ),
        )
    }
}

// -----------------------------------------------------------------------------
// 5.5 AmbientBackdrop
// -----------------------------------------------------------------------------

/**
 * Animated radial-gradient blobs (violet / cyan / good) drifting slowly across the
 * background. Disabled in light mode (solid background only).
 *
 * Place as the FIRST child inside a Box(Modifier.fillMaxSize) so it sits below the
 * content layer.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val hm = LocalHmTokens.current
    if (!hm.isGlassEnabled) {
        Box(modifier.fillMaxSize().background(hm.background))
        return
    }
    val transition = rememberInfiniteTransition(label = "ambient")
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob1",
    )
    val t2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(55_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob2",
    )
    val t3 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blob3",
    )
    Box(
        modifier
            .fillMaxSize()
            .background(hm.background)
            .drawWithCache {
                onDrawBehind {
                    val w = size.width; val h = size.height
                    val r = maxOf(w, h) * 0.6f
                    // blob 1 — violet, drifting top-left ↔ top-right
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(hm.ambientViolet.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(w * (0.2f + 0.6f * t1), h * 0.18f),
                            radius = r,
                        ),
                    )
                    // blob 2 — cyan, drifting bottom-right ↔ bottom-left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(hm.ambientCyan.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(w * (0.8f - 0.5f * t2), h * 0.85f),
                            radius = r,
                        ),
                    )
                    // blob 3 — good (green), drifting center-y
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(StatusGood.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(w * 0.5f, h * (0.3f + 0.4f * t3)),
                            radius = r * 0.7f,
                        ),
                    )
                }
            },
    )
}

// -----------------------------------------------------------------------------
// 5.6 GradientText
// -----------------------------------------------------------------------------

/** Text whose pixels are tinted with the accent gradient via BlendMode.SrcAtop. */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontFamily = ManropeFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.5).sp,
    ),
) {
    val hm = LocalHmTokens.current
    Text(
        text = text,
        style = style.copy(color = Color.White),
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = hm.accentGradient, blendMode = BlendMode.SrcAtop)
                }
            },
    )
}

// -----------------------------------------------------------------------------
// 5.7 SegmentedTabs
// -----------------------------------------------------------------------------

/** N-tab toggle. Glass-pill background; active tab gets accent-gradient fill. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val outerShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(14.dp)
    val outerBg: Brush = if (hm.isGlassEnabled)
        Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom))
    else Brush.linearGradient(listOf(hm.cardSurface, hm.cardSurface))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(outerShape)
            .background(outerBg)
            .border(1.dp, hm.glassBorder, outerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            SegmentedTab(
                label = label,
                active = index == selectedIndex,
                onClick = { onSelect(index) },
                hm = hm,
                innerShape = innerShape,
            )
        }
    }
}

@Composable
private fun RowScope.SegmentedTab(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    hm: HmTokens,
    innerShape: androidx.compose.foundation.shape.RoundedCornerShape,
) {
    val interaction = remember { MutableInteractionSource() }
    val bg: Brush = if (active) hm.accentGradient
        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(innerShape)
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) hm.fgPrimary else hm.fgSecondary,
            style = TextStyle(
                fontFamily = ManropeFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 0.3.sp,
            ),
        )
    }
}

// -----------------------------------------------------------------------------
// 5.8 SeverityBar
// -----------------------------------------------------------------------------

/**
 * Vertical 4dp×56dp bar for Log entries. Color depends on severity 1..5.
 * 1=good, 2=good@80, 3=relax, 4=overUl@80, 5=overUl.
 */
@Composable
fun SeverityBar(
    severity: Int,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
) {
    val color = when (severity.coerceIn(1, 5)) {
        1 -> StatusGood
        2 -> StatusGood.copy(alpha = 0.8f)
        3 -> StatusRelax
        4 -> StatusOverUl.copy(alpha = 0.8f)
        else -> StatusOverUl
    }
    Box(
        modifier
            .width(4.dp)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}
