package de.healthforge.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ============ Auth DTOs (mirror server) ============

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val inviteCode: String,
    val email: String,
    val displayName: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceLabel: String? = null,
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class LogoutRequest(val refreshToken: String)

@JsonClass(generateAdapter = true)
data class RequestPasswordResetRequest(val email: String)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresInSeconds: Long,
    val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String,
    val status: String,
    val emailVerified: Boolean,
    val createdAt: String,
)

// ============ Retrofit API ============

interface AuthApi {
    @POST("v1/auth/register")
    suspend fun register(@Body req: RegisterRequest): AuthResponse

    @POST("v1/auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body req: RefreshRequest): AuthResponse

    @POST("v1/auth/logout")
    suspend fun logout(@Body req: LogoutRequest)

    @POST("v1/auth/request-password-reset")
    suspend fun requestPasswordReset(@Body req: RequestPasswordResetRequest)

    @GET("v1/auth/me")
    suspend fun me(@Header("Authorization") authHeader: String): UserDto
}
