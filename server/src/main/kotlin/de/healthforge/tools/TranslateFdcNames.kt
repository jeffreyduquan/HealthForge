package de.healthforge.tools

import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration

/**
 * P7.S2 Slice 3b / REQ-DATA-TRANSLATE-001 — Übersetzt die englischen FDC-Namen
 * (`name_en`) ins Deutsche (`name_de`) via **DeepL Free API**.
 *
 * Ersetzt das frühere Standalone-Script `server/tools/translate_fdc_names.main.kts`
 * (jetzt obsolet) durch ein reguläres Kotlin-Tool analog [BuildUsdaSeed]:
 * Standalone-JVM-Klasse (kein `@Component`), aufrufbar via Gradle-Task
 * `:translateFdcNames`.
 *
 * Eingabe (ENV via `server/.env` → `loadDotEnv()` im build.gradle.kts):
 *   DEEPL_API_KEY — DeepL-Auth-Key. Free-Tier-Keys enden auf `:fx`, Pro nicht.
 *                   Free: 500.000 Zeichen/Monat. 8351 × ⌀25 Zeichen ≈ 210k → passt.
 *
 * CLI-Args:
 *   --in PATH       = Eingabe-CSV (Default `src/main/resources/seed/usda_fdc.csv`)
 *   --out PATH      = Ausgabe-CSV (Default = `--in`, also in-place mit atomic-rename)
 *   --limit N       = nur die ersten N pending Rows übersetzen (Dry-Smoke-Test)
 *   --no-resume     = vorhandene `name_de`-Werte überschreiben (Default: skip)
 *   --batch N       = DeepL-Batch-Größe (Default 50; DeepL: max 50 `text=`-Params/Req)
 *   --rate-ms MS    = Sleep zwischen API-Calls (Default 1100 ≈ <1 req/s, defensiv)
 *   --dry-run       = KEIN API-Call; zeigt Anzahl pending Rows + erste 10 Samples
 *
 * Verhalten:
 *  - Liest komplettes CSV in Memory (8354 Rows × ~200 B = ~2 MB → safe).
 *  - Findet Rows mit leerem `name_de` AND nicht-leerem `name_en` → pending.
 *  - Batches à 50 Texte → 1 POST `https://api-free.deepl.com/v2/translate`.
 *  - Bei HTTP 429/503: Exponential-Backoff bis 60s, max 6 Retries.
 *  - HTTP 456 = Quota erschöpft → Abbruch mit klarer Meldung.
 *  - **Persistenz nach JEDEM Batch**: write to `.tmp` + atomic-rename
 *    → Interrupts verlieren max. 50 Übersetzungen, kein CSV-Korruptions-Risiko.
 *
 * Sicherheit:
 *  - Key NIE in CLI-Args, immer ENV.
 *  - URL-Encoding für alle DeepL-Body-Parts (`URLEncoder.encode`, UTF-8).
 *  - CSV-Quoting wiederverwendet ([csvEscape]) — kein Re-Implement.
 */
object TranslateFdcNames {

    private const val DEFAULT_PATH = "src/main/resources/seed/usda_fdc.csv"
    private const val HEADER =
        "fdc_id;name_de;name_en;brand;ingredients_en;kcal;protein;carbs;sugar;" +
            "fat;satfat;fiber;salt;micronutrients_json"

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    @JvmStatic
    fun main(args: Array<String>) {
        val inPath = argValue(args, "--in") ?: DEFAULT_PATH
        val outPath = argValue(args, "--out") ?: inPath
        val limit = argValue(args, "--limit")?.toIntOrNull() ?: Int.MAX_VALUE
        val batchSize = argValue(args, "--batch")?.toIntOrNull()?.coerceIn(1, 50) ?: 50
        val rateMs = argValue(args, "--rate-ms")?.toLongOrNull() ?: 1100L
        val resume = "--no-resume" !in args
        val dryRun = "--dry-run" in args

        val inFile = File(inPath)
        require(inFile.exists()) { "Eingabe-CSV fehlt: ${inFile.absolutePath}" }
        val outFile = File(outPath)

        println("[translate] In : ${inFile.absolutePath}")
        println("[translate] Out: ${outFile.absolutePath}")
        println("[translate] Resume: $resume | DryRun: $dryRun | Batch: $batchSize | Rate: ${rateMs}ms")

        // ── 1. Read full CSV into memory ─────────────────────────────────────
        val allLines = inFile.readLines(Charsets.UTF_8)
        require(allLines.isNotEmpty()) { "Eingabe-CSV ist leer." }
        val headerLine = allLines.first()
        require(headerLine.startsWith("fdc_id;name_de;name_en;")) {
            "Unerwarteter Header. Erwartet beginnt mit 'fdc_id;name_de;name_en;'. Got: ${headerLine.take(120)}"
        }
        val rows: MutableList<List<String>> = allLines.drop(1)
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }
            .toMutableList()
        println("[translate] Total rows: ${rows.size}")

