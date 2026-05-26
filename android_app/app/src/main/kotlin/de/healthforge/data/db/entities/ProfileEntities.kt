package de.healthforge.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local-only user profile. REQ-PROFILE-001/002: never sent to server.
 *
 * Singleton row (id = 1L always) — the device is single-user. All fields except [id]
 * and [createdAt] are nullable so onboarding-skip is supported (REQ-ONBOARD-002).
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val displayName: String? = null,
    val email: String? = null,
    val ageYears: Int? = null,
    val biologicalSex: BiologicalSex? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val activityLevel: ActivityLevel? = null,
    val dietGoal: DietGoal? = null,
    val histamineSensitivity: HistamineSensitivity = HistamineSensitivity.NONE,
    val mealSlotsJson: String = "[]",      // comma-stored MealSlot names
    val maxPrepTimeMin: Int? = null,
    val waterGoalMl: Int = 2000,
    /**
     * Per-Nutrient-Tagesziele als JSON-Map `{ "kcal": 2200.0, "protein": 110.0, ... }`.
     * Leeres Objekt = automatisch aus age/sex/weight/activity/goal berechnen.
     * REQ-PROFILE-GOALS-001 (P6.S6, F-011).
     */
    val dailyNutrientGoalsJson: String = "{}",
    /**
     * Welche Nährstoffe als Ringe/Karten auf dem Home/Insights-Screen sichtbar sind.
     * JSON-Array von Nutrient-Keys. Default = Makros.
     * REQ-PROFILE-GOALS-001 (P6.S6, F-011).
     */
    val pinnedNutrientsJson: String = "[\"kcal\",\"protein\",\"carbs\",\"fat\"]",
    val onboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "allergy", primaryKeys = ["allergen"])
data class AllergyEntity(
    val allergen: AllergenType,
)

@Entity(tableName = "intolerance", primaryKeys = ["fodmap"])
data class IntoleranceEntity(
    val fodmap: FodmapType,
)
