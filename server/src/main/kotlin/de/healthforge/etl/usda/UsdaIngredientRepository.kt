package de.healthforge.etl.usda

import de.healthforge.ingredient.IngredientEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

/**
 * P7.S2 — Eigenes Repository-Interface für FDC-Idempotenz-Lookups, damit
 * der bestehende [de.healthforge.ingredient.IngredientRepository] nicht
 * mit Importer-spezifischen Methoden verunreinigt wird.
 */
interface UsdaIngredientRepository : JpaRepository<IngredientEntity, UUID> {
    fun findByFdcId(fdcId: Long): Optional<IngredientEntity>
}
