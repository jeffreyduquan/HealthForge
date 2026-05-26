package de.healthforge.ingredient

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/ingredients")
class IngredientController(
    private val repo: IngredientRepository,
    private val search: IngredientSearchRepository,
) {

    /** REQ-INGR-002: full-text search with optional allergen / FODMAP exclusion filters. */
    @GetMapping
    fun search(
        @RequestParam("q") query: String,
        @RequestParam("limit", required = false, defaultValue = "20") limit: Int,
        @RequestParam("excludeAllergens", required = false) excludeAllergens: List<String>? = null,
        @RequestParam("excludeFodmap", required = false) excludeFodmap: List<String>? = null,
    ): List<IngredientDto> = search.search(
        query = query,
        limit = limit,
        excludeAllergens = excludeAllergens.orEmpty(),
        excludeFodmap = excludeFodmap.orEmpty(),
    ).map(IngredientDto::from)

    @GetMapping("/{id}")
    fun byId(@PathVariable id: UUID): ResponseEntity<IngredientDto> =
        repo.findById(id)
            .map { ResponseEntity.ok(IngredientDto.from(it)) }
            .orElseGet { ResponseEntity.notFound().build() }

    @GetMapping("/by-barcode/{barcode}")
    fun byBarcode(@PathVariable barcode: String): ResponseEntity<IngredientDto> =
        repo.findByBarcode(barcode)
            .map { ResponseEntity.ok(IngredientDto.from(it)) }
            .orElseGet { ResponseEntity.notFound().build() }
}
