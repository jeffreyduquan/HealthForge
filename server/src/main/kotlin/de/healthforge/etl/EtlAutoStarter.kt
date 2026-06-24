package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Executors

/**
 * P7.S4 — Auto-Start des BLS-ETL beim Boot (HINTERGRUND-Thread).
 *
 * 1. USDA_FDC-Purge läuft synchron via native SQL.
 * 2. BLS-Import läuft ASYNCHRON mit TransactionTemplate (damit @Transactional
 *    im Hintergrund-Thread greift).
 *
 * Idempotenz: USDA-Löschung bei jedem Boot. BLS-Import nur wenn noch keine
 * BLS-Einträge existieren.
 */
@Component
@Order(Int.MAX_VALUE)
class EtlAutoStarter(
    private val orchestrator: EtlOrchestrator,
    private val ingredients: IngredientRepository,
    private val em: EntityManager,
    private val txManager: PlatformTransactionManager,
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
        val txTemplate = TransactionTemplate(txManager)
        etlExecutor.submit {
            txTemplate.executeWithoutResult {
                importBls()
            }
        }
    }

    /**
     * Löscht alle USDA_FDC-Einträge via native SQL, inklusive abhängiger
     * recipe_ingredients-Rows (FK constraint hat kein ON DELETE CASCADE).
     */
    private fun purgeUsdaFdc() {
        val staleIds = em.createNativeQuery(
            "SELECT id FROM ingredients WHERE source = 'USDA_FDC'"
        ).resultList
        if (staleIds.isEmpty()) {
            log.info("EtlAutoStarter: keine USDA_FDC-Einträge zum Löschen")
            return
        }
        log.info("EtlAutoStarter: lösche {} USDA_FDC-Einträge + abhängige recipe_ingredients...", staleIds.size)

        // Zuerst abhängige recipe_ingredients-Rows löschen
        em.createNativeQuery(
            "DELETE FROM recipe_ingredients WHERE ingredient_id IN (SELECT id FROM ingredients WHERE source = 'USDA_FDC')"
        ).executeUpdate()

        // Dann die Ingredients selbst
        val deleted = em.createNativeQuery(
            "DELETE FROM ingredients WHERE source = 'USDA_FDC'"
        ).executeUpdate()
        log.info("EtlAutoStarter: {} USDA_FDC-Einträge gelöscht", deleted)
    }

    /** Importiert BLS (läuft in TransactionTemplate-Transaktion). */
    private fun importBls() {
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
