package de.healthforge.auth

import de.healthforge.common.ApiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class AdminUserDto(
    val id: UUID,
    val email: String,
    val displayName: String,
    val role: String,
    val status: String,
    val emailVerifiedAt: Instant?,
    val lastLoginAt: Instant?,
    val createdAt: Instant,
)

@RestController
@RequestMapping("/admin/v1/users")
@PreAuthorize("hasRole('ADMIN')")
class AdminUserController(
    private val userRepo: UserRepository,
    private val refreshTokenRepo: RefreshTokenRepository,
) {
    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping
    @Transactional(readOnly = true)
    fun list(@AuthenticationPrincipal principal: AuthPrincipal?): List<AdminUserDto> {
        require(principal)
        return userRepo.findAll().map { u ->
            AdminUserDto(
                id = u.id,
                email = u.email,
                displayName = u.displayName,
                role = u.role.name,
                status = u.status.name,
                emailVerifiedAt = u.emailVerifiedAt,
                lastLoginAt = u.lastLoginAt,
                createdAt = u.createdAt,
            )
        }.sortedByDescending { it.createdAt }
    }

    @PostMapping("/{id}/ban")
    @Transactional
    fun ban(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        if (p.userId == id) throw ApiException(HttpStatus.BAD_REQUEST, "SELF_BAN", "Cannot ban yourself")
        val u = userRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User $id not found")
        }
        if (u.role == UserRole.ADMIN) {
            throw ApiException(HttpStatus.FORBIDDEN, "CANNOT_BAN_ADMIN", "Admins cannot be banned via API")
        }
        u.status = UserStatus.BANNED
        u.updatedAt = Instant.now()
        userRepo.save(u)
        refreshTokenRepo.revokeAllForUser(id, Instant.now())
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/unban")
    @Transactional
    fun unban(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        require(principal)
        val u = userRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User $id not found")
        }
        if (u.status != UserStatus.BANNED) {
            throw ApiException(HttpStatus.CONFLICT, "NOT_BANNED", "User is not banned (status=${u.status})")
        }
        u.status = UserStatus.ACTIVE
        u.updatedAt = Instant.now()
        userRepo.save(u)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun softDelete(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        if (p.userId == id) throw ApiException(HttpStatus.BAD_REQUEST, "SELF_DELETE", "Cannot delete yourself")
        val u = userRepo.findById(id).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User $id not found")
        }
        if (u.role == UserRole.ADMIN) {
            throw ApiException(HttpStatus.FORBIDDEN, "CANNOT_DELETE_ADMIN", "Admins cannot be deleted via API")
        }
        u.status = UserStatus.DELETED
        u.updatedAt = Instant.now()
        userRepo.save(u)
        refreshTokenRepo.revokeAllForUser(id, Instant.now())
        return ResponseEntity.noContent().build()
    }
}
