package de.healthforge.export

import com.fasterxml.jackson.databind.ObjectMapper
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import de.healthforge.auth.UserRepository
import de.healthforge.common.ApiException
import de.healthforge.recipe.RecipeRepo
import de.healthforge.recipe.RecipeStatus
import de.healthforge.supplement.SupplementSuggestionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ExportService(
    private val userRepo: UserRepository,
    private val recipeRepo: RecipeRepo,
    private val suggestionRepo: SupplementSuggestionRepository,
    private val objectMapper: ObjectMapper,
) {

    @Transactional(readOnly = true)
    fun buildPayload(userId: UUID): ServerExportPayload {
        val user = userRepo.findById(userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User $userId not found")
        }
        val recipes = recipeRepo.findAllByAuthorIdAndStatusOrderByCreatedAtDesc(userId, RecipeStatus.PUBLISHED.name)
        val suggestions = suggestionRepo.findAllByProposerIdOrderByCreatedAtDesc(userId)
        return ServerExportPayload(
            generatedAt = Instant.now(),
            account = AccountSection(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                role = user.role.name,
                status = user.status.name,
                emailVerifiedAt = user.emailVerifiedAt,
                createdAt = user.createdAt,
                lastLoginAt = user.lastLoginAt,
            ),
            ownedRecipes = recipes.map { r ->
                OwnedRecipe(
                    id = r.id,
                    title = r.title,
                    description = r.description,
                    visibility = r.visibility,
                    status = r.status,
                    servings = r.servings,
                    prepMinutes = r.prepMinutes,
                    cookMinutes = r.cookMinutes,
                    slotTags = r.slotTags.toList(),
                    createdAt = r.createdAt,
                )
            },
            supplementSuggestions = suggestions.map { s ->
                SupplementSuggestionLine(
                    id = s.id,
                    nameDe = s.nameDe,
                    brand = s.brand,
                    unitLabel = s.unitLabel,
                    defaultDose = s.defaultDose,
                    status = s.status,
                    createdAt = s.createdAt,
                    reviewedAt = s.reviewedAt,
                    reviewNote = s.reviewNote,
                )
            },
        )
    }

    fun toJson(payload: ServerExportPayload): ByteArray =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload)

    fun toPdf(payload: ServerExportPayload): ByteArray {
        val out = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 48f, 48f)
        PdfWriter.getInstance(document, out)
        document.open()

        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f, Font.BOLD)
        val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, Font.BOLD)
        val bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10f)
        val labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.BOLD)

        document.add(Paragraph("HealthForge — Datenexport", titleFont))
        document.add(Paragraph("Generiert am: ${humanInstant(payload.generatedAt)}", bodyFont))
        document.add(Paragraph("Schema: ${payload.schema}", bodyFont))
        document.add(Paragraph(" ", bodyFont))

        // Account
        document.add(Paragraph("Konto", sectionFont))
        val accountTable = PdfPTable(2).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(1f, 3f))
        }
        fun row(label: String, value: String) {
            accountTable.addCell(PdfPCell(Phrase(label, labelFont)).apply { border = 0 })
            accountTable.addCell(PdfPCell(Phrase(value, bodyFont)).apply { border = 0 })
        }
        val a = payload.account
        row("ID", a.id.toString())
        row("E-Mail", a.email)
        row("Anzeigename", a.displayName)
        row("Rolle", a.role)
        row("Status", a.status)
        row("Registriert", humanInstant(a.createdAt))
        row("E-Mail verifiziert", a.emailVerifiedAt?.let(::humanInstant) ?: "—")
        row("Letzter Login", a.lastLoginAt?.let(::humanInstant) ?: "—")
        document.add(accountTable)
        document.add(Paragraph(" ", bodyFont))

        // Recipes
        document.add(Paragraph("Eigene Rezepte (${payload.ownedRecipes.size})", sectionFont))
        if (payload.ownedRecipes.isEmpty()) {
            document.add(Paragraph("Keine eigenen Rezepte.", bodyFont))
        } else {
            val table = PdfPTable(4).apply {
                widthPercentage = 100f
                setWidths(floatArrayOf(3f, 1.2f, 1.2f, 1.6f))
            }
            listOf("Titel", "Sichtbarkeit", "Portionen", "Erstellt").forEach { h ->
                table.addCell(PdfPCell(Phrase(h, labelFont)).apply {
                    horizontalAlignment = Element.ALIGN_LEFT
                })
            }
            payload.ownedRecipes.forEach { r ->
                table.addCell(Phrase(r.title, bodyFont))
                table.addCell(Phrase(r.visibility, bodyFont))
                table.addCell(Phrase(r.servings.toString(), bodyFont))
                table.addCell(Phrase(humanInstant(r.createdAt), bodyFont))
            }
            document.add(table)
        }
        document.add(Paragraph(" ", bodyFont))

        // Supplement suggestions
        document.add(Paragraph("Supplement-Vorschläge (${payload.supplementSuggestions.size})", sectionFont))
        if (payload.supplementSuggestions.isEmpty()) {
            document.add(Paragraph("Keine Vorschläge eingereicht.", bodyFont))
        } else {
            val table = PdfPTable(4).apply {
                widthPercentage = 100f
                setWidths(floatArrayOf(2.5f, 1f, 1.2f, 1.6f))
            }
            listOf("Name", "Dosis", "Status", "Eingereicht").forEach { h ->
                table.addCell(PdfPCell(Phrase(h, labelFont)))
            }
            payload.supplementSuggestions.forEach { s ->
                val name = listOfNotNull(s.nameDe, s.brand?.let { "($it)" }).joinToString(" ")
                table.addCell(Phrase(name, bodyFont))
                table.addCell(Phrase("${s.defaultDose} ${s.unitLabel}", bodyFont))
                table.addCell(Phrase(s.status, bodyFont))
                table.addCell(Phrase(humanInstant(s.createdAt), bodyFont))
            }
            document.add(table)
        }

        document.close()
        return out.toByteArray()
    }

    private fun humanInstant(t: Instant): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Europe/Berlin"))
            .format(t)
}
