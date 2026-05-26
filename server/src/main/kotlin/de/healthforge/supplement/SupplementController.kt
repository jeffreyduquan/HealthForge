package de.healthforge.supplement

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public-facing supplement endpoints (REQ-SUPP-004).
 *
 *  - `GET  /v1/supplements/public`              — globaler Katalog (Lese-Zugriff)
 *  - `POST /v1/supplements/suggestions`         — User schlägt neuen Eintrag vor
 */
@RestController
@RequestMapping("/v1/supplements")
class SupplementController(
    private val service: SupplementService,
) {

    @GetMapping("/public")
    fun listPublic(): List<PublicSupplementDto> = service.listPublic()

    @PostMapping("/suggestions")
    fun suggest(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @Valid @RequestBody body: SupplementInput,
    ): ResponseEntity<SupplementSuggestionCreatedResponse> {
        val p = principal ?: throw ApiException(
            HttpStatus.UNAUTHORIZED,
            "NO_PRINCIPAL",
            "authentication required",
        )
        val id = service.suggest(p.userId, body)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SupplementSuggestionCreatedResponse(id = id, status = SupplementSuggestionStatus.PENDING.name),
        )
    }
}
