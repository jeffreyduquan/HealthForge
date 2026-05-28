package de.healthforge.etl.usda

import com.fasterxml.jackson.databind.ObjectMapper
import de.healthforge.domain.nutrition.NutrientCatalog
import de.healthforge.etl.Counts
import de.healthforge.etl.EtlSource
import de.healthforge.etl.Importer
import de.healthforge.ingredient.IngredientEntity
import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.math.BigDecimal
import java.time.Instant

/**
 * P7.S2 / REQ-DATA-SOURCE-001 / REQ-INGR-MICRONUTRIENTS-001 — importiert
 * USDA FoodData Central Slice in `ingredients`.
 *
 * Erwartet CSV unter `resources/seed/usda_fdc.csv` (Semikolon-separiert).
 * Spaltenreihenfolge:
 *
 *   1. `fdc_id`              — USDA FDC-ID (Long, unique)
 *   2. `name_de`             — bereits übersetzter DE-Name (via `translate_fdc_names.main.kts`)
 *   3. `name_en`             — Original-EN-Name (für Allergen-Erkennung + Debug)
 *   4. `brand`               — optional, sonst leer
 *   5. `ingredients_en`      — Original-EN-Zutatenliste (für Allergen-Mapper)
 *   6. `kcal_per_100g`       — Energie
 *   7. `protein_g_per_100g`  — Hauptmakros (entsprechen IngredientEntity-Spalten)
 *   8. `carbs_g_per_100g`
 *   9. `sugar_g_per_100g`
 *  10. `fat_g_per_100g`
 *  11. `satfat_g_per_100g`
 *  12. `fiber_g_per_100g`
 *  13. `salt_g_per_100g`
 *  14. `micronutrients_json` — JSON-Map<NutrientCatalog.key, Wert pro 100g>
 *                              (z.B. `{"vitamin_c":53.2,"calcium":40,"eisen":1.1}`)
 *
 * Idempotenz: Suche per `fdcId` → Update (sonst Insert). Bestehende Rows aus
 * anderen Quellen werden NICHT überschrieben.
 *
 * Bei fehlender Seed-Datei → [Counts.skipped] (UI-Hinweis: "ETL übersprungen").
 */
@Component
class UsdaFdcImporter(
    private val ingredients: IngredientRepository,
    private val usdaIngredients: UsdaIngredientRepository,
) : Importer {

    override val source: EtlSource = EtlSource.USDA_FDC
    override fun seedResourcePath(): String = "seed/usda_fdc.csv"

    private val log = LoggerFactory.getLogger(UsdaFdcImporter::class.java)
    private val mapper = ObjectMapper()

    @Transactional
    override fun import(): Counts {
        val reader = classpathReader(seedResourcePath()) ?: run {
            log.info("USDA-FDC seed not present at {} — skipping", seedResourcePath())
            return Counts.skipped
        }
        var inserted = 0
        var updated = 0
        var skipped = 0
        reader.useLines { lines ->
            lines.drop(1).forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val cols = parseCsvLine(line)
                if (cols.size < 14) { skipped++; return@forEach }

                val fdcId = cols[0].trim().toLongOrNull()
                    ?: run { skipped++; return@forEach }
                // Slice 3c (2026-05-28): name_de fällt auf name_en zurück, falls leer.
                // Voll-Lauf von TranslateFdcNames hat alle 8354 Rows befüllt, aber bei
                // zukünftigen Importen mit frisch generiertem Seed (vor DeepL-Run) sollen
                // Einträge sichtbar bleiben statt skipped — Slice 3b SprintPlan-Akzeptanz.
                val nameEn = cols[2].trim()
                val nameDe = cols[1].trim().ifBlank { nameEn }
                if (nameDe.isBlank()) { skipped++; return@forEach } // beide leer → wirklich nichts
                val brand = cols[3].trim().ifBlank { null }
                val ingredientsEn = cols[4].trim()

                val micronutrients = parseMicros(cols[13].trim())

                // Allergene aus EN-Quelltext (Name + Ingredients-Liste).
                val allergens = AllergenMapper.extractAsStrings("$nameEn $ingredientsEn")
                val allergensJson = mapper.writeValueAsString(allergens)

                val existing = usdaIngredients.findByFdcId(fdcId).orElse(null)
                val entity = existing ?: IngredientEntity(
                    nameDe = nameDe,
                    source = IngredientSource.USDA_FDC,
                    sourceId = fdcId.toString(),
                    fdcId = fdcId,
                )
                entity.nameDe = nameDe
                entity.brand = brand
                entity.source = IngredientSource.USDA_FDC
                entity.sourceId = fdcId.toString()
                entity.fdcId = fdcId
                entity.energyKcalPer100g = cols[5].toBigDec()
                entity.proteinGPer100g = cols[6].toBigDec()
                entity.carbsGPer100g = cols[7].toBigDec()
                entity.sugarGPer100g = cols[8].toBigDec()
                entity.fatGPer100g = cols[9].toBigDec()
                entity.satfatGPer100g = cols[10].toBigDec()
                entity.fiberGPer100g = cols[11].toBigDec()
                entity.saltGPer100g = cols[12].toBigDec()
                entity.micronutrientsJson = mapper.writeValueAsString(micronutrients)
                entity.allergensJson = allergensJson
                entity.locked = true
                entity.updatedAt = Instant.now()
                ingredients.save(entity)
                if (existing != null) updated++ else inserted++
            }
        }
        log.info("USDA-FDC import: inserted={} updated={} skipped={}", inserted, updated, skipped)
        return Counts(inserted, updated, skipped)
    }

    /**
     * Parst die `micronutrients_json`-Spalte und filtert auf bekannte Catalog-Keys.
     * Unbekannte Keys werden verworfen (mit Log-Hinweis), um DB-Drift zu vermeiden.
     */
    private fun parseMicros(json: String): Map<String, Double> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            val raw = mapper.readValue(json, Map::class.java) as Map<String, Any?>
            raw.mapNotNull { (k, v) ->
                val value = (v as? Number)?.toDouble() ?: return@mapNotNull null
                if (NutrientCatalog.byKeyOrNull(k) == null) {
                    log.warn("USDA-FDC: unbekannter Nutrient-Key '{}' verworfen", k)
                    null
                } else {
                    k to value
                }
            }.toMap()
        } catch (e: Exception) {
            log.warn("USDA-FDC: micronutrients_json Parse-Fehler: {}", e.message)
            emptyMap()
        }
    }

    /**
     * Minimalparser für Semikolon-CSV mit `"..."`-Quoting. Innerhalb von
     * Quotes werden Semikolons nicht als Trenner gewertet; `""` → `"`.
     */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ';' && !inQuotes -> {
                    out += cur.toString(); cur.clear()
                }
                else -> cur.append(c)
            }
            i++
        }
        out += cur.toString()
        return out
    }

    private fun String.toBigDec(): BigDecimal? =
        trim().replace(',', '.').takeIf { it.isNotBlank() }?.toBigDecimalOrNull()

    private fun classpathReader(path: String): BufferedReader? = try {
        val res = ClassPathResource(path)
        if (!res.exists()) null else res.inputStream.bufferedReader(Charsets.UTF_8)
    } catch (e: Exception) {
        log.warn("USDA-FDC: failed to open classpath resource '{}': {}", path, e.message)
        null
    }
}
