package de.healthforge.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Food category icons using Material Icons (Outlined).
 * Clean, professional, consistent with Histamind design.
 */
object FoodIcons {

    // Proteins
    val RIND: ImageVector = Icons.Outlined.Restaurant
    val SCHWEIN: ImageVector = Icons.Outlined.Restaurant
    val GEFLUEGEL: ImageVector = Icons.Outlined.Egg
    val FISCH: ImageVector = Icons.Outlined.SetMeal
    val WURST: ImageVector = Icons.Outlined.Restaurant

    // Dairy
    val MILCH: ImageVector = Icons.Outlined.WaterDrop
    val KAESE: ImageVector = Icons.Outlined.LocalDining
    val JOGHURT: ImageVector = Icons.Outlined.WaterDrop

    // Eggs
    val EIER: ImageVector = Icons.Outlined.Egg

    // Grains
    val BROT: ImageVector = Icons.Outlined.BakeryDining
    val NUDELN: ImageVector = Icons.Outlined.RamenDining
    val MUESLI: ImageVector = Icons.Outlined.Grain

    // Produce
    val GEMUESE: ImageVector = Icons.Outlined.Grass
    val SALAT: ImageVector = Icons.Outlined.Grass
    val OBST: ImageVector = Icons.Outlined.Spa
    val NUESSE: ImageVector = Icons.Outlined.Grain

    // Fats & Spices
    val OELE: ImageVector = Icons.Outlined.WaterDrop
    val GEWUERZE: ImageVector = Icons.Outlined.Science

    // Sweets
    val SUESSES: ImageVector = Icons.Outlined.Cake
    val KUCHEN: ImageVector = Icons.Outlined.Cake

    // Drinks & Ready Meals
    val GETRAENKE: ImageVector = Icons.Outlined.LocalCafe
    val FERTIGGERICHTE: ImageVector = Icons.Outlined.RamenDining
    val SOSSEN: ImageVector = Icons.Outlined.WaterDrop

    // Supplement
    val SUPPLEMENT: ImageVector = Icons.Outlined.Medication

    // Fallback
    val FALLBACK: ImageVector = Icons.Outlined.Restaurant
}
