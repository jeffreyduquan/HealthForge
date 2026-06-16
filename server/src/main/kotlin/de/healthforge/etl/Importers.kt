package de.healthforge.etl

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
import java.util.UUID

/**
 * Common contract for all CSV importers. Implementations are stateless beans;
 * the orchestrator opens an [EtlRunEntity] before and closes it after the call.
 *
 * Hinweis (P7.S2): bewusst NICHT `sealed`, damit Source-spezifische Importer
 * (z.B. `UsdaFdcImporter` im Sub-Package) registriert werden können.
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
 * Skeleton importer for the German Bundeslebensmittelschlüssel (BLS).
 *
 * Expected CSV columns (semicolon-separated, no header tolerated by row 1 check):
 *   sbls;name;kcal;protein;carb;fat;fiber;salt
 *
 * Until a licensed seed file is placed at `resources/seed/bls.csv`, this importer
 * returns [Counts.skipped]. The licensing constraints are documented in P1.S4 backlog.
 *
 * @deprecated P7.S2 Slice 3a (2026-05-28) — Superseded by [de.healthforge.etl.usda.UsdaFdcImporter]
 *   als kanonischem Lebensmittel-Korpus (REQ-DATA-SOURCE-001). BLS bleibt aus
 *   Lizenzgründen ungenutzt; Bean bleibt registriert für historische `etl_runs`-Rows.
 *   Manuelle `POST /admin/v1/etl/run?source=BLS` Triggers loggen eine Warnung im Orchestrator.
 */
@Deprecated(
    message = "BLS-Importer wird durch UsdaFdcImporter abgelöst (P7.S2). Nicht für neuen Code verwenden.",
    level = DeprecationLevel.WARNING,
)
@Component
class BlsImporter(private val ingredients: IngredientRepository) : Importer {
    override val source = EtlSource.BLS
    override fun seedResourcePath() = "seed/bls.csv"

    @Transactional
    override fun import(): Counts {
        val reader = classpathReader(seedResourcePath()) ?: return Counts.skipped
        var inserted = 0; var updated = 0; var skipped = 0
        reader.useLines { lines ->
            lines.drop(1).forEach { raw ->
                val cols = raw.split(';')
                if (cols.size < 8) { skipped++; return@forEach }
                val sourceId = cols[0].trim()
                val name = cols[1].trim().ifBlank { return@forEach.also { skipped++ } }
                val existing = ingredients.findBySourceAndSourceId(IngredientSource.BLS, sourceId)
                val entity = existing.orElseGet {
                    IngredientEntity(
                        nameDe = name,
                        source = IngredientSource.BLS,
                        sourceId = sourceId,
                    )
                }
                entity.nameDe = name
                entity.energyKcalPer100g = cols[2].toBigDecimalOrNull()
                entity.proteinGPer100g = cols[3].toBigDecimalOrNull()
                entity.carbsGPer100g = cols[4].toBigDecimalOrNull()
                entity.fatGPer100g = cols[5].toBigDecimalOrNull()
                entity.fiberGPer100g = cols[6].toBigDecimalOrNull()
                entity.saltGPer100g = cols[7].toBigDecimalOrNull()
                entity.locked = true
                entity.updatedAt = Instant.now()
                ingredients.save(entity)
                if (existing.isPresent) updated++ else inserted++
            }
        }
        return Counts(inserted, updated, skipped)
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? =
    trim().replace(',', '.').takeIf { it.isNotBlank() }?.toBigDecimalOrNull()

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
            // P7.S5c: Auch ASCII-Umlaut-Varianten normalisieren (ae→a, oe→o, ue→u),
            // da viele DB-Namen "Haehnchen" statt "Hähnchen" schreiben.
            val nameAlt = name.replace("ae", "a").replace("oe", "o").replace("ue", "u")
            if (name.isBlank()) { skipped++; continue }
            // pick best matching rule: max score, ties → longer keyword
            val best = rules
                .asSequence()
                .filter { it.normalized.isNotBlank() && (name.contains(it.normalized) || nameAlt.contains(it.normalized)) }
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
        val lower = s.lowercase().replace('ß', 's').let { it.replace("ss", "ss") } // ß→ss (idempotent)
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
 * @deprecated P7.S2 Slice 3a (2026-05-28) — Superseded by [de.healthforge.etl.usda.UsdaFdcImporter].
 *   OFF-Datenqualität ist heterogen (Crowdsourced, fehlende Mikros); USDA-FDC ist die kuratierte
 *   Single-Source-of-Truth (REQ-DATA-SOURCE-001). Bean bleibt registriert für historische `etl_runs`.
 */
@Deprecated(
    message = "OFF-Importer wird durch UsdaFdcImporter abgelöst (P7.S2). Nicht für neuen Code verwenden.",
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
            // P7.S2 Slice 3a: BLS+OFF sind @Deprecated zugunsten USDA_FDC. Trigger werden
            // zugelassen (für Migrations-/Audit-Zwecke) aber im Log markiert.
            log.warn("ETL: triggered DEPRECATED importer source={} — prefer USDA_FDC (REQ-DATA-SOURCE-001)", source)
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
