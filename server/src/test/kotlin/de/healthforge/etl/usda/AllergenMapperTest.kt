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
}
