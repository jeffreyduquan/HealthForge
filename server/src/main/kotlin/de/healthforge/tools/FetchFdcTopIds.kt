package de.healthforge.tools

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * P7.S2 / REQ-DATA-SOURCE-001 — Holt die kuratierte Top-FDC-ID-Liste
 * (Foundation + SR-Legacy + Top-Branded) aus der USDA FoodData Central API
 * und schreibt sie als CSV-Asset, das vom nachgelagerten Build-Seed-Skript
 * verarbeitet wird.
 *
 * Eingabe (ENV):
 *   FDC_API_KEY   = USDA-FDC-API-Key (https://fdc.nal.usda.gov/api-key-signup.html)
 *
 * Optionale CLI-Argumente:
 *   --branded-top N   = Anzahl Branded-Foods (Default 300)
 *   --out PATH        = Ausgabe-Pfad (Default server/src/main/resources/seed/fdc_top_ids.csv)
 *
 * Ausgabe (CSV, Semikolon-getrennt, Header in Zeile 1):
 *   fdc_id;data_type;name_en;brand
 *
 * Aufruf:
 *
 *   # PowerShell (lädt FDC_API_KEY aus server/.env):
 *   $env:FDC_API_KEY = (Get-Content server/.env | Select-String '^FDC_API_KEY=' | ForEach-Object { ($_ -split '=', 2)[1] })
 *   ./gradlew :server:fetchFdcTopIds
 *
 *   # mit Argumenten:
 *   ./gradlew :server:fetchFdcTopIds --args="--branded-top 500"
 *
 * Verhalten:
 *  - Foundation: pageSize=200, alle Pages (~200 IDs gesamt).
 *  - SR-Legacy:  pageSize=200, alle Pages (~7800 IDs gesamt).
 *  - Branded:    pageSize=200, sortiert nach publishedDate desc,
 *                gefiltert auf marketCountry=United States, Cap = --branded-top.
 *  - Rate-Limit: 1000 req/h pro Key → defensiv 1 req/s Sleep zwischen Pages.
 *  - Bei HTTP 429: 60s Wait + Retry (max 3x), dann Abort + Resume-Hinweis.
 *  - Idempotenz: Skript überschreibt das CSV-Asset komplett (kein Append).
 *
 * Begründung "Build-Time-Skript, kein Server-Runtime":
 *  - Top-Listen ändern sich selten (USDA-Releases ~quartalsweise) → einmaliger
 *    Lauf reicht. Kein Prod-Bedarf für FDC-API-Zugriff zur Laufzeit
 *    (Architecture §4.5b). Klasse liegt unter `tools/` ohne `@Component` /
 *    `@SpringBootApplication`, wird nicht vom Boot-Classpath gescannt für
 *    Runtime, nur per `JavaExec`-Task ausgeführt.
 *
 * SICHERHEIT:
 *  - Key NIE in CLI-Args, immer ENV-Var. CSV enthält keine Secrets.
 */
object FetchFdcTopIds {

    private const val PAGE_SIZE = 200
    private const val SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search"
    private const val DEFAULT_OUT = "src/main/resources/seed/fdc_top_ids.csv"

    private val mapper = ObjectMapper().registerKotlinModule()
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    @JvmStatic
    fun main(args: Array<String>) {
        val apiKey = System.getenv("FDC_API_KEY")
            ?: error(
                "FDC_API_KEY env-var nicht gesetzt. " +
                    "Hole Key unter https://fdc.nal.usda.gov/api-key-signup.html " +
                    "und trage in server/.env ein."
            )

        var brandedTop = 300
        var outPath = DEFAULT_OUT
        val it = args.iterator()
        while (it.hasNext()) {
            when (val a = it.next()) {
                "--branded-top" -> brandedTop = it.next().toInt()
                "--out"         -> outPath = it.next()
                else            -> error("Unbekanntes Argument: $a")
            }
        }

        val outputFile = File(outPath).apply { parentFile?.mkdirs() }
        println("[fetch] FDC top-ids fetch starting…")
        println("[fetch] Output: ${outputFile.absolutePath}")
        println("[fetch] Branded top-N: $brandedTop")

        println("[fetch] Foundation Foods…")
        val foundation = fetchAll(apiKey, "Foundation")
        println("[fetch] Foundation: ${foundation.size} items\n")

        println("[fetch] SR Legacy…")
        val srLegacy = fetchAll(apiKey, "SR Legacy")
        println("[fetch] SR Legacy: ${srLegacy.size} items\n")

        println("[fetch] Branded (first $brandedTop, default sort)…")
        val branded = runCatching { fetchAll(apiKey, "Branded", cap = brandedTop) }
            .onFailure { println("[fetch] Branded fetch failed (${it.message}) — continuing without Branded") }
            .getOrDefault(emptyList())
        println("[fetch] Branded: ${branded.size} items\n")

        val seen = mutableSetOf<Long>()
        data class Row(val fdcId: Long, val dataType: String, val nameEn: String, val brand: String)
        val rows = mutableListOf<Row>()

        fun addAll(list: List<JsonNode>, type: String) {
            list.forEach { f ->
                val id = f.path("fdcId").asLong()
                if (id == 0L || !seen.add(id)) return@forEach
                val nameEn = f.path("description").asText("").trim()
                val brand = f.path("brandOwner").asText("")
                    .ifBlank { f.path("brandName").asText("") }
                    .trim()
                rows += Row(id, type, nameEn, brand)
            }
        }

        addAll(foundation, "Foundation")
        addAll(srLegacy, "SR Legacy")
        addAll(branded, "Branded")

        println("[fetch] Total unique: ${rows.size}")

        outputFile.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("fdc_id;data_type;name_en;brand")
            w.newLine()
            rows.forEach { r ->
                w.write("${r.fdcId};${csvEscape(r.dataType)};${csvEscape(r.nameEn)};${csvEscape(r.brand)}")
                w.newLine()
            }
        }

        println("[fetch] ✅ Wrote ${rows.size} rows to ${outputFile.absolutePath}")
        println("[fetch] File size: ${outputFile.length() / 1024} KB")
    }

    private data class SearchResult(val totalPages: Int, val foods: List<JsonNode>)

    private fun searchPage(
        apiKey: String,
        dataType: String,
        pageNumber: Int,
        sortBy: String? = null,
    ): SearchResult {
        val body = buildString {
            append("{")
            append("\"dataType\":[\"$dataType\"],")
            append("\"pageSize\":$PAGE_SIZE,")
            append("\"pageNumber\":$pageNumber")
            if (sortBy != null) {
                append(",\"sortBy\":\"$sortBy\"")
                append(",\"sortOrder\":\"desc\"")
            }
            append("}")
        }

        val req = HttpRequest.newBuilder()
            .uri(URI("$SEARCH_URL?api_key=$apiKey"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build()

        var attempt = 0
        while (true) {
            val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
            when (resp.statusCode()) {
                200 -> {
                    val json = mapper.readTree(resp.body())
                    val totalPages = json.path("totalPages").asInt(0)
                    val foods = json.path("foods").elements().asSequence().toList()
                    return SearchResult(totalPages, foods)
                }
                429 -> {
                    attempt++
                    if (attempt > 3) error("HTTP 429 nach 3 Retries — Limit erreicht. Resume später.")
                    println("[fetch]   HTTP 429 — warte 60s (attempt $attempt/3)…")
                    Thread.sleep(60_000)
                }
                else -> error(
                    "HTTP ${resp.statusCode()} für $dataType page=$pageNumber: ${resp.body().take(200)}"
                )
            }
        }
    }

    private fun fetchAll(
        apiKey: String,
        dataType: String,
        sortBy: String? = null,
        cap: Int = Int.MAX_VALUE,
    ): List<JsonNode> {
        val all = mutableListOf<JsonNode>()
        var page = 1
        while (all.size < cap) {
            val result = searchPage(apiKey, dataType, page, sortBy)
            all += result.foods
            println(
                "[fetch]   $dataType page $page/${result.totalPages} → " +
                    "${result.foods.size} foods (total so far: ${all.size})"
            )
            if (page >= result.totalPages || result.foods.isEmpty()) break
            page++
            Thread.sleep(1_000) // 1 req/s defensive rate-limit
        }
        return all.take(cap)
    }

    private fun csvEscape(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        val needsQuote = s.contains(';') || s.contains('"') || s.contains('\n')
        return if (needsQuote) "\"${s.replace("\"", "\"\"")}\"" else s
    }
}
