package de.healthforge.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom SVG food-category icons for the HealthForge Home screen.
 *
 * All icons are 24×24dp, stroke-width 1.5dp, stroke-cap round, stroke-join round.
 * Designed in the Histamind style — minimalist geometric silhouettes tinted via
 * [LocalHmTokens.current.fgSecondary] or accent variants.
 *
 * Categories (24 total) match German food taxonomy:
 *   Rind/Kalb, Schwein, Geflügel, Fisch, Wurst/Aufschnitt,
 *   Milch, Käse, Joghurt/Quark, Eier,
 *   Brot, Nudeln/Reis, Müsli/Getreide,
 *   Gemüse, Salat, Obst, Nüsse/Samen,
 *   Öle/Fette, Gewürze, Süßes, Kuchen/Gebäck,
 *   Getränke, Fertiggerichte, Soßen/Dips,
 *   Supplement (with color variants)
 */

// ─────────────────────────────────────────────────────────────────────────────
// Helper functions for icon construction
// ─────────────────────────────────────────────────────────────────────────────

private fun ImageVector.Builder.strokePath(
    pathData: List<Triple<Float, Float, Boolean>>, // x, y, isMoveTo
    fill: Brush = SolidColor(Color.White), // tinted later
    strokeWidth: Float = 1.5f,
) {
    path(
        fill = null,
        stroke = fill,
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        pathData.forEachIndexed { i, (x, y, isMove) ->
            if (isMove || i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
}

private fun ImageVector.Builder.filledPath(
    pathData: List<Triple<Float, Float, Boolean>>,
    fill: Brush = SolidColor(Color.White),
) {
    path(
        fill = fill,
        stroke = null,
        pathFillType = PathFillType.EvenOdd,
    ) {
        pathData.forEachIndexed { i, (x, y, isMove) ->
            if (isMove || i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
}

// Helpers to build path data concisely
private fun M(x: Float, y: Float) = Triple(x, y, true)
private fun L(x: Float, y: Float) = Triple(x, y, false)

// ─────────────────────────────────────────────────────────────────────────────
// Icon definitions — 24 categories
// ─────────────────────────────────────────────────────────────────────────────

object FoodIcons {

    // 1 ─ Rind/Kalb: stylized cow silhouette (circle head + curved body)
    val RIND: ImageVector = ImageVector.Builder("rind", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(7f,10f), L(5.5f,7f), L(7f,4f), L(9f,3f), L(12f,3.5f),
            L(15f,3f), L(17f,4f), L(18.5f,7f), L(17f,10f),
            L(18f,12f), L(17f,16f), L(14f,20f), L(10f,20f), L(7f,16f), L(6f,12f),
            L(7f,10f)  // close loop
        ))
        // eyes
        strokePath(listOf(M(10f,7.5f), L(10.5f,8f)))
        strokePath(listOf(M(14f,7.5f), L(13.5f,8f)))
    }.build()

    // 2 ─ Schwein: round body + snout + ears
    val SCHWEIN: ImageVector = ImageVector.Builder("schwein", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(8f,8f), L(6f,7f), L(5f,9f), L(5f,13f), L(6f,15f),
            L(9f,15f), L(10f,19f), L(14f,19f), L(15f,15f),
            L(18f,15f), L(19f,13f), L(19f,9f), L(18f,7f), L(16f,8f),
            L(14f,6f), L(10f,6f), L(8f,8f)
        ))
        // snout
        strokePath(listOf(M(6f,10f), L(5.5f,11f)))
        strokePath(listOf(M(6f,12f), L(5.5f,12f)))
        // eye
        strokePath(listOf(M(8.5f,9.5f), L(9f,10f)))
        strokePath(listOf(M(15.5f,9.5f), L(15f,10f)))
    }.build()

    // 3 ─ Geflügel: chicken/drumstick
    val GEFLUEGEL: ImageVector = ImageVector.Builder("gefluegel", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(12f,2f), L(14f,3f), L(15f,5f), L(16f,7f),
            L(17f,10f), L(16f,13f), L(14f,15f), L(12f,16f),
            L(10f,17f), L(8f,18f), L(7f,20f), L(7f,22f)
        ))
        // wing hint
        strokePath(listOf(M(13f,8f), L(17f,6f), L(19f,8f)))
        strokePath(listOf(M(17f,6f), L(18f,10f)))
    }.build()

    // 4 ─ Fisch: fish silhouette
    val FISCH: ImageVector = ImageVector.Builder("fisch", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(20f,12f), L(17f,9f), L(12f,8f), L(7f,7f),
            L(4f,9f), L(3f,12f), L(4f,15f), L(7f,17f),
            L(12f,16f), L(17f,15f), L(20f,12f)
        ))
        // tail
        strokePath(listOf(M(4f,9f), L(2f,7f), L(3f,12f)))
        strokePath(listOf(M(4f,15f), L(2f,17f), L(3f,12f)))
        // eye
        strokePath(listOf(M(7f,11f), L(7.5f,11.5f)))
    }.build()

    // 5 ─ Wurst/Aufschnitt: sausage shapes
    val WURST: ImageVector = ImageVector.Builder("wurst", 24.dp, 24.dp, 24f, 24f).apply {
        // two sausages
        strokePath(listOf(
            M(4f,8f), L(8f,6f), L(14f,6f), L(18f,7f),
            L(20f,9f), L(20f,11f), L(18f,13f), L(14f,14f),
            L(8f,14f), L(4f,13f)
        ))
        strokePath(listOf(
            M(5f,11f), L(9f,10f), L(15f,10f), L(19f,11f),
            L(20f,13f), L(20f,16f), L(18f,18f), L(14f,19f),
            L(8f,19f), L(4f,18f)
        ))
    }.build()

    // 6 ─ Milch: milk carton
    val MILCH: ImageVector = ImageVector.Builder("milch", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(7f,3f), L(7f,21f), L(17f,21f), L(17f,3f)
        ))
        strokePath(listOf(M(7f,3f), L(9f,5f), L(17f,3f)))
        // spout
        strokePath(listOf(M(7f,3f), L(5.5f,4f), L(7f,5.5f)))
        // label line
        strokePath(listOf(M(9f,10f), L(15f,10f)))
        strokePath(listOf(M(9f,14f), L(13f,14f)))
    }.build()

    // 7 ─ Käse: cheese wedge with holes
    val KAESE: ImageVector = ImageVector.Builder("kaese", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(4f,10f), L(12f,3f), L(20f,10f), L(20f,20f), L(4f,20f), L(4f,10f)
        ))
        // holes
        strokePath(listOf(M(9f,10f), L(9.8f,10.5f), L(9.3f,11.3f), L(9f,10f)))
        strokePath(listOf(M(14f,12f), L(14.6f,12.4f), L(14.2f,13.2f), L(14f,12f)))
        strokePath(listOf(M(11f,16f), L(11.7f,16.3f), L(11.4f,17.1f), L(11f,16f)))
    }.build()

    // 8 ─ Joghurt/Quark: cup/pot
    val JOGHURT: ImageVector = ImageVector.Builder("joghurt", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(9f,3f), L(9f,6f), L(18f,7f), L(19f,20f), L(5f,20f), L(6f,7f), L(9f,6f)
        ))
        // lid line
        strokePath(listOf(M(6.5f,8f), L(17.5f,9f)))
    }.build()

    // 9 ─ Eier: egg shapes
    val EIER: ImageVector = ImageVector.Builder("eier", 24.dp, 24.dp, 24f, 24f).apply {
        // egg 1
        strokePath(listOf(
            M(9f,6f), L(7f,8f), L(6f,12f), L(7f,16f), L(9f,19f),
            L(12f,18f), L(13f,16f), L(12f,12f), L(10f,8f), L(9f,6f)
        ))
        // egg 2
        strokePath(listOf(
            M(15f,5f), L(13f,7f), L(12f,11f), L(13f,15f),
            L(15f,17f), L(18f,16f), L(19f,14f), L(18f,10f), L(16f,7f), L(15f,5f)
        ))
    }.build()

    // 10 ─ Brot: bread loaf
    val BROT: ImageVector = ImageVector.Builder("brot", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(5f,12f), L(3f,10f), L(4f,7f), L(8f,5f), L(16f,5f),
            L(20f,7f), L(21f,10f), L(19f,12f), L(19f,20f), L(5f,20f), L(5f,12f)
        ))
        // bread score marks
        strokePath(listOf(M(8f,5f), L(9f,9f)))
        strokePath(listOf(M(12f,5f), L(12f,9f)))
        strokePath(listOf(M(16f,5f), L(15f,9f)))
    }.build()

    // 11 ─ Nudeln/Reis: pasta shapes
    val NUDELN: ImageVector = ImageVector.Builder("nudeln", 24.dp, 24.dp, 24f, 24f).apply {
        // bowl
        strokePath(listOf(
            M(4f,14f), L(3f,20f), L(21f,20f), L(20f,14f)
        ))
        // noodles inside
        strokePath(listOf(M(6f,13f), L(8f,16f), L(10f,13f), L(12f,16f), L(14f,13f), L(16f,16f), L(18f,13f)))
    }.build()

    // 12 ─ Müsli/Getreide: grain/wheat
    val MUESLI: ImageVector = ImageVector.Builder("muesli", 24.dp, 24.dp, 24f, 24f).apply {
        // wheat stalk
        strokePath(listOf(M(12f,3f), L(12f,21f)))
        // grains left
        strokePath(listOf(M(12f,6f), L(8f,4f)))
        strokePath(listOf(M(12f,9f), L(7f,8f)))
        strokePath(listOf(M(12f,12f), L(8f,11f)))
        // grains right
        strokePath(listOf(M(12f,5f), L(16f,3f)))
        strokePath(listOf(M(12f,8f), L(17f,7f)))
        strokePath(listOf(M(12f,11f), L(16f,10f)))
        // bowl bottom
        strokePath(listOf(M(6f,20f), L(18f,20f), L(19f,22f), L(5f,22f), L(6f,20f)))
    }.build()

    // 13 ─ Gemüse: carrot/root vegetable
    val GEMUESE: ImageVector = ImageVector.Builder("gemuese", 24.dp, 24.dp, 24f, 24f).apply {
        // carrot body
        strokePath(listOf(
            M(12f,3f), L(9f,7f), L(7f,12f), L(6f,17f),
            L(8f,21f), L(16f,21f), L(18f,17f),
            L(17f,12f), L(15f,7f), L(12f,3f)
        ))
        // leaves
        strokePath(listOf(M(12f,3f), L(9f,1f)))
        strokePath(listOf(M(12f,3f), L(12f,0.5f)))
        strokePath(listOf(M(12f,3f), L(15f,1f)))
    }.build()

    // 14 ─ Salat: lettuce/leafy head
    val SALAT: ImageVector = ImageVector.Builder("salat", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(12f,2f), L(8f,5f), L(4f,10f), L(5f,16f),
            L(8f,20f), L(12f,22f), L(16f,20f), L(19f,16f),
            L(20f,10f), L(16f,5f), L(12f,2f)
        ))
        // leaf veins
        strokePath(listOf(M(12f,4f), L(12f,10f)))
        strokePath(listOf(M(12f,8f), L(8f,12f)))
        strokePath(listOf(M(12f,8f), L(16f,12f)))
    }.build()

    // 15 ─ Obst: apple with leaf
    val OBST: ImageVector = ImageVector.Builder("obst", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(12f,3f), L(9f,4f), L(6.5f,7f), L(5f,12f),
            L(5.5f,17f), L(8f,20f), L(12f,21.5f),
            L(16f,20f), L(18.5f,17f), L(19f,12f),
            L(17.5f,7f), L(15f,4f), L(12f,3f)
        ))
        // stem
        strokePath(listOf(M(12f,3f), L(12f,0.5f)))
        // leaf
        strokePath(listOf(M(12f,2f), L(14f,0f), L(16f,2f)))
    }.build()

    // 16 ─ Nüsse/Samen: almond/nut shape
    val NUESSE: ImageVector = ImageVector.Builder("nuesse", 24.dp, 24.dp, 24f, 24f).apply {
        // almond 1
        strokePath(listOf(
            M(8f,4f), L(5f,8f), L(4f,14f), L(5f,18f),
            L(8f,20f), L(11f,18f), L(12f,14f), L(11f,8f), L(8f,4f)
        ))
        // almond 2
        strokePath(listOf(
            M(16f,2f), L(13f,7f), L(12f,13f), L(13f,17f),
            L(16f,19f), L(19f,17f), L(20f,13f), L(19f,7f), L(16f,2f)
        ))
    }.build()

    // 17 ─ Öle/Fette: oil drop + bottle hint
    val OELE: ImageVector = ImageVector.Builder("oele", 24.dp, 24.dp, 24f, 24f).apply {
        // bottle
        strokePath(listOf(
            M(9f,5f), L(9f,9f), L(7f,12f), L(7f,20f), L(17f,20f), L(17f,12f), L(15f,9f), L(15f,5f)
        ))
        // bottleneck
        strokePath(listOf(M(9f,5f), L(9f,2f), L(15f,2f), L(15f,5f)))
        // drip
        strokePath(listOf(M(12f,2f), L(12f,3f)))
        // oil level
        strokePath(listOf(M(8.5f,16f), L(15.5f,16f)))
    }.build()

    // 18 ─ Gewürze: mortar & pestle
    val GEWUERZE: ImageVector = ImageVector.Builder("gewuerze", 24.dp, 24.dp, 24f, 24f).apply {
        // mortar bowl
        strokePath(listOf(
            M(4f,16f), L(2f,20f), L(10f,22f), L(18f,22f), L(22f,20f), L(20f,16f)
        ))
        // pestle
        strokePath(listOf(M(18f,16f), L(19f,10f), L(18f,4f), L(16f,2f), L(15f,3f), L(16f,5f)))
        // rim highlight
        strokePath(listOf(M(5f,18f), L(19f,18f)))
    }.build()

    // 19 ─ Süßes: candy/wrapped sweet
    val SUESSES: ImageVector = ImageVector.Builder("suesses", 24.dp, 24.dp, 24f, 24f).apply {
        // candy body
        strokePath(listOf(
            M(6f,9f), L(5f,12f), L(6f,15f), L(10f,17f),
            L(14f,17f), L(18f,15f), L(19f,12f), L(18f,9f),
            L(14f,7f), L(10f,7f), L(6f,9f)
        ))
        // wrapper twists
        strokePath(listOf(M(6f,9f), L(4f,7f), L(5f,6f)))
        strokePath(listOf(M(18f,9f), L(20f,7f), L(19f,6f)))
    }.build()

    // 20 ─ Kuchen/Gebäck: cake slice
    val KUCHEN: ImageVector = ImageVector.Builder("kuchen", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(4f,14f), L(3f,20f), L(12f,22f), L(21f,20f), L(20f,14f)
        ))
        // slice lines
        strokePath(listOf(M(12f,22f), L(12f,14f)))
        // cherry on top
        strokePath(listOf(M(12f,11f), L(12.5f,12f)))
        strokePath(listOf(M(12f,11f), L(12f,9f)))
        // whipped cream wave
        strokePath(listOf(M(5f,14f), L(8f,12f), L(11f,14f), L(14f,12f), L(17f,14f), L(19f,12f)))
    }.build()

    // 21 ─ Getränke: cup/glass
    val GETRAENKE: ImageVector = ImageVector.Builder("getraenke", 24.dp, 24.dp, 24f, 24f).apply {
        strokePath(listOf(
            M(6f,4f), L(6f,20f), L(15f,20f), L(15f,8f), L(20f,8f), L(20f,5f),
            L(18f,4f), L(16f,5f), L(16f,7f), L(15f,8f)
        ))
        // liquid level
        strokePath(listOf(M(7.5f,12f), L(13.5f,12f)))
        // straw
        strokePath(listOf(M(18f,5f), L(19f,3f)))
    }.build()

    // 22 ─ Fertiggerichte: bowl/plate with steam
    val FERTIGGERICHTE: ImageVector = ImageVector.Builder("fertiggerichte", 24.dp, 24.dp, 24f, 24f).apply {
        // bowl
        strokePath(listOf(
            M(4f,14f), L(3f,19f), L(21f,19f), L(20f,14f)
        ))
        // rim
        strokePath(listOf(M(4f,14f), L(20f,14f)))
        // steam
        strokePath(listOf(M(9f,11f), L(8f,7f)))
        strokePath(listOf(M(12f,10f), L(12f,5f)))
        strokePath(listOf(M(15f,11f), L(16f,7f)))
    }.build()

    // 23 ─ Soßen/Dips: sauce pour
    val SOSSEN: ImageVector = ImageVector.Builder("sossen", 24.dp, 24.dp, 24f, 24f).apply {
        // small bowl
        strokePath(listOf(
            M(4f,16f), L(3f,20f), L(13f,21f), L(20f,20f), L(19f,16f)
        ))
        // pour stream
        strokePath(listOf(
            M(16f,10f), L(17f,6f), L(16f,3f), L(15f,2f),
            L(14f,3f), L(15f,6f), L(14f,10f), L(16f,16f)
        ))
        // splash
        strokePath(listOf(M(15f,16f), L(14f,18f)))
        strokePath(listOf(M(17f,16f), L(18f,18f)))
    }.build()

    // 24 ─ Supplement: capsule/pill (used with color variants)
    val SUPPLEMENT: ImageVector = ImageVector.Builder("supplement", 24.dp, 24.dp, 24f, 24f).apply {
        // left half
        strokePath(listOf(
            M(10f,3f), L(6f,5f), L(3f,10f), L(4f,14f),
            L(6f,17f), L(10f,19f), L(12f,21f), L(12f,3f), L(10f,3f)
        ))
        // right half
        strokePath(listOf(
            M(14f,3f), L(18f,5f), L(21f,10f), L(20f,14f),
            L(18f,17f), L(14f,19f), L(12f,21f), L(12f,3f), L(14f,3f)
        ))
        // center line
        strokePath(listOf(M(12f,4f), L(12f,20f)))
    }.build()

    // ── Supplement color variants (5 accent tints) ──
    /** Map supplement id → icon + color. Deterministic: id % 5. */
    val SUPPLEMENT_VARIANTS: List<Pair<ImageVector, Long>> = listOf(
        SUPPLEMENT to 0L, // ambientViolet
        SUPPLEMENT to 1L, // ambientCyan
        SUPPLEMENT to 2L, // statusGood
        SUPPLEMENT to 3L, // statusRelax
        SUPPLEMENT to 4L, // fgPrimary
    )

    /** Fallback icon for unmatched categories */
    val FALLBACK: ImageVector = ImageVector.Builder("fallback", 24.dp, 24.dp, 24f, 24f).apply {
        // generic plate/food circle
        strokePath(listOf(
            M(12f,2f), L(6f,5f), L(3f,10f), L(3f,16f),
            L(6f,20f), L(12f,22f), L(18f,20f), L(21f,16f),
            L(21f,10f), L(18f,5f), L(12f,2f)
        ))
        // inner dot
        strokePath(listOf(M(12f,11f), L(12.5f,11.5f)))
    }.build()
}
