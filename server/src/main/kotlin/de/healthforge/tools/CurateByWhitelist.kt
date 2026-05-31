package de.healthforge.tools

import java.io.File
import java.text.Normalizer

/**
 * REQ-DATA-CURATION-002 — Whitelist-driven Curation (deutsche Essentials).
 *
 * Liest `essentials_de.csv` (kuratierte ~650 Pflicht-Zutaten in 24 Kategorien)
 * und matcht jeden Eintrag gegen den USDA-Voll-Seed `usda_fdc.csv` per
 * Token-Overlap-Score auf `name_en`. Output ist `usda_fdc_curated.csv`
 * (überschreibt das alte Top-N-Set) mit Whitelist-`name_de` + USDA-Nährwerten.
 *
 * Eingabe (Defaults):
 *  - `src/main/resources/seed/essentials_de.csv`  Format: `category;name_de;search_en;search_alt_en`
 *  - `src/main/resources/seed/usda_fdc.csv`       Voll-Seed mit allen Nährwerten
 *
 * Ausgabe (Defaults):
 *  - `src/main/resources/seed/usda_fdc_curated.csv`  (selbes Schema wie Voll-Seed)
 *  - `src/main/resources/seed/curation_report.md`     (Matches + Misses + Score-Stats)
 *
 * Matching-Score (höher = besser):
 *  + 10  je Whitelist-Token, das in name_en als ganzes Wort vorkommt
 *  +  8  je Whitelist-Token-Substring-Match
 *  + 15  name_en beginnt mit erstem Whitelist-Token
 *  +  5  alle Whitelist-Tokens treffen
 *  +  1  je vorhandenem Mikronährstoff (cap 20)
 *  +  3  kcal > 0 (Makros komplett)
 *  −  1  je Token in name_en das NICHT in der Whitelist ist (Spezifitäts-Penalty)
 *  −  5  brand != ""
 *  − 50  fdc_id bereits in Output (kein Mehrfachverbrauch)
 *
 * Fallback: wenn primary search_en keinen Match > 5 ergibt, versuche search_alt_en.
 *
 * Sicherheit: read-only auf Source-CSVs, schreibt nur Output-Pfade. Kein Netzwerk.
 */
object CurateByWhitelist {

    private const val DEFAULT_WHITELIST = "src/main/resources/seed/essentials_de.csv"
    private const val DEFAULT_POOL = "src/main/resources/seed/usda_fdc.csv"
    private const val DEFAULT_OUT = "src/main/resources/seed/usda_fdc_curated.csv"
    private const val DEFAULT_REPORT = "src/main/resources/seed/curation_report.md"
    private const val MIN_SCORE = 10

    private val STOP_WORDS = setOf(
        "and", "or", "with", "without", "the", "of", "in", "to", "a",
        "raw", "cooked", "fresh", "dried", "ground", "whole", "ready",
        "to", "eat", "prepared", "serve", "regular", "type"
    )

    /** Descriptor terms that radically change what a food IS. If the query doesn't mention them, penalize hard. */
    private val FORM_PENALTIES = mapOf(
        "juice" to 40,
        "peel" to 50,
        "peels" to 50,
        "skin" to 25,
        "skins" to 25,
        "powder" to 30,
        "powdered" to 30,
        "dehydrated" to 25,
        "concentrate" to 30,
        "concentrated" to 30,
        "syrup" to 25,
        "extract" to 25,
        "leaf" to 20,
        "leaves" to 20,
        "stalk" to 15,
        "stalks" to 15,
        "shoot" to 15,
        "shoots" to 15,
        "freeze-dried" to 20,
        "candied" to 30,
        "sweetened" to 15,
        "jam" to 25,
        "jelly" to 25,
        "puree" to 20,
        "sauce" to 15,
        "spread" to 15,
        "drink" to 20,
        "beverage" to 15,
        "smoothie" to 25,
        "frozen" to 8,
        "canned" to 5,
    )

    data class UsdaRow(
        val fdcId: String,
        val nameDe: String,
        val nameEn: String,
        val brand: String,
        val ingredientsEn: String,
        val kcal: String,
        val protein: String,
        val carbs: String,
        val sugar: String,
        val fat: String,
        val satfat: String,
        val fiber: String,
        val salt: String,
        val micronutrientsJson: String,
        val nameEnTokens: Set<String>,
        val microCount: Int,
    )

