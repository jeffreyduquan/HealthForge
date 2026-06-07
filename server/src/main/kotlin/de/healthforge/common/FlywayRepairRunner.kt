package de.healthforge.common

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * Repariert VOR Flyway-Migrationen die Datenbank, falls die vorherige
 * V17-Migration fehlgeschlagen ist (FK-Constraint auf dev-* Zutaten).
 *
 * Nutzt FlywayMigrationStrategy — läuft VOR Flyway, nicht danach (wie ApplicationRunner).
 */
@Configuration
class FlywayRepairConfig(dataSource: DataSource) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val jdbc = JdbcTemplate(dataSource)

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy = FlywayMigrationStrategy { flyway: Flyway ->
        try {
            val deleted = jdbc.update(
                "DELETE FROM flyway_schema_history WHERE version = '17' AND success = false"
            )
            if (deleted > 0) {
                log.warn("🧹 Flyway repair: deleted {} failed V17 entry(ies)", deleted)
            }
        } catch (e: Exception) {
            log.debug("Flyway repair skipped: {}", e.message)
        }
        flyway.migrate()
    }
}
