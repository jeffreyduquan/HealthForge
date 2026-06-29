package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.math.BigDecimal
import java.time.Instant

/**
 * P7.S5d — BLS-Nährstoff-Anreicherung für bestehende Lebensmittel.
 *
 * Trigger: `-DmergeBlsNutrients=true`
 *
 * Liest `seed/bls_4_0.csv` und matcht per normalisiertem Namen gegen alle
 * `IngredientEntity`-Einträge in der DB. Füllt NUR fehlende (NULL/leere)
 * Makro- und Mikronährstoffe — bestehende Werte (SIGHI/Allergene/FODMAP)
 * bleiben unangetastet.
 *
 * So können die ~7.000 BLS-Nährstoffdatensätze genutzt werden, ohne dass
 * der (deprecate) BLS-Import neu laufen muss.
 */
@Component
@Order(Int.MAX_VALUE - 1)
class BlsNutrientEnricher(
    private val ingredients: IngredientRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(BlsNutrientEnricher::class.java)

    companion object {
        // Gleiche Macro-Mapping wie BlsImporter
        val MACRO_CODES = setOf("ENERCC", "PROT625", "FAT", "CHO", "FIBT", "SUGAR", "FASAT", "NACL")

        // Gleiches Micro-Mapping wie BlsImporter: BLS-Code → (unser Key, Faktor)
        val BLS_TO_MICRO: Map<String, Pair<String, Double>> = mapOf(
            "VITAA" to ("vitamin_a" to 1.0),
            "VITD" to ("vitamin_d" to 1.0),
            "TOCPHA" to ("vitamin_e" to 1.0),
            "VITK1" to ("vitamin_k" to 1.0),
            "THIA" to ("vitamin_b1" to 1.0),
            "RIBF" to ("vitamin_b2" to 1.0),
            "NIA" to ("vitamin_b3" to 1.0),
            "PANTAC" to ("vitamin_b5" to 1.0),
            "VITB6" to ("vitamin_b6" to 0.001),   // µg→mg
            "FOL" to ("vitamin_b9" to 1.0),
            "VITB12" to ("vitamin_b12" to 1.0),
            "VITC" to ("vitamin_c" to 1.0),
            "CA" to ("calcium" to 1.0),
            "FE" to ("eisen" to 1.0),
            "K" to ("kalium" to 1.0),
            "CU" to ("kupfer" to 0.001),           // µg→mg
            "MG" to ("magnesium" to 1.0),
            "MN" to ("mangan" to 0.001),            // µg→mg
            "NA" to ("natrium" to 1.0),
            "P" to ("phosphor" to 1.0),
            "SE" to ("selen" to 1.0),
            "ZN" to ("zink" to 1.0),
        )

        fun normalize(s: String): String {
            val lower = s.lowercase()
                .replace("ae", "ä").replace("oe", "ö").replace("ue", "ü")
                .replace("ß", "ss")
            val nfd = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
            return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }
    }

    /**
     * Einzelner Nährstoff-Datensatz aus der BLS-CSV.
     */
    private data class BlsNutrientRow(
        val nameDe: String,
        val energyKcal: BigDecimal?,
        val protein: BigDecimal?,
        val fat: BigDecimal?,
        val carbs: BigDecimal?,
        val fiber: BigDecimal?,
        val sugar: BigDecimal?,
        val satfat: BigDecimal?,
        val salt: BigDecimal?,
        val micronutrients: Map<String, Double>,
    )

    override fun run(vararg args: String?) {
        if (System.getProperty("mergeBlsNutrients") != "true") {
            log.info("BlsNutrientEnricher: skipped (set -DmergeBlsNutrients=true)")
            return
        }
        log.info("BlsNutrientEnricher: starte BLS-Nährstoff-Anreicherung...")

        val blsRows = loadBlsNutrientRows()
        if (blsRows.isEmpty()) {
            log.warn("BlsNutrientEnricher: keine BLS-Nährstoffdaten geladen — Abbruch")
            return
        }
        log.info("BlsNutrientEnricher: {} BLS-Nährstoffdatensätze geladen", blsRows.size)

        // Normalized-Name → BlsNutrientRow (bei Mehrfachtreffern: letzter gewinnt — BLS-Reihenfolge ist meist aufsteigend)
        val blsIndex = mutableMapOf<String, BlsNutrientRow>()
        for (row in blsRows) {
            blsIndex[normalize(row.nameDe)] = row
        }
        log.info("BlsNutrientEnricher: {} unique normalized BLS-Namen im Index", blsIndex.size)

        var matched = 0
        var enriched = 0
        var skipped = 0
        var noMatch = 0

        for (ing in ingredients.findAll()) {
            val key = normalize(ing.nameDe)
            val blsRow = blsIndex[key]
            if (blsRow == null) { noMatch++; continue }
            matched++

            var changed = false

            // Makros: nur NULL-Werte überschreiben
            if (ing.energyKcalPer100g == null && blsRow.energyKcal != null) {
                ing.energyKcalPer100g = blsRow.energyKcal; changed = true
            }
            if (ing.proteinGPer100g == null && blsRow.protein != null) {
                ing.proteinGPer100g = blsRow.protein; changed = true
            }
            if (ing.fatGPer100g == null && blsRow.fat != null) {
                ing.fatGPer100g = blsRow.fat; changed = true
            }
            if (ing.carbsGPer100g == null && blsRow.carbs != null) {
                ing.carbsGPer100g = blsRow.carbs; changed = true
            }
            if (ing.fiberGPer100g == null && blsRow.fiber != null) {
                ing.fiberGPer100g = blsRow.fiber; changed = true
            }
            if (ing.sugarGPer100g == null && blsRow.sugar != null) {
                ing.sugarGPer100g = blsRow.sugar; changed = true
            }
            if (ing.satfatGPer100g == null && blsRow.satfat != null) {
                ing.satfatGPer100g = blsRow.satfat; changed = true
            }
            if (ing.saltGPer100g == null && blsRow.salt != null) {
                ing.saltGPer100g = blsRow.salt; changed = true
            }

            // Mikros: nur fehlende Keys ergänzen
            val existingMicros = parseMicros(ing.micronutrientsJson)
            val mergedMicros = existingMicros.toMutableMap()
            for ((key, value) in blsRow.micronutrients) {
                if (!mergedMicros.containsKey(key)) {
                    mergedMicros[key] = value
                }
            }
            if (mergedMicros.size > existingMicros.size) {
                ing.micronutrientsJson = mergedMicros.entries.joinToString(
                    prefix = "{", postfix = "}"
                ) { (k, v) -> "\"$k\":$v" }
                changed = true
            }

            if (changed) {
                ing.updatedAt = Instant.now()
                ingredients.save(ing)
                enriched++
            } else {
                skipped++
            }
        }

        log.info(
            "BlsNutrientEnricher: fertig — matched={} enriched={} skipped(bereits voll)={} noMatch={}",
            matched, enriched, skipped, noMatch
        )
    }

    /**
     * Liest `seed/bls_4_0.csv` vom Classpath und extrahiert Nährstoffdaten.
     * Gleiche Parsing-Logik wie [BlsImporter.import].
     */
    private fun loadBlsNutrientRows(): List<BlsNutrientRow> {
        val reader = classpathReader("seed/bls_4_0.csv") ?: run {
            log.warn("BlsNutrientEnricher: seed/bls_4_0.csv nicht im Classpath")
            return emptyList()
        }
        val rows = mutableListOf<BlsNutrientRow>()
        val lines = reader.readLines()
        reader.close()

        val iter = lines.iterator()
        if (!iter.hasNext()) return rows
        val header = parseCsvLine(iter.next())
        val colIndex = header.withIndex().associate { (i, h) -> h.trim().substringBefore(" ") to i }

        val idxNameDe = colIndex["Lebensmittelbezeichnung"] ?: -1
        if (idxNameDe < 0) {
            log.warn("BlsNutrientEnricher: 'Lebensmittelbezeichnung' nicht im Header")
            return rows
        }

        val macroIdx = MACRO_CODES.mapNotNull { code ->
            colIndex[code]?.let { code to it }
        }.toMap()

        val microIdx: Map<String, Pair<String, Pair<Int, Double>>> = BLS_TO_MICRO.mapNotNull { (blsCode, pair) ->
            colIndex[blsCode]?.let { idx -> blsCode to (pair.first to (idx to pair.second)) }
        }.toMap()

        log.info("BlsNutrientEnricher: {} Macro-Spalten, {} Micro-Spalten gefunden",
            macroIdx.size, microIdx.size)

        for (raw in iter) {
            if (raw.isBlank()) continue
            val cols = parseCsvLine(raw)
            val nameDeRaw = cols.getOrNull(idxNameDe)?.trim().orEmpty()
            val nameDe = nameDeRaw.removeSurrounding("\"")
            if (nameDe.isEmpty()) continue

            rows += BlsNutrientRow(
                nameDe = nameDe,
                energyKcal = macroIdx["ENERCC"]?.let { parseBLS(cols.getOrNull(it)) },
                protein    = macroIdx["PROT625"]?.let { parseBLS(cols.getOrNull(it)) },
                fat        = macroIdx["FAT"]?.let { parseBLS(cols.getOrNull(it)) },
                carbs      = macroIdx["CHO"]?.let { parseBLS(cols.getOrNull(it)) },
                fiber      = macroIdx["FIBT"]?.let { parseBLS(cols.getOrNull(it)) },
                sugar      = macroIdx["SUGAR"]?.let { parseBLS(cols.getOrNull(it)) },
                satfat     = macroIdx["FASAT"]?.let { parseBLS(cols.getOrNull(it)) },
                salt       = macroIdx["NACL"]?.let { parseBLS(cols.getOrNull(it)) },
                micronutrients = parseMicrosFromBls(cols, microIdx),
            )
        }
        return rows
    }

    private fun classpathReader(path: String): BufferedReader? = try {
        val res = ClassPathResource(path)
        if (!res.exists()) null else res.inputStream.bufferedReader(Charsets.UTF_8)
    } catch (e: Exception) {
        log.warn("BlsNutrientEnricher: failed to open classpath resource '{}': {}", path, e.message)
        null
    }

    private fun parseMicrosFromBls(
        cols: List<String>,
        microIdx: Map<String, Pair<String, Pair<Int, Double>>>,
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for ((_, triple) in microIdx) {
            val (ourKey, idxAndFactor) = triple
            val (idx, factor) = idxAndFactor
            val raw = cols.getOrNull(idx)?.trim()?.removeSurrounding("\"") ?: continue
            if (raw.isEmpty() || raw == "-" || raw.startsWith("<")) continue
            val num = raw.replace(',', '.').toDoubleOrNull() ?: continue
            val converted = if (factor != 1.0) num * factor else num
            if (converted > 0.0) {
                result[ourKey] = Math.round(converted * 1000.0) / 1000.0
            }
        }
        return result
    }

    private fun parseBLS(raw: String?): BigDecimal? {
        if (raw == null) return null
        val s = raw.trim().removeSurrounding("\"")
        if (s.isEmpty() || s == "-" || s.startsWith("<")) return null
        return s.replace(',', '.').toBigDecimalOrNull()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val curr = StringBuilder()
        var inQ = false
        for (ch in line) {
            when {
                ch == '"' -> inQ = !inQ
                ch == ',' && !inQ -> { result += curr.toString(); curr.clear() }
                else -> curr.append(ch)
            }
        }
        result += curr.toString()
        return result
    }

    private fun parseMicros(json: String): Map<String, Double> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            // Einfacher Parser für JSON-Objekt mit Double-Werten
            val trimmed = json.trim().removeSurrounding("{").removeSurrounding("}")
            if (trimmed.isBlank()) return emptyMap()
            trimmed.split(",").mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
                key to value
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
