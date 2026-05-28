package de.healthforge.etl.usda

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AllergenMapperTest {

    @Test
    fun `extracts multiple allergens from typical FDC ingredient text`() {
        val text = "ENRICHED WHEAT FLOUR, MILK, EGG, SOY LECITHIN, CONTAINS PEANUTS."
        val codes = AllergenMapper.extract(text).map { it.name }.toSet()
        assertTrue(codes.containsAll(setOf("GLUTEN", "LACTOSE", "EGG", "SOY", "PEANUT"))) {
            "Expected GLUTEN+LACTOSE+EGG+SOY+PEANUT, got $codes"
        }
    }

    @Test
    fun `word boundary prevents partial matches`() {
        // "soybean" → SOY match; "soylent" → no match (word boundary).
        assertEquals(listOf(AllergenMapper.Code.SOY), AllergenMapper.extract("soybean flour"))
        assertEquals(emptyList<AllergenMapper.Code>(), AllergenMapper.extract("soylent green"))
    }

    @Test
    fun `empty or null text returns empty list`() {
        assertEquals(emptyList<AllergenMapper.Code>(), AllergenMapper.extract(null))
        assertEquals(emptyList<AllergenMapper.Code>(), AllergenMapper.extract(""))
        assertEquals(emptyList<AllergenMapper.Code>(), AllergenMapper.extract("   "))
    }

    @Test
    fun `result order is deterministic following Code enum declaration`() {
        val codes = AllergenMapper.extract("milk and wheat and egg")
        assertEquals(listOf("GLUTEN", "EGG", "LACTOSE"), codes.map { it.name })
    }

    @Test
    fun `fermented foods flagged as histamine source`() {
        val codes = AllergenMapper.extract("aged cheese with sauerkraut")
        val names = codes.map { it.name }
        assertTrue("LACTOSE" in names && "HISTAMINE" in names)
    }

    // --- Negativ-Liste (ReqSpec §12 REQ-INGR-ALLERGEN-MAPPING-001) ---

    @Test
    fun `negative list strips mustard-seed-oil so MUSTARD is not flagged`() {
        // FDC-Realfall: hochraffiniertes Senfsaatöl ist kein EU-Allergen.
        val codes = AllergenMapper.extract("WATER, MUSTARD-SEED-OIL, SALT.").map { it.name }
        assertTrue("MUSTARD" !in codes) { "MUSTARD-SEED-OIL should not trigger MUSTARD, got $codes" }

        // Variante mit Leerzeichen + Bindestrich-Mix.
        assertTrue("MUSTARD" !in AllergenMapper.extract("mustard seed oil emulsion").map { it.name })
        assertTrue("MUSTARD" !in AllergenMapper.extract("contains mustard oil").map { it.name })
    }

    @Test
    fun `mustard alone still triggers MUSTARD after negative list applied`() {
        // Sanity: Negativ-Liste darf das normale Match nicht zerstören.
        val codes = AllergenMapper.extract("WATER, MUSTARD, VINEGAR.").map { it.name }
        assertTrue("MUSTARD" in codes) { "Plain MUSTARD should still flag, got $codes" }
    }

    @Test
    fun `coconut and nutmeg do not trigger NUT (already safe, regression guard)`() {
        // Beide standen historisch auf der Negativ-Liste in ReqSpec §665; aktuelle
        // NUT-Keywords matchen sie zwar ohnehin nicht, aber Test sichert ab gegen
        // zukünftige Keyword-Erweiterungen (z.B. wenn jemand "nut" naiv ergänzt).
        assertTrue("NUT" !in AllergenMapper.extract("coconut flakes").map { it.name })
        assertTrue("NUT" !in AllergenMapper.extract("coconut oil base").map { it.name })
        assertTrue("NUT" !in AllergenMapper.extract("nutmeg spice").map { it.name })
    }

    @Test
    fun `coconut milk does not pollute LACTOSE match for real milk in same row`() {
        // Realfall: "Coconut milk, whey powder" — Coconut wird gestrippt, whey bleibt.
        val codes = AllergenMapper.extract("COCONUT MILK, WHEY POWDER.").map { it.name }
        assertTrue("LACTOSE" in codes) { "WHEY should still trigger LACTOSE, got $codes" }
    }
}
