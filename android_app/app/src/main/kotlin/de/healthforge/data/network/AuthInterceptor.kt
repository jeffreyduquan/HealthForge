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
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: SecureTokenStore,
    private val authApiProvider: Provider<AuthApi>,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization")?.startsWith("Bearer ") != true) return null
        // Prevent infinite loop: only one retry
        if (responseCount(response) >= 2) return null
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
