package de.healthforge.ingredient

import de.healthforge.auth.UserRepository
import de.healthforge.common.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * P4.S1 — User-Ingredient-Submissions + Field-PR.
 *
 *  - REQ-INGR-USER-001/-002: `suggest()` legt eine Ingredient-Reihe mit
 *    `status=PENDING` + `submitted_by=userId` an; via [IngredientSearchRepository]
 *    bleibt sie nur für den Submitter sichtbar bis Admin sie approved.
 *  - REQ-FIELDPR-001..003: `proposeFieldChange()` legt eine `ingredient_field_pr`-
 *    Reihe an; angezeigter Wert bleibt unverändert bis Approve den Wert auf das
 *    Ingredient kopiert + `last_admin_edit_at` setzt.
 */
@Service
class IngredientSubmissionService(
    private val ingredients: IngredientRepository,
    private val fieldPrs: IngredientFieldPrRepository,
    private val users: UserRepository,
) {

    /** Whitelist editierbarer Felder + Konvertierung String→Entity. */
    private val fieldHandlers: Map<String, (IngredientEntity, String) -> Unit> = mapOf(
        "histamine_score" to { e, v -> e.histamineScore = v.toShort() },
        "energy_kcal_per_100g" to { e, v -> e.energyKcalPer100g = BigDecimal(v) },
        "protein_g_per_100g" to { e, v -> e.proteinGPer100g = BigDecimal(v) },
        "carbs_g_per_100g" to { e, v -> e.carbsGPer100g = BigDecimal(v) },
        "sugar_g_per_100g" to { e, v -> e.sugarGPer100g = BigDecimal(v) },
        "fat_g_per_100g" to { e, v -> e.fatGPer100g = BigDecimal(v) },
        "satfat_g_per_100g" to { e, v -> e.satfatGPer100g = BigDecimal(v) },
        "fiber_g_per_100g" to { e, v -> e.fiberGPer100g = BigDecimal(v) },
        "salt_g_per_100g" to { e, v -> e.saltGPer100g = BigDecimal(v) },
        "allergens_json" to { e, v -> e.allergensJson = sanitizeJsonArray(v) },
        "fodmap_flags_json" to { e, v -> e.fodmapFlagsJson = sanitizeJsonArray(v) },
    )

    private val fieldReader: Map<String, (IngredientEntity) -> String?> = mapOf(
        "histamine_score" to { it.histamineScore?.toString() },
        "energy_kcal_per_100g" to { it.energyKcalPer100g?.toPlainString() },
        "protein_g_per_100g" to { it.proteinGPer100g?.toPlainString() },
        "carbs_g_per_100g" to { it.carbsGPer100g?.toPlainString() },
        "sugar_g_per_100g" to { it.sugarGPer100g?.toPlainString() },
        "fat_g_per_100g" to { it.fatGPer100g?.toPlainString() },
        "satfat_g_per_100g" to { it.satfatGPer100g?.toPlainString() },
        "fiber_g_per_100g" to { it.fiberGPer100g?.toPlainString() },
        "salt_g_per_100g" to { it.saltGPer100g?.toPlainString() },
        "allergens_json" to { it.allergensJson },
        "fodmap_flags_json" to { it.fodmapFlagsJson },
    )

    @Transactional
    fun suggest(userId: UUID, input: IngredientSuggestionInput): UUID {
        val name = input.nameDe.trim()
        if (name.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_NAME", "name_de must not be blank")
        }
        val entity = IngredientEntity(
            id = UUID.randomUUID(),
            nameDe = name,
            brand = input.brand?.trim().takeUnless { it.isNullOrBlank() },
            barcode = input.barcode?.trim().takeUnless { it.isNullOrBlank() },
            source = IngredientSource.USER,
            sourceId = null,
            energyKcalPer100g = input.energyKcalPer100g,
            proteinGPer100g = input.proteinGPer100g,
            carbsGPer100g = input.carbsGPer100g,
            sugarGPer100g = input.sugarGPer100g,
            fatGPer100g = input.fatGPer100g,
            satfatGPer100g = input.satfatGPer100g,
            fiberGPer100g = input.fiberGPer100g,
            saltGPer100g = input.saltGPer100g,
            histamineScore = input.histamineScore,
            allergensJson = toJsonArray(input.allergens),
            fodmapFlagsJson = toJsonArray(input.fodmapFlags),
            locked = false,
            status = IngredientStatus.PENDING.name,
            submittedBy = userId,
        )
        ingredients.save(entity)
        return entity.id
    }

    @Transactional(readOnly = true)
    fun listPendingIngredients(): List<IngredientQueueEntryDto> {
        val rows = ingredients.findAllByStatusOrderByCreatedAtAsc(IngredientStatus.PENDING.name)
        val emails = users.findAllById(rows.mapNotNull { it.submittedBy }.toSet()).associateBy { it.id }
        return rows.map { r ->
            IngredientQueueEntryDto(
                id = r.id,
                nameDe = r.nameDe,
                brand = r.brand,
                barcode = r.barcode,
                submittedBy = r.submittedBy,
                submitterEmail = r.submittedBy?.let { emails[it]?.email },
                status = r.status,
                createdAt = r.createdAt,
            )
        }
    }

    @Transactional
    fun approveIngredient(id: UUID, adminId: UUID) {
        val row = loadPendingIngredient(id)
        row.status = IngredientStatus.APPROVED.name
        row.reviewerId = adminId
        row.reviewedAt = Instant.now()
        row.locked = true
        row.updatedAt = Instant.now()
        ingredients.save(row)
    }

    @Transactional
    fun rejectIngredient(id: UUID, adminId: UUID, note: String?) {
        val row = loadPendingIngredient(id)
        row.status = IngredientStatus.REJECTED.name
        row.reviewerId = adminId
        row.reviewedAt = Instant.now()
        row.reviewNote = note?.trim().takeUnless { it.isNullOrBlank() }
        row.updatedAt = Instant.now()
        ingredients.save(row)
    }

    private fun loadPendingIngredient(id: UUID): IngredientEntity {
        val row = ingredients.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "INGREDIENT_NOT_FOUND", "ingredient $id not found")
        }
        if (row.status != IngredientStatus.PENDING.name) {
            throw ApiException(
                HttpStatus.CONFLICT, "INGREDIENT_NOT_PENDING",
                "ingredient $id is not pending (status=${row.status})",
            )
        }
        return row
    }

    @Transactional
    fun proposeFieldChange(proposerId: UUID, ingredientId: UUID, input: FieldPrInput): UUID {
        val ingredient = ingredients.findById(ingredientId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "INGREDIENT_NOT_FOUND", "ingredient $ingredientId not found")
        }
        val fieldName = input.fieldName.trim()
        val reader = fieldReader[fieldName]
            ?: throw ApiException(
                HttpStatus.BAD_REQUEST, "INVALID_FIELD",
                "field_name '$fieldName' is not eligible for field-PRs",
            )
        val newValue = input.newValue.trim()
        if (newValue.isEmpty()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "EMPTY_VALUE", "new_value must not be blank")
        }
        // Validate parseability via the dry-run handler — throws if non-parseable.
        runCatching { fieldHandlers.getValue(fieldName)(IngredientEntity(
            nameDe = "_dryrun_", source = IngredientSource.MANUAL,
        ), newValue) }.onFailure {
            throw ApiException(
                HttpStatus.BAD_REQUEST, "INVALID_VALUE",
                "new_value not parseable for $fieldName: ${it.message}",
            )
        }
        val pr = IngredientFieldPrEntity(
            ingredientId = ingredientId,
            proposerId = proposerId,
            fieldName = fieldName,
            oldValue = reader(ingredient),
            newValue = newValue,
            rationale = input.rationale?.trim().takeUnless { it.isNullOrBlank() },
        )
        fieldPrs.save(pr)
        return pr.id
    }

    @Transactional(readOnly = true)
    fun listFieldPrs(onlyPending: Boolean): List<FieldPrAdminDto> {
        val rows = if (onlyPending) {
            fieldPrs.findAllByStatusOrderByCreatedAtAsc(IngredientStatus.PENDING.name)
        } else {
            fieldPrs.findAllByOrderByCreatedAtDesc()
        }
        if (rows.isEmpty()) return emptyList()
        val ingredientById = ingredients.findAllById(rows.map { it.ingredientId }.toSet())
            .associateBy { it.id }
        val emails = users.findAllById(rows.map { it.proposerId }.toSet()).associateBy { it.id }
        return rows.map { r ->
            FieldPrAdminDto(
                id = r.id,
                ingredientId = r.ingredientId,
                ingredientName = ingredientById[r.ingredientId]?.nameDe ?: "—",
                proposerId = r.proposerId,
                proposerEmail = emails[r.proposerId]?.email,
                fieldName = r.fieldName,
                oldValue = r.oldValue,
                newValue = r.newValue,
                rationale = r.rationale,
                status = r.status,
                createdAt = r.createdAt,
                reviewedAt = r.reviewedAt,
                reviewNote = r.reviewNote,
            )
        }
    }

    @Transactional
    fun approveFieldPr(id: UUID, adminId: UUID) {
        val pr = loadPendingFieldPr(id)
        val ingredient = ingredients.findById(pr.ingredientId).orElseThrow {
            ApiException(
                HttpStatus.NOT_FOUND, "INGREDIENT_NOT_FOUND",
                "ingredient ${pr.ingredientId} not found",
            )
        }
        val handler = fieldHandlers[pr.fieldName] ?: throw ApiException(
            HttpStatus.BAD_REQUEST, "INVALID_FIELD",
            "field_name '${pr.fieldName}' no longer eligible",
        )
        handler(ingredient, pr.newValue)
        val now = Instant.now()
        ingredient.lastAdminEditAt = now
        ingredient.updatedAt = now
        ingredients.save(ingredient)

        pr.status = IngredientStatus.APPROVED.name
        pr.reviewerId = adminId
        pr.reviewedAt = now
        fieldPrs.save(pr)
    }

    @Transactional
    fun rejectFieldPr(id: UUID, adminId: UUID, note: String?) {
        val pr = loadPendingFieldPr(id)
        pr.status = IngredientStatus.REJECTED.name
        pr.reviewerId = adminId
        pr.reviewedAt = Instant.now()
        pr.reviewNote = note?.trim().takeUnless { it.isNullOrBlank() }
        fieldPrs.save(pr)
    }

    private fun loadPendingFieldPr(id: UUID): IngredientFieldPrEntity {
        val pr = fieldPrs.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "FIELD_PR_NOT_FOUND", "field-pr $id not found")
        }
        if (pr.status != IngredientStatus.PENDING.name) {
            throw ApiException(
                HttpStatus.CONFLICT, "FIELD_PR_NOT_PENDING",
                "field-pr $id is not pending (status=${pr.status})",
            )
        }
        return pr
    }

    private fun toJsonArray(values: List<String>): String {
        val cleaned = values
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() && it.length <= 32 && it.all { c -> c.isLetterOrDigit() || c == '_' } }
            .distinct()
        return cleaned.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }

    private fun sanitizeJsonArray(raw: String): String {
        // Defensive: parse out codes from any JSON-array-looking string and re-serialize.
        val regex = Regex("\"([A-Za-z0-9_]{1,32})\"")
        val codes = regex.findAll(raw).map { it.groupValues[1].uppercase() }.distinct().toList()
        return codes.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    }
}
