package de.healthforge.community

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateReportRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 500)
    val reason: String,
)

data class ReportAdminDto(
    val id: UUID,
    val recipeId: UUID,
    val recipeTitle: String?,
    val recipeStatus: String?,
    val reporterId: UUID,
    val reporterEmail: String?,
    val reason: String,
    val status: String,
    val resolvedBy: UUID?,
    val resolvedAt: Instant?,
    val createdAt: Instant,
)
