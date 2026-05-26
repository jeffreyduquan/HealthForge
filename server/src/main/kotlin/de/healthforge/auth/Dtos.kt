package de.healthforge.auth

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// ============ Requests ============

data class RegisterRequest(
    @field:NotBlank
    val inviteCode: String,
    @field:Email @field:NotBlank
    val email: String,
    @field:NotBlank @field:Size(min = 1, max = 50)
    val displayName: String,
    @field:NotBlank @field:Size(min = 10, max = 128)
    val password: String,
)

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String,
    val deviceLabel: String? = null,
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String,
)

data class LogoutRequest(
    @field:NotBlank val refreshToken: String,
)

data class VerifyEmailRequest(
    @field:NotBlank val token: String,
)

data class RequestPasswordResetRequest(
    @field:Email @field:NotBlank val email: String,
)

data class PasswordResetRequest(
    @field:NotBlank val token: String,
    @field:NotBlank @field:Size(min = 10, max = 128) val newPassword: String,
)

// ============ Responses ============

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserDto,
)

data class UserDto(
    val id: UUID,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val status: UserStatus,
    val emailVerified: Boolean,
    val createdAt: Instant,
)

fun UserEntity.toDto() = UserDto(
    id = id,
    email = email,
    displayName = displayName,
    role = role,
    status = status,
    emailVerified = emailVerifiedAt != null,
    createdAt = createdAt,
)

// ============ Invite admin DTOs ============

data class CreateInviteRequest(
    val note: String? = null,
    val validDays: Int = 30,
)

data class InviteDto(
    val id: UUID,
    val code: String,
    val note: String?,
    val createdBy: UUID?,
    val usedBy: UUID?,
    val usedAt: Instant?,
    val expiresAt: Instant,
    val createdAt: Instant,
)

fun InviteEntity.toDto() = InviteDto(
    id = id,
    code = code,
    note = note,
    createdBy = createdBy,
    usedBy = usedBy,
    usedAt = usedAt,
    expiresAt = expiresAt,
    createdAt = createdAt,
)
