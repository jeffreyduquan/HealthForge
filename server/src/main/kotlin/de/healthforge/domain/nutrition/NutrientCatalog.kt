package de.healthforge.domain.nutrition

/**
 * P7.S1 Server-Mirror des Nährstoff-Katalogs (REQ-NUTRIENT-CATALOG-001).
 *
 * MUSS identische Keys + Units zu
 * `android_app/.../domain/nutrition/NutrientCatalog.kt` halten.
 * Parity wird über [NutrientCatalogParityTest] geprüft.
 *
 * Wird vom USDA-FDC-Importer (P7.S2) genutzt, um FDC-Nutrient-IDs auf Katalog-Keys
 * zu mappen und Werte in [de.healthforge.ingredient.entity.IngredientEntity]
 * `micronutrients_json` zu schreiben.
 */
object NutrientCatalog {

    enum class Category { MACRO, VITAMIN, MINERAL, WATER }

    enum class Unit(val label: String) {
        KCAL("kcal"),
        G("g"),
        MG("mg"),
        UG("µg"),
        ML("ml"),
    }

    data class Nutrient(
        val key: String,
        val displayDe: String,
        val unit: Unit,
        val category: Category,
        val defaultPerDay: Double,
        val min: Double,
        val max: Double,
    )

    private val macros = listOf(
        Nutrient("kcal",      "Kalorien",        Unit.KCAL, Category.MACRO, 2200.0, 1000.0, 4000.0),
        Nutrient("protein",   "Eiweiß",          Unit.G,    Category.MACRO,  120.0,   30.0,  300.0),
        Nutrient("carbs",     "Kohlenhydrate",   Unit.G,    Category.MACRO,  260.0,   50.0,  600.0),
        Nutrient("sugar",     "Zucker",          Unit.G,    Category.MACRO,   50.0,    0.0,  150.0),
        Nutrient("fat",       "Fett",            Unit.G,    Category.MACRO,   80.0,   20.0,  200.0),
        Nutrient("satfat",    "Gesättigte Fette", Unit.G,   Category.MACRO,   20.0,    0.0,   80.0),
        Nutrient("fiber",     "Ballaststoffe",   Unit.G,    Category.MACRO,   30.0,   10.0,   80.0),
        Nutrient("salt",      "Salz",            Unit.G,    Category.MACRO,    5.0,    0.0,   15.0),
    )

    private val vitamins = listOf(
        Nutrient("vitamin_a",  "Vitamin A",                Unit.UG, Category.VITAMIN, 900.0,  300.0, 3000.0),
        Nutrient("vitamin_d",  "Vitamin D",                Unit.UG, Category.VITAMIN,  20.0,    5.0,  100.0),
        Nutrient("vitamin_e",  "Vitamin E",                Unit.MG, Category.VITAMIN,  13.0,    4.0,  300.0),
        Nutrient("vitamin_k",  "Vitamin K",                Unit.UG, Category.VITAMIN,  70.0,   20.0, 1000.0),
        Nutrient("vitamin_b1", "Vitamin B1 (Thiamin)",     Unit.MG, Category.VITAMIN,   1.1,    0.3,  100.0),
        Nutrient("vitamin_b2", "Vitamin B2 (Riboflavin)",  Unit.MG, Category.VITAMIN,   1.3,    0.3,  100.0),
        Nutrient("vitamin_b3", "Vitamin B3 (Niacin)",      Unit.MG, Category.VITAMIN,  15.0,    3.0,  900.0),
        Nutrient("vitamin_b5", "Vitamin B5 (Pantothens.)", Unit.MG, Category.VITAMIN,   6.0,    1.0,  500.0),
        Nutrient("vitamin_b6", "Vitamin B6",               Unit.MG, Category.VITAMIN,   1.5,    0.3,  100.0),
        Nutrient("vitamin_b7", "Vitamin B7 (Biotin)",      Unit.UG, Category.VITAMIN,  40.0,    5.0,  900.0),
        Nutrient("vitamin_b9", "Vitamin B9 (Folsäure)",    Unit.UG, Category.VITAMIN, 300.0,   50.0, 1000.0),
        Nutrient("vitamin_b12","Vitamin B12",              Unit.UG, Category.VITAMIN,   4.0,    0.5,  500.0),
        Nutrient("vitamin_c",  "Vitamin C",                Unit.MG, Category.VITAMIN, 100.0,   20.0, 2000.0),
    )

    private val minerals = listOf(
        Nutrient("calcium",   "Calcium",   Unit.MG, Category.MINERAL, 1000.0,  200.0, 2500.0),
        Nutrient("eisen",     "Eisen",     Unit.MG, Category.MINERAL,   14.0,    3.0,  100.0),
        Nutrient("magnesium", "Magnesium", Unit.MG, Category.MINERAL,  350.0,   80.0, 1000.0),
        Nutrient("zink",      "Zink",      Unit.MG, Category.MINERAL,   10.0,    2.0,   80.0),
        Nutrient("kupfer",    "Kupfer",    Unit.MG, Category.MINERAL,    1.3,    0.2,   20.0),
        Nutrient("mangan",    "Mangan",    Unit.MG, Category.MINERAL,    3.5,    0.5,   30.0),
        Nutrient("selen",     "Selen",     Unit.UG, Category.MINERAL,   65.0,   10.0,  400.0),
        Nutrient("jod",       "Jod",       Unit.UG, Category.MINERAL,  200.0,   50.0, 1000.0),
        Nutrient("kalium",    "Kalium",    Unit.MG, Category.MINERAL, 4000.0,  500.0, 6000.0),
        Nutrient("natrium",   "Natrium",   Unit.MG, Category.MINERAL, 1500.0,    0.0, 5000.0),
        Nutrient("phosphor",  "Phosphor",  Unit.MG, Category.MINERAL,  700.0,  100.0, 4000.0),
    )

    private val water = Nutrient(
        key = "water",
        displayDe = "Wasser",
        unit = Unit.ML,
        category = Category.WATER,
        defaultPerDay = 2000.0,
        min = 500.0,
        max = 5000.0,
    )

    val all: List<Nutrient> = macros + vitamins + minerals + listOf(water)

    private val byKey: Map<String, Nutrient> = all.associateBy { it.key }

    fun byKeyOrNull(key: String): Nutrient? = byKey[key]

    fun requireByKey(key: String): Nutrient =
        byKey[key] ?: error("Unknown nutrient key: $key (Catalog out of sync?)")

    fun ofCategory(c: Category): List<Nutrient> = all.filter { it.category == c }

    val defaultPinnedKeys: List<String> = listOf("kcal", "protein", "carbs", "fat", "water")
}
