package de.healthforge.auth

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

enum class UserRole { USER, ADMIN }

enum class UserStatus { PENDING_VERIFICATION, ACTIVE, BANNED, DELETED }

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.PENDING_VERIFICATION,

    @Column(name = "email_verified_at")
    var emailVerifiedAt: Instant? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "invites")
class InviteEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var code: String,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID? = null,

    var note: String? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "used_by", columnDefinition = "uuid")
    var usedBy: UUID? = null,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "device_label")
    var deviceLabel: String? = null,

    @Column(name = "issued_at", nullable = false)
    var issuedAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "replaced_by", columnDefinition = "uuid")
    var replacedBy: UUID? = null,

    @Column(name = "ip_address")
    var ipAddress: String? = null,

    @Column(name = "user_agent")
    var userAgent: String? = null,
)

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationTokenEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
