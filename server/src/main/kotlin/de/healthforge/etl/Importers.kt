package de.healthforge.etl

import de.healthforge.ingredient.IngredientEntity
import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Common contract for all CSV importers. Implementations are stateless beans;
 * the orchestrator opens an [EtlRunEntity] before and closes it after the call.
 *
 * Hinweis (P7.S2): bewusst NICHT `sealed`, damit Source-spezifische Importer
 * korrekt registriert werden können.
 */
interface Importer {
    val source: EtlSource
    fun seedResourcePath(): String

    /**
     * Imports rows from the configured classpath seed file. Returns a [Counts] triple.
     * If the seed file is missing the importer SHOULD return [Counts.skipped].
     */
    fun import(): Counts
}

data class Counts(val inserted: Int, val updated: Int, val skipped: Int, val skippedNoFile: Boolean = false) {
    companion object {
        val skipped = Counts(0, 0, 0, skippedNoFile = true)
    }
}

private val LOG = LoggerFactory.getLogger("etl")

private fun classpathReader(path: String): BufferedReader? = try {
    val res = ClassPathResource(path)
    if (!res.exists()) null else res.inputStream.bufferedReader(Charsets.UTF_8)
} catch (e: Exception) {
    LOG.warn("ETL: failed to open classpath resource '{}': {}", path, e.message)
    null
}

/**
 * BLS 4.0 Importer (Bundeslebensmittelschlüssel, CC BY 4.0).
 *
 * Phase 1 — RAW-Baseline: importiert NUR Einträge mit ", roh" im Namen.
 * Reichert allergens_json, histamine_score, fodmap_flags_json aus bestehender
 * DB per normalisiertem Name-Match an.
 *
 * Macro-Mapping (direkte Spalten):
 *   ENERCC→kcal, PROT625→protein, FAT→fat, CHO→carbs, FIBT→fiber,
 *   SUGAR→sugar, FASAT→satfat, NACL→salt
 *
 * Micro-Mapping (→ micronutrients_json):
 *   VITAA→vitamin_a(µg), VITD→vitamin_d(µg), TOCPHA→vitamin_e(mg),
 *   VITK1→vitamin_k(µg), THIA→vitamin_b1(mg), RIBF→vitamin_b2(mg),
 *   NIA→vitamin_b3(mg), PANTAC→vitamin_b5(mg), VITB6→vitamin_b6(mg*),
 *   FOL→vitamin_b9(µg), VITB12→vitamin_b12(µg), VITC→vitamin_c(mg),
 *   CA→calcium(mg), FE→eisen(mg), K→kalium(mg), CU→kupfer(mg*),
 *   MG→magnesium(mg), MN→mangan(mg*), NA→natrium(mg),
 *   P→phosphor(mg), SE→selen(µg), ZN→zink(mg)
 *   (* = BLS liefert µg, wir konvertieren zu mg)
 */
@Component
class BlsImporter(private val ingredients: IngredientRepository) : Importer {
    override val source = EtlSource.BLS
    override fun seedResourcePath() = "seed/bls_4_0.csv"

    companion object {
        // BLS code → (our micro key, unit conversion factor: BLS→our)
        val BLS_TO_MICRO: Map<String, Pair<String, Double>> = mapOf(
            "VITAA" to ("vitamin_a" to 1.0),     // µg→µg
            "VITD" to ("vitamin_d" to 1.0),       // µg→µg
            "TOCPHA" to ("vitamin_e" to 1.0),     // mg→mg
            "VITK1" to ("vitamin_k" to 1.0),      // µg→µg
            "THIA" to ("vitamin_b1" to 1.0),      // mg→mg
            "RIBF" to ("vitamin_b2" to 1.0),      // mg→mg
            "NIA" to ("vitamin_b3" to 1.0),       // mg→mg
            "PANTAC" to ("vitamin_b5" to 1.0),    // mg→mg
            "VITB6" to ("vitamin_b6" to 0.001),   // µg→mg
            "FOL" to ("vitamin_b9" to 1.0),       // µg→µg
            "VITB12" to ("vitamin_b12" to 1.0),   // µg→µg
            "VITC" to ("vitamin_c" to 1.0),       // mg→mg
            "CA" to ("calcium" to 1.0),           // mg→mg
            "FE" to ("eisen" to 1.0),             // mg→mg
            "K" to ("kalium" to 1.0),             // mg→mg
            "CU" to ("kupfer" to 0.001),          // µg→mg
            "MG" to ("magnesium" to 1.0),         // mg→mg
            "MN" to ("mangan" to 0.001),          // µg→mg
            "NA" to ("natrium" to 1.0),           // mg→mg
            "P" to ("phosphor" to 1.0),           // mg→mg
            "SE" to ("selen" to 1.0),             // µg→µg
            "ZN" to ("zink" to 1.0),              // mg→mg
        )
        val MACRO_CODES = setOf("ENERCC", "PROT625", "FAT", "CHO", "FIBT", "SUGAR", "FASAT", "NACL")

        /** Name-Normalisierung wie SIGHI-Importer: lowercase, ß→ss, Diakritika entfernen. */
        fun normalize(s: String): String {
            val lower = s.lowercase()
                .replace("ae", "ä").replace("oe", "ö").replace("ue", "ü")
                .replace("ß", "ss")
            val nfd = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
            return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }
    }

