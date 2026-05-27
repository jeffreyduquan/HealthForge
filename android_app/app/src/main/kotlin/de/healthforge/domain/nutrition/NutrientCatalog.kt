package de.healthforge.domain.nutrition

/**
 * P7.S1 Big-Nutrition-Catalog (REQ-NUTRIENT-CATALOG-001).
 *
 * Statische Liste aller ~30 unterstützten Nährstoffe (8 Makros + 13 Vitamine +
 * 11 Mineralstoffe + Pseudo-`water`). Slug-Keys sind stabil und werden in
 *
 *  - `ingredients.micronutrients_json` (Server, V12) — Wert je 100 g
 *  - `UserProfileEntity.dailyNutrientGoalsJson` (Room, device-local) — User-Override je Tag
 *  - `UserProfileEntity.pinnedNutrientsJson` (Room, device-local) — Pin-Reihenfolge
 *
 * Default-Werte = DGE-Empfehlung Erwachsene 25–50 J. (Misch-Geschlecht-Default).
 * Für Makros (kcal/protein/carbs/fat) wird der Default zur Laufzeit von
 * [de.healthforge.domain.ComputeNutrientTargetsUseCase] aus dem Profil errechnet
 * und überschreibt den hier hinterlegten statischen Default.
 *
 * **Server-Mirror:** `server/.../domain/nutrition/NutrientCatalog.kt` muss
 * identische Keys + Units halten (Parity-Unit-Test [NutrientCatalogParityTest]).
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
        /** DGE-Default Erwachsene 25–50 J., per Tag, in [unit]. */
        val defaultPerDay: Double,
        /** Slider-Min im Profile-Override-Editor. */
        val min: Double,
        /** Slider-Max im Profile-Override-Editor. */
        val max: Double,
    )

    // ─── Makros ───────────────────────────────────────────────────────────────
    private val macros = listOf(
        Nutrient("kcal",      "Kalorien",       Unit.KCAL, Category.MACRO, 2200.0, 1000.0, 4000.0),
        Nutrient("protein",   "Eiweiß",         Unit.G,    Category.MACRO,  120.0,   30.0,  300.0),
        Nutrient("carbs",     "Kohlenhydrate",  Unit.G,    Category.MACRO,  260.0,   50.0,  600.0),
        Nutrient("sugar",     "Zucker",         Unit.G,    Category.MACRO,   50.0,    0.0,  150.0),
        Nutrient("fat",       "Fett",           Unit.G,    Category.MACRO,   80.0,   20.0,  200.0),
        Nutrient("satfat",    "Gesättigte Fette", Unit.G,  Category.MACRO,   20.0,    0.0,   80.0),
        Nutrient("fiber",     "Ballaststoffe",  Unit.G,    Category.MACRO,   30.0,   10.0,   80.0),
        Nutrient("salt",      "Salz",           Unit.G,    Category.MACRO,    5.0,    0.0,   15.0),
    )

    // ─── Vitamine (DGE Erwachsene m/w-Durchschnitt) ───────────────────────────
    private val vitamins = listOf(
        Nutrient("vitamin_a",  "Vitamin A",   Unit.UG, Category.VITAMIN,  900.0,  300.0, 3000.0),
        Nutrient("vitamin_d",  "Vitamin D",   Unit.UG, Category.VITAMIN,   20.0,    5.0,  100.0),
        Nutrient("vitamin_e",  "Vitamin E",   Unit.MG, Category.VITAMIN,   13.0,    4.0,  300.0),
        Nutrient("vitamin_k",  "Vitamin K",   Unit.UG, Category.VITAMIN,   70.0,   20.0, 1000.0),
        Nutrient("vitamin_b1", "Vitamin B1 (Thiamin)",     Unit.MG, Category.VITAMIN,  1.1,  0.3,  100.0),
        Nutrient("vitamin_b2", "Vitamin B2 (Riboflavin)",  Unit.MG, Category.VITAMIN,  1.3,  0.3,  100.0),
        Nutrient("vitamin_b3", "Vitamin B3 (Niacin)",      Unit.MG, Category.VITAMIN, 15.0,  3.0,  900.0),
        Nutrient("vitamin_b5", "Vitamin B5 (Pantothens.)", Unit.MG, Category.VITAMIN,  6.0,  1.0,  500.0),
        Nutrient("vitamin_b6", "Vitamin B6",               Unit.MG, Category.VITAMIN,  1.5,  0.3,  100.0),
        Nutrient("vitamin_b7", "Vitamin B7 (Biotin)",      Unit.UG, Category.VITAMIN, 40.0,  5.0,  900.0),
        Nutrient("vitamin_b9", "Vitamin B9 (Folsäure)",    Unit.UG, Category.VITAMIN, 300.0, 50.0, 1000.0),
        Nutrient("vitamin_b12","Vitamin B12",              Unit.UG, Category.VITAMIN,  4.0,  0.5,  500.0),
        Nutrient("vitamin_c",  "Vitamin C",                Unit.MG, Category.VITAMIN,100.0, 20.0, 2000.0),
    )

    // ─── Mineralstoffe ────────────────────────────────────────────────────────
    private val minerals = listOf(
        Nutrient("calcium",   "Calcium",     Unit.MG, Category.MINERAL, 1000.0,  200.0, 2500.0),
        Nutrient("eisen",     "Eisen",       Unit.MG, Category.MINERAL,   14.0,    3.0,  100.0),
        Nutrient("magnesium", "Magnesium",   Unit.MG, Category.MINERAL,  350.0,   80.0, 1000.0),
        Nutrient("zink",      "Zink",        Unit.MG, Category.MINERAL,   10.0,    2.0,   80.0),
        Nutrient("kupfer",    "Kupfer",      Unit.MG, Category.MINERAL,    1.3,    0.2,   20.0),
        Nutrient("mangan",    "Mangan",      Unit.MG, Category.MINERAL,    3.5,    0.5,   30.0),
        Nutrient("selen",     "Selen",       Unit.UG, Category.MINERAL,   65.0,    10.0,  400.0),
        Nutrient("jod",       "Jod",         Unit.UG, Category.MINERAL,  200.0,    50.0, 1000.0),
        Nutrient("kalium",    "Kalium",      Unit.MG, Category.MINERAL, 4000.0,  500.0, 6000.0),
        Nutrient("natrium",   "Natrium",     Unit.MG, Category.MINERAL, 1500.0,    0.0, 5000.0),
        Nutrient("phosphor",  "Phosphor",    Unit.MG, Category.MINERAL,  700.0,  100.0, 4000.0),
    )

    // ─── Wasser (Pseudo-Nährstoff, separate Persistenz via WaterIntakeEntity) ─
    private val water = Nutrient(
        key = "water",
        displayDe = "Wasser",
        unit = Unit.ML,
        category = Category.WATER,
        defaultPerDay = 2000.0,
        min = 500.0,
        max = 5000.0,
    )

    /** Vollständiger Katalog in stabiler Anzeige-Reihenfolge. */
    val all: List<Nutrient> = macros + vitamins + minerals + listOf(water)

    private val byKey: Map<String, Nutrient> = all.associateBy { it.key }

    fun byKeyOrNull(key: String): Nutrient? = byKey[key]

    fun requireByKey(key: String): Nutrient =
        byKey[key] ?: error("Unknown nutrient key: $key (Catalog out of sync?)")

    fun ofCategory(c: Category): List<Nutrient> = all.filter { it.category == c }

    /** Default-Pin-Set nach Onboarding (REQ-HOME-NUTRIENT-LIST-001). */
    val defaultPinnedKeys: List<String> = listOf("kcal", "protein", "carbs", "fat", "water")
}
