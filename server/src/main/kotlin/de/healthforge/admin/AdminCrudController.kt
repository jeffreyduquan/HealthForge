package de.healthforge.admin

import com.fasterxml.jackson.annotation.JsonProperty
import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import de.healthforge.common.AuditLogService
import de.healthforge.ingredient.IngredientEntity
import de.healthforge.ingredient.IngredientRepository
import de.healthforge.ingredient.IngredientSource
import de.healthforge.ingredient.IngredientStatus
import de.healthforge.recipe.RecipeRepo
import de.healthforge.recipe.RecipeEntity
import de.healthforge.supplement.PublicSupplementEntity
import de.healthforge.supplement.PublicSupplementRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Admin DB-Editor (P7.S4).
 * Vollzugriff auf Ingredients, Recipes, Supplements → View, Edit, Create, Delete.
 * Jede Änderung wird mit Warning-Toast quittiert (UI-seitig).
 */
@RestController
@RequestMapping("/admin/v1/crud")
@PreAuthorize("hasRole('ADMIN')")
class AdminCrudController(
    private val ingredientRepo: IngredientRepository,
    private val recipeRepo: RecipeRepo,
    private val supplementRepo: PublicSupplementRepository,
    private val auditService: AuditLogService,
) {
    private fun require(p: AuthPrincipal?): AuthPrincipal =
        p ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    // ==================== INGREDIENTS ====================

    @GetMapping("/ingredients")
    fun listIngredients(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("q", required = false) q: String? = null,
        @RequestParam("limit", required = false, defaultValue = "100") limit: Int,
        @RequestParam("offset", required = false, defaultValue = "0") offset: Int,
    ): List<IngredientCrudDto> {
        require(principal)
        val all = ingredientRepo.findAll()
        return all
            .filter { q == null || it.nameDe.contains(q, ignoreCase = true) || (it.barcode?.contains(q, ignoreCase = true) == true) }
            .drop(offset)
            .take(limit.coerceIn(1, 5_000))
            .map { it.toIngredientCrudDto() }
    }

    @GetMapping("/ingredients/{id}")
    fun getIngredient(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): IngredientCrudDto {
        require(principal)
        val ing = ingredientRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ingredient $id not found")
        }
        return ing.toIngredientCrudDto()
    }

    @PutMapping("/ingredients/{id}")
    @Transactional
    fun updateIngredient(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @RequestBody req: IngredientCrudInput,
    ): IngredientCrudDto {
        val p = require(principal)
        val ing = ingredientRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ingredient $id not found")
        }
        applyIngredientUpdate(ing, req)
        ing.lastAdminEditAt = Instant.now()
        ingredientRepo.save(ing)
        auditService.record(
            action = "INGREDIENT_ADMIN_UPDATE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "INGREDIENT",
            targetId = id.toString(),
        )
        return ing.toIngredientCrudDto()
    }

    @PostMapping("/ingredients")
    @Transactional
    fun createIngredient(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestBody req: IngredientCrudInput,
    ): ResponseEntity<Map<String, UUID>> {
        val p = require(principal)
        val ing = IngredientEntity(
            id = UUID.randomUUID(),
            nameDe = req.nameDe.trim(),
            brand = req.brand?.trim()?.ifEmpty { null },
            barcode = req.barcode?.trim()?.ifEmpty { null },
            source = IngredientSource.MANUAL,
            sourceId = null,
            energyKcalPer100g = req.energyKcalPer100g,
            proteinGPer100g = req.proteinGPer100g,
            carbsGPer100g = req.carbsGPer100g,
            sugarGPer100g = req.sugarGPer100g,
            fatGPer100g = req.fatGPer100g,
            satfatGPer100g = req.satfatGPer100g,
            fiberGPer100g = req.fiberGPer100g,
            saltGPer100g = req.saltGPer100g,
            histamineScore = req.histamineScore,
            allergensJson = req.allergensJson ?: "[]",
            fodmapFlagsJson = req.fodmapFlagsJson ?: "[]",
            micronutrientsJson = req.micronutrientsJson ?: "{}",
            status = IngredientStatus.APPROVED.name,
            locked = req.locked ?: false,
        )
        ingredientRepo.save(ing)
        auditService.record(
            action = "INGREDIENT_ADMIN_CREATE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "INGREDIENT",
            targetId = ing.id.toString(),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to ing.id))
    }

    @DeleteMapping("/ingredients/{id}")
    @Transactional
    fun deleteIngredient(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        if (!ingredientRepo.existsById(id)) {
            throw ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ingredient $id not found")
        }
        ingredientRepo.deleteById(id)
        auditService.record(
            action = "INGREDIENT_ADMIN_DELETE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "INGREDIENT",
            targetId = id.toString(),
        )
        return ResponseEntity.noContent().build()
    }

    // ==================== SUPPLEMENTS ====================

    @GetMapping("/supplements")
    fun listSupplements(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("q", required = false) q: String? = null,
    ): List<SupplementCrudDto> {
        require(principal)
        val all = supplementRepo.findAll()
        return all
            .filter { q == null || it.nameDe.contains(q, ignoreCase = true) || (it.brand?.contains(q, ignoreCase = true) == true) }
            .map { it.toSupplementCrudDto() }
    }

    @GetMapping("/supplements/{id}")
    fun getSupplement(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): SupplementCrudDto {
        require(principal)
        val sup = supplementRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Supplement $id not found")
        }
        return sup.toSupplementCrudDto()
    }

    @PutMapping("/supplements/{id}")
    @Transactional
    fun updateSupplement(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @RequestBody req: SupplementCrudInput,
    ): SupplementCrudDto {
        val p = require(principal)
        val sup = supplementRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Supplement $id not found")
        }
        sup.nameDe = req.nameDe.trim()
        sup.brand = req.brand?.trim()?.ifEmpty { null }
        sup.unitLabel = req.unitLabel.trim()
        sup.defaultDose = req.defaultDose
        sup.kcalPerDose = req.kcalPerDose
        sup.proteinPerDose = req.proteinPerDose
        sup.carbsPerDose = req.carbsPerDose
        sup.fatPerDose = req.fatPerDose
        sup.micronutrientsJson = req.micronutrientsJson
        sup.notes = req.notes?.trim()?.ifEmpty { null }
        supplementRepo.save(sup)
        auditService.record(
            action = "SUPPLEMENT_ADMIN_UPDATE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "SUPPLEMENT",
            targetId = id.toString(),
        )
        return sup.toSupplementCrudDto()
    }

    @PostMapping("/supplements")
    @Transactional
    fun createSupplement(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestBody req: SupplementCrudInput,
    ): ResponseEntity<Map<String, UUID>> {
        val p = require(principal)
        val sup = PublicSupplementEntity(
            id = UUID.randomUUID(),
            nameDe = req.nameDe.trim(),
            brand = req.brand?.trim()?.ifEmpty { null },
            unitLabel = req.unitLabel.trim(),
            defaultDose = req.defaultDose,
            kcalPerDose = req.kcalPerDose,
            proteinPerDose = req.proteinPerDose,
            carbsPerDose = req.carbsPerDose,
            fatPerDose = req.fatPerDose,
            micronutrientsJson = req.micronutrientsJson,
            notes = req.notes?.trim()?.ifEmpty { null },
        )
        supplementRepo.save(sup)
        auditService.record(
            action = "SUPPLEMENT_ADMIN_CREATE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "SUPPLEMENT",
            targetId = sup.id.toString(),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to sup.id))
    }

    @DeleteMapping("/supplements/{id}")
    @Transactional
    fun deleteSupplement(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        if (!supplementRepo.existsById(id)) {
            throw ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Supplement $id not found")
        }
        supplementRepo.deleteById(id)
        auditService.record(
            action = "SUPPLEMENT_ADMIN_DELETE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "SUPPLEMENT",
            targetId = id.toString(),
        )
        return ResponseEntity.noContent().build()
    }

    // ==================== RECIPES ====================

    @GetMapping("/recipes")
    fun listRecipes(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("q", required = false) q: String? = null,
    ): List<RecipeCrudDto> {
        require(principal)
        val all = recipeRepo.findAll()
        return all
            .filter { q == null || it.title.contains(q, ignoreCase = true) || (it.description?.contains(q, ignoreCase = true) == true) }
            .map { it.toRecipeCrudDto() }
    }

    @GetMapping("/recipes/{id}")
    fun getRecipe(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): RecipeCrudDto {
        require(principal)
        val recipe = recipeRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recipe $id not found")
        }
        return recipe.toRecipeCrudDto()
    }

    @PutMapping("/recipes/{id}")
    @Transactional
    fun updateRecipe(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @RequestBody req: RecipeCrudInput,
    ): RecipeCrudDto {
        val p = require(principal)
        val recipe = recipeRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recipe $id not found")
        }
        recipe.title = req.title.trim()
        recipe.description = req.description?.trim()?.ifEmpty { null }
        recipe.servings = req.servings
        recipe.prepMinutes = req.prepMinutes
        recipe.cookMinutes = req.cookMinutes
        recipe.status = req.status
        recipe.visibility = req.visibility
        recipe.updatedAt = Instant.now()
        recipeRepo.save(recipe)
        auditService.record(
            action = "RECIPE_ADMIN_UPDATE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "RECIPE",
            targetId = id.toString(),
        )
        return recipe.toRecipeCrudDto()
    }

    @DeleteMapping("/recipes/{id}")
    @Transactional
    fun deleteRecipe(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        if (!recipeRepo.existsById(id)) {
            throw ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recipe $id not found")
        }
        recipeRepo.deleteById(id)
        auditService.record(
            action = "RECIPE_ADMIN_DELETE",
            actorUserId = p.userId,
            actorKind = de.healthforge.common.ActorKind.ADMIN,
            targetType = "RECIPE",
            targetId = id.toString(),
        )
        return ResponseEntity.noContent().build()
    }

    // ==================== Helpers ====================

    private fun applyIngredientUpdate(ing: IngredientEntity, req: IngredientCrudInput) {
        ing.nameDe = req.nameDe.trim()
        ing.brand = req.brand?.trim()?.ifEmpty { null }
        ing.barcode = req.barcode?.trim()?.ifEmpty { null }
        ing.energyKcalPer100g = req.energyKcalPer100g
        ing.proteinGPer100g = req.proteinGPer100g
        ing.carbsGPer100g = req.carbsGPer100g
        ing.sugarGPer100g = req.sugarGPer100g
        ing.fatGPer100g = req.fatGPer100g
        ing.satfatGPer100g = req.satfatGPer100g
        ing.fiberGPer100g = req.fiberGPer100g
        ing.saltGPer100g = req.saltGPer100g
        ing.histamineScore = req.histamineScore
        ing.allergensJson = req.allergensJson ?: "[]"
        ing.fodmapFlagsJson = req.fodmapFlagsJson ?: "[]"
        ing.micronutrientsJson = req.micronutrientsJson ?: "{}"
        ing.locked = req.locked ?: ing.locked
    }
}

