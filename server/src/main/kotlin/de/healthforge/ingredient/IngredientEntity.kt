package de.healthforge.ingredient

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class IngredientSource { BLS, SIGHI, OFF, USER, MANUAL, USDA_FDC }

enum class IngredientStatus { PENDING, APPROVED, REJECTED }

@Entity
@Table(name = "ingredients")
class IngredientEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "name_de", nullable = false)
    var nameDe: String,

    @Column(name = "brand")
    var brand: String? = null,

    @Column(name = "barcode")
    var barcode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    var source: IngredientSource,

    @Column(name = "source_id")
    var sourceId: String? = null,

    @Column(name = "energy_kcal_per_100g")
    var energyKcalPer100g: BigDecimal? = null,

    @Column(name = "protein_g_per_100g")
    var proteinGPer100g: BigDecimal? = null,

    @Column(name = "carbs_g_per_100g")
    var carbsGPer100g: BigDecimal? = null,

    @Column(name = "sugar_g_per_100g")
    var sugarGPer100g: BigDecimal? = null,

    @Column(name = "fat_g_per_100g")
    var fatGPer100g: BigDecimal? = null,

    @Column(name = "satfat_g_per_100g")
    var satfatGPer100g: BigDecimal? = null,

    @Column(name = "fiber_g_per_100g")
    var fiberGPer100g: BigDecimal? = null,

    @Column(name = "salt_g_per_100g")
    var saltGPer100g: BigDecimal? = null,

    @Column(name = "histamine_score")
    var histamineScore: Short? = null,

    @Column(name = "allergens_json", nullable = false)
    var allergensJson: String = "[]",

    @Column(name = "fodmap_flags_json", nullable = false)
    var fodmapFlagsJson: String = "[]",

    @Column(name = "micronutrients_json", nullable = false, columnDefinition = "jsonb")
    var micronutrientsJson: String = "{}",

    @Column(name = "fdc_id")
    var fdcId: Long? = null,

    @Column(name = "locked", nullable = false)
    var locked: Boolean = true,

    @Column(name = "status", nullable = false)
    var status: String = IngredientStatus.APPROVED.name,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "submitted_by", columnDefinition = "uuid")
    var submittedBy: UUID? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "reviewer_id", columnDefinition = "uuid")
    var reviewerId: UUID? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_note")
    var reviewNote: String? = null,

    @Column(name = "last_admin_edit_at")
    var lastAdminEditAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
