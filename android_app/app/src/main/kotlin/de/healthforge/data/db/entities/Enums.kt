package de.healthforge.data.db.entities

/** Biological sex for BMR calculations (Mifflin–St Jeor). REQ-PROFILE-006. */
enum class BiologicalSex { MALE, FEMALE, OTHER }

/** Activity level multiplier for TDEE. */
enum class ActivityLevel(val tdeeMultiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
}

/** Diet goal: affects calorie target offset. */
enum class DietGoal(val kcalDeltaPct: Double) {
    LOSE(-0.20),
    MAINTAIN(0.0),
    GAIN(0.15),
}

/** Histamine sensitivity self-assessment. */
enum class HistamineSensitivity { NONE, MILD, MODERATE, HIGH }

/** Meal-slot template for daily plan + reminders. */
enum class MealSlot { FRUEHSTUECK, ZWEITES_FRUEHSTUECK, MITTAG, SNACK, ABENDESSEN, NACHT }

/**
 * EU-14 baseline allergens (REQ-PROFILE-004). German UI labels in [germanLabel].
 *
 * Source: EU Regulation 1169/2011 Annex II.
 */
enum class AllergenType(val germanLabel: String) {
    GLUTEN("Glutenhaltige Getreide"),
    KREBSTIERE("Krebstiere"),
    EIER("Eier"),
    FISCH("Fisch"),
    ERDNUESSE("Erdn\u00fcsse"),
    SOJA("Soja"),
    MILCH("Milch / Laktose"),
    SCHALENFRUECHTE("Schalenfr\u00fcchte (N\u00fcsse)"),
    SELLERIE("Sellerie"),
    SENF("Senf"),
    SESAM("Sesam"),
    SCHWEFELDIOXID("Schwefeldioxid / Sulfite"),
    LUPINEN("Lupinen"),
    WEICHTIERE("Weichtiere"),
}

/** FODMAP intolerance types (REQ-PROFILE-005). */
enum class FodmapType(val germanLabel: String) {
    FRUCTOSE("Fruktose"),
    LACTOSE("Laktose"),
    FRUCTANS("Fruktane"),
    GOS("GOS (Galakto-Oligosaccharide)"),
    POLYOLS("Polyole"),
}