// ==================== DTOs ====================

// --- Ingredient CRUD ---

data class IngredientCrudInput(
    @JsonProperty("name_de") val nameDe: String,
    val brand: String? = null,
    val barcode: String? = null,
    @JsonProperty("energy_kcal_per_100g") val energyKcalPer100g: BigDecimal? = null,
    @JsonProperty("protein_g_per_100g") val proteinGPer100g: BigDecimal? = null,
    @JsonProperty("carbs_g_per_100g") val carbsGPer100g: BigDecimal? = null,
    @JsonProperty("sugar_g_per_100g") val sugarGPer100g: BigDecimal? = null,
    @JsonProperty("fat_g_per_100g") val fatGPer100g: BigDecimal? = null,
    @JsonProperty("satfat_g_per_100g") val satfatGPer100g: BigDecimal? = null,
    @JsonProperty("fiber_g_per_100g") val fiberGPer100g: BigDecimal? = null,
    @JsonProperty("salt_g_per_100g") val saltGPer100g: BigDecimal? = null,
    @JsonProperty("histamine_score") val histamineScore: Short? = null,
    @JsonProperty("allergens_json") val allergensJson: String? = null,
    @JsonProperty("fodmap_flags_json") val fodmapFlagsJson: String? = null,
    @JsonProperty("micronutrients_json") val micronutrientsJson: String? = null,
    val locked: Boolean? = null,
)

