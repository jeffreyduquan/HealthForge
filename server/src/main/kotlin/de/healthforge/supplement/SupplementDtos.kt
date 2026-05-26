package de.healthforge.supplement

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

/**
 * Eingabe für einen neuen Supplement-Vorschlag oder einen direkten Admin-Eintrag.
 * Hauptfelder spiegeln Android `SupplementEntity` (lokal) — server-side stays language/serialization-agnostic.
 */
data class SupplementInput(
    @field:NotBlank
    @field:Size(max = 200)
    @JsonProperty("name_de")
    val nameDe: String,
    @field:Size(max = 200)
    val brand: String? = null,
    @field:NotBlank
    @field:Size(max = 40)
    @JsonProperty("unit_label")
    val unitLabel: String,
    @field:Positive
    @JsonProperty("default_dose")
    val defaultDose: Double,
    @JsonProperty("kcal_per_dose")
    val kcalPerDose: Double? = null,
    @JsonProperty("protein_per_dose")
    val proteinPerDose: Double? = null,
    @JsonProperty("carbs_per_dose")
    val carbsPerDose: Double? = null,
    @JsonProperty("fat_per_dose")
    val fatPerDose: Double? = null,
    @JsonProperty("micronutrients_json")
    @field:Size(max = 4000)
    val micronutrientsJson: String? = null,
    @field:Size(max = 2000)
    val notes: String? = null,
)

data class RejectRequest(
    @field:Size(max = 500)
    val note: String? = null,
)

data class PublicSupplementDto(
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
    @JsonProperty("created_at") val createdAt: Instant,
)

data class SupplementSuggestionAdminDto(
    val id: UUID,
    @JsonProperty("proposer_id") val proposerId: UUID,
    @JsonProperty("proposer_email") val proposerEmail: String?,
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
    val status: String,
    @JsonProperty("reviewer_id") val reviewerId: UUID?,
    @JsonProperty("reviewed_at") val reviewedAt: Instant?,
    @JsonProperty("review_note") val reviewNote: String?,
    @JsonProperty("public_id") val publicId: UUID?,
    @JsonProperty("created_at") val createdAt: Instant,
)

data class SupplementSuggestionCreatedResponse(
    val id: UUID,
    val status: String,
)
