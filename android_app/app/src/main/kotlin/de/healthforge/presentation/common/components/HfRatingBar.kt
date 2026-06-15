package de.healthforge.presentation.common.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.LocalSemanticColors
import de.healthforge.presentation.theme.StatusGood
import de.healthforge.presentation.theme.StatusOverUl

// =============================================================================
// HfRatingBar — Unified Rating Component for ALL screens
// =============================================================================
// Single source of truth for Like/Dislike + Community-Recommend/Not-Recommend.
// Replaces: IconButton-based rating (IngredientDetail/LebensmittelListe),
//           FilterChip-based rating (RecipeDetail),
//           missing rating (Supplements, Home IntakeCard).
// =============================================================================

/**
 * Unified rating bar with two independent rows:
 * 1. Personal: "Gefällt mir" (Like/Unlike — REQ-RATING-001, local)
 * 2. Community: "Empfehle ich" / "Nicht empfehlen" (REQ-RATING-002, server)
 *
 * Each row is optional — pass null callbacks to hide.
 *
 * @param liked Whether current user has liked this item
 * @param likeCount Total like count
 * @param likeBusy Whether a like/unlike operation is in progress
 * @param onToggleLike Callback to toggle like
 * @param myCommunityRating Current user's community rating ("RECOMMEND" / "NOT_RECOMMEND" / null)
 * @param recommendCount Count of RECOMMEND ratings
 * @param notRecommendCount Count of NOT_RECOMMEND ratings
 * @param onRate Callback for community rating (pass "RECOMMEND", "NOT_RECOMMEND", or null to revoke)
 */
@Composable
fun HfRatingBar(
    liked: Boolean = false,
    likeCount: Long = 0,
    likeBusy: Boolean = false,
    onToggleLike: (() -> Unit)? = null,
    myCommunityRating: String? = null,
    recommendCount: Long = 0,
    notRecommendCount: Long = 0,
    onRate: ((String?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val sem = LocalSemanticColors.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Personal Like (Gefällt mir) ──
        if (onToggleLike != null) {
            FilterChip(
                selected = liked,
                onClick = { if (!likeBusy) onToggleLike() },
                leadingIcon = {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (liked) MaterialTheme.colorScheme.error else hm.fgSecondary,
                    )
                },
                label = {
                    Text(
                        if (likeCount > 0) "Gefällt mir · $likeCount" else "Gefällt mir",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }

        // ── Community Recommend ──
        if (onRate != null) {
            FilterChip(
                selected = myCommunityRating == "RECOMMEND",
                onClick = { onRate(if (myCommunityRating == "RECOMMEND") null else "RECOMMEND") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = if (myCommunityRating == "RECOMMEND") sem.ratingRecommend else hm.fgSecondary,
                    )
                },
                label = { Text(recommendCount.toString(), style = MaterialTheme.typography.labelMedium) },
            )
            FilterChip(
                selected = myCommunityRating == "NOT_RECOMMEND",
                onClick = { onRate(if (myCommunityRating == "NOT_RECOMMEND") null else "NOT_RECOMMEND") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.ThumbDown,
                        contentDescription = null,
                        tint = if (myCommunityRating == "NOT_RECOMMEND") sem.ratingNotRecommend else hm.fgSecondary,
                    )
                },
                label = { Text(notRecommendCount.toString(), style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}
