package de.healthforge.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): Optional<UserEntity>
    fun existsByEmail(email: String): Boolean
}

interface InviteRepository : JpaRepository<InviteEntity, UUID> {
    fun findByCode(code: String): Optional<InviteEntity>
    fun findAllByOrderByCreatedAtDesc(): List<InviteEntity>
}

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHashAndRevokedAtIsNull(tokenHash: String): Optional<RefreshTokenEntity>
    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<RefreshTokenEntity>

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
        "UPDATE RefreshTokenEntity r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS NULL",
    )
    fun revokeAllForUser(userId: UUID, now: Instant): Int
}

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationTokenEntity, UUID> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): Optional<EmailVerificationTokenEntity>
}

interface PasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, UUID> {
    fun findByTokenHashAndUsedAtIsNull(tokenHash: String): Optional<PasswordResetTokenEntity>
}