    data class Whitelist(
        val category: String,
        val nameDe: String,
        val searchEn: String,
        val searchAltEn: String,
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val whitelistPath = argValue(args, "--whitelist") ?: DEFAULT_WHITELIST
        val poolPath = argValue(args, "--pool") ?: DEFAULT_POOL
        val outPath = argValue(args, "--out") ?: DEFAULT_OUT
        val reportPath = argValue(args, "--report") ?: DEFAULT_REPORT

        println("[CurateByWhitelist] whitelist=$whitelistPath")
        println("[CurateByWhitelist] pool=$poolPath")
        println("[CurateByWhitelist] out=$outPath")

        val whitelist = readWhitelist(File(whitelistPath))
        val pool = readPool(File(poolPath))
        println("[CurateByWhitelist] Loaded ${whitelist.size} essentials, ${pool.size} pool entries")

        val usedFdcIds = mutableSetOf<String>()
        val matches = mutableMapOf<Whitelist, UsdaRow>()
        val scoreOf = mutableMapOf<Whitelist, Int>()

        // 2-Pass: pass 1 nimmt nur sehr gute Matches (≥40), pass 2 fällt auf MIN_SCORE zurück.
        // Verhindert dass marginale Fallbacks ("Garam Masala"→garlic powder) gute Targets
        // ("Knoblauchpulver") wegnehmen.
        for (threshold in listOf(40, MIN_SCORE)) {
            for (w in whitelist) {
                if (w in matches) continue
                val match = bestMatch(w.searchEn, pool, usedFdcIds)
                    ?: bestMatch(w.searchAltEn, pool, usedFdcIds)
                if (match != null && match.second >= threshold) {
                    matches[w] = match.first
                    usedFdcIds.add(match.first.fdcId)
                    scoreOf[w] = match.second
                }
            }
        }

        val matchList = whitelist.mapNotNull { w -> matches[w]?.let { w to it } }
        val misses = whitelist.filter { it !in matches }
        val matchScores = scoreOf.values.toList()

        writeOutput(File(outPath), matchList)
        writeReport(File(reportPath), whitelist, matchList, misses, matchScores)

        println("[CurateByWhitelist] Matched: ${matchList.size}/${whitelist.size} (${matchList.size * 100 / whitelist.size}%)")
        println("[CurateByWhitelist] Misses:  ${misses.size}")
        println("[CurateByWhitelist] Wrote $outPath + $reportPath")
    }

    private fun argValue(args: Array<String>, key: String): String? {
        val i = args.indexOf(key)
        return if (i >= 0 && i + 1 < args.size) args[i + 1] else null
    }

    private fun readWhitelist(file: File): List<Whitelist> {
        require(file.exists()) { "Whitelist not found: ${file.absolutePath}" }
        val lines = file.readLines(Charsets.UTF_8)
        return lines.drop(1).mapNotNull { line ->
            val cols = line.split(';')
            if (cols.size < 3 || line.isBlank()) return@mapNotNull null
            Whitelist(
                category = cols[0].trim(),
                nameDe = cols[1].trim(),
                searchEn = cols.getOrNull(2)?.trim().orEmpty(),
                searchAltEn = cols.getOrNull(3)?.trim().orEmpty(),
            )
        }
    }

    private fun readPool(file: File): List<UsdaRow> {
        require(file.exists()) { "Pool not found: ${file.absolutePath}" }
        val rows = mutableListOf<UsdaRow>()
        val lines = file.readLines(Charsets.UTF_8)
        for ((idx, line) in lines.withIndex()) {
            if (idx == 0) continue
            val cols = parseCsvLine(line)
            if (cols.size < 14) continue
            val nameEn = cols[2]
            val micros = cols[13]
            val microCount = if (micros.isBlank() || micros == "\"\"" || micros == "{}") 0
                else micros.count { it == ':' }
            rows.add(
                UsdaRow(
                    fdcId = cols[0],
                    nameDe = cols[1],
                    nameEn = nameEn,
                    brand = cols[3],
                    ingredientsEn = cols[4],
                    kcal = cols[5],
                    protein = cols[6],
                    carbs = cols[7],
                    sugar = cols[8],
                    fat = cols[9],
                    satfat = cols[10],
                    fiber = cols[11],
                    salt = cols[12],
                    micronutrientsJson = micros,
                    nameEnTokens = tokenize(nameEn),
                    microCount = microCount,
                )
            )
        }
        return rows
    }

    /** CSV-escape RFC-4180-style: wrap in `"` and double inner `"` if field needs quoting. */
    private fun csvEscape(s: String): String {
        if (s.isEmpty()) return s
        val needsQuote = s.contains('"') || s.contains(';') || s.contains('\n') || s.contains('\r')
        if (!needsQuote) return s
        return "\"" + s.replace("\"", "\"\"") + "\""
    }

    /** CSV parser supporting double-quoted fields containing `;` and `""`. */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
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