data class IngredientCrudDto(
    val id: UUID,
    @JsonProperty("name_de") val nameDe: String,
    val brand: String?,
    val barcode: String?,
    val source: String,
    val status: String,
    val locked: Boolean,
    @JsonProperty("energy_kcal_per_100g") val energyKcalPer100g: BigDecimal?,
    @JsonProperty("protein_g_per_100g") val proteinGPer100g: BigDecimal?,
    @JsonProperty("carbs_g_per_100g") val carbsGPer100g: BigDecimal?,
    @JsonProperty("sugar_g_per_100g") val sugarGPer100g: BigDecimal?,
    @JsonProperty("fat_g_per_100g") val fatGPer100g: BigDecimal?,
    @JsonProperty("satfat_g_per_100g") val satfatGPer100g: BigDecimal?,
    @JsonProperty("fiber_g_per_100g") val fiberGPer100g: BigDecimal?,
    @JsonProperty("salt_g_per_100g") val saltGPer100g: BigDecimal?,
    @JsonProperty("histamine_score") val histamineScore: Short?,
    @JsonProperty("allergens_json") val allergensJson: String?,
    @JsonProperty("fodmap_flags_json") val fodmapFlagsJson: String?,
    @JsonProperty("micronutrients_json") val micronutrientsJson: String?,
    @JsonProperty("created_at") val createdAt: String,
)

