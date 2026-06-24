package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * P7.S4 — Auto-Start des BLS-ETL beim Boot.
 *
 * 1. Löscht ALLE USDA_FDC-Einträge (Migration von USDA → BLS als alleinige Quelle).
 * 2. Importiert BLS (Whitelist aus bls_curation.csv, ~400 RAW-Einträge mit
 *    SIGHI/FODMAP-Kuration), falls noch keine BLS-Einträge existieren.
 *
 * Reihenfolge: [Order(LOWEST_PRECEDENCE)] stellt sicher, dass Flyway-Migrationen
 * und alle anderen CommandLineRunner VOR diesem Runner gelaufen sind.
 *
 * Idempotenz: USDA_Löschung läuft bei JEDEM Boot (bis keine mehr da sind).
 * BLS-Import läuft nur, wenn noch keine BLS-Einträge existieren.
 */
@Component
@Order(Int.MAX_VALUE)
class EtlAutoStarter(
    private val orchestrator: EtlOrchestrator,
    private val ingredients: IngredientRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EtlAutoStarter::class.java)

    @Transactional
    override fun run(vararg args: String?) {
        purgeUsdaFdc()
        importBlsIfMissing()
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

    /** Importiert BLS, falls noch keine BLS-Einträge in der DB sind. */
    private fun importBlsIfMissing() {
        val blsCount = ingredients.findAll().count { it.source == IngredientSource.BLS }
        if (blsCount > 0) {
            log.info("EtlAutoStarter: {} BLS-Einträge vorhanden — Import übersprungen", blsCount)
            return
        }

        log.info("EtlAutoStarter: keine BLS-Einträge — starte BLS-ETL...")
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
