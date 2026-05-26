package de.healthforge.community

import de.healthforge.auth.UserRepository
import de.healthforge.common.ApiException
import de.healthforge.recipe.RecipeRepo
import de.healthforge.recipe.RecipeStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ReportService(
    private val reportRepo: RecipeReportRepository,
    private val recipeRepo: RecipeRepo,
    private val userRepo: UserRepository,
) {

    @Transactional
    fun createReport(recipeId: UUID, reporterId: UUID, reason: String): UUID {
        val recipe = recipeRepo.findById(recipeId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $recipeId not found")
        }
        if (recipe.status == RecipeStatus.REMOVED.name) {
            throw ApiException(HttpStatus.GONE, "RECIPE_REMOVED", "Recipe already removed")
        }
        if (recipe.authorId == reporterId) {
            throw ApiException(HttpStatus.BAD_REQUEST, "SELF_REPORT", "Cannot report own recipe")
        }
        if (reportRepo.countOpenByRecipeAndReporter(recipeId, reporterId) > 0) {
            throw ApiException(HttpStatus.CONFLICT, "ALREADY_REPORTED", "Open report from this user already exists")
        }
        val trimmed = reason.trim()
        if (trimmed.isBlank()) {
            throw ApiException(HttpStatus.BAD_REQUEST, "REASON_BLANK", "reason required")
        }
        val saved = reportRepo.save(
            RecipeReportEntity(
                recipeId = recipeId,
                reporterId = reporterId,
                reason = trimmed,
            ),
        )
        return saved.id
    }

    @Transactional(readOnly = true)
    fun listReports(onlyOpen: Boolean): List<ReportAdminDto> {
        val rows = if (onlyOpen) {
            reportRepo.findAllByStatusOrderByCreatedAtAsc(RecipeReportStatus.OPEN.name)
        } else {
            reportRepo.findAllByOrderByCreatedAtDesc()
        }
        if (rows.isEmpty()) return emptyList()
        val recipeIds = rows.map { it.recipeId }.toSet()
        val reporterIds = rows.map { it.reporterId }.toSet()
        val recipes = recipeRepo.findAllById(recipeIds).associateBy { it.id }
        val reporters = userRepo.findAllById(reporterIds).associateBy { it.id }
        return rows.map { r ->
            val rec = recipes[r.recipeId]
            val rep = reporters[r.reporterId]
            ReportAdminDto(
                id = r.id,
                recipeId = r.recipeId,
                recipeTitle = rec?.title,
                recipeStatus = rec?.status,
                reporterId = r.reporterId,
                reporterEmail = rep?.email,
                reason = r.reason,
                status = r.status,
                resolvedBy = r.resolvedBy,
                resolvedAt = r.resolvedAt,
                createdAt = r.createdAt,
            )
        }
    }

    @Transactional
    fun resolve(id: UUID, adminId: UUID) = transitionStatus(id, adminId, RecipeReportStatus.RESOLVED)

    @Transactional
    fun dismiss(id: UUID, adminId: UUID) = transitionStatus(id, adminId, RecipeReportStatus.DISMISSED)

    private fun transitionStatus(id: UUID, adminId: UUID, newStatus: RecipeReportStatus) {
        val r = reportRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Report $id not found")
        }
        if (r.status != RecipeReportStatus.OPEN.name) {
            throw ApiException(HttpStatus.CONFLICT, "REPORT_NOT_OPEN", "Report already ${r.status}")
        }
        r.status = newStatus.name
        r.resolvedBy = adminId
        r.resolvedAt = Instant.now()
        reportRepo.save(r)
    }

    @Transactional
    fun adminRemoveRecipe(recipeId: UUID, adminId: UUID) {
        val recipe = recipeRepo.findById(recipeId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Recipe $recipeId not found")
        }
        recipe.status = RecipeStatus.REMOVED.name
        recipeRepo.save(recipe)
        // Auto-resolve all open reports for this recipe
        val open = reportRepo.findAllByStatusOrderByCreatedAtAsc(RecipeReportStatus.OPEN.name)
            .filter { it.recipeId == recipeId }
        val now = Instant.now()
        open.forEach {
            it.status = RecipeReportStatus.RESOLVED.name
            it.resolvedBy = adminId
            it.resolvedAt = now
        }
        if (open.isNotEmpty()) reportRepo.saveAll(open)
    }
}