    private fun tokenize(s: String): Set<String> {
        val norm = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return norm.split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() && it.length > 1 && it !in STOP_WORDS }
            .toSet()
    }

    private fun bestMatch(query: String, pool: List<UsdaRow>, used: Set<String>): Pair<UsdaRow, Int>? {
        if (query.isBlank()) return null
        val qTokens = tokenize(query)
        if (qTokens.isEmpty()) return null
        val firstToken = query.lowercase().split(Regex("[^a-z0-9]+"))
            .firstOrNull { it.length > 1 && it !in STOP_WORDS } ?: return null

        var best: UsdaRow? = null
        var bestScore = Int.MIN_VALUE
        for (row in pool) {
            if (row.fdcId in used) continue
            if (row.brand.isNotBlank()) continue
            var score = 0
            val nameLower = row.nameEn.lowercase()
            for (t in qTokens) {
                if (t in row.nameEnTokens) score += 10
                else if (nameLower.contains(t)) score += 8
            }
            if (nameLower.startsWith(firstToken)) score += 15
            if (qTokens.all { it in row.nameEnTokens || nameLower.contains(it) }) score += 5
            score += minOf(row.microCount, 20)
            if ((row.kcal.toDoubleOrNull() ?: 0.0) > 0) score += 3
            val extra = (row.nameEnTokens - qTokens).size
            score -= extra
            // Form-Penalty: descriptor words in name_en that aren't wanted by query
            val queryLower = query.lowercase()
            for ((form, pen) in FORM_PENALTIES) {
                if (form in row.nameEnTokens && !queryLower.contains(form)) {
                    score -= pen
                }
            }
            if (score > bestScore) {
                bestScore = score
                best = row
            }
        }
        return best?.let { it to bestScore }
    }

    private fun writeOutput(file: File, matches: List<Pair<Whitelist, UsdaRow>>) {
        file.parentFile?.mkdirs()
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("fdc_id;name_de;name_en;brand;ingredients_en;kcal;protein;carbs;sugar;fat;satfat;fiber;salt;micronutrients_json")
            w.newLine()
            for ((wl, row) in matches) {
                val nameDe = wl.nameDe.replace(';', ',')
                w.write(listOf(
                    row.fdcId,
                    nameDe,
                    row.nameEn,
                    row.brand,
                    row.ingredientsEn,
                    row.kcal,
                    row.protein,
                    row.carbs,
                    row.sugar,
                    row.fat,
                    row.satfat,
                    row.fiber,
                    row.salt,
                    csvEscape(row.micronutrientsJson),
                ).joinToString(";"))
                w.newLine()
            }
        }
    }

    private fun writeReport(
        file: File,
        whitelist: List<Whitelist>,
        matches: List<Pair<Whitelist, UsdaRow>>,
        misses: List<Whitelist>,
        scores: List<Int>,
    ) {
        file.parentFile?.mkdirs()
        val byCategory = whitelist.groupBy { it.category }
        val matchByCategory = matches.groupBy { it.first.category }
        val sb = StringBuilder()
        sb.appendLine("# Curation Report — REQ-DATA-CURATION-002 (Whitelist-driven)\n")
        sb.appendLine("**Eingabe:** ${whitelist.size} kuratierte deutsche Essentials (`essentials_de.csv`).")
        sb.appendLine("**Pool:** USDA-FDC Voll-Seed (~8.354 Zeilen).")
        sb.appendLine("**Output:** ${matches.size} Matches geschrieben in `usda_fdc_curated.csv`.\n")

        sb.appendLine("## Match-Statistik")
        sb.appendLine("- Total Whitelist: **${whitelist.size}**")
        sb.appendLine("- Gematcht:        **${matches.size}** (${matches.size * 100 / whitelist.size}%)")
        sb.appendLine("- Misses:          **${misses.size}**")
        if (scores.isNotEmpty()) {
            sb.appendLine("- Score: min=${scores.min()}, max=${scores.max()}, avg=${scores.average().toInt()}")
        }
        sb.appendLine()

        sb.appendLine("## Pro Kategorie")
        sb.appendLine("| Kategorie | Whitelist | Matched | Miss% |")
        sb.appendLine("|---|---:|---:|---:|")
        for (cat in byCategory.keys.sorted()) {
            val total = byCategory[cat]?.size ?: 0
            val matched = matchByCategory[cat]?.size ?: 0
            val missPct = if (total > 0) (total - matched) * 100 / total else 0
            sb.appendLine("| $cat | $total | $matched | $missPct% |")
        }
        sb.appendLine()

        if (misses.isNotEmpty()) {
            sb.appendLine("## Misses (Whitelist-Einträge ohne USDA-Match)")
            for (m in misses) {
                sb.appendLine("- **${m.category}** / `${m.nameDe}` — search: `${m.searchEn}` / alt: `${m.searchAltEn}`")
            }
            sb.appendLine()
        }

        sb.appendLine("## Match-Sample (erste 30)")
        for ((wl, row) in matches.take(30)) {
            sb.appendLine("- `${wl.nameDe}` ← fdc=${row.fdcId} `${row.nameEn}`")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
    }
}
