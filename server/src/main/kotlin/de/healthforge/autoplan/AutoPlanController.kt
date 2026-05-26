package de.healthforge.autoplan

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/plans")
class AutoPlanController(
    private val service: AutoPlanService,
) {

    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @PostMapping("/generate")
    fun generate(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @Valid @RequestBody body: AutoPlanGenerateRequest,
    ): AutoPlanGenerateResponse {
        val p = require(principal)
        return service.generate(body, p.userId)
    }
}