        // ── 2. Identify pending rows ─────────────────────────────────────────
        data class Pending(val rowIdx: Int, val nameEn: String)
        val pending = mutableListOf<Pending>()
        rows.forEachIndexed { i, cols ->
            val nameDe = cols.getOrNull(1).orEmpty().trim()
            val nameEn = cols.getOrNull(2).orEmpty().trim()
            val skipExisting = resume && nameDe.isNotBlank()
            if (!skipExisting && nameEn.isNotBlank()) pending += Pending(i, nameEn)
        }
        val limited = pending.take(limit)
        println("[translate] Pending (need DeepL): ${pending.size}; nach --limit: ${limited.size}")
        val totalChars = limited.sumOf { it.nameEn.length }
        println("[translate] Estimated DeepL chars: $totalChars (Free-Tier 500k/Monat)")

        if (limited.isEmpty()) {
            println("[translate] Nichts zu tun. Done.")
            return
        }

        if (dryRun) {
            println("[translate] --dry-run aktiv. Samples (erste 10):")
            limited.take(10).forEach { p ->
                println("  rowIdx=${p.rowIdx}  '${p.nameEn.take(80)}'")
            }
            return
        }

        // ── 3. Validate ENV ──────────────────────────────────────────────────
        val apiKey = System.getenv("DEEPL_API_KEY")?.trim()
            ?: error("DEEPL_API_KEY env-var nicht gesetzt. Free-Account: https://www.deepl.com/pro-api (Key endet auf ':fx').")
        require(apiKey.isNotBlank()) { "DEEPL_API_KEY ist leer." }
        val isFree = apiKey.endsWith(":fx")
        val endpoint = URI(if (isFree) "https://api-free.deepl.com/v2/translate" else "https://api.deepl.com/v2/translate")
        println("[translate] DeepL endpoint: $endpoint (tier=${if (isFree) "FREE" else "PRO"})")

        // ── 4. Batch translate + persist after each batch ────────────────────
        val totalBatches = (limited.size + batchSize - 1) / batchSize
        var batchIdx = 0
        var translated = 0

        limited.chunked(batchSize).forEach { batch ->
            batchIdx++
            val translations = translateBatch(apiKey, endpoint, batch.map { it.nameEn })
            require(translations.size == batch.size) {
                "DeepL response size mismatch: ${translations.size} vs ${batch.size}"
            }
            batch.forEachIndexed { j, p ->
                val cols = rows[p.rowIdx].toMutableList()
                while (cols.size < 14) cols += ""
                cols[1] = translations[j]
                rows[p.rowIdx] = cols
            }
            translated += batch.size

            // Persist atomically after each batch (interrupt-safe).
            writeCsvAtomic(outFile, headerLine, rows)
            println("[translate]   batch $batchIdx/$totalBatches → ${batch.size} texts | total translated: $translated")

            if (batchIdx < totalBatches) Thread.sleep(rateMs)
        }

