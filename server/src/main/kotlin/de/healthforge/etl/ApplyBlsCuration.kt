package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File

@Component
class ApplyBlsCuration(
    private val ingredients: IngredientRepository,
    private val blsImporter: BlsImporter,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(ApplyBlsCuration::class.java)

    override fun run(vararg args: String?) {
        if (System.getProperty("applyCurated") != "true") {
            log.info("ApplyBlsCuration: skipped")
            return
        }
        val csvFile = File("../data/bls_curation.csv")
        if (!csvFile.exists()) {
            log.warn("ApplyBlsCuration: {} not found", csvFile.absolutePath)
            return
        }
        log.info("ApplyBlsCuration: removing all existing BLS entries...")
        val stale = ingredients.findAll().filter { it.source == de.healthforge.ingredient.IngredientSource.BLS }
        ingredients.deleteAll(stale)
        log.info("ApplyBlsCuration: removed {} stale BLS entries", stale.size)

        log.info("ApplyBlsCuration: running BLS ETL...")
        val counts = blsImporter.import()
        log.info("ApplyBlsCuration: ETL done i={} u={} s={}", counts.inserted, counts.updated, counts.skipped)
        var updated = 0; var skipped = 0; var notFound = 0
        csvFile.useLines { lines ->
            for ((lineNo, raw) in lines.withIndex()) {
                if (lineNo == 0 || raw.isBlank() || raw.startsWith("#")) continue
                val cols = parseCsvLine(raw)
                if (cols.size < 6) { skipped++; continue }
                val blsCode = cols[0].trim()
                val nameDe = cols[1].trim().removeSurrounding("\"")
                val type = cols[2].trim()
                if (type != "RAW") { skipped++; continue }
                val allergensRaw = cols[3].trim()
                val sighiRaw = cols[4].trim()
                val fodmapRaw = cols[5].trim()
                val sighiScore: Short? = sighiRaw.toShortOrNull()
                val allergensJson = toJsonArray(allergensRaw)
                val fodmapJson = toJsonArray(fodmapRaw)
                val entity = ingredients.findBySourceAndSourceId(IngredientSource.BLS, blsCode)
                if (!entity.isPresent) { notFound++; continue }
                val ing = entity.get()
                var changed = false
                if (sighiScore != null && ing.histamineScore != sighiScore) { ing.histamineScore = sighiScore; changed = true }
                if (allergensJson != "[]" && ing.allergensJson != allergensJson) { ing.allergensJson = allergensJson; changed = true }
                if (fodmapJson != "[]" && ing.fodmapFlagsJson != fodmapJson) { ing.fodmapFlagsJson = fodmapJson; changed = true }
                if (changed) { ingredients.save(ing); updated++ }
            }
        }
        log.info("ApplyBlsCuration: done u={} nf={} s={}", updated, notFound, skipped)
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

    private fun toJsonArray(raw: String): String {
        val trimmed = raw.trim().removeSurrounding("\"")
        if (trimmed == "[]" || trimmed.isBlank()) return "[]"
        val inner = trimmed.removeSurrounding("[").removeSurrounding("]")
        val items = inner.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return items.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }
}
