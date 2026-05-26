package de.healthforge.auth

import de.healthforge.common.ActorKind
import de.healthforge.common.ApiException
import de.healthforge.common.AuditLogService
import de.healthforge.common.MailService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class AuthService(
    private val userRepo: UserRepository,
    private val inviteRepo: InviteRepository,
    private val refreshRepo: RefreshTokenRepository,
    private val emailVerifyRepo: EmailVerificationTokenRepository,
    private val passwordResetRepo: PasswordResetTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService,
    private val auditService: AuditLogService,
    @Value("\${healthforge.invite.require-invite}") private val requireInvite: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(req: RegisterRequest, request: HttpServletRequest): AuthResponse {
        if (userRepo.existsByEmail(req.email.lowercase())) {
            throw ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email already registered")
        }
        if (requireInvite) {
            val invite = inviteRepo.findByCode(req.inviteCode).orElseThrow {
                ApiException(HttpStatus.FORBIDDEN, "INVALID_INVITE", "Invite code invalid")
            }
            if (invite.usedAt != null) {
                throw ApiException(HttpStatus.FORBIDDEN, "INVITE_USED", "Invite already redeemed")
            }
            if (invite.expiresAt.isBefore(Instant.now())) {
                throw ApiException(HttpStatus.FORBIDDEN, "INVITE_EXPIRED", "Invite expired")
            }
            val user = createUser(req)
            invite.usedAt = Instant.now()
            invite.usedBy = user.id
            inviteRepo.save(invite)
            issueVerificationEmail(user)
            auditService.record("AUTH_REGISTER", actorUserId = user.id, ipAddress = request.remoteAddr)
            return issueTokens(user, request)
        } else {
            val user = createUser(req)
            issueVerificationEmail(user)
            auditService.record("AUTH_REGISTER", actorUserId = user.id, ipAddress = request.remoteAddr)
            return issueTokens(user, request)
        }
    }

    private fun createUser(req: RegisterRequest): UserEntity {
        val user = UserEntity(
            email = req.email.lowercase(),
            displayName = req.displayName,
            passwordHash = passwordEncoder.encode(req.password),
            role = UserRole.USER,
            status = UserStatus.PENDING_VERIFICATION,
        )
        return userRepo.save(user)
    }

    private fun issueVerificationEmail(user: UserEntity) {
        val plain = randomToken()
        val token = EmailVerificationTokenEntity(
            userId = user.id,
            tokenHash = sha256(plain),
            expiresAt = Instant.now().plusSeconds(86_400),
        )
        emailVerifyRepo.save(token)
        // For dev: link points to backend; for prod: send a deep link to the app.
        val link = "https://api.healthforge.endgear.de/v1/auth/verify-email?token=$plain"
        mailService.sendVerificationEmail(user.email, link)
    }

    @Transactional
    fun login(req: LoginRequest, request: HttpServletRequest): AuthResponse {
        val user = userRepo.findByEmail(req.email.lowercase()).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password")
        }
        if (!passwordEncoder.matches(req.password, user.passwordHash)) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid email or password")
        }
        if (user.status == UserStatus.BANNED) {
            throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_BANNED", "Account banned")
        }
        if (user.status == UserStatus.DELETED) {
            throw ApiException(HttpStatus.GONE, "ACCOUNT_DELETED", "Account deleted")
        }
        user.lastLoginAt = Instant.now()
        userRepo.save(user)
        auditService.record("AUTH_LOGIN", actorUserId = user.id, ipAddress = request.remoteAddr)
        return issueTokens(user, request, deviceLabel = req.deviceLabel)
    }

    @Transactional
    fun refresh(req: RefreshRequest, request: HttpServletRequest): AuthResponse {
        val hash = sha256(req.refreshToken)
        val token = refreshRepo.findByTokenHashAndRevokedAtIsNull(hash).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH", "Refresh token invalid")
        }
        if (token.expiresAt.isBefore(Instant.now())) {
            throw ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRED", "Refresh token expired")
        }
        val user = userRepo.findById(token.userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found")
        }
        if (user.status != UserStatus.ACTIVE && user.status != UserStatus.PENDING_VERIFICATION) {
            throw ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Account inactive")
        }
        // Rotation: revoke old, issue new
        token.revokedAt = Instant.now()
        val newAuth = issueTokens(user, request, deviceLabel = token.deviceLabel)
        token.replacedBy = refreshRepo.findByTokenHashAndRevokedAtIsNull(sha256(newAuth.refreshToken)).map { it.id }.orElse(null)
        refreshRepo.save(token)
        return newAuth
    }

    @Transactional
    fun logout(req: LogoutRequest) {
        val hash = sha256(req.refreshToken)
        refreshRepo.findByTokenHashAndRevokedAtIsNull(hash).ifPresent {
            it.revokedAt = Instant.now()
            refreshRepo.save(it)
        }
    }

    @Transactional
    fun verifyEmail(token: String) {
        val hash = sha256(token)
        val verification = emailVerifyRepo.findByTokenHashAndUsedAtIsNull(hash).orElseThrow {
            ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Invalid verification token")
        }
        if (verification.expiresAt.isBefore(Instant.now())) {
            throw ApiException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "Token expired")
        }
        val user = userRepo.findById(verification.userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found")
        }
        user.emailVerifiedAt = Instant.now()
        user.status = UserStatus.ACTIVE
        userRepo.save(user)
        verification.usedAt = Instant.now()
        emailVerifyRepo.save(verification)
        auditService.record("AUTH_VERIFY_EMAIL", actorUserId = user.id)
    }

    @Transactional
    fun requestPasswordReset(email: String) {
        val user = userRepo.findByEmail(email.lowercase()).orElse(null) ?: return // silent: don't reveal
        val plain = randomToken()
        val token = PasswordResetTokenEntity(
            userId = user.id,
            tokenHash = sha256(plain),
            expiresAt = Instant.now().plusSeconds(3_600),
        )
        passwordResetRepo.save(token)
        val link = "https://api.healthforge.endgear.de/v1/auth/password-reset?token=$plain"
        mailService.sendPasswordResetEmail(user.email, link)
        auditService.record("AUTH_REQUEST_PW_RESET", actorUserId = user.id)
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val hash = sha256(token)
        val reset = passwordResetRepo.findByTokenHashAndUsedAtIsNull(hash).orElseThrow {
            ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Invalid reset token")
        }
        if (reset.expiresAt.isBefore(Instant.now())) {
            throw ApiException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED", "Token expired")
        }
        val user = userRepo.findById(reset.userId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found")
        }
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepo.save(user)
        reset.usedAt = Instant.now()
        passwordResetRepo.save(reset)
        // Revoke all sessions on password change
        refreshRepo.revokeAllForUser(user.id, Instant.now())
        auditService.record("AUTH_PW_RESET", actorUserId = user.id)
    }

    private fun issueTokens(user: UserEntity, request: HttpServletRequest, deviceLabel: String? = null): AuthResponse {
        val access = jwtService.issueAccessToken(user)
        val refreshPlain = jwtService.issueRefreshTokenPlain()
        val refresh = RefreshTokenEntity(
            userId = user.id,
            tokenHash = sha256(refreshPlain),
            deviceLabel = deviceLabel,
            expiresAt = jwtService.refreshTokenExpiresAt(),
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent"),
        )
        refreshRepo.save(refresh)
        return AuthResponse(
            accessToken = access,
            refreshToken = refreshPlain,
            expiresInSeconds = jwtService.accessTokenTtlSeconds(),
            user = user.toDto(),
        )
    }

    private fun sha256(plain: String): String = jwtService.hashRefreshToken(plain)

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

@org.springframework.context.annotation.Configuration
class PasswordEncoderConfig {
    @org.springframework.context.annotation.Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
