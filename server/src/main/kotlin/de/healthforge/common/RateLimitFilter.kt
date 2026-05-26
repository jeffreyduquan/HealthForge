package de.healthforge.common

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory rate limiter for sensitive endpoints (login, register, password reset).
 * Per-IP buckets keyed on remote address. For multi-instance deploys this would need Redis,
 * but HealthForge is single-instance (LOCKED Q12 Caffeine in-process).
 */
@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val loginBuckets = ConcurrentHashMap<String, Bucket>()
    private val registerBuckets = ConcurrentHashMap<String, Bucket>()
    private val resetBuckets = ConcurrentHashMap<String, Bucket>()

    private fun bucket(map: ConcurrentHashMap<String, Bucket>, key: String, capacity: Long, periodMin: Long): Bucket =
        map.computeIfAbsent(key) {
            Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofMinutes(periodMin))))
                .build()
        }

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val ip = req.remoteAddr ?: "unknown"
        val path = req.requestURI
        val bucket = when {
            path.endsWith("/auth/login") -> bucket(loginBuckets, ip, 5, 1)
            path.endsWith("/auth/register") -> bucket(registerBuckets, ip, 3, 60)
            path.endsWith("/auth/password-reset") || path.endsWith("/auth/request-password-reset") ->
                bucket(resetBuckets, ip, 3, 60)
            else -> null
        }
        if (bucket != null && !bucket.tryConsume(1)) {
            res.status = HttpStatus.TOO_MANY_REQUESTS.value()
            res.contentType = "application/json"
            res.writer.write("""{"status":429,"errorCode":"RATE_LIMIT","message":"Too many requests"}""")
            return
        }
        chain.doFilter(req, res)
    }
}
