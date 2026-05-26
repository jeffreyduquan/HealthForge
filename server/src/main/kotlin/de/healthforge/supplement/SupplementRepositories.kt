package de.healthforge.supplement

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PublicSupplementRepository : JpaRepository<PublicSupplementEntity, UUID> {
    fun findAllByOrderByNameDeAsc(): List<PublicSupplementEntity>
}

@Repository
interface SupplementSuggestionRepository : JpaRepository<SupplementSuggestionEntity, UUID> {
    fun findAllByStatusOrderByCreatedAtAsc(status: String): List<SupplementSuggestionEntity>
    fun findAllByOrderByCreatedAtDesc(): List<SupplementSuggestionEntity>
    fun findAllByProposerIdOrderByCreatedAtDesc(proposerId: UUID): List<SupplementSuggestionEntity>
}
