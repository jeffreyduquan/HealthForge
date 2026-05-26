package de.healthforge.common

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Sanity endpoint for bootstrap smoke-test.
 * Will be removed or replaced once real endpoints land in P1.S2.
 */
@RestController
@RequestMapping("/v1")
class PingController {
    @GetMapping("/ping")
    fun ping(): Map<String, Any> = mapOf(
        "service" to "healthforge-server",
        "timestamp" to Instant.now().toString(),
        "ok" to true,
    )
}