        println("\n[translate] ✅ Done. Translated: $translated rows. File: ${outFile.absolutePath} (${outFile.length() / 1024} KB)")
    }

    // ─── DeepL ───────────────────────────────────────────────────────────────

    private fun translateBatch(apiKey: String, endpoint: URI, texts: List<String>): List<String> {
        if (texts.isEmpty()) return emptyList()
        val body = buildString {
            append("source_lang=EN&target_lang=DE")
            // formality=more wäre PRO-only; im Free-Tier nicht verfügbar → weglassen.
            texts.forEach { t ->
                append("&text=").append(URLEncoder.encode(t, StandardCharsets.UTF_8))
            }
        }
        var attempt = 0
        while (true) {
            val req = HttpRequest.newBuilder(endpoint)
                .header("Authorization", "DeepL-Auth-Key $apiKey")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build()
            val res = try {
                http.send(req, HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                attempt++
                if (attempt > 6) throw IllegalStateException("DeepL Netzwerk-Fehler nach 6 Retries: ${e.message}", e)
                val waitMs = minOf(60_000L, 1000L * (1L shl attempt.coerceAtMost(6)))
                System.err.println("[translate]   network error (${e.javaClass.simpleName}), retry in ${waitMs}ms…")
                Thread.sleep(waitMs)
                continue
            }
            when (res.statusCode()) {
                200 -> return parseTranslations(res.body())
                429, 503 -> {
                    attempt++
                    if (attempt > 6) error("DeepL HTTP ${res.statusCode()} nach 6 Retries — Quota oder Overload?")
                    val waitMs = minOf(60_000L, 1000L * (1L shl attempt.coerceAtMost(6)))
                    System.err.println("[translate]   rate-limit HTTP ${res.statusCode()}, warte ${waitMs}ms (attempt $attempt)…")
                    Thread.sleep(waitMs)
                }
                456 -> error("DeepL Quota erschöpft (HTTP 456). Free-Tier: 500k chars/Monat. Bisheriger Fortschritt ist persistiert.")
                403 -> error("DeepL HTTP 403 — ungültiger Key oder falscher Tier? (Free-Key muss auf ':fx' enden.) Body: ${res.body().take(300)}")
                else -> error("DeepL HTTP ${res.statusCode()}: ${res.body().take(300)}")
            }
        }
    }

    /**
     * Minimaler JSON-Extractor für `{"translations":[{"text":"…"},…]}`.
     * Reicht aus für DeepL-Response (keine Nested-Strukturen relevant).
     */
    private fun parseTranslations(json: String): List<String> {
        val out = mutableListOf<String>()
        val key = "\"text\":\""
        var i = 0
        while (true) {
            val k = json.indexOf(key, i)
            if (k < 0) break
            var j = k + key.length
            val sb = StringBuilder()
            while (j < json.length) {
                val c = json[j]
                if (c == '\\' && j + 1 < json.length) {
                    val nx = json[j + 1]
                    sb.append(
                        when (nx) {
                            '"' -> '"'
                            '\\' -> '\\'
                            '/' -> '/'
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            'u' -> {
                                val hex = json.substring(j + 2, j + 6)
                                j += 4
                                hex.toInt(16).toChar()
                            }
                            else -> nx
                        }
                    )
                    j += 2
                } else if (c == '"') {
                    break
                } else {
                    sb.append(c); j++
                }
            }
            out += sb.toString()
            i = j + 1
        }
        return out
    }

    // ─── CSV I/O ─────────────────────────────────────────────────────────────

    private fun writeCsvAtomic(outFile: File, headerLine: String, rows: List<List<String>>) {
        // Schreibe in `.tmp` und renames atomar → kein halbgeschriebenes CSV.
        val tmp = File(outFile.parentFile ?: File("."), outFile.name + ".tmp")
        tmp.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write(headerLine); w.write("\n")
            rows.forEach { cols ->
                w.write(cols.joinToString(";") { csvEscape(it) })
                w.write("\n")
            }
        }
        Files.move(tmp.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** Minimaler CSV-Parser für `;` mit `"..."`-Quoting + Escape `""`. Identisch zu [BuildUsdaSeed]. */
    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var inQuote = false
        while (i < line.length) {
            val c = line[i]
            when {
                inQuote && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQuote = !inQuote
                c == ';' && !inQuote -> { out += sb.toString(); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out += sb.toString()
        return out
    }

    private fun csvEscape(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        val needsQuote = s.contains(';') || s.contains('"') || s.contains('\n') || s.contains('\r')
        return if (needsQuote) "\"${s.replace("\"", "\"\"")}\"" else s
    }

    private fun argValue(args: Array<String>, flag: String): String? {
        val idx = args.indexOf(flag)
        return if (idx >= 0 && idx + 1 < args.size) args[idx + 1] else null
    }
}
