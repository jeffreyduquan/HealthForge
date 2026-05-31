package de.healthforge.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.text.Normalizer
import java.util.Locale

/**
 * P7.S3 Slice 1 / REQ-DATA-CURATION-001 — kuratiert `usda_fdc.csv` runter auf
 * eine schlanke, app-relevante Top-N-Liste ("Qualität vor Quantität").
 *
 * Eingabe (Default):
 *   - `src/main/resources/seed/usda_fdc.csv`     (Voll-Seed, ~8.355 Zeilen)
 *   - `src/main/resources/seed/fdc_top_ids.csv`  (data_type-Mapping pro fdc_id)
 *
 * Ausgabe (Default):
 *   - `src/main/resources/seed/usda_fdc_curated.csv` (selbes Format wie Input)
 *   - `src/main/resources/seed/curation_report.md`   (Statistiken + Dropped-Buckets)
 *
 * Filter-Regeln (in Reihenfolge):
 *  1. data_type ∈ {Foundation, SR Legacy}  (Branded/Survey raus)
 *  2. name_de nicht leer UND ≠ name_en      (DeepL-Lauf hat übersetzt)
 *  3. Makros vollständig (kcal > 0, protein/carbs/fat ≥ 0, nicht alle 0)
 *  4. Mindestens 3 Mikronährstoff-Werte    (sonst kein App-Mehrwert)
 *  5. Name nicht in Blacklist (Fragmente, NFS, Mixes, Babynahrung etc.)
 *  6. Brand leer (keine Markenprodukte)
 *  7. Dedupe: Namensschlüssel normalisieren → besten Qualitäts-Score behalten
 *  8. Sortieren nach Quality-Score absteigend, Top-N nehmen
 *
 * Quality-Score (höher = besser):
 *  + 10  data_type == Foundation
 *  +  5  data_type == SR Legacy
 *  +  1  je Mikronährstoff-Eintrag (cap 20)
 *  +  3  Name kurz (< 40 Zeichen, vermeidet "X with Y, prepared, ...")
 *  −  5  Name enthält Komma-Listen-Marker (", prepared", ", cooked, with salt")
 *  −  3  Name auf Englisch enthält "infant", "baby", "formula"
 *
 * CLI:
 *   --in PATH            Voll-Seed (default oben)
 *   --top-ids PATH       data_type-Quelle (default oben)
 *   --out PATH           Kuratierte CSV
 *   --report PATH        Report-MD
 *   --limit N            Top-N (Default 1500)
 *   --min-micros N       Min. Mikronährstoff-Werte (Default 3)
 *
 * Sicherheit:
 *  - Read-Only auf Source-CSVs, schreibt nur Output-Pfade.
 *  - Keine Netzwerk-Calls.
 */
object CurateUsdaSeed {

    private const val DEFAULT_IN = "src/main/resources/seed/usda_fdc.csv"
    private const val DEFAULT_TOP_IDS = "src/main/resources/seed/fdc_top_ids.csv"
    private const val DEFAULT_OUT = "src/main/resources/seed/usda_fdc_curated.csv"
    private const val DEFAULT_REPORT = "src/main/resources/seed/curation_report.md"
    private const val DEFAULT_LIMIT = 1500
    private const val DEFAULT_MIN_MICROS = 3

