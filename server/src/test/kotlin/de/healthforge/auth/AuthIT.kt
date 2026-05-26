package de.healthforge.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

/**
 * P1.S2 Smoke-Test (LOCKED Q10 reopen). Happy path: register - login - me - refresh - logout.
 *
 * Requires an externally running Postgres on localhost:5435 with db=healthforge_test, user=test, pass=test.
 * Bootstrap via:
 *   docker run -d --rm --name healthforge-it-pg -p 5435:5432 \
 *     -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test -e POSTGRES_DB=healthforge_test postgres:16-alpine
 *
 * (Testcontainers proved incompatible with Docker Desktop npipe on this host.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:postgresql://localhost:5435/healthforge_test",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
    ],
)
class AuthIT {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var rest: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    @Test
    fun `register login me refresh logout happy path`() {
        val email = "smoke+${System.currentTimeMillis()}@healthforge.local"
        val password = "S3cret-password-123"

        // 1) Register
        val registerResp = rest.postForEntity(
            url("/v1/auth/register"),
            json(
                mapOf(
                    "inviteCode" to "IGNORED",
                    "email" to email,
                    "displayName" to "Smoke Tester",
                    "password" to password,
                ),
            ),
            Map::class.java,
        )
        assertThat(registerResp.statusCode.is2xxSuccessful).isTrue()
        assertThat(registerResp.body!!["accessToken"] as String).isNotBlank()
        assertThat(registerResp.body!!["refreshToken"] as String).isNotBlank()

        // 2) Login
        val loginResp = rest.postForEntity(
            url("/v1/auth/login"),
            json(mapOf("email" to email, "password" to password)),
            Map::class.java,
        )
        assertThat(loginResp.statusCode.is2xxSuccessful).isTrue()
        val accessToken = loginResp.body!!["accessToken"] as String
        val refreshToken = loginResp.body!!["refreshToken"] as String

        // 3) /me with bearer
        val meHeaders = HttpHeaders().apply {
            setBearerAuth(accessToken)
            contentType = MediaType.APPLICATION_JSON
        }
        val meResp = rest.exchange(
            url("/v1/auth/me"),
            HttpMethod.GET,
            HttpEntity<Void>(meHeaders),
            Map::class.java,
        )
        assertThat(meResp.statusCode.is2xxSuccessful).isTrue()
        assertThat(meResp.body!!["email"]).isEqualTo(email)

        // 4) Refresh (rotation)
        val refreshResp = rest.postForEntity(
            url("/v1/auth/refresh"),
            json(mapOf("refreshToken" to refreshToken)),
            Map::class.java,
        )
        assertThat(refreshResp.statusCode.is2xxSuccessful).isTrue()
        val newRefresh = refreshResp.body!!["refreshToken"] as String
        assertThat(newRefresh).isNotEqualTo(refreshToken)

        // 5) Logout with rotated refresh token
        val logoutResp = rest.postForEntity(
            url("/v1/auth/logout"),
            json(mapOf("refreshToken" to newRefresh)),
            Void::class.java,
        )
        assertThat(logoutResp.statusCode.is2xxSuccessful).isTrue()
    }

    private fun json(body: Any): HttpEntity<Any> {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        return HttpEntity(body, headers)
    }
}