private fun IngredientEntity.toIngredientCrudDto() = IngredientCrudDto(
    id = id, nameDe = nameDe, brand = brand, barcode = barcode,
    source = source.name, status = status, locked = locked,
    energyKcalPer100g = energyKcalPer100g, proteinGPer100g = proteinGPer100g,
    carbsGPer100g = carbsGPer100g, sugarGPer100g = sugarGPer100g,
    fatGPer100g = fatGPer100g, satfatGPer100g = satfatGPer100g,
    fiberGPer100g = fiberGPer100g, saltGPer100g = saltGPer100g,
    histamineScore = histamineScore, allergensJson = allergensJson,
    fodmapFlagsJson = fodmapFlagsJson, micronutrientsJson = micronutrientsJson,
    createdAt = createdAt.toString(),
)

// --- Supplement CRUD ---

data class SupplementCrudInput(
    @JsonProperty("name_de") val nameDe: String,
    val brand: String? = null,
    @JsonProperty("unit_label") val unitLabel: String,
    @JsonProperty("default_dose") val defaultDose: Double,
    @JsonProperty("kcal_per_dose") val kcalPerDose: Double? = null,
    @JsonProperty("protein_per_dose") val proteinPerDose: Double? = null,
    @JsonProperty("carbs_per_dose") val carbsPerDose: Double? = null,
    @JsonProperty("fat_per_dose") val fatPerDose: Double? = null,
    @JsonProperty("micronutrients_json") val micronutrientsJson: String? = null,
    val notes: String? = null,
)

data class SupplementCrudDto(
    val id: UUID,
    @JsonProperty("name_de") val nameDe: String,
    val brand: String?,
    @JsonProperty("unit_label") val unitLabel: String,
    @JsonProperty("default_dose") val defaultDose: Double,
    @JsonProperty("kcal_per_dose") val kcalPerDose: Double?,
    @JsonProperty("protein_per_dose") val proteinPerDose: Double?,
    @JsonProperty("carbs_per_dose") val carbsPerDose: Double?,
    @JsonProperty("fat_per_dose") val fatPerDose: Double?,
    @JsonProperty("micronutrients_json") val micronutrientsJson: String?,
    val notes: String?,
    @JsonProperty("created_at") val createdAt: String,
)

private fun PublicSupplementEntity.toSupplementCrudDto() = SupplementCrudDto(
    id = id, nameDe = nameDe, brand = brand, unitLabel = unitLabel,
    defaultDose = defaultDose, kcalPerDose = kcalPerDose,
    proteinPerDose = proteinPerDose, carbsPerDose = carbsPerDose,
    fatPerDose = fatPerDose, micronutrientsJson = micronutrientsJson,
    notes = notes, createdAt = createdAt.toString(),
)

// --- Recipe CRUD ---

data class RecipeCrudInput(
    val title: String,
    val description: String? = null,
    val servings: Int = 1,
    @JsonProperty("prep_minutes") val prepMinutes: Int = 0,
    @JsonProperty("cook_minutes") val cookMinutes: Int? = null,
    val status: String = "PUBLISHED",
    val visibility: String = "PUBLIC",
)

data class RecipeCrudDto(
    val id: UUID,
    val title: String,
    val description: String?,
    val status: String,
    val visibility: String,
    val authorId: UUID,
    val servings: Int,
    @JsonProperty("prep_minutes") val prepMinutes: Int,
    @JsonProperty("cook_minutes") val cookMinutes: Int?,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("updated_at") val updatedAt: String,
)

private fun RecipeEntity.toRecipeCrudDto() = RecipeCrudDto(
    id = id, title = title, description = description,
    status = status, visibility = visibility, authorId = authorId,
    servings = servings, prepMinutes = prepMinutes, cookMinutes = cookMinutes,
    createdAt = createdAt.toString(), updatedAt = updatedAt.toString(),
)
