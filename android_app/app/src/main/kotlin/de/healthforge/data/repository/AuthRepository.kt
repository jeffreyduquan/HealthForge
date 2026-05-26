package de.healthforge.data.repository

import de.healthforge.data.network.AuthApi
import de.healthforge.data.network.LoginRequest
import de.healthforge.data.network.LogoutRequest
import de.healthforge.data.network.RefreshRequest
import de.healthforge.data.network.RegisterRequest
import de.healthforge.data.network.RequestPasswordResetRequest
import de.healthforge.data.network.UserDto
import de.healthforge.data.prefs.SecureTokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: SecureTokenStore,
) {
    suspend fun login(email: String, password: String, deviceLabel: String? = null): Result<UserDto> = runCatching {
        val response = api.login(LoginRequest(email, password, deviceLabel))
        persist(response.accessToken, response.refreshToken, response.user)
        response.user
    }

    suspend fun register(
        inviteCode: String,
        email: String,
        displayName: String,
        password: String,
    ): Result<UserDto> = runCatching {
        val response = api.register(RegisterRequest(inviteCode, email, displayName, password))
        persist(response.accessToken, response.refreshToken, response.user)
        response.user
    }

    suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        api.requestPasswordReset(RequestPasswordResetRequest(email))
    }

    suspend fun logout(): Result<Unit> = runCatching {
        val refresh = tokenStore.refreshToken
        if (refresh != null) {
            runCatching { api.logout(LogoutRequest(refresh)) }
        }
        tokenStore.clear()
    }

    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    private fun persist(access: String, refresh: String, user: UserDto) {
        tokenStore.accessToken = access
        tokenStore.refreshToken = refresh
        tokenStore.userId = user.id
        tokenStore.userEmail = user.email
    }
}
