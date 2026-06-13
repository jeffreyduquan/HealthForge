package de.healthforge.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.healthforge.data.db.entities.IntakeEntryEntity
import de.healthforge.data.db.entities.IntakeSourceType
import de.healthforge.data.network.IngredientDto
import de.healthforge.presentation.theme.FoodIcon
import de.healthforge.presentation.theme.FoodIcons
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.foodIconForName
import de.healthforge.presentation.theme.supplementIconVariant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Intake card with category icon (or recipe photo), name, time,
 * and ONLY the currently pinned nutrients (no default g/kcal line).
 * Swipe-to-dismiss = permanent cascade delete.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IntakeCard(
    entry: IntakeEntryEntity,
    pinnedKeys: List<String>,
    recipeImageUrl: String? = null,
    ingredientDto: IngredientDto? = null,
    onDelete: () -> Unit,
    onToggleConsumed: () -> Unit = {},
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val foodIcon = resolveIcon(entry)
    val isRecipe = entry.sourceType == IntakeSourceType.RECIPE

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
        backgroundContent = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // ── Top row: icon/photo + name + time ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Recipe photo or category icon
                    if (isRecipe && !recipeImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(recipeImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = entry.snapshotName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.dp, hm.glassBorder, CircleShape),
                        )
                    } else {
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
                    }

                    // Name
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.snapshotName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = hm.fgPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${"%.0f".format(entry.portionGrams)} g",
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
                    // Consumed toggle
                    IconButton(onClick = onToggleConsumed, modifier = Modifier.size(42.dp)) {
                        Icon(
                            imageVector = if (entry.consumed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            contentDescription = if (entry.consumed) "Gegessen" else "Nicht gegessen",
                            tint = if (entry.consumed) hm.ambientCyan else hm.fgTertiary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                // ── Pinned nutrients row ──
                val nutrients = computePinnedNutrients(entry, pinnedKeys, ingredientDto)
                if (nutrients.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        nutrients.forEach { (label, value) ->
                            Text(
                                text = "$label $value",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W500),
                                color = hm.fgSecondary,
                            )
                        }
                    }
                }
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

/** Compute nutrient contributions for this entry, filtered to pinned keys. Uses German labels. */
fun computePinnedNutrients(
    entry: IntakeEntryEntity,
    pinnedKeys: List<String>,
    ingredientDto: IngredientDto? = null,
): List<Pair<String, String>> {
    val f = entry.portionGrams / 100.0
    return pinnedKeys.mapNotNull { key ->
        // Try snapshot fields first, then micronutrients from DTO
        val value: Double? = when (key) {
            "kcal" -> (entry.snapshotKcalPer100g ?: 0.0) * f
            "protein" -> (entry.snapshotProteinPer100g ?: 0.0) * f
            "carbs" -> (entry.snapshotCarbsPer100g ?: 0.0) * f
            "fat" -> (entry.snapshotFatPer100g ?: 0.0) * f
            "sugar" -> ingredientDto?.sugar_g_per_100g?.times(f)
            "fiber" -> ingredientDto?.fiber_g_per_100g?.times(f)
            "salt" -> ingredientDto?.salt_g_per_100g?.times(f)
            "satfat" -> ingredientDto?.satfat_g_per_100g?.times(f)
            "water" -> null // skip water on cards
            else -> ingredientDto?.micronutrients?.get(key)?.times(f)
        }
        value?.let { v ->
            val label = nutrientLabelDe(key)
            val formatted = when {
                v < 0.1 -> "%.2f".format(v)
                v < 1 -> "%.1f".format(v)
                v < 10 -> "%.1f".format(v)
                v < 100 -> "%.0f".format(v)
                else -> "%.0f".format(v)
            }
            if (v > 0) label to formatted else null
        }
    }
}

private fun nutrientLabelDe(key: String): String = when (key) {
    "kcal" -> "kcal"
    "protein" -> "Eiweiß"
    "carbs" -> "KH"
    "fat" -> "Fett"
    "sugar" -> "Zucker"
    "fiber" -> "Ballast."
    "salt" -> "Salz"
    "satfat" -> "ges.Fett"
    "vitamin_a" -> "Vit.A"
    "vitamin_b1" -> "Vit.B1"
    "vitamin_b2" -> "Vit.B2"
    "vitamin_b3" -> "Vit.B3"
    "vitamin_b5" -> "Vit.B5"
    "vitamin_b6" -> "Vit.B6"
    "vitamin_b7" -> "Vit.B7"
    "vitamin_b9" -> "Vit.B9"
    "vitamin_b12" -> "Vit.B12"
    "vitamin_c" -> "Vit.C"
    "vitamin_d" -> "Vit.D"
    "vitamin_e" -> "Vit.E"
    "vitamin_k" -> "Vit.K"
    "calcium" -> "Calcium"
    "iron" -> "Eisen"
    "magnesium" -> "Magnesium"
    "zinc" -> "Zink"
    "iodine" -> "Jod"
    "potassium" -> "Kalium"
    "sodium" -> "Natrium"
    "phosphorus" -> "Phosphor"
    "selenium" -> "Selen"
    "manganese" -> "Mangan"
    "copper" -> "Kupfer"
    "chromium" -> "Chrom"
    "molybdenum" -> "Molybdän"
    "chloride" -> "Chlorid"
    else -> key.take(6)
}

/**
 * Dotted "+" add button — navigates to Essen tab.
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

