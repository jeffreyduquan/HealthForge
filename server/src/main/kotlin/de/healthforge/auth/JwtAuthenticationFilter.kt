package de.healthforge.auth

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

data class AuthPrincipal(
    val userId: UUID,
    val email: String,
    val role: UserRole,
)

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val header = req.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)
            try {
                val parsed = jwtService.parseAndValidate(token)
                val principal = AuthPrincipal(parsed.userId, parsed.email, parsed.role)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_${parsed.role.name}"))
                val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                auth.details = WebAuthenticationDetailsSource().buildDetails(req)
                SecurityContextHolder.getContext().authentication = auth
            } catch (e: JwtException) {
                log.debug("JWT validation failed: {}", e.message)
                SecurityContextHolder.clearContext()
            } catch (e: Exception) {
                log.warn("Unexpected error parsing JWT", e)
                SecurityContextHolder.clearContext()
            }
        }
        chain.doFilter(req, res)
    }
}
