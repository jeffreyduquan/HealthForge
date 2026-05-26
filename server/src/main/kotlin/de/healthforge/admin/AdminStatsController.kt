package de.healthforge.admin

import com.fasterxml.jackson.annotation.JsonProperty
import de.healthforge.auth.AuthPrincipal
import de.healthforge.auth.UserRepository
import de.healthforge.common.ApiException
import de.healthforge.community.RecipeReportRepository
import de.healthforge.ingredient.IngredientFieldPrRepository
import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientStatus
import de.healthforge.recipe.RecipeRepo
import de.healthforge.supplement.PublicSupplementRepository
import de.healthforge.supplement.SupplementSuggestionRepository
import de.healthforge.supplement.SupplementSuggestionStatus
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin Dashboard + Statistics endpoints. REQ-ADMIN-FULL-001 (Dashboard) — P4.S5.
 *
 * Stateless aggregates; jeder Request berechnet live aus den Repos.
 * Single Source of Truth bleiben die Fach-Repositories (kein neuer Stats-Cache).
 */
@RestController
@RequestMapping("/admin/v1/stats")
@PreAuthorize("hasRole('ADMIN')")
class AdminStatsController(
    private val users: UserRepository,
    private val recipes: RecipeRepo,
    private val ingredients: IngredientRepository,
    private val supplementsPublic: PublicSupplementRepository,
    private val supplementSuggestions: SupplementSuggestionRepository,
    private val recipeReports: RecipeReportRepository,
    private val ingredientFieldPrs: IngredientFieldPrRepository,
) {
    private fun require(p: AuthPrincipal?): AuthPrincipal =
        p ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping("/dashboard")
    fun dashboard(@AuthenticationPrincipal principal: AuthPrincipal?): DashboardDto {
        require(principal)
        return DashboardDto(
            userCount = users.count(),
            recipeCount = recipes.count(),
            ingredientCount = ingredients.count(),
            supplementCount = supplementsPublic.count(),
            pendingIngredients = ingredients.findAllByStatusOrderByCreatedAtAsc(IngredientStatus.PENDING.name).size.toLong(),
            pendingFieldPrs = ingredientFieldPrs.findAllByStatusOrderByCreatedAtAsc(IngredientFieldPrStatusPENDING).size.toLong(),
            pendingSupplements = supplementSuggestions.findAllByStatusOrderByCreatedAtAsc(SupplementSuggestionStatus.PENDING.name).size.toLong(),
            openRecipeReports = recipeReports.countByStatus("OPEN"),
        )
    }

    @GetMapping("/statistics")
    fun statistics(@AuthenticationPrincipal principal: AuthPrincipal?): StatisticsDto {
        require(principal)
        // Pure read-only aggregate; same values as dashboard plus historical placeholders.
        val d = dashboard(principal)
        return StatisticsDto(
            users = d.userCount,
            recipes = d.recipeCount,
            ingredients = d.ingredientCount,
            supplements = d.supplementCount,
            approvedIngredients = ingredients.findAllByStatusOrderByCreatedAtAsc(IngredientStatus.APPROVED.name).size.toLong(),
            rejectedIngredients = ingredients.findAllByStatusOrderByCreatedAtAsc(IngredientStatus.REJECTED.name).size.toLong(),
            approvedSupplements = supplementSuggestions.findAllByStatusOrderByCreatedAtAsc(SupplementSuggestionStatus.APPROVED.name).size.toLong(),
            rejectedSupplements = supplementSuggestions.findAllByStatusOrderByCreatedAtAsc(SupplementSuggestionStatus.REJECTED.name).size.toLong(),
        )
    }

    companion object {
        /** Mirrors `IngredientFieldPrStatus.PENDING` literal so we don't add an unused enum import. */
        private const val IngredientFieldPrStatusPENDING = "PENDING"
    }
}

data class DashboardDto(
    @JsonProperty("user_count") val userCount: Long,
    @JsonProperty("recipe_count") val recipeCount: Long,
    @JsonProperty("ingredient_count") val ingredientCount: Long,
    @JsonProperty("supplement_count") val supplementCount: Long,
    @JsonProperty("pending_ingredients") val pendingIngredients: Long,
    @JsonProperty("pending_field_prs") val pendingFieldPrs: Long,
    @JsonProperty("pending_supplements") val pendingSupplements: Long,
    @JsonProperty("open_recipe_reports") val openRecipeReports: Long,
)

data class StatisticsDto(
    val users: Long,
    val recipes: Long,
    val ingredients: Long,
    val supplements: Long,
    @JsonProperty("approved_ingredients") val approvedIngredients: Long,
    @JsonProperty("rejected_ingredients") val rejectedIngredients: Long,
    @JsonProperty("approved_supplements") val approvedSupplements: Long,
    @JsonProperty("rejected_supplements") val rejectedSupplements: Long,
)
