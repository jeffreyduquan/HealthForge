package de.healthforge.presentation.lebensmittel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.data.db.entities.AllergenType
import de.healthforge.data.db.entities.FodmapType
import de.healthforge.data.network.IngredientDto
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.SectionPill
import kotlin.math.roundToInt

/**
 * P7.S5 — Modal-Bottom-Sheet, das alle Details eines [IngredientDto] zeigt:
 *  • Header: [IngredientDto.name_de] + optionale [IngredientDto.brand].
 *  • Quelle (z. B. „USDA-FDC #170150").
 *  • Makro-Sektion (kcal/Protein/Carbs/Sugar/Fat/SatFat/Fiber/Salt pro 100 g).
 *  • Mikro-Sektion: alle Werte > 0 aus [IngredientDto.micronutrients], gruppiert
 *    nach Vitaminen / Mineralstoffen in Katalog-Reihenfolge. Pro Zeile zusätzlich
 *    Prozent DGE (Wert pro 100 g ÷ DGE-Default).
 *  • Allergene + FODMAP-Flags (Chips).
 *
 * Mikronährwerte-Coverage in DB (Audit 2026-05-29): 87.9 % der 8354 USDA-Rows
 * haben ≥ 10 Mikros, 66 % haben 20+. Keine Coverage für Histamin (REQ-INGR-003)
 * → Histamin-Block wird nur gerendert, wenn [IngredientDto.histamine_score]
 * tatsächlich gesetzt ist.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientDetailSheet(
    item: IngredientDto,
    onDismiss: () -> Unit,
) {
    val hm = LocalHmTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = hm.cardSurface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 24.dp),
        ) {
            // ─── Header ──────────────────────────────────────────────────────
            Text(
                text = item.name_de,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = hm.fgPrimary,
            )
            item.brand?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = hm.fgSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            SourceBadge(source = item.source, fdcId = item.fdc_id)

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = hm.glassBorder)

            // ─── Makros pro 100 g ────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            SectionPill("Nährwerte pro 100 g")
            Spacer(Modifier.height(6.dp))
            MacrosGrid(item)

            // ─── Mikronährwerte ──────────────────────────────────────────────
            val microsPresent = item.micronutrients.filter { (_, v) -> v > 0.0 }
            if (microsPresent.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionPill("Mikronährstoffe (pro 100 g)")
                Spacer(Modifier.height(6.dp))
                MicroSection(NutrientCatalog.Category.VITAMIN, "Vitamine", microsPresent)
                MicroSection(NutrientCatalog.Category.MINERAL, "Mineralstoffe", microsPresent)
            }

            // ─── Allergene ───────────────────────────────────────────────────
            if (item.allergens.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionPill("Allergene")
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.allergens.forEach { code ->
                        val label = runCatching { AllergenType.valueOf(code).germanLabel }.getOrDefault(code)
                        AssistChip(onClick = {}, label = { Text(label) })
                    }
                }
            }

            // ─── FODMAP ──────────────────────────────────────────────────────
            if (item.fodmap_flags.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionPill("FODMAP")
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.fodmap_flags.forEach { code ->
                        val label = runCatching { FodmapType.valueOf(code).germanLabel }.getOrDefault(code)
                        AssistChip(onClick = {}, label = { Text(label) })
                    }
                }
            }

            // ─── Histamin (nur wenn vorhanden) ───────────────────────────────
            item.histamine_score?.let { score ->
                Spacer(Modifier.height(20.dp))
                SectionPill("Histamin")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$score / 3 (SIGHI-Skala)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = hm.fgPrimary,
                )
            }
        }
    }
}

// =============================================================================
// Sub-Components
// =============================================================================

@Composable
private fun SourceBadge(source: String, fdcId: Long?) {
    val hm = LocalHmTokens.current
    val text = buildString {
        append(source.replace('_', '-'))
        if (fdcId != null) append(" #").append(fdcId)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(hm.ambientViolet.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = hm.fgSecondary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun MacrosGrid(item: IngredientDto) {
    val rows = listOf(
        "Kalorien" to item.energy_kcal_per_100g?.let { "${it.roundToInt()} kcal" },
        "Eiweiß" to item.protein_g_per_100g?.let { "${format(it)} g" },
        "Kohlenhydrate" to item.carbs_g_per_100g?.let { "${format(it)} g" },
        "  davon Zucker" to item.sugar_g_per_100g?.let { "${format(it)} g" },
        "Fett" to item.fat_g_per_100g?.let { "${format(it)} g" },
        "  davon gesättigt" to item.satfat_g_per_100g?.let { "${format(it)} g" },
        "Ballaststoffe" to item.fiber_g_per_100g?.let { "${format(it)} g" },
        "Salz" to item.salt_g_per_100g?.let { "${format(it)} g" },
    ).filter { it.second != null }

    Column {
        rows.forEach { (label, value) ->
            ValueRow(label = label, value = value!!, percentDge = null)
        }
    }
}

@Composable
private fun MicroSection(
    category: NutrientCatalog.Category,
    title: String,
    micros: Map<String, Double>,
) {
    val hm = LocalHmTokens.current
    val cat = NutrientCatalog.ofCategory(category).filter { micros.containsKey(it.key) }
    if (cat.isEmpty()) return

    Spacer(Modifier.height(6.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = hm.fgTertiary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )

    cat.forEach { n ->
        val raw = micros[n.key] ?: return@forEach
        // FDC speichert µg/mg pro 100 g — DGE-Default ist in n.unit. Wir nehmen
        // an, dass Server-Werte bereits in n.unit normalisiert sind (BuildUsdaSeed
        // mappt USDA-Einheiten direkt auf Katalog-Unit, siehe BuildUsdaSeed.kt).
        val pctDge = if (n.defaultPerDay > 0) ((raw / n.defaultPerDay) * 100.0).roundToInt() else null
        ValueRow(
            label = n.displayDe,
            value = "${format(raw)} ${n.unit.label}",
            percentDge = pctDge,
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, percentDge: Int?) {
    val hm = LocalHmTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = hm.fgSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = hm.fgPrimary,
            fontWeight = FontWeight.Medium,
        )
        if (percentDge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(hm.ambientViolet.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "$percentDge %",
                    style = MaterialTheme.typography.labelSmall,
                    color = hm.fgSecondary,
                )
            }
        }
    }
}

private fun format(v: Double): String =
    if (v >= 100.0) v.roundToInt().toString()
    else if (v >= 10.0) "%.1f".format(v)
    else "%.2f".format(v)
