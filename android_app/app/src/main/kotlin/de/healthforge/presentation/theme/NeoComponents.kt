package de.healthforge.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Neo-Bio section header — tracking-wide uppercase grey label.
 * Replaces the legacy `SectionPill` chip for the P7 home redesign.
 */
@Composable
fun NeoSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
        ),
        color = hm.fgTertiary,
        modifier = modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp),
    )
}

/**
 * Neo-Bio borderless dark card. Plain `cardSurface` fill, 20dp rounded, thin border, no overlay gradient.
 * Used by the P7 Home redesign in place of [GlassCard] where the design calls for borderless cards.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    content: @Composable () -> Unit,
) {
    val hm = LocalHmTokens.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(hm.cardSurface)
            .border(1.dp, hm.cardBorder, RoundedCornerShape(20.dp))
            .padding(contentPadding),
    ) {
        content()
    }
}