    // ENGLISCHE Blacklist-Muster — gegen name_en geprüft (untranslated patterns).
    // Fragmente, Mixes, NFS-Bezeichnungen, Babykost, Süßwarenrohstoffe.
    private val NAME_BLACKLIST = listOf(
        Regex("""\bNFS\b""", RegexOption.IGNORE_CASE),
        Regex("""\bnot further specified\b""", RegexOption.IGNORE_CASE),
        Regex("""\binfant formula\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbaby ?food\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(junior|toddler|strained)\b,?""", RegexOption.IGNORE_CASE),
        Regex("""\bleavening agents?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bspices,?\s+(mixed|blend)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgelatin desserts?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbeverages?,\s+carbonated\b""", RegexOption.IGNORE_CASE),
        Regex("""\bfast foods?\b""", RegexOption.IGNORE_CASE),
        Regex("""\brestaurant,\s+""", RegexOption.IGNORE_CASE),
        // Restaurant-Chains (USDA-Konvention: ALL-CAPS-Brand, dann Komma).
        Regex("""^(APPLEBEE'?S|ARBY'?S|AU BON PAIN|BAJA FRESH|BOJANGLES|BOSTON MARKET|BURGER KING|CAPTAIN D'?S|CARL'?S JR|CHICK[- ]?FIL[- ]?A|CHIPOTLE|CHURCH'?S|CRACKER BARREL|DAIRY QUEEN|DENNY'?S|DOMINO'?S|DUNKIN|FAZOLI'?S|FRIENDLY'?S|HARDEE'?S|IHOP|JACK IN THE BOX|JIMMY JOHN'?S|KFC|KRYSTAL|LITTLE CAESARS|LONG JOHN SILVER|MCDONALD'?S|OLIVE GARDEN|PANDA EXPRESS|PANERA|PAPA JOHN'?S|PERKINS|PIZZA HUT|POPEYES?|QUIZNOS|RED LOBSTER|RUBY TUESDAY|SONIC|STARBUCKS|SUBWAY|TACO BELL|TGI FRIDAY'?S|WENDY'?S|WHATABURGER|WHITE CASTLE)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsnacks?,\s+""", RegexOption.IGNORE_CASE),
        Regex("""\bcandies?,\s+""", RegexOption.IGNORE_CASE),
        Regex("""\bsweeteners?,\s+(tabletop|syrup)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdietary supplement\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmeal replacement\b""", RegexOption.IGNORE_CASE),
        // Doppelte Komma-Modifier-Ketten → meist Industrieprodukte:
        Regex("""(,[^,]+){4,}"""),
    )

    private val mapper = ObjectMapper().registerKotlinModule()

