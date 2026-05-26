package de.healthforge.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * JWT-Issuer / Verifier — LOCKED Q6: HS512 mit Symmetric Key.
 * Access-Token TTL = 15 min, Refresh-Token TTL = 30 d, mit Rotation bei jedem Refresh.
 */
@Component
class JwtService(
    @Value("\${healthforge.jwt.secret}") private val secret: String,
    @Value("\${healthforge.jwt.access-token-ttl-minutes}") private val accessTtlMinutes: Long,
    @Value("\${healthforge.jwt.refresh-token-ttl-days}") private val refreshTtlDays: Long,
) {
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        require(secret.toByteArray(Charsets.UTF_8).size >= 64) {
            "JWT secret must be at least 64 bytes for HS512"
        }
        key = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    }

    fun issueAccessToken(user: UserEntity): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTtlMinutes * 60)))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun issueRefreshTokenPlain(): String = UUID.randomUUID().toString() + "." + UUID.randomUUID().toString()

    fun hashRefreshToken(plain: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(plain.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun accessTokenTtlSeconds(): Long = accessTtlMinutes * 60

    fun refreshTokenExpiresAt(): Instant = Instant.now().plusSeconds(refreshTtlDays * 86_400)

    fun parseAndValidate(token: String): ParsedToken {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return ParsedToken(
            userId = UUID.fromString(claims.subject),
            email = claims["email"] as String,
            role = UserRole.valueOf(claims["role"] as String),
            expiresAt = claims.expiration.toInstant(),
        )
    }

    data class ParsedToken(
        val userId: UUID,
        val email: String,
        val role: UserRole,
        val expiresAt: Instant,
    )
}
