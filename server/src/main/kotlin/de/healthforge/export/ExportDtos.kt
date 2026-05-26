package de.healthforge.export

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

/**
 * Server-Side Export-Payload (REQ-EXPORT-001 / -003 / -004).
 *
 * Enthält genau das, was der Server über den User weiß. Lokale Daten
 * (Intake, Water, Symptome, Reminder) werden vom Android-Client separat
 * als zweites JSON daneben exportiert.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServerExportPayload(
    @JsonProperty("generated_at") val generatedAt: Instant,
    val schema: String = "healthforge.server-export.v1",
    val account: AccountSection,
    @JsonProperty("owned_recipes") val ownedRecipes: List<OwnedRecipe>,
    @JsonProperty("supplement_suggestions") val supplementSuggestions: List<SupplementSuggestionLine>,
)

data class AccountSection(
    val id: UUID,
    val email: String,
    @JsonProperty("display_name") val displayName: String,
    val role: String,
    val status: String,
    @JsonProperty("email_verified_at") val emailVerifiedAt: Instant?,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("last_login_at") val lastLoginAt: Instant?,
)

data class OwnedRecipe(
    val id: UUID,
    val title: String,
    val description: String?,
    val visibility: String,
    val status: String,
    val servings: Int,
    @JsonProperty("prep_minutes") val prepMinutes: Int,
    @JsonProperty("cook_minutes") val cookMinutes: Int?,
    @JsonProperty("slot_tags") val slotTags: List<String>,
    @JsonProperty("created_at") val createdAt: Instant,
)

data class SupplementSuggestionLine(
    val id: UUID,
    @JsonProperty("name_de") val nameDe: String,
    val brand: String?,
    @JsonProperty("unit_label") val unitLabel: String,
    @JsonProperty("default_dose") val defaultDose: Double,
    val status: String,
    @JsonProperty("created_at") val createdAt: Instant,
    @JsonProperty("reviewed_at") val reviewedAt: Instant?,
    @JsonProperty("review_note") val reviewNote: String?,
)
