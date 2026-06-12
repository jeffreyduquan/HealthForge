package de.healthforge.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.presentation.theme.FoodIcon
import de.healthforge.presentation.theme.FoodIcons
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.foodIconForName
import de.healthforge.presentation.theme.supplementIconVariant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A single intake entry rendered as a GlassCard row.
 * Swipe-to-dismiss triggers permanent deletion (IntakeEntry + linked MealPlanItem).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeCard(
    entry: IntakeEntryEntity,
    onDelete: () -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val foodIcon = resolveIcon(entry)

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                try { onDelete() } catch (_: Exception) {}
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        enableDismissFromEndToStart = false,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(hm.statusOverUl.copy(alpha = 0.15f)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Löschen",
                    tint = hm.statusOverUl,
                    modifier = Modifier.padding(start = 20.dp).size(24.dp),
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (hm.isGlassEnabled) {
                        Modifier
                            .background(hm.cardSurface)
                            .background(Brush.verticalGradient(listOf(hm.glassFillTop, hm.glassFillBottom)))
                            .border(1.dp, hm.glassBorder, RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                            .background(hm.cardSurface)
                            .border(1.dp, hm.cardBorder, RoundedCornerShape(16.dp))
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Category icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    hm.ambientViolet.copy(alpha = 0.15f),
                                    hm.ambientCyan.copy(alpha = 0.08f),
                                )
                            )
                        )
                        .border(1.dp, hm.glassBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = foodIcon.icon,
                        contentDescription = null,
                        tint = foodIcon.tint ?: hm.fgSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Name + portion
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.snapshotName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = hm.fgPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    val portion = "${"%.0f".format(entry.portionGrams)} g"
                    val kcal = entry.snapshotKcalPer100g?.let {
                        " \u00b7 ${(it * entry.portionGrams / 100.0).toInt()} kcal"
                    } ?: ""
                    Text(
                        text = "$portion$kcal",
                        style = MaterialTheme.typography.labelMedium,
                        color = hm.fgTertiary,
                    )
                }

                // Time
                Text(
                    text = formatEntryTime(entry.loggedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = hm.fgTertiary,
                )
            }
        }
    }
}

private fun formatEntryTime(epochMillis: Long): String {
    return try {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("HH:mm").format(dt)
    } catch (_: Exception) { "" }
}

fun resolveIcon(entry: IntakeEntryEntity): FoodIcon {
    return when (entry.sourceType) {
        IntakeSourceType.RECIPE -> FoodIcon(FoodIcons.FERTIGGERICHTE, null)
        IntakeSourceType.SUPPLEMENT -> {
            val sid = entry.sourceId.toLongOrNull() ?: 0L
            supplementIconVariant(sid)
        }
        IntakeSourceType.INGREDIENT -> foodIconForName(entry.snapshotName)
    }
}

/**
 * Dotted "+" add button — large, centered, opens the item picker.
 */
@Composable
fun DottedAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val dashColor = hm.fgTertiary.copy(alpha = 0.35f)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(10.dp.toPx(), 6.dp.toPx()), 0f
                        ),
                    ),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Lebensmittel hinzufügen",
            tint = dashColor,
            modifier = Modifier.size(36.dp),
        )
    }
}
