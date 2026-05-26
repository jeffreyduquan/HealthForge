package de.healthforge.supplement

import de.healthforge.auth.UserRepository
import de.healthforge.common.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SupplementService(
    private val publicRepo: PublicSupplementRepository,
    private val suggestionRepo: SupplementSuggestionRepository,
    private val userRepo: UserRepository,
) {

    // ---- public catalog ----

    @Transactional(readOnly = true)
    fun listPublic(): List<PublicSupplementDto> =
        publicRepo.findAllByOrderByNameDeAsc().map(::toPublicDto)

    // ---- user suggestion ----

    @Transactional
    fun suggest(proposerId: UUID, input: SupplementInput): UUID {
        val name = input.nameDe.trim()
        val unit = input.unitLabel.trim()
        if (name.isBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "NAME_BLANK", "name_de required")
        if (unit.isBlank()) throw ApiException(HttpStatus.BAD_REQUEST, "UNIT_BLANK", "unit_label required")
        if (input.defaultDose <= 0.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "DOSE_INVALID", "default_dose must be > 0")
        }
        val saved = suggestionRepo.save(
            SupplementSuggestionEntity(
                proposerId = proposerId,
                nameDe = name,
                brand = input.brand?.trim()?.ifBlank { null },
                unitLabel = unit,
                defaultDose = input.defaultDose,
                kcalPerDose = input.kcalPerDose,
                proteinPerDose = input.proteinPerDose,
                carbsPerDose = input.carbsPerDose,
                fatPerDose = input.fatPerDose,
                micronutrientsJson = input.micronutrientsJson,
                notes = input.notes,
            ),
        )
        return saved.id
    }

    // ---- admin queue ----

    @Transactional(readOnly = true)
    fun listSuggestions(onlyPending: Boolean): List<SupplementSuggestionAdminDto> {
        val rows = if (onlyPending) {
            suggestionRepo.findAllByStatusOrderByCreatedAtAsc(SupplementSuggestionStatus.PENDING.name)
        } else {
            suggestionRepo.findAllByOrderByCreatedAtDesc()
        }
        if (rows.isEmpty()) return emptyList()
        val proposers = userRepo.findAllById(rows.map { it.proposerId }.toSet()).associateBy { it.id }
        return rows.map { s ->
            SupplementSuggestionAdminDto(
                id = s.id,
                proposerId = s.proposerId,
                proposerEmail = proposers[s.proposerId]?.email,
                nameDe = s.nameDe,
                brand = s.brand,
                unitLabel = s.unitLabel,
                defaultDose = s.defaultDose,
                kcalPerDose = s.kcalPerDose,
                proteinPerDose = s.proteinPerDose,
                carbsPerDose = s.carbsPerDose,
                fatPerDose = s.fatPerDose,
                micronutrientsJson = s.micronutrientsJson,
                notes = s.notes,
                status = s.status,
                reviewerId = s.reviewerId,
                reviewedAt = s.reviewedAt,
                reviewNote = s.reviewNote,
                publicId = s.publicId,
                createdAt = s.createdAt,
            )
        }
    }

    @Transactional
    fun approve(id: UUID, adminId: UUID): UUID {
        val s = loadPending(id)
        val pub = publicRepo.save(
            PublicSupplementEntity(
                nameDe = s.nameDe,
                brand = s.brand,
                unitLabel = s.unitLabel,
                defaultDose = s.defaultDose,
                kcalPerDose = s.kcalPerDose,
                proteinPerDose = s.proteinPerDose,
                carbsPerDose = s.carbsPerDose,
                fatPerDose = s.fatPerDose,
                micronutrientsJson = s.micronutrientsJson,
                notes = s.notes,
                createdBy = s.proposerId,
            ),
        )
        s.status = SupplementSuggestionStatus.APPROVED.name
        s.reviewerId = adminId
        s.reviewedAt = Instant.now()
        s.publicId = pub.id
        suggestionRepo.save(s)
        return pub.id
    }

    @Transactional
    fun reject(id: UUID, adminId: UUID, note: String?) {
        val s = loadPending(id)
        s.status = SupplementSuggestionStatus.REJECTED.name
        s.reviewerId = adminId
        s.reviewedAt = Instant.now()
        s.reviewNote = note?.trim()?.ifBlank { null }
        suggestionRepo.save(s)
    }

    private fun loadPending(id: UUID): SupplementSuggestionEntity {
        val s = suggestionRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "SUGGESTION_NOT_FOUND", "Suggestion $id not found")
        }
        if (s.status != SupplementSuggestionStatus.PENDING.name) {
            throw ApiException(HttpStatus.CONFLICT, "SUGGESTION_NOT_PENDING", "Suggestion already ${s.status}")
        }
        return s
    }

    private fun toPublicDto(e: PublicSupplementEntity) = PublicSupplementDto(
        id = e.id,
        nameDe = e.nameDe,
        brand = e.brand,
        unitLabel = e.unitLabel,
        defaultDose = e.defaultDose,
        kcalPerDose = e.kcalPerDose,
        proteinPerDose = e.proteinPerDose,
        carbsPerDose = e.carbsPerDose,
        fatPerDose = e.fatPerDose,
        micronutrientsJson = e.micronutrientsJson,
        notes = e.notes,
        createdAt = e.createdAt,
    )
}
