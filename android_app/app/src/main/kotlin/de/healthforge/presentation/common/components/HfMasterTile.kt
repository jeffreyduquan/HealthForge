package de.healthforge.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.LocalSemanticColors

// =============================================================================
// HfMasterTile — THE unified card for ALL item types across ALL screens
// =============================================================================
// Used in: Home (IntakeCard), Essen (all 3 tabs), Gruppen (recipes)
//
// Layout:
// ┌──────────────────────────────────────────┐
// │ [Img]  Name                      Source  │
// │        Subtitle                          │
// │        ────────────────────────────────  │
// │        📊 LABEL                          │
// │        Nutrient  value  ▓▓▓▓▓░░░  XX%   │  ← repeated per pinned nutrient
// │        ────────────────────────────────  │
// │        ❤ 142  👍 89%  👎 11%  [Action]  │
// └──────────────────────────────────────────┘
// =============================================================================

data class MasterTileNutrient(
    val key: String,
    val label: String,
    val value: String,
    val percentDge: Double,
)

@Composable
fun HfMasterTile(
    title: String,
    subtitle: String,
    nutrients: List<MasterTileNutrient>,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    sourceBadge: String? = null,
    // Rating (optional)
    likeCount: Long = 0,
    liked: Boolean = false,
    recommendCount: Long = 0,
    notRecommendCount: Long = 0,
    myCommunityRating: String? = null,
    // Actions
    onClick: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    onRate: ((String?) -> Unit)? = null,
    trailingSlot: (@Composable () -> Unit)? = null,
    // Label for nutrient section
    nutrientLabel: String = "PRO 100 G",
) {
    val hm = LocalHmTokens.current
    val sem = LocalSemanticColors.current
    val hasRating = onToggleLike != null || onRate != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(hm.cardSurface)
            .border(1.dp, hm.cardBorder, RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ── Header: Image + Title + Source ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (imageUrl != null) {
                    coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, hm.cardBorder, RoundedCornerShape(8.dp)),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = hm.fgPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = hm.fgSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (sourceBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(hm.ambientViolet.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = sourceBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = hm.ambientViolet,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // ── Nutrient Progress Bars ──
            if (nutrients.isNotEmpty()) {
                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.2f))
                Text(
                    text = nutrientLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = hm.fgTertiary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    nutrients.forEach { nut ->
                        HfNutrientProgressRow(
                            label = nut.label,
                            value = nut.value,
                            percentDge = nut.percentDge,
                        )
                    }
                }
            }

            // ── Footer: Rating + Trailing ──
            if (hasRating || trailingSlot != null) {
                HorizontalDivider(color = hm.fgTertiary.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (hasRating) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (onToggleLike != null) {
                                Icon(
                                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (liked) MaterialTheme.colorScheme.error else hm.fgTertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                                if (likeCount > 0) {
                                    Text(
                                        likeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = hm.fgSecondary,
                                    )
                                }
                            }
                            if (onRate != null) {
                                Icon(
                                    imageVector = Icons.Filled.ThumbUp,
                                    contentDescription = "Empfehlen",
                                    tint = if (myCommunityRating == "RECOMMEND") sem.ratingRecommend else hm.fgTertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    recommendCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = hm.fgSecondary,
                                )
                                Icon(
                                    imageVector = Icons.Filled.ThumbDown,
                                    contentDescription = "Nicht empfehlen",
                                    tint = if (myCommunityRating == "NOT_RECOMMEND") sem.ratingNotRecommend else hm.fgTertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    notRecommendCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = hm.fgSecondary,
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    trailingSlot?.invoke()
                }
            }
        }
    }
}
