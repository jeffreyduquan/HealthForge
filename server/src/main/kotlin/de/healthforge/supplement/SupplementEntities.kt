package de.healthforge.supplement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

enum class SupplementSuggestionStatus { PENDING, APPROVED, REJECTED }

@Entity
@Table(name = "supplements_public")
class PublicSupplementEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "name_de", nullable = false)
    var nameDe: String,

    @Column(name = "brand")
    var brand: String? = null,

    @Column(name = "unit_label", nullable = false)
    var unitLabel: String,

    @Column(name = "default_dose", nullable = false)
    var defaultDose: Double,

    @Column(name = "kcal_per_dose")
    var kcalPerDose: Double? = null,

    @Column(name = "protein_per_dose")
    var proteinPerDose: Double? = null,

    @Column(name = "carbs_per_dose")
    var carbsPerDose: Double? = null,

    @Column(name = "fat_per_dose")
    var fatPerDose: Double? = null,

    @Column(name = "micronutrients_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var micronutrientsJson: String? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "supplement_suggestions")
class SupplementSuggestionEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "proposer_id", nullable = false, columnDefinition = "uuid")
    var proposerId: UUID,

    @Column(name = "name_de", nullable = false)
    var nameDe: String,

    @Column(name = "brand")
    var brand: String? = null,

    @Column(name = "unit_label", nullable = false)
    var unitLabel: String,

    @Column(name = "default_dose", nullable = false)
    var defaultDose: Double,

    @Column(name = "kcal_per_dose")
    var kcalPerDose: Double? = null,

    @Column(name = "protein_per_dose")
    var proteinPerDose: Double? = null,

    @Column(name = "carbs_per_dose")
    var carbsPerDose: Double? = null,

    @Column(name = "fat_per_dose")
    var fatPerDose: Double? = null,

    @Column(name = "micronutrients_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var micronutrientsJson: String? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @Column(name = "status", nullable = false)
    var status: String = SupplementSuggestionStatus.PENDING.name,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "reviewer_id", columnDefinition = "uuid")
    var reviewerId: UUID? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_note")
    var reviewNote: String? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "public_id", columnDefinition = "uuid")
    var publicId: UUID? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
