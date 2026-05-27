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
 * P7.S2 Slice 2 / REQ-DATA-SOURCE-001 — FDC-Detail-Fetch + Seed-CSV-Build.
 *
 * Liest die kuratierten Top-IDs aus `src/main/resources/seed/fdc_top_ids.csv`
 * (P7.S2 Slice 1, [FetchFdcTopIds]), holt für jede ID die vollen Nährwerte
 * via `POST /v1/foods?api_key=…` (Batches à 20 IDs, USDA-Hardlimit) und
 * schreibt das finale Seed-CSV mit 14 Spalten:
 *
 *   fdc_id;name_de;name_en;brand;ingredients_en;kcal;protein;carbs;sugar;
 *   fat;satfat;fiber;salt;micronutrients_json
 *
 * `name_de` wird leer geschrieben — P7.S2 Slice 3 (`translate_fdc_names.main.kts`)
 * füllt das via DeepL-API nach.
 *
 * Eingabe (ENV):
 *   FDC_API_KEY — USDA-FDC-Key (Free-Tier 1000 req/h).
 *
 * CLI-Args:
 *   --in PATH       = Top-IDs-CSV (Default `src/main/resources/seed/fdc_top_ids.csv`)
 *   --out PATH      = Output-CSV (Default `src/main/resources/seed/usda_fdc.csv`)
 *   --limit N       = nur die ersten N IDs verarbeiten (Dev-Smoke-Test)
 *   --no-resume     = vorhandenes Output-CSV ignorieren (Default: resume → schon
 *                     im Output enthaltene fdc_ids werden übersprungen)
 *   --rate-ms MS    = Sleep zwischen API-Calls (Default 1100 ≈ ~3270/h, sicher
 *                     unter 1000/h-Limit wenn nicht parallel gefeuert)
 *
 * FDC-Nutrient-ID → CatalogKey Mapping (siehe `NUTRIENT_MAPPING`):
 *  - Quelle: USDA FoodData Central Nutrient-Liste (https://fdc.nal.usda.gov/api-guide.html).
 *  - Catalog-Keys gemäß [de.healthforge.domain.nutrition.NutrientCatalog] (33 Keys, P7.S1).
 *  - `salt` wird aus Sodium (1093) errechnet: `salt_g = sodium_mg × 2.5 / 1000`.
 *  - Folate: nutrient.id `1177` (Folate, total) wird auf `vitamin_b9` gemappt.
 *    Wenn nur DFE (`1190`) vorhanden, fallback auf DFE.
 *
 * Verhalten:
 *  - Batches à 20 IDs → 1 POST → 20 Foods. ~425 Batches für 8487 IDs.
 *  - Bei HTTP 429: 60s Wait + Retry (max 3x).
 *  - Resume: liest existierendes Output, sammelt schon vorhandene fdc_ids,
 *    skipt diese IDs in der Eingabe. Append-Mode für Neue.
 *  - Idempotenz: vorhandene Zeilen werden NICHT überschrieben (siehe `--no-resume`).
 *
 * Sicherheit:
 *  - Key NIE in CLI-Args, immer ENV.
 *  - CSV enthält keine Secrets.
 */
object BuildUsdaSeed {

    private const val BATCH_SIZE = 20 // USDA hard-limit on /v1/foods endpoint
    private const val FOODS_URL = "https://api.nal.usda.gov/fdc/v1/foods"
    private const val DEFAULT_IN = "src/main/resources/seed/fdc_top_ids.csv"
    private const val DEFAULT_OUT = "src/main/resources/seed/usda_fdc.csv"

    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    // ─── FDC nutrient.id → NutrientCatalog.key ───────────────────────────────
    // Macros sind separate Spalten; Mikros gehen in micronutrients_json.
    // Quelle: USDA FDC Nutrient List + NutrientCatalog (33 Keys).
    //
    // Energy: Foundation-Foods nutzen 2047 (Atwater General) und/oder 2048
    // (Atwater Specific). SR-Legacy + Branded nutzen 1008. Wir mappen alle drei
    // auf "kcal" mit Präzedenz 1008 > 2048 > 2047 (siehe mapFood: erste Zuweisung
    // bleibt nicht erhalten, letzte gewinnt → daher prüfen wir in mapFood die
    // Reihenfolge nicht, sondern bevorzugen vorhandene Werte unten).
    private val MACRO_MAP: Map<Int, String> = mapOf(
        1008 to "kcal",      // Energy (kcal) — SR-Legacy/Branded
        2047 to "kcal",      // Energy (Atwater General Factors) — Foundation
        2048 to "kcal",      // Energy (Atwater Specific Factors) — Foundation
        1003 to "protein",   // Protein (g)
        1005 to "carbs",     // Carbohydrate, by difference (g)
        2000 to "sugar",     // Sugars, total including NLEA (g)
        1004 to "fat",       // Total lipid (fat) (g)
        1258 to "satfat",    // Fatty acids, total saturated (g)
        1079 to "fiber",     // Fiber, total dietary (g)
        // salt wird aus Sodium berechnet (siehe SODIUM_ID)
    )
    private const val SODIUM_ID = 1093 // Sodium, Na (mg) → salt_g = mg × 2.5 / 1000

    private val MICRO_MAP: Map<Int, String> = mapOf(
        // Vitamine
        1106 to "vitamin_a",   // Vitamin A, RAE (µg)
        1114 to "vitamin_d",   // Vitamin D (D2 + D3) (µg)
        1109 to "vitamin_e",   // Vitamin E (alpha-tocopherol) (mg)
        1185 to "vitamin_k",   // Vitamin K (phylloquinone) (µg)
        1165 to "vitamin_b1",  // Thiamin (mg)
        1166 to "vitamin_b2",  // Riboflavin (mg)
        1167 to "vitamin_b3",  // Niacin (mg)
        1170 to "vitamin_b5",  // Pantothenic acid (mg)
        1175 to "vitamin_b6",  // Vitamin B6 (mg)
        1176 to "vitamin_b7",  // Biotin (µg)
        1177 to "vitamin_b9",  // Folate, total (µg) — preferred over DFE
        1190 to "vitamin_b9",  // Folate, DFE (µg) — fallback if 1177 missing
        1178 to "vitamin_b12", // Vitamin B12 (µg)
        1162 to "vitamin_c",   // Vitamin C, total ascorbic acid (mg)
        // Mineralstoffe
        1087 to "calcium",     // Calcium, Ca (mg)
        1089 to "eisen",       // Iron, Fe (mg)
        1090 to "magnesium",   // Magnesium, Mg (mg)
        1095 to "zink",        // Zinc, Zn (mg)
        1098 to "kupfer",      // Copper, Cu (mg)
        1101 to "mangan",      // Manganese, Mn (mg)
        1103 to "selen",       // Selenium, Se (µg)
        1100 to "jod",         // Iodine, I (µg)
        1092 to "kalium",      // Potassium, K (mg)
        1093 to "natrium",     // Sodium, Na (mg) — auch Quelle für salt
        1091 to "phosphor",    // Phosphorus, P (mg)
    )

    private data class TopRow(val fdcId: Long, val dataType: String, val nameEn: String, val brand: String)
    private data class SeedRow(
        val fdcId: Long,
        val nameEn: String,
        val brand: String,
        val ingredientsEn: String,
        val kcal: Double,
        val protein: Double,
        val carbs: Double,
        val sugar: Double,
        val fat: Double,
        val satfat: Double,
        val fiber: Double,
        val salt: Double,
        val micros: Map<String, Double>,
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val apiKey = System.getenv("FDC_API_KEY")
            ?: error(
                "FDC_API_KEY env-var nicht gesetzt. Hole Key unter " +
                    "https://fdc.nal.usda.gov/api-key-signup.html und trage in server/.env ein."
            )

        var inPath = DEFAULT_IN
        var outPath = DEFAULT_OUT
        var limit = Int.MAX_VALUE
        var resume = true
        var rateMs = 1100L

        val it = args.iterator()
        while (it.hasNext()) {
            when (val a = it.next()) {
                "--in"          -> inPath = it.next()
                "--out"         -> outPath = it.next()
                "--limit"       -> limit = it.next().toInt()
                "--no-resume"   -> resume = false
                "--rate-ms"     -> rateMs = it.next().toLong()
                else            -> error("Unbekanntes Argument: $a")
            }
        }

        val inFile = File(inPath)
        require(inFile.exists()) { "Input-CSV nicht gefunden: ${inFile.absolutePath} — laufe erst `:fetchFdcTopIds`." }
        val outFile = File(outPath).apply { parentFile?.mkdirs() }

        println("[seed] BuildUsdaSeed starting…")
        println("[seed]   Input : ${inFile.absolutePath}")
        println("[seed]   Output: ${outFile.absolutePath}")
        println("[seed]   Resume: $resume   Limit: ${if (limit == Int.MAX_VALUE) "all" else limit}   Rate: ${rateMs}ms/req")

        val topRows = readTopIds(inFile).take(limit)
        println("[seed] Top-IDs gesamt (nach --limit): ${topRows.size}")

        val alreadyDone: Set<Long> = if (resume && outFile.exists()) readExistingIds(outFile) else emptySet()
        println("[seed] Schon im Output: ${alreadyDone.size} fdc_ids (werden übersprungen)")

        val pending = topRows.filter { it.fdcId !in alreadyDone }
        println("[seed] Zu verarbeiten: ${pending.size} fdc_ids in ${(pending.size + BATCH_SIZE - 1) / BATCH_SIZE} Batches\n")

        val writeHeader = !outFile.exists() || outFile.length() == 0L || !resume
        if (writeHeader) {
            outFile.writeText(
                "fdc_id;name_de;name_en;brand;ingredients_en;kcal;protein;carbs;sugar;fat;satfat;fiber;salt;micronutrients_json\n",
                Charsets.UTF_8
            )
        }

        var written = 0
        var skipped = 0
        val totalBatches = (pending.size + BATCH_SIZE - 1) / BATCH_SIZE
        var batchIdx = 0

        pending.chunked(BATCH_SIZE).forEach { batch ->
            batchIdx++
            val foods = fetchBatch(apiKey, batch.map { it.fdcId })
            val byId = foods.associateBy { it.path("fdcId").asLong() }

            val rowsOut = mutableListOf<SeedRow>()
            batch.forEach { top ->
                val food = byId[top.fdcId]
                if (food == null) {
                    skipped++
                    return@forEach
                }
                val row = mapFood(top, food)
                if (row == null) {
                    skipped++
                } else {
                    rowsOut += row
                }
            }

            outFile.appendText(rowsOut.joinToString(separator = "") { rowToCsv(it) + "\n" }, Charsets.UTF_8)
            written += rowsOut.size
            println(
                "[seed]   batch $batchIdx/$totalBatches → wrote ${rowsOut.size}, " +
                    "skipped ${batch.size - rowsOut.size} | total written: $written"
            )

            if (batchIdx < totalBatches) Thread.sleep(rateMs)
        }

        println("\n[seed] ✅ Done. Written: $written rows. Skipped (no kcal / no detail): $skipped.")
        println("[seed] Final file size: ${outFile.length() / 1024} KB")
    }

    // ─── CSV I/O ─────────────────────────────────────────────────────────────

    private fun readTopIds(file: File): List<TopRow> {
        val lines = file.readLines(Charsets.UTF_8)
        if (lines.isEmpty()) return emptyList()
        return lines.drop(1).mapNotNull { raw ->
            if (raw.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(raw)
            if (cols.size < 4) return@mapNotNull null
            val id = cols[0].toLongOrNull() ?: return@mapNotNull null
            TopRow(id, cols[1], cols[2], cols[3])
        }
    }

    private fun readExistingIds(file: File): Set<Long> {
        val lines = file.readLines(Charsets.UTF_8)
        if (lines.isEmpty()) return emptySet()
        return lines.drop(1).mapNotNull { raw ->
            if (raw.isBlank()) return@mapNotNull null
            val cols = parseCsvLine(raw)
            cols.getOrNull(0)?.toLongOrNull()
        }.toSet()
    }

    /** Minimaler CSV-Parser für `;` mit `"..."`-Quoting + Escape `""`. */
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

    private fun rowToCsv(r: SeedRow): String = buildString {
        append(r.fdcId); append(';')
        append("");      append(';') // name_de — wird von DeepL-Slice gefüllt
        append(csvEscape(r.nameEn)); append(';')
        append(csvEscape(r.brand)); append(';')
        append(csvEscape(r.ingredientsEn)); append(';')
        append(fmt(r.kcal)); append(';')
        append(fmt(r.protein)); append(';')
        append(fmt(r.carbs)); append(';')
        append(fmt(r.sugar)); append(';')
        append(fmt(r.fat)); append(';')
        append(fmt(r.satfat)); append(';')
        append(fmt(r.fiber)); append(';')
        append(fmt(r.salt)); append(';')
        append(csvEscape(mapper.writeValueAsString(r.micros)))
    }

    private fun fmt(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else "%.3f".format(java.util.Locale.US, d).trimEnd('0').trimEnd('.')

    // ─── FDC mapping ─────────────────────────────────────────────────────────

    private fun mapFood(top: TopRow, food: JsonNode): SeedRow? {
        val macros = mutableMapOf<String, Double>()
        val micros = mutableMapOf<String, Double>()
        var sodiumMg: Double? = null

        food.path("foodNutrients").elements().forEach { fn ->
            // Detail-Format (`/v1/foods`) verwendet `nutrient.id` + `amount`.
            // Branded-Foods nutzen identische Struktur, Werte auch per 100g.
            val nutrientId = fn.path("nutrient").path("id").asInt(-1)
            val amount = fn.path("amount").asDouble(Double.NaN)
            if (nutrientId < 0 || amount.isNaN()) return@forEach

            MACRO_MAP[nutrientId]?.let { macros[it] = amount }
            if (nutrientId == SODIUM_ID) sodiumMg = amount

            MICRO_MAP[nutrientId]?.let { key ->
                // Folate: 1177 wins über 1190 (DFE), wenn beide da sind
                if (key == "vitamin_b9" && nutrientId == 1190 && micros.containsKey("vitamin_b9")) {
                    return@let
                }
                micros[key] = amount
            }
        }

        val kcal = macros["kcal"] ?: return null // ohne Energie kein sinnvoller Eintrag

        val salt = sodiumMg?.let { it * 2.5 / 1000.0 } ?: 0.0

        // Branded-Foods haben `ingredients` (label-Text). Foundation/SR-Legacy haben
        // teilweise `inputFoods[]` oder `description` — wir nehmen nur `ingredients`.
        val ingredientsEn = food.path("ingredients").asText("").trim()
        // `description` aus Detail kann genauer sein als top.nameEn — bevorzugen.
        val nameEn = food.path("description").asText(top.nameEn).trim().ifBlank { top.nameEn }
        val brand = food.path("brandOwner").asText("")
            .ifBlank { food.path("brandName").asText("") }
            .ifBlank { top.brand }
            .trim()

        return SeedRow(
            fdcId = top.fdcId,
            nameEn = nameEn,
            brand = brand,
            ingredientsEn = ingredientsEn,
            kcal = kcal,
            protein = macros["protein"] ?: 0.0,
            carbs = macros["carbs"] ?: 0.0,
            sugar = macros["sugar"] ?: 0.0,
            fat = macros["fat"] ?: 0.0,
            satfat = macros["satfat"] ?: 0.0,
            fiber = macros["fiber"] ?: 0.0,
            salt = salt,
            micros = micros.toSortedMap(),
        )
    }

    // ─── HTTP ────────────────────────────────────────────────────────────────

    private fun fetchBatch(apiKey: String, ids: List<Long>): List<JsonNode> {
        val body = buildString {
            append("{\"fdcIds\":[")
            append(ids.joinToString(","))
            append("]}")
        }

        val req = HttpRequest.newBuilder()
            .uri(URI("$FOODS_URL?api_key=$apiKey"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build()

        var attempt = 0
        while (true) {
            val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
            when (resp.statusCode()) {
                200 -> {
                    val json = mapper.readTree(resp.body())
                    return if (json.isArray) json.elements().asSequence().toList() else emptyList()
                }
                429 -> {
                    attempt++
                    if (attempt > 3) error("HTTP 429 nach 3 Retries — Rate-Limit. Resume später via --resume (Default).")
                    println("[seed]   HTTP 429 — warte 60s (attempt $attempt/3)…")
                    Thread.sleep(60_000)
                }
                else -> error(
                    "HTTP ${resp.statusCode()} für Batch ids=${ids.take(3)}…: ${resp.body().take(200)}"
                )
            }
        }
    }
}
