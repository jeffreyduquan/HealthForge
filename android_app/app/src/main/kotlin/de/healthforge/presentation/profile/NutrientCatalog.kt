package de.healthforge.presentation.profile

/**
 * Statischer Nährstoff-Katalog für den Goals-Editor (P6.S6 Slice B, REQ-PROFILE-GOALS-001).
 * Slug-Strings sind stabil (werden in `UserProfileEntity.dailyNutrientGoalsJson` / `pinnedNutrientsJson`
 * gespeichert). Reihenfolge bestimmt die Anzeige im "Angeheftete Nährstoffe"-Chip-Grid.
 */
internal object NutrientCatalog {

    data class NutrientCfg(
        val slug: String,
        val label: String,
        val unit: String,
        val min: Float,
        val max: Float,
        val steps: Int,
        val default: Double,
    )

    val all: List<NutrientCfg> = listOf(
        NutrientCfg("kcal",       "Kalorien",  "kcal", 1000f, 4000f, 30, 2200.0),
        NutrientCfg("protein",    "Eiweiß",    "g",      30f,  300f, 27,  120.0),
        NutrientCfg("carbs",      "Kohlenh.",  "g",      50f,  600f, 54,  260.0),
        NutrientCfg("fat",        "Fett",      "g",      20f,  200f, 18,   80.0),
        NutrientCfg("fiber",      "Ballaststoffe", "g", 10f, 80f, 14, 30.0),
        NutrientCfg("sugar",      "Zucker",    "g",       0f,  150f, 30,   50.0),
        NutrientCfg("salt",       "Salz",      "g",       0f,   15f, 30,    5.0),
        NutrientCfg("saturated",  "Ges. Fett", "g",       0f,   80f, 16,   20.0),
    )

    private val bySlug = all.associateBy { it.slug }

    fun byOrNull(slug: String): NutrientCfg? = bySlug[slug]

    fun fallback(slug: String): NutrientCfg =
        NutrientCfg(slug = slug, label = slug, unit = "g", min = 0f, max = 500f, steps = 50, default = 50.0)
}
