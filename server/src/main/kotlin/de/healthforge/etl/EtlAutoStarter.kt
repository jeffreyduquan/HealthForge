package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * P7.S4 — Auto-Start des USDA-FDC-ETL beim Boot, falls die ingredients-Tabelle
 * leer ist (z.B. nach V14-Reset oder Erst-Deployment).
 *
 * Reihenfolge: [Order(LOWEST_PRECEDENCE)] stellt sicher, dass Flyway-Migrationen
 * und alle anderen CommandLineRunner VOR dem ETL-Import gelaufen sind.
 *
 * Idempotenz: Läuft NUR, wenn `ingredients` leer ist (count = 0). Bei jedem
 * weiteren Boot wird der Import übersprungen.
 */
@Component
@Order(Int.MAX_VALUE)
class EtlAutoStarter(
    private val orchestrator: EtlOrchestrator,
    private val ingredients: IngredientRepository,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EtlAutoStarter::class.java)

    override fun run(vararg args: String?) {
        val count = ingredients.count()
        if (count > 0) {
            log.info("EtlAutoStarter: ingredients-Tabelle hat {} Einträge — ETL übersprungen", count)
            return
        }

        log.info("EtlAutoStarter: ingredients-Tabelle ist LEER — starte USDA_FDC-ETL...")
        try {
            val run = orchestrator.run(EtlSource.USDA_FDC)
            log.info(
                "EtlAutoStarter: USDA_FDC-ETL abgeschlossen — status={} inserted={} updated={} skipped={}",
                run.status, run.rowsInserted, run.rowsUpdated, run.rowsSkipped,
            )
        } catch (e: Exception) {
            log.error("EtlAutoStarter: USDA_FDC-ETL fehlgeschlagen — {}", e.message, e)
        }
    }
}
