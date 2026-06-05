package de.healthforge.data.network

import de.healthforge.data.prefs.SecureTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Adds Authorization: Bearer <accessToken> to every outgoing request,
 * except the public auth endpoints (login/register/refresh).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: SecureTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (path.contains("/v1/auth/login") ||
            path.contains("/v1/auth/register") ||
            path.contains("/v1/auth/refresh") ||
            path.contains("/v1/auth/request-password-reset") ||
            path.contains("/v1/auth/password-reset") ||
            path.contains("/v1/auth/verify-email")
        ) {
            return chain.proceed(original)
        }
        val token = tokenStore.accessToken ?: return chain.proceed(original)
        val req = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(req)
    }
}

/**
 * On 401 responses, attempts a single refresh-token round-trip and retries the request once.
 *
 * Thread-safe: synchronisiert konkurrierende Refresh-Aufrufe, da der Server den
 * Refresh-Token rotiert (REQ-AUTH-005). Nur der erste Aufruf führt das Refresh aus;
 * alle weiteren warten und verwenden den neuen Token.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val authApiProvider: Provider<AuthApi>,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Nur Requests mit Bearer-Token behandeln
        val oldAuth = response.request.header("Authorization")
        if (oldAuth?.startsWith("Bearer ") != true) return null
        if (responseCount(response) >= 2) return null

        val oldToken = oldAuth.removePrefix("Bearer ")

        // Race-Condition-Guard: Wurde der Token schon von einem anderen Thread erneuert?
        val currentAccess = tokenStore.accessToken
        if (currentAccess != null && currentAccess != oldToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentAccess")
                .build()
        }

        synchronized(lock) {
            // Double-Check nach Lock-Erwerb
            val afterLock = tokenStore.accessToken
            if (afterLock != null && afterLock != oldToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $afterLock")
                    .build()
            }

            val refresh = tokenStore.refreshToken ?: return null
            return try {
                val newAuth = runBlocking {
                    authApiProvider.get().refresh(RefreshRequest(refresh))
                }
                tokenStore.accessToken = newAuth.accessToken
                tokenStore.refreshToken = newAuth.refreshToken
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newAuth.accessToken}")
                    .build()
            } catch (e: Exception) {
                tokenStore.clear()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