    @JvmStatic
    fun main(args: Array<String>) {
        val argMap = parseArgs(args)
        val inPath = argMap["--in"] ?: DEFAULT_IN
        val topIdsPath = argMap["--top-ids"] ?: DEFAULT_TOP_IDS
        val outPath = argMap["--out"] ?: DEFAULT_OUT
        val reportPath = argMap["--report"] ?: DEFAULT_REPORT
        val limit = argMap["--limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
        val minMicros = argMap["--min-micros"]?.toIntOrNull() ?: DEFAULT_MIN_MICROS

        val inFile = File(inPath)
        val topIdsFile = File(topIdsPath)
        require(inFile.exists()) { "Input not found: $inPath" }
        require(topIdsFile.exists()) { "Top-IDs not found: $topIdsPath" }

        // 1. data_type je fdc_id einlesen.
        val dataTypeById: Map<Long, String> = readDataTypes(topIdsFile)
        println("Loaded data_type for ${dataTypeById.size} IDs from $topIdsPath")

        // 2. Voll-Seed laden (Header behalten).
        val lines = inFile.readLines(Charsets.UTF_8)
        require(lines.isNotEmpty()) { "Empty input: $inPath" }
        val header = lines.first()
        val dataLines = lines.drop(1).filter { it.isNotBlank() && !it.startsWith("#") }
        println("Loaded ${dataLines.size} data rows from $inPath")

        // 3. Parsen + filtern pro Zeile, Stats sammeln.
        val drop = DropStats()
        val candidates = mutableListOf<Candidate>()
        for (raw in dataLines) {
            val cols = parseCsvLine(raw)
            if (cols.size < 14) { drop.malformed++; continue }
            val fdcId = cols[0].trim().toLongOrNull()
            if (fdcId == null) { drop.malformed++; continue }
            val nameDe = cols[1].trim()
            val nameEn = cols[2].trim()
            val brand = cols[3].trim()
            val kcal = cols[5].toDoubleOrNullSafe() ?: 0.0
            val protein = cols[6].toDoubleOrNullSafe() ?: 0.0
            val carbs = cols[7].toDoubleOrNullSafe() ?: 0.0
            val fat = cols[9].toDoubleOrNullSafe() ?: 0.0
            val microsJson = cols[13].trim()
            val dataType = dataTypeById[fdcId]

            // Filter 1: data_type
            if (dataType != "Foundation" && dataType != "SR Legacy") {
                drop.wrongDataType++; continue
            }
            // Filter 2: Übersetzung vorhanden
            if (nameDe.isBlank() || nameDe.equals(nameEn, ignoreCase = true)) {
                drop.noTranslation++; continue
            }
            // Filter 3: Makros
            if (kcal <= 0.0 || (protein == 0.0 && carbs == 0.0 && fat == 0.0)) {
                drop.noMacros++; continue
            }
            // Filter 4: Mikronährstoffe-Mindestmenge
            val microCount = countMicros(microsJson)
            if (microCount < minMicros) { drop.tooFewMicros++; continue }
            // Filter 5: Blacklist
            if (NAME_BLACKLIST.any { it.containsMatchIn(nameEn) }) {
                drop.blacklisted++; continue
            }
            // Filter 6: keine Marken
            if (brand.isNotBlank()) { drop.brandedOnly++; continue }

            val score = qualityScore(dataType, microCount, nameEn)
            candidates.add(Candidate(fdcId, nameDe, nameEn, dataType, score, microCount, raw))
        }
        println("After filters: ${candidates.size} candidates (dropped: $drop)")

        // 4. Dedupe: Namens-Key normalisieren → besten Score behalten.
        val byKey = mutableMapOf<String, Candidate>()
        var duped = 0
        for (c in candidates) {
            val key = normalizeName(c.nameDe)
            val prev = byKey[key]
            if (prev == null || c.score > prev.score) {
                if (prev != null) duped++
                byKey[key] = c
            } else {
                duped++
            }
        }
        println("After dedupe: ${byKey.size} unique ($duped duplicates removed)")

        // 5. Sortieren + Top-N.
        val sorted = byKey.values.sortedWith(
            compareByDescending<Candidate> { it.score }.thenBy { it.nameDe.lowercase(Locale.ROOT) },
        )
        val kept = sorted.take(limit)
        val cutOff = sorted.size - kept.size
        println("Final: ${kept.size} kept (cut off ${cutOff.coerceAtLeast(0)} below quality threshold)")

        // 6. Output schreiben.
        val outFile = File(outPath)
        outFile.parentFile?.mkdirs()
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write(header)
            w.newLine()
            kept.forEach {
                w.write(it.rawLine)
                w.newLine()
            }
        }
        println("Wrote ${kept.size} rows to $outPath")

        // 7. Report.
        writeReport(File(reportPath), dataLines.size, candidates.size, byKey.size, kept.size, drop, kept, sorted.drop(kept.size).take(30))
        println("Wrote report to $reportPath")
    }

