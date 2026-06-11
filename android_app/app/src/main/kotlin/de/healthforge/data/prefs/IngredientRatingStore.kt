package de.healthforge.data.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local ingredient rating storage (like / dislike).
 * Persisted via plain SharedPreferences — lightweight, no Room migration needed.
 *
 * - Like  (REQ-INGREDIENT-LIKE-001): user prefers this ingredient.
 * - Heart (REQ-INGREDIENT-HEART-001): user dislikes this ingredient;
 *   algorithm SHOULD exclude recipes containing it.
 */
@Singleton
class IngredientRatingStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** @return set of ingredient IDs the user liked. */
    fun getLiked(): Set<String> = prefs.getStringSet(KEY_LIKED, emptySet()) ?: emptySet()

    /** @return set of ingredient IDs the user disliked (hearted). */
    fun getDisliked(): Set<String> = prefs.getStringSet(KEY_DISLIKED, emptySet()) ?: emptySet()

    fun isLiked(id: String): Boolean = getLiked().contains(id)
    fun isDisliked(id: String): Boolean = getDisliked().contains(id)

    fun toggleLike(id: String) {
        val set = getLiked().toMutableSet()
        if (set.contains(id)) set.remove(id) else set.add(id)
        prefs.edit().putStringSet(KEY_LIKED, set).apply()
    }

    fun toggleDislike(id: String) {
        val set = getDisliked().toMutableSet()
        if (set.contains(id)) set.remove(id) else set.add(id)
        prefs.edit().putStringSet(KEY_DISLIKED, set).apply()
    }

    companion object {
        private const val PREFS_NAME = "ingredient_ratings"
        private const val KEY_LIKED = "liked"
        private const val KEY_DISLIKED = "disliked"
    }
}
