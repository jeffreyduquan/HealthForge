package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Executors

/**
 * P7.S4 — Auto-Start des BLS-ETL beim Boot (HINTERGRUND-Thread).
 *
 * 1. USDA_FDC-Purge läuft synchron (schnell, <1s).
 * 2. BLS-Import läuft ASYNCHRON im Hintergrund-Thread, damit der Server sofort
 *    ready ist und kein 502 Bad Gateway entsteht. Der Import von ~15.000 BLS-
 *    Einträgen dauert 30-90s — währenddessen sind noch keine Zutaten sichtbar,
 *    aber die API antwortet (leere Liste).
 *
 * Idempotenz: USDA-Löschung bei jedem Boot. BLS-Import nur wenn noch keine
 * BLS-Einträge existieren.
 */
@Component
@Order(Int.MAX_VALUE)
class EtlAutoStarter(
    private val orchestrator: EtlOrchestrator,
    private val ingredients: IngredientRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EtlAutoStarter::class.java)
    private val etlExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "etl-auto-starter").apply { isDaemon = true }
    }

    override fun run(vararg args: String?) {
        purgeUsdaFdc()

        val blsCount = ingredients.findAll().count { it.source == IngredientSource.BLS }
        if (blsCount > 0) {
            log.info("EtlAutoStarter: {} BLS-Einträge vorhanden — Import übersprungen", blsCount)
            return
        }

        log.info("EtlAutoStarter: keine BLS-Einträge — starte BLS-ETL im Hintergrund...")
        etlExecutor.submit {
            try {
                importBls()
            } catch (e: Exception) {
                log.error("EtlAutoStarter: BLS-ETL im Hintergrund fehlgeschlagen", e)
            }
        }
    }

    /** Löscht alle USDA_FDC-Einträge — idempotent, bei jedem Boot. */
    private fun purgeUsdaFdc() {
        val stale = ingredients.findAll().filter { it.source == IngredientSource.USDA_FDC }
        if (stale.isEmpty()) {
            log.info("EtlAutoStarter: keine USDA_FDC-Einträge zum Löschen")
            return
        }
        log.info("EtlAutoStarter: lösche {} USDA_FDC-Einträge...", stale.size)
        ingredients.deleteAll(stale)
        log.info("EtlAutoStarter: {} USDA_FDC-Einträge gelöscht", stale.size)
    }

    /** Importiert BLS (läuft im Hintergrund-Thread, nicht-blockierend). */
    @Transactional
    fun importBls() {
        log.info("EtlAutoStarter: BLS-ETL gestartet...")
        try {
            val run = orchestrator.run(EtlSource.BLS)
            log.info(
                "EtlAutoStarter: BLS abgeschlossen — status={} inserted={} updated={} skipped={}",
                run.status, run.rowsInserted, run.rowsUpdated, run.rowsSkipped,
            )
        } catch (e: Exception) {
            log.error("EtlAutoStarter: BLS fehlgeschlagen — {}", e.message, e)
        }

        val finalCount = ingredients.findAll().count { it.source == IngredientSource.BLS }
        log.info("EtlAutoStarter: Fertig — {} BLS-Einträge in der DB", finalCount)
    }
}