    @Transactional
    override fun import(): Counts {
        val reader = classpathReader(seedResourcePath()) ?: return Counts.skipped
        var inserted = 0; var updated = 0; var enriched = 0; var skipped = 0

        // --- Curation-Map aus bls_curation.csv laden (BLS-Code → SIGHI/Allergene/FODMAP) ---
        val curationMap = loadCurationMap()
        LOG.info("BLS 4.0: {} kuratierte BLS-Codes geladen (SIGHI/Allergene/FODMAP)", curationMap.size)

        // --- Name-basierte Enrichment-Map aus bestehender DB (Fallback) ---
        val enrichment = mutableMapOf<String, EnrichmentData>()
        for (e in ingredients.findAll()) {
            val key = normalize(e.nameDe)
            if (key.isNotBlank() && (e.histamineScore != null || e.allergensJson != "[]" || e.fodmapFlagsJson != "[]")) {
                val existing = enrichment[key]
                if (existing == null || (e.histamineScore ?: 0) > (existing.histamineScore ?: 0)) {
                    enrichment[key] = EnrichmentData(
                        histamineScore = e.histamineScore,
                        allergensJson = e.allergensJson,
                        fodmapFlagsJson = e.fodmapFlagsJson,
                    )
                }
            }
        }
        LOG.info("BLS 4.0: {} name-basierte Enrichment-Einträge aus bestehender DB", enrichment.size)

        reader.useLines { lines ->
            val iter = lines.iterator()
            if (!iter.hasNext()) return@useLines
            val header = parseCsvLine(iter.next())
            // BLS 4.0 CSV header: "ENERCC Energie (Kilokalorien) [kcal/100g]"
            // → wir indexieren nur den ersten Token (den BLS-Code)
            val colIndex = mutableMapOf<String, Int>()
            for ((i, h) in header.withIndex()) {
                colIndex.putIfAbsent(h.trim().substringBefore(" "), i)
            }

            val idxCode = colIndex["BLS"] ?: -1
            val idxNameDe = colIndex["Lebensmittelbezeichnung"] ?: -1
            if (idxCode < 0 || idxNameDe < 0) {
                LOG.warn("BLS 4.0: 'BLS Code'/'Lebensmittelbezeichnung' nicht im Header — Abbruch")
                return@useLines
            }

            val macroIdx = MACRO_CODES.mapNotNull { code ->
                colIndex[code]?.let { code to it }
            }.toMap()

            val microIdx: Map<String, Pair<String, Pair<Int, Double>>> = BLS_TO_MICRO.mapNotNull { (blsCode, pair) ->
                colIndex[blsCode]?.let { idx -> blsCode to (pair.first to (idx to pair.second)) }
            }.toMap()

            LOG.info("BLS 4.0: {} Macro-Spalten, {} von {} Micro-Spalten gemappt",
                macroIdx.size, microIdx.size, BLS_TO_MICRO.size)

            val batch = mutableListOf<IngredientEntity>()
            for (raw in iter) {
                if (raw.isBlank()) continue
                val cols = parseCsvLine(raw)
                val sourceId = cols.getOrNull(idxCode)?.trim().orEmpty()
                val nameDeRaw = cols.getOrNull(idxNameDe)?.trim().orEmpty()
                val nameDe = nameDeRaw.removeSurrounding("\"")
                if (sourceId.isEmpty() || nameDe.isEmpty()) { skipped++; continue }

                // KEINE Whitelist mehr — ALLE BLS-Einträge werden importiert

                val existing = ingredients.findBySourceAndSourceId(IngredientSource.BLS, sourceId)
                val entity = existing.orElseGet {
                    IngredientEntity(nameDe = nameDe, source = IngredientSource.BLS, sourceId = sourceId)
                }
                entity.nameDe = nameDe
                entity.energyKcalPer100g = macroIdx["ENERCC"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.proteinGPer100g    = macroIdx["PROT625"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.fatGPer100g        = macroIdx["FAT"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.carbsGPer100g      = macroIdx["CHO"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.fiberGPer100g      = macroIdx["FIBT"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.sugarGPer100g      = macroIdx["SUGAR"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.satfatGPer100g     = macroIdx["FASAT"]?.let { parseBLS(cols.getOrNull(it)) }
                entity.saltGPer100g       = macroIdx["NACL"]?.let { parseBLS(cols.getOrNull(it)) }

                // Micronutrients mit Unit-Konversion
                val micros = mutableMapOf<String, Double>()
                for ((_, triple) in microIdx) {
                    val (ourKey, idxAndFactor) = triple
                    val (idx, factor) = idxAndFactor
                    val num = parseBLS(cols.getOrNull(idx)) ?: continue
                    val converted = if (factor != 1.0) num.toDouble() * factor else num.toDouble()
                    if (converted > 0.0) micros[ourKey] = Math.round(converted * 1000.0) / 1000.0
                }
                if (micros.isNotEmpty()) {
                    entity.micronutrientsJson = micros.entries.joinToString(
                        prefix = "{", postfix = "}"
                    ) { (k, v) -> "\"$k\":$v" }
                }

                // --- Enrichment: zuerst Curation-Map (BLS-Code-basiert, präzise), dann Name-Fallback ---
                val cur = curationMap[sourceId]
                if (cur != null) {
                    if (cur.histamineScore != null) entity.histamineScore = cur.histamineScore
                    if (cur.allergensJson != "[]") entity.allergensJson = cur.allergensJson
                    if (cur.fodmapFlagsJson != "[]") entity.fodmapFlagsJson = cur.fodmapFlagsJson
                    enriched++
                } else {
                    // Fallback: name-basierte Anreicherung aus bestehender DB
                    val enr = enrichment[normalize(nameDe)]
                    if (enr != null) {
                        if (enr.histamineScore != null) entity.histamineScore = enr.histamineScore
                        if (enr.allergensJson != "[]") entity.allergensJson = enr.allergensJson
                        if (enr.fodmapFlagsJson != "[]") entity.fodmapFlagsJson = enr.fodmapFlagsJson
                        enriched++
                    }
                }

                entity.locked = true
                entity.updatedAt = Instant.now()
                batch.add(entity)
                if (existing.isPresent) updated++ else inserted++

                // Batch-Save alle 500 Entities + EntityManager clearen (Speicher)
                if (batch.size >= 500) {
                    ingredients.saveAll(batch)
                    batch.clear()
                }
            }
            // Rest-Batch
            if (batch.isNotEmpty()) {
                ingredients.saveAll(batch)
            }
        }
        LOG.info("BLS 4.0: {} inserted, {} updated, {} enriched, {} skipped (KOMPLETT-Import)", inserted, updated, enriched, skipped)
        return Counts(inserted, updated, skipped)
    }

    private data class EnrichmentData(
        val histamineScore: Short?,
        val allergensJson: String,
        val fodmapFlagsJson: String,
    )

    private data class CurationRow(
        val histamineScore: Short?,
        val allergensJson: String,
        val fodmapFlagsJson: String,
    )

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

    private fun parseBLS(raw: String?): BigDecimal? {
        if (raw == null) return null
        val s = raw.trim().removeSurrounding("\"")
        if (s.isEmpty() || s == "-" || s.startsWith("<")) return null
        return s.replace(',', '.').toBigDecimalOrNull()
    }

    /** Liest `seed/bls_curation.csv` vom Classpath und gibt eine Map BLS-Code → CurationRow zurück. */
    private fun loadCurationMap(): Map<String, CurationRow> {
        val reader = classpathReader("seed/bls_curation.csv")
        if (reader == null) { LOG.warn("Curation-Map: seed/bls_curation.csv nicht im Classpath"); return emptyMap() }
        val map = mutableMapOf<String, CurationRow>()
        reader.useLines { lines ->
            for ((lineNo, raw) in lines.withIndex()) {
                if (lineNo == 0 || raw.isBlank() || raw.startsWith("#")) continue
                val cols = parseCsvLine(raw)
                if (cols.size < 6) continue
                val blsCode = cols[0].trim()
                val allergensRaw = cols[3].trim()
                val sighiRaw = cols[4].trim()
                val fodmapRaw = cols[5].trim()
                val sighiScore: Short? = sighiRaw.toShortOrNull()
                val allergensJson = toJsonArray(allergensRaw)
                val fodmapJson = toJsonArray(fodmapRaw)
                // Nur eintragen wenn mindestens ein Wert gesetzt ist
                if (sighiScore != null || allergensJson != "[]" || fodmapJson != "[]") {
                    map[blsCode] = CurationRow(sighiScore, allergensJson, fodmapJson)
                }
            }
        }
        return map
    }

    private fun toJsonArray(raw: String): String {
        val trimmed = raw.trim().removeSurrounding("\"")
        if (trimmed == "[]" || trimmed.isBlank()) return "[]"
        val inner = trimmed.removeSurrounding("[").removeSurrounding("]")
        val items = inner.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return items.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? {
    val cleaned = trim().replace(',', '.')
    if (cleaned.isBlank()) return null
    return try { BigDecimal(cleaned) } catch (_: NumberFormatException) { null }
}

/**
 * SIGHI Histamin-Verträglichkeits-Importer (REQ-INGR-003).
 *
 * UPDATE-only: matcht Keywords aus `seed/sighi.csv` als Substring (akzent-/case-insensitiv)
 * gegen alle vorhandenen `Ingredient.nameDe` und setzt `histamineScore` (0..3).
 *
 * CSV-Format (Kommentar-Zeilen `#…` + Header `keyword;…` werden übersprungen):
 *   keyword;score;category
 *
 * Source der Daten: SIGHI-Merkblatt v2021-11-17 (PDF, public, c) SIGHI),
 * kuratiert in 3 Buckets (3 = zu meiden, 1 = unsicher, 0 = gut verträglich).
 *
 * Match-Regeln:
 *  - Score wird normalisiert: keyword + nameDe → lowercase, ß→ss, Diakritika entfernt.
 *  - Substring-Match. Bei mehreren Treffern in einer Zutat gewinnt der **höchste Score**
 *    (Vorsichtsprinzip: schon eine Hochrisiko-Komponente macht das Lebensmittel kritisch).
 *  - Bei Score-Gleichstand gewinnt das **längere Keyword** (spezifischer).
 *  - Nicht-gematchte Zutaten behalten ihren bisherigen `histamineScore` (null = unbekannt
 *    laut REQ-QUALITY-003).
 */
@Component
class SighiImporter(private val ingredients: IngredientRepository) : Importer {
    override val source = EtlSource.SIGHI
    override fun seedResourcePath() = "seed/sighi.csv"

    private data class Rule(val keyword: String, val normalized: String, val score: Short)

    @Transactional
    override fun import(): Counts {
        val reader = classpathReader(seedResourcePath()) ?: return Counts.skipped
        val rules = mutableListOf<Rule>()
        reader.useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val cols = line.split(';')
                if (cols.size < 2) return@forEach
                val keyword = cols[0].trim()
                if (keyword.equals("keyword", ignoreCase = true)) return@forEach // header
                if (keyword.isBlank()) return@forEach
                val score = cols[1].trim().toShortOrNull()?.takeIf { it in 0..3 } ?: return@forEach
                rules += Rule(keyword, normalize(keyword), score)
            }
        }
        if (rules.isEmpty()) {
            LOG.warn("SIGHI: keine Regeln aus {} geladen", seedResourcePath())
            return Counts.skipped
        }
        LOG.info("SIGHI: {} Verträglichkeits-Regeln geladen", rules.size)

        var updated = 0; var skipped = 0
        for (e in ingredients.findAll()) {
            val name = normalize(e.nameDe)
            if (name.isBlank()) { skipped++; continue }
            // pick best matching rule: max score, ties → longer keyword
            val best = rules
                .asSequence()
                .filter { it.normalized.isNotBlank() && name.contains(it.normalized) }
                .maxWithOrNull(
                    compareBy<Rule>({ it.score }, { it.normalized.length })
                )
            if (best == null) { skipped++; continue }
            if (e.histamineScore == best.score) { skipped++; continue }
            e.histamineScore = best.score
            e.updatedAt = Instant.now()
            ingredients.save(e)
            updated++
        }
        LOG.info("SIGHI: {} Ingredients aktualisiert, {} ohne Match", updated, skipped)
        return Counts(0, updated, skipped)
    }

    private fun normalize(s: String): String {
        // P7.S5c: Erst ASCII-Umlaute zu echten Umlauten konvertieren,
        // dann NFD-normalisieren → "Haehnchen" und "Hähnchen" werden beide zu "hahnchen".
        val lower = s.lowercase()
            .replace("ae", "ä").replace("oe", "ö").replace("ue", "ü")
            .replace("ß", "ss")
        val nfd = java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }
}

/**
 * Skeleton importer for Open Food Facts (OFF). Reads NDJSON snapshots later;
 * for now only opens a single-row CSV fixture if present at `resources/seed/off.csv`.
 *
 * Expected CSV columns:
 *   code;product_name;brands;energy_kcal_100g;proteins_100g;carbohydrates_100g;sugars_100g;fat_100g;saturated_fat_100g;fiber_100g;salt_100g
 *
 * @deprecated LEGACY/Fallback-Importer für historische OFF-Dateien.
 *   OFF-Datenqualität ist heterogen (Crowdsourced, fehlende Mikros).
 *   Bean bleibt registriert für historische `etl_runs`.
 */
@Deprecated(
    message = "OFF-Importer ist Legacy/Fallback. Nicht für neuen Code verwenden.",
    level = DeprecationLevel.WARNING,
)
@Component
class OffImporter(private val ingredients: IngredientRepository) : Importer {
    override val source = EtlSource.OFF
    override fun seedResourcePath() = "seed/off.csv"

    @Transactional
    override fun import(): Counts {
        val reader = classpathReader(seedResourcePath()) ?: return Counts.skipped
        var inserted = 0; var updated = 0; var skipped = 0
        reader.useLines { lines ->
            lines.drop(1).forEach { raw ->
                val cols = raw.split(';')
                if (cols.size < 11) { skipped++; return@forEach }
                val barcode = cols[0].trim().ifBlank { return@forEach.also { skipped++ } }
                val name = cols[1].trim().ifBlank { return@forEach.also { skipped++ } }
                val existing = ingredients.findBySourceAndSourceId(IngredientSource.OFF, barcode)
                val entity = existing.orElseGet {
                    IngredientEntity(
                        nameDe = name,
                        source = IngredientSource.OFF,
                        sourceId = barcode,
                        barcode = barcode,
                    )
                }
                entity.nameDe = name
                entity.brand = cols[2].trim().ifBlank { null }
                entity.barcode = barcode
                entity.energyKcalPer100g = cols[3].toBigDecimalOrNull()
                entity.proteinGPer100g = cols[4].toBigDecimalOrNull()
                entity.carbsGPer100g = cols[5].toBigDecimalOrNull()
                entity.sugarGPer100g = cols[6].toBigDecimalOrNull()
                entity.fatGPer100g = cols[7].toBigDecimalOrNull()
                entity.satfatGPer100g = cols[8].toBigDecimalOrNull()
                entity.fiberGPer100g = cols[9].toBigDecimalOrNull()
                entity.saltGPer100g = cols[10].toBigDecimalOrNull()
                entity.locked = true
                entity.updatedAt = Instant.now()
                ingredients.save(entity)
                if (existing.isPresent) updated++ else inserted++
            }
        }
        return Counts(inserted, updated, skipped)
    }
}

/**
 * Orchestrates importer runs and protocols them in [EtlRunEntity].
 */
@Component
class EtlOrchestrator(
    private val runs: EtlRunRepository,
    importers: List<Importer>,
) {
    private val byName = importers.associateBy { it.source }
    private val log = LoggerFactory.getLogger(EtlOrchestrator::class.java)

    fun run(source: EtlSource, triggeredBy: UUID? = null): EtlRunEntity {
        val importer = byName[source] ?: error("No importer registered for $source")
        if (source == EtlSource.BLS || source == EtlSource.OFF) {
            // Legacy Sources kept for historical compatibility (z. B. Migrations-/Audit-Zwecke).
            log.warn("ETL: triggered legacy importer source={} — current single-source baseline is BLS", source)
        }
        val run = runs.save(EtlRunEntity(source = source, triggeredBy = triggeredBy))
        try {
            val counts = importer.import()
            run.rowsInserted = counts.inserted
            run.rowsUpdated = counts.updated
            run.rowsSkipped = counts.skipped
            run.status = if (counts.skippedNoFile) EtlStatus.SKIPPED_NO_FILE else EtlStatus.SUCCESS
        } catch (e: Exception) {
            log.error("ETL run $source failed", e)
            run.status = EtlStatus.FAILED
            run.errorMessage = (e.message ?: e.javaClass.simpleName).take(2000)
        } finally {
            run.finishedAt = Instant.now()
            runs.save(run)
        }
        return run
    }
}
