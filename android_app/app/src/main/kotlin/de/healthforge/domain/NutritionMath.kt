package de.healthforge.domain

import de.healthforge.data.db.entities.ActivityLevel
import de.healthforge.data.db.entities.BiologicalSex
import de.healthforge.data.db.entities.DietGoal
import kotlin.math.roundToInt

/**
 * Mifflin–St Jeor BMR + activity TDEE + goal-adjusted target.
 *
 * REQ-PROFILE-006:
 * - Male  : 10·w + 6.25·h − 5·a + 5
 * - Female: 10·w + 6.25·h − 5·a − 161
 * - Other : average of the two formulas
 */
object NutritionMath {

    fun bmr(weightKg: Double, heightCm: Int, ageYears: Int, sex: BiologicalSex): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears
        return when (sex) {
            BiologicalSex.MALE -> base + 5
            BiologicalSex.FEMALE -> base - 161
            BiologicalSex.OTHER -> base - 78  // average of +5 and −161
        }
    }

    fun tdee(bmr: Double, activity: ActivityLevel): Double = bmr * activity.tdeeMultiplier

    fun targetKcal(tdee: Double, goal: DietGoal): Int =
        (tdee * (1.0 + goal.kcalDeltaPct)).roundToInt()

    /**
     * Default macro split (REQ-PROFILE-006): 30% P / 40% C / 30% F.
     * Returns grams of protein, carbs, fat.
     */
    fun macros(targetKcal: Int): Triple<Int, Int, Int> {
        val protein = (targetKcal * 0.30 / 4.0).roundToInt()
        val carbs = (targetKcal * 0.40 / 4.0).roundToInt()
        val fat = (targetKcal * 0.30 / 9.0).roundToInt()
        return Triple(protein, carbs, fat)
    }
}
