package de.healthforge.auth

import de.healthforge.common.ApiException
import de.healthforge.common.AuditLogService
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userRepo: UserRepository,
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest, request: HttpServletRequest): AuthResponse =
        authService.register(req, request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest, request: HttpServletRequest): AuthResponse =
        authService.login(req, request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: RefreshRequest, request: HttpServletRequest): AuthResponse =
        authService.refresh(req, request)

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody req: LogoutRequest): ResponseEntity<Void> {
        authService.logout(req)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/verify-email")
    fun verifyEmailGet(@RequestParam token: String): ResponseEntity<String> {
        authService.verifyEmail(token)
        return ResponseEntity.ok("E-Mail bestätigt. Du kannst dich jetzt in der HealthForge-App anmelden.")
    }

    @PostMapping("/verify-email")
    fun verifyEmailPost(@Valid @RequestBody req: VerifyEmailRequest): ResponseEntity<Void> {
        authService.verifyEmail(req.token)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/request-password-reset")
    fun requestReset(@Valid @RequestBody req: RequestPasswordResetRequest): ResponseEntity<Void> {
        authService.requestPasswordReset(req.email)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password-reset")
    fun resetPassword(@Valid @RequestBody req: PasswordResetRequest): ResponseEntity<Void> {
        authService.resetPassword(req.token, req.newPassword)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: Any?): UserDto {
        if (principal !is AuthPrincipal) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Not logged in")
        }
        return userRepo.findById(principal.userId)
            .map { it.toDto() }
            .orElseThrow { ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found") }
    }
}