    private fun parseArgs(args: Array<String>): Map<String, String> {
        val m = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val a = args[i]
            if (a.startsWith("--") && i + 1 < args.size) {
                m[a] = args[i + 1]; i += 2
            } else i++
        }
        return m
    }

    private fun readDataTypes(f: File): Map<Long, String> {
        val map = HashMap<Long, String>()
        f.useLines { seq ->
            seq.drop(1).forEach { line ->
                val cols = line.split(';')
                if (cols.size >= 2) {
                    val id = cols[0].trim().toLongOrNull() ?: return@forEach
                    map[id] = cols[1].trim()
                }
            }
        }
        return map
    }

    private fun countMicros(json: String): Int {
        if (json.isBlank() || json == "{}") return 0
        return try {
            @Suppress("UNCHECKED_CAST")
            val raw = mapper.readValue(json, Map::class.java) as Map<String, Any?>
            raw.count { (_, v) -> (v as? Number)?.toDouble()?.let { it > 0.0 } == true }
        } catch (_: Exception) {
            0
        }
    }

    private fun qualityScore(dataType: String, microCount: Int, nameEn: String): Int {
        var s = 0
        s += if (dataType == "Foundation") 10 else 5
        s += microCount.coerceAtMost(20)
        if (nameEn.length < 40) s += 3
        if (Regex(""",\s*(prepared|cooked|raw)\b""", RegexOption.IGNORE_CASE).containsMatchIn(nameEn)) s -= 5
        if (Regex("""\b(infant|baby|formula)\b""", RegexOption.IGNORE_CASE).containsMatchIn(nameEn)) s -= 3
        return s
    }

    /** Normalisiert auf Vergleichs-Key: lowercase, ohne Diakritika, Whitespace komprimiert. */
    private fun normalizeName(name: String): String {
        val nfd = Normalizer.normalize(name, Normalizer.Form.NFD)
        val noDiacritics = nfd.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return noDiacritics.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9 ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /** Übernommen aus UsdaFdcImporter — Semikolon-CSV mit `"..."`-Quoting. */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i += 2; continue
                }
                c == '"' -> inQuotes = !inQuotes
                c == ';' && !inQuotes -> {
                    out.add(sb.toString()); sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    private fun String.toDoubleOrNullSafe(): Double? =
        trim().replace(',', '.').toDoubleOrNull()

    private data class Candidate(
        val fdcId: Long,
        val nameDe: String,
        val nameEn: String,
        val dataType: String,
        val score: Int,
        val microCount: Int,
        val rawLine: String,
    )

    private class DropStats {
        var malformed = 0
        var wrongDataType = 0
        var noTranslation = 0
        var noMacros = 0
        var tooFewMicros = 0
        var blacklisted = 0
        var brandedOnly = 0
        override fun toString() =
            "malformed=$malformed wrongDataType=$wrongDataType noTranslation=$noTranslation " +
                "noMacros=$noMacros tooFewMicros=$tooFewMicros blacklisted=$blacklisted brandedOnly=$brandedOnly"
    }

    private fun writeReport(
        f: File,
        totalIn: Int,
        afterFilters: Int,
        afterDedupe: Int,
        finalKept: Int,
        drop: DropStats,
        kept: List<Candidate>,
        nearMiss: List<Candidate>,
    ) {
        f.parentFile?.mkdirs()
        f.bufferedWriter(Charsets.UTF_8).use { w ->
            w.appendLine("# USDA-Seed-Kuration — Report")
            w.appendLine()
            w.appendLine("Generiert: ${java.time.LocalDateTime.now()}")
            w.appendLine()
            w.appendLine("## Pipeline-Statistiken")
            w.appendLine()
            w.appendLine("| Stufe | Anzahl |")
            w.appendLine("|---|---:|")
            w.appendLine("| Eingelesen | $totalIn |")
            w.appendLine("| Nach Filtern | $afterFilters |")
            w.appendLine("| Nach Dedupe | $afterDedupe |")
            w.appendLine("| **Final (Top-N)** | **$finalKept** |")
            w.appendLine()
            w.appendLine("## Drop-Buckets")
            w.appendLine()
            w.appendLine("| Grund | Anzahl |")
            w.appendLine("|---|---:|")
            w.appendLine("| Malformed CSV | ${drop.malformed} |")
            w.appendLine("| Falscher data_type (Branded/Survey) | ${drop.wrongDataType} |")
            w.appendLine("| Keine Übersetzung (name_de leer/=name_en) | ${drop.noTranslation} |")
            w.appendLine("| Makros fehlen | ${drop.noMacros} |")
            w.appendLine("| Zu wenige Mikronährstoffe | ${drop.tooFewMicros} |")
            w.appendLine("| Blacklist-Match | ${drop.blacklisted} |")
            w.appendLine("| Markenprodukt (brand gesetzt) | ${drop.brandedOnly} |")
            w.appendLine()
            w.appendLine("## Top 50 (höchster Quality-Score)")
            w.appendLine()
            w.appendLine("| Score | name_de | data_type | Mikros |")
            w.appendLine("|---:|---|---|---:|")
            kept.take(50).forEach {
                w.appendLine("| ${it.score} | ${it.nameDe} | ${it.dataType} | ${it.microCount} |")
            }
            w.appendLine()
            w.appendLine("## Knapp aussortiert (nächste 30 unterhalb der Top-N-Schwelle)")
            w.appendLine()
            w.appendLine("| Score | name_de | Grund-Hinweis |")
            w.appendLine("|---:|---|---|")
            nearMiss.forEach {
                w.appendLine("| ${it.score} | ${it.nameDe} | unter Cutoff |")
            }
        }
    }
}
