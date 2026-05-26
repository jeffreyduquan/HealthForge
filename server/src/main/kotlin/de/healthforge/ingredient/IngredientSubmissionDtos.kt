package de.healthforge.ingredient

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * REQ-INGR-USER-001 / -002 — User-Submission eines neuen Ingredients.
 * Bean-Validation hält Eingaben in Grenzen; Nährwerte sind optional.
 */
data class IngredientSuggestionInput(
    @field:NotBlank @field:Size(max = 200)
    @JsonProperty("name_de") val nameDe: String,
    @field:Size(max = 200) val brand: String? = null,
    @field:Size(max = 64) val barcode: String? = null,
    @JsonProperty("energy_kcal_per_100g") val energyKcalPer100g: BigDecimal? = null,
    @JsonProperty("protein_g_per_100g") val proteinGPer100g: BigDecimal? = null,
    @JsonProperty("carbs_g_per_100g") val carbsGPer100g: BigDecimal? = null,
    @JsonProperty("sugar_g_per_100g") val sugarGPer100g: BigDecimal? = null,
    @JsonProperty("fat_g_per_100g") val fatGPer100g: BigDecimal? = null,
    @JsonProperty("satfat_g_per_100g") val satfatGPer100g: BigDecimal? = null,
    @JsonProperty("fiber_g_per_100g") val fiberGPer100g: BigDecimal? = null,
    @JsonProperty("salt_g_per_100g") val saltGPer100g: BigDecimal? = null,
    @JsonProperty("histamine_score") val histamineScore: Short? = null,
    val allergens: List<String> = emptyList(),
    @JsonProperty("fodmap_flags") val fodmapFlags: List<String> = emptyList(),
)

data class IngredientSuggestionCreatedResponse(val id: UUID, val status: String)

data class IngredientQueueEntryDto(
    val id: UUID,
    @JsonProperty("name_de") val nameDe: String,
    val brand: String?,
    val barcode: String?,
    @JsonProperty("submitted_by") val submittedBy: UUID?,
    @JsonProperty("submitter_email") val submitterEmail: String?,
    val status: String,
    @JsonProperty("created_at") val createdAt: Instant,
)

/**
 * REQ-FIELDPR-001 — Vorschlag zur Änderung eines einzelnen Feldes.
 * Whitelist der zulässigen Felder wird im Service erzwungen.
 */
data class FieldPrInput(
    @field:NotBlank @field:Size(max = 64)
    @JsonProperty("field_name") val fieldName: String,
    @field:NotBlank @field:Size(max = 4000)
    @JsonProperty("new_value") val newValue: String,
    @field:Size(max = 1000) val rationale: String? = null,
)

data class FieldPrAdminDto(
    val id: UUID,
    @JsonProperty("ingredient_id") val ingredientId: UUID,
    @JsonProperty("ingredient_name") val ingredientName: String,
    @JsonProperty("proposer_id") val proposerId: UUID,
    @JsonProperty("proposer_email") val proposerEmail: String?,
    @JsonProperty("field_name") val fieldName: String,
    @JsonProperty("old_value") val oldValue: String?,
    @JsonProperty("new_value") val newValue: String,
    val rationale: String?,
    val status: String,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("reviewed_at") val reviewedAt: Instant?,
    @JsonProperty("review_note") val reviewNote: String?,
)

data class RejectReviewRequest(
    @field:Size(max = 500) val note: String? = null,
)
