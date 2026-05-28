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
 * **Baseline** (= auto-berechnete biologische Tagesziele OHNE Profil-Override).
 *
 * Für Mifflin/Macros: berechnet aus age/sex/weight/activity/goal.
 * Für Wasser: KATALOG-Default (2000 ml), NICHT `profile.waterGoalMl`, damit der
 * Profil-Slider-Range stabil bleibt (REQ-PROFILE-LAYOUT-001 / Slice 4d).
 *
 * Wenn Onboarding-Pflichtfelder fehlen → [DailyTargets.FALLBACK].
 */
class ComputeNutrientTargetsUseCase @Inject constructor() {

    operator fun invoke(profile: UserProfileEntity?): DailyTargets {
        val w = profile?.weightKg
        val h = profile?.heightCm
        val a = profile?.ageYears
        val s = profile?.biologicalSex
        val act = profile?.activityLevel
        val goal = profile?.dietGoal

        if (w == null || h == null || a == null || s == null || act == null || goal == null) {
            return DailyTargets.FALLBACK
        }
        val bmr = NutritionMath.bmr(w, h, a, s)
        val tdee = NutritionMath.tdee(bmr, act)
        val kcal = NutritionMath.targetKcal(tdee, goal)
        val (p, c, f) = NutritionMath.macros(kcal)
        return DailyTargets(kcal, p, c, f, DailyTargets.FALLBACK.waterMl)
    }
}

/**
 * Wendet Profil-Overrides auf eine [DailyTargets]-Baseline an:
 * - Wasser: `profile.waterGoalMl` ersetzt Baseline-Wasser immer (Single-Source).
 * - Makros (kcal/protein/carbs/fat): JSON-Key in `dailyNutrientGoalsJson` ersetzt Baseline.
 *
 * Damit gilt: "Profil-Werte sind ground truth" (REQ-PROFILE-LAYOUT-001 / Slice 4d).
 * Home/Insights/Plan rufen das nach [ComputeNutrientTargetsUseCase] auf.
 */
fun DailyTargets.applyOverrides(profile: UserProfileEntity?): DailyTargets {
    if (profile == null) return this
    val obj = runCatching { org.json.JSONObject(profile.dailyNutrientGoalsJson) }.getOrNull()
    val kcalOv = obj?.optDouble("kcal", Double.NaN)
    val proteinOv = obj?.optDouble("protein", Double.NaN)
    val carbsOv = obj?.optDouble("carbs", Double.NaN)
    val fatOv = obj?.optDouble("fat", Double.NaN)
    return DailyTargets(
        kcal = if (kcalOv != null && !kcalOv.isNaN()) kcalOv.toInt() else this.kcal,
        proteinG = if (proteinOv != null && !proteinOv.isNaN()) proteinOv.toInt() else this.proteinG,
        carbsG = if (carbsOv != null && !carbsOv.isNaN()) carbsOv.toInt() else this.carbsG,
        fatG = if (fatOv != null && !fatOv.isNaN()) fatOv.toInt() else this.fatG,
        waterMl = profile.waterGoalMl,
    )
}
