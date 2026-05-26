package de.healthforge.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ===================== DTOs =====================
// REQ-GROUP-001..006 — mirrors server `group/GroupDtos.kt`

@JsonClass(generateAdapter = true)
data class GroupSummaryDto(
    val id: String,
    val name: String,
    val description: String?,
    /** PUBLIC | PRIVATE */
    val visibility: String,
    @Json(name = "invite_code") val inviteCode: String?,
    @Json(name = "owner_id") val ownerId: String,
    @Json(name = "member_count") val memberCount: Int,
    /** OWNER | ADMIN | MEMBER | null (non-member viewing public discover) */
    @Json(name = "my_role") val myRole: String?,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class GroupMemberDto(
    @Json(name = "user_id") val userId: String,
    /** OWNER | ADMIN | MEMBER */
    val role: String,
    @Json(name = "joined_at") val joinedAt: String,
)

@JsonClass(generateAdapter = true)
data class GroupCreateRequest(
    val name: String,
    val description: String? = null,
    /** PUBLIC | PRIVATE — defaults to PRIVATE server-side */
    val visibility: String = "PRIVATE",
)

@JsonClass(generateAdapter = true)
data class GroupJoinByCodeRequest(
    @Json(name = "invite_code") val inviteCode: String,
)

// ===================== API =====================

interface GroupApi {

    @GET("v1/groups")
    suspend fun myGroups(): List<GroupSummaryDto>

    @GET("v1/groups/discover")
    suspend fun discover(
        @Query("q") q: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): List<GroupSummaryDto>

    @POST("v1/groups")
    suspend fun create(@Body req: GroupCreateRequest): GroupSummaryDto

    @GET("v1/groups/{id}")
    suspend fun detail(@Path("id") id: String): GroupSummaryDto

    @GET("v1/groups/{id}/members")
    suspend fun members(@Path("id") id: String): List<GroupMemberDto>

    @POST("v1/groups/join")
    suspend fun joinByCode(@Body req: GroupJoinByCodeRequest): GroupSummaryDto

    @POST("v1/groups/{id}/join")
    suspend fun joinPublic(@Path("id") id: String): GroupSummaryDto

    @POST("v1/groups/{id}/leave")
    suspend fun leave(@Path("id") id: String)

    @DELETE("v1/groups/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)

    @POST("v1/groups/{id}/transfer-ownership")
    suspend fun transferOwnership(
        @Path("id") id: String,
        @Query("new_owner_id") newOwnerId: String,
    ): GroupSummaryDto
}
