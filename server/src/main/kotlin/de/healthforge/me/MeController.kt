package de.healthforge.me

import com.fasterxml.jackson.annotation.JsonProperty
import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import de.healthforge.ingredient.IngredientRepository
import de.healthforge.recipe.RecipeRepo
import de.healthforge.supplement.SupplementSuggestionRepository
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * "Meine Vorschläge" — user can see their own submissions (PENDING/APPROVED/REJECTED).
 */
@RestController
@RequestMapping("/v1/me")
class MeController(
    private val ingredientRepo: IngredientRepository,
    private val recipeRepo: RecipeRepo,
    private val supplementSuggestionRepo: SupplementSuggestionRepository,
) {
    private fun require(p: AuthPrincipal?): AuthPrincipal =
        p ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping("/submissions")
    fun mySubmissions(@AuthenticationPrincipal principal: AuthPrincipal?): MySubmissionsDto {
        val p = require(principal)
        val ingredients = ingredientRepo.findAllBySubmittedByOrderByCreatedAtDesc(p.userId)
        val recipes = recipeRepo.findAllByAuthorIdOrderByCreatedAtDesc(p.userId)
        val supplements = supplementSuggestionRepo.findAllByProposerIdOrderByCreatedAtDesc(p.userId)
        return MySubmissionsDto(
            ingredients = ingredients.map { it.toSubmissionDto() },
            recipes = recipes.map { it.toSubmissionDto() },
            supplements = supplements.map { it.toSubmissionDto() },
        )
    }
}

data class MySubmissionsDto(
    val ingredients: List<SubmissionDto>,
    val recipes: List<SubmissionDto>,
    val supplements: List<SubmissionDto>,
)

data class SubmissionDto(
    val id: String,
    @JsonProperty("name_de") val nameDe: String,
    val status: String,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("review_note") val reviewNote: String?,
)

private fun de.healthforge.ingredient.IngredientEntity.toSubmissionDto() = SubmissionDto(
    id = id.toString(),
    nameDe = nameDe,
    status = status,
    createdAt = createdAt.toString(),
    reviewNote = reviewNote,
)

private fun de.healthforge.recipe.RecipeEntity.toSubmissionDto() = SubmissionDto(
    id = id.toString(),
    nameDe = title,
    status = status,
    createdAt = createdAt.toString(),
    reviewNote = null,
)

private fun de.healthforge.supplement.SupplementSuggestionEntity.toSubmissionDto() = SubmissionDto(
    id = id.toString(),
    nameDe = nameDe,
    status = status,
    createdAt = createdAt.toString(),
    reviewNote = reviewNote,
)
