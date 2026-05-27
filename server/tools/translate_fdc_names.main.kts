#!/usr/bin/env kotlin
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

/**
 * P7.S2 / REQ-DATA-TRANSLATE-001 — Offline-Batch-Übersetzung der englischen
 * USDA-FDC Lebensmittelnamen ins Deutsche via DeepL Free API.
 *
 * Eingabe (CSV, Semikolon-getrennt, Header in Zeile 1):
 *   fdc_id;name_en;...   (weitere Spalten werden 1:1 durchgereicht)
 *
 * Ausgabe (gleicher Pfad mit `.de.csv`-Suffix):
 *   fdc_id;name_de;name_en;...
 *
 * Aufruf:
 *
 *   $env:DEEPL_API_KEY = "your-free-key:fx"
 *   kotlin tools/translate_fdc_names.main.kts seed_raw.csv
 *
 * Begründung Batch-Strategie:
 *  - DeepL Free: 500.000 Zeichen/Monat. Ein FDC-Name ≈ 30 Zeichen → ~16.000 Namen
 *    pro Monat im Free-Tier ausreichend für unsere 5.000er Test-Slice.
 *  - Pro Request bis zu 50 Texte (DeepL: `text=...&text=...`-Parameter wiederholen).
 *  - Bei HTTP 429 (Rate-Limit) Exponential-Backoff bis 60s, dann Abort + Resume-Hinweis.
 *  - Idempotenz: Skript überspringt Zeilen, deren `name_de` bereits gefüllt ist
 *    (für Wiederaufnahme nach Unterbrechung).
 *
 * Begründung "kein Service auf Server":
 *  - Translate ist Build-Time-Aktivität (einmalig pro USDA-Release-Snapshot),
 *    nicht Runtime. Keine Notwendigkeit DeepL-Key auf Prod-Server zu hinterlegen
 *    → Reduktion Angriffsfläche (Architecture §4.5b).
 */

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

// ── Konfiguration ────────────────────────────────────────────────────────────

val apiKey = System.getenv("DEEPL_API_KEY")
    ?: error("DEEPL_API_KEY env-var nicht gesetzt. (Free-Key endet auf ':fx', Pro nicht.)")

val inputArg = args.firstOrNull()
    ?: error("Usage: kotlin translate_fdc_names.main.kts <input.csv>")

val inputFile = File(inputArg).also {
    require(it.exists()) { "Eingabedatei nicht gefunden: ${it.absolutePath}" }
}
val outputFile = File(inputFile.parentFile, inputFile.nameWithoutExtension + ".de.csv")

val isFreeTier = apiKey.endsWith(":fx")
val apiHost = if (isFreeTier) "api-free.deepl.com" else "api.deepl.com"
val endpoint = URI("https://$apiHost/v2/translate")

val batchSize = 50
val httpClient: HttpClient = HttpClient.newHttpClient()
val mapper = ObjectMapper().registerKotlinModule()

// ── CSV-IO ───────────────────────────────────────────────────────────────────

val lines = inputFile.readLines(Charsets.UTF_8)
require(lines.isNotEmpty()) { "Leere Eingabedatei." }

val header = lines.first().split(";")
val nameEnIdx = header.indexOf("name_en").also {
    require(it >= 0) { "Header muss 'name_en' enthalten. Gefunden: $header" }
}
val nameDeIdxExisting = header.indexOf("name_de")  // -1 falls noch nicht da

println("[translate] Input: ${inputFile.name} (${lines.size - 1} rows)")
println("[translate] DeepL endpoint: $endpoint (tier=${if (isFreeTier) "FREE" else "PRO"})")
println("[translate] Output: ${outputFile.name}")

// Build new header: füge name_de ein wenn nicht vorhanden (nach name_en)
val outHeader: List<String> = if (nameDeIdxExisting >= 0) header
    else header.toMutableList().apply { add(nameEnIdx, "name_de") }

// ── Translate ────────────────────────────────────────────────────────────────

fun translateBatch(texts: List<String>): List<String> {
    if (texts.isEmpty()) return emptyList()
    val body = buildString {
        append("source_lang=EN&target_lang=DE")
        texts.forEach { t ->
            append("&text=").append(URLEncoder.encode(t, StandardCharsets.UTF_8))
        }
    }
    var attempt = 0
    while (true) {
        val req = HttpRequest.newBuilder(endpoint)
            .header("Authorization", "DeepL-Auth-Key $apiKey")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build()
        val res = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        when (res.statusCode()) {
            200 -> {
                val parsed = mapper.readTree(res.body())
                return parsed["translations"].map { it["text"].asText() }
            }
            429, 503 -> {
                attempt++
                val waitMs = minOf(60_000L, 1000L * (1 shl attempt.coerceAtMost(6)))
                System.err.println("[translate] rate-limit (HTTP ${res.statusCode()}), warte ${waitMs}ms…")
                Thread.sleep(waitMs)
                if (attempt > 6) error("Zu viele Retries — DeepL-Quota erschöpft? Abbruch.")
            }
            456 -> error("DeepL Quota exceeded (HTTP 456). Free: 500k chars/Monat.")
            else -> error("DeepL HTTP ${res.statusCode()}: ${res.body().take(300)}")
        }
    }
}

val outLines = mutableListOf(outHeader.joinToString(";"))
val dataRows = lines.drop(1).map { it.split(";") }
val pending = mutableListOf<Pair<Int, String>>()  // (row-index, name_en)
val nameDeCache = mutableMapOf<Int, String>()

for ((i, row) in dataRows.withIndex()) {
    val existing = if (nameDeIdxExisting >= 0 && nameDeIdxExisting < row.size) row[nameDeIdxExisting] else ""
    if (existing.isNotBlank()) {
        nameDeCache[i] = existing
    } else {
        val en = row.getOrNull(nameEnIdx)?.trim().orEmpty()
        if (en.isBlank()) {
            nameDeCache[i] = ""
        } else {
            pending += i to en
        }
    }
}

println("[translate] ${pending.size} Zeilen brauchen Übersetzung (${nameDeCache.size} bereits vorhanden).")

pending.chunked(batchSize).forEachIndexed { batchNo, batch ->
    val translations = translateBatch(batch.map { it.second })
    require(translations.size == batch.size) {
        "DeepL response size mismatch: ${translations.size} vs ${batch.size}"
    }
    batch.forEachIndexed { j, (rowIdx, _) ->
        nameDeCache[rowIdx] = translations[j]
    }
    println("[translate] batch ${batchNo + 1} done (${batch.size} texts).")
}

// ── Write output ─────────────────────────────────────────────────────────────

for ((i, row) in dataRows.withIndex()) {
    val nameDe = nameDeCache[i].orEmpty()
    val outRow = if (nameDeIdxExisting >= 0) {
        row.toMutableList().also { if (nameDeIdxExisting < it.size) it[nameDeIdxExisting] = nameDe }
    } else {
        row.toMutableList().also { it.add(nameEnIdx, nameDe) }
    }
    outLines += outRow.joinToString(";")
}

outputFile.writeText(outLines.joinToString("\n"), Charsets.UTF_8)
println("[translate] OK — ${outputFile.absolutePath} (${outLines.size - 1} rows).")
