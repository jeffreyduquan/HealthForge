package de.healthforge.ingredient

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/ingredients")
class IngredientController(
    private val repo: IngredientRepository,
    private val search: IngredientSearchRepository,
    private val submissions: IngredientSubmissionService,
) {

    /** REQ-INGR-002 / REQ-INGR-USER-002: full-text search; eigene PENDING-Einträge bleiben sichtbar. */
    @GetMapping
    fun search(
        @RequestParam("q", required = false, defaultValue = "") query: String,
        @RequestParam("limit", required = false, defaultValue = "20") limit: Int,
        @RequestParam("excludeAllergens", required = false) excludeAllergens: List<String>? = null,
        @RequestParam("excludeFodmap", required = false) excludeFodmap: List<String>? = null,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): List<IngredientDto> = search.search(
        query = query,
        limit = limit,
        excludeAllergens = excludeAllergens.orEmpty(),
        excludeFodmap = excludeFodmap.orEmpty(),
        viewerId = principal?.userId,
    ).map(IngredientDto::from)

    @GetMapping("/{id}")
    fun byId(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<IngredientDto> =
        repo.findById(id)
            .filter { isVisible(it, principal?.userId) }
            .map { ResponseEntity.ok(IngredientDto.from(it)) }
            .orElseGet { ResponseEntity.notFound().build() }

    @GetMapping("/by-barcode/{barcode}")
    fun byBarcode(
        @PathVariable barcode: String,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<IngredientDto> =
        repo.findByBarcode(barcode)
            .filter { isVisible(it, principal?.userId) }
            .map { ResponseEntity.ok(IngredientDto.from(it)) }
            .orElseGet { ResponseEntity.notFound().build() }

    /** REQ-INGR-USER-001 — User schlägt neues Ingredient vor; bleibt PENDING bis Admin reviewt. */
    @PostMapping("/suggest")
    fun suggest(
        @Valid @RequestBody input: IngredientSuggestionInput,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<IngredientSuggestionCreatedResponse> {
        val p = requirePrincipal(principal)
        val id = submissions.suggest(p.userId, input)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(IngredientSuggestionCreatedResponse(id, IngredientStatus.PENDING.name))
    }

    /** REQ-FIELDPR-001 — User schlägt Korrektur eines Feldes vor. */
    @PostMapping("/{id}/field-pr")
    fun proposeFieldChange(
        @PathVariable id: UUID,
        @Valid @RequestBody input: FieldPrInput,
        @AuthenticationPrincipal principal: AuthPrincipal?,
    ): ResponseEntity<Map<String, Any>> {
        val p = requirePrincipal(principal)
        val prId = submissions.proposeFieldChange(p.userId, id, input)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapOf("id" to prId, "status" to IngredientStatus.PENDING.name))
    }

    private fun requirePrincipal(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    private fun isVisible(e: IngredientEntity, viewerId: UUID?): Boolean = when (e.status) {
        IngredientStatus.APPROVED.name -> true
        IngredientStatus.PENDING.name -> viewerId != null && viewerId == e.submittedBy
        else -> false
    }
}
