package de.healthforge.domain.nutrition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * P7.S1 / REQ-NUTRIENT-CATALOG-001 — verifiziert dass Server-Katalog
 * Schlüssel+Einheiten 1:1 mit Android-Katalog spiegelt.
 *
 * Liest die Android-Quelle aus dem Repo (Pfad relativ zum `server/`-CWD)
 * und extrahiert via Regex `Nutrient("key", ..., Unit.UNIT, ...)`-Einträge.
 */
class NutrientCatalogParityTest {

    @Test
    fun `android and server catalogs share identical keys and units`() {
        val androidSource = locateAndroidCatalogSource()
        val androidMap = parseAndroidCatalog(androidSource.readText())
        val serverMap = NutrientCatalog.all.associate { it.key to it.unit.name }

        assertTrue(androidMap.isNotEmpty(), "Android-Katalog konnte nicht geparst werden ($androidSource)")
        assertEquals(
            androidMap.keys.sorted(),
            serverMap.keys.sorted(),
            "Nutrient-Keys divergieren zwischen Android und Server",
        )
        androidMap.forEach { (key, unit) ->
            assertEquals(unit, serverMap[key], "Unit-Mismatch für '$key'")
        }
    }

    private fun locateAndroidCatalogSource(): File {
        val candidates = listOf(
            File("../android_app/app/src/main/kotlin/de/healthforge/domain/nutrition/NutrientCatalog.kt"),
            File("android_app/app/src/main/kotlin/de/healthforge/domain/nutrition/NutrientCatalog.kt"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Android NutrientCatalog.kt nicht gefunden; geprüft: ${candidates.map { it.absolutePath }}")
    }

    /** Greift `Nutrient(...)`-Aufrufe ab — sowohl positional als auch mit Named-Args. */
    private fun parseAndroidCatalog(src: String): Map<String, String> {
        val positional = Regex(
            """Nutrient\(\s*"([a-z0-9_]+)"\s*,\s*"[^"]*"\s*,\s*Unit\.([A-Z]+)"""
        ).findAll(src).map { it.groupValues[1] to it.groupValues[2] }

        // Named-Arg-Stil: `Nutrient(\n key = "x", ..., unit = Unit.Y, ...)`
        val namedBlock = Regex(
            """Nutrient\(\s*key\s*=\s*"([a-z0-9_]+)"[^)]*?unit\s*=\s*Unit\.([A-Z]+)""",
            RegexOption.DOT_MATCHES_ALL,
        ).findAll(src).map { it.groupValues[1] to it.groupValues[2] }

        return (positional + namedBlock).toMap()
    }
}
