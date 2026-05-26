package de.healthforge.domain

import de.healthforge.data.db.entities.UserProfileEntity
import javax.inject.Inject

/** Daily targets derived from profile. REQ-HOME-002 / REQ-PROFILE-006. */
data class DailyTargets(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val waterMl: Int,
) {
    companion object {
        val FALLBACK = DailyTargets(2000, 150, 200, 67, 2000)
    }
}

/**
 * Computes [DailyTargets] from a [UserProfileEntity].
 *
 * If any of weight/height/age/sex/activity/goal are null (user skipped onboarding),
 * returns [DailyTargets.FALLBACK] but uses the profile's `waterGoalMl`.
 */
class ComputeNutrientTargetsUseCase @Inject constructor() {

    operator fun invoke(profile: UserProfileEntity?): DailyTargets {
        val w = profile?.weightKg
        val h = profile?.heightCm
        val a = profile?.ageYears
        val s = profile?.biologicalSex
        val act = profile?.activityLevel
        val goal = profile?.dietGoal
        val water = profile?.waterGoalMl ?: DailyTargets.FALLBACK.waterMl

        if (w == null || h == null || a == null || s == null || act == null || goal == null) {
            return DailyTargets.FALLBACK.copy(waterMl = water)
        }
        val bmr = NutritionMath.bmr(w, h, a, s)
        val tdee = NutritionMath.tdee(bmr, act)
        val kcal = NutritionMath.targetKcal(tdee, goal)
        val (p, c, f) = NutritionMath.macros(kcal)
        return DailyTargets(kcal, p, c, f, water)
    }
}
