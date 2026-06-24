package de.healthforge.etl

import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Executors

/**
 * P7.S4 — Auto-Start des BLS-ETL beim Boot (HINTERGRUND-Thread).
 *
 * 1. USDA_FDC-Purge läuft synchron (schnell, <1s). Nutzt native SQL um
 *    FK-Constraints auf recipe_ingredients zu umgehen.
 * 2. BLS-Import läuft ASYNCHRON im Hintergrund-Thread, damit der Server sofort
 *    ready ist und kein 502 Bad Gateway entsteht.
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
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EtlAutoStarter::class.java)
    private val etlExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "etl-auto-starter").apply { isDaemon = true }
    }

    @Transactional
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
