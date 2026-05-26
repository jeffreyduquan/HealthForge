package de.healthforge.community

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RecipeReportRepository : JpaRepository<RecipeReportEntity, UUID> {
    fun findAllByStatusOrderByCreatedAtAsc(status: String): List<RecipeReportEntity>
    fun findAllByOrderByCreatedAtDesc(): List<RecipeReportEntity>
    fun countByStatus(status: String): Long

    @Query("SELECT COUNT(r) FROM RecipeReportEntity r WHERE r.recipeId = :recipeId AND r.reporterId = :reporterId AND r.status = 'OPEN'")
    fun countOpenByRecipeAndReporter(recipeId: UUID, reporterId: UUID): Long
}
