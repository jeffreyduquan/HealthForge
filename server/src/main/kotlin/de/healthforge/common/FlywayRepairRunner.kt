package de.healthforge.common

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Repariert vor Flyway-Migrationen die Datenbank, falls die vorherige
 * V17-Migration fehlgeschlagen ist (FK-Constraint auf dev-* Zutaten).
 *
 * Läuft VOR Flyway durch @Order(HIGHEST_PRECEDENCE).
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class FlywayRepairRunner(dataSource: DataSource) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val jdbc = JdbcTemplate(dataSource)

    override fun run(args: ApplicationArguments) {
        try {
            val deleted = jdbc.update(
                "DELETE FROM flyway_schema_history WHERE version = '17' AND success = false"
            )
            if (deleted > 0) {
                log.warn("🧹 Repaired: deleted {} failed V17 migration entry(ies) from flyway_schema_history", deleted)
            }
        } catch (e: Exception) {
            // table may not exist yet (first startup)
            log.debug("FlywayRepairRunner: {}", e.message)
        }
    }
}
