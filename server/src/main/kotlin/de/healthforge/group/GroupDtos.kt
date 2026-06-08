package de.healthforge.group

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class GroupCreateRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
    @field:Size(max = 1000) val description: String? = null,
    @field:NotNull val visibility: GroupVisibility = GroupVisibility.PRIVATE,
)

data class GroupUpdateRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
    @field:Size(max = 1000) val description: String? = null,
    val visibility: GroupVisibility? = null,
)

data class GroupJoinByCodeRequest(
    @field:NotBlank @JsonProperty("invite_code") val inviteCode: String,
)

data class GroupSummaryDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val visibility: GroupVisibility,
    @JsonProperty("invite_code") val inviteCode: String?,
    @JsonProperty("owner_id") val ownerId: UUID,
    @JsonProperty("member_count") val memberCount: Int,
    @JsonProperty("my_role") val myRole: GroupRole?,
    @JsonProperty("created_at") val createdAt: Instant,
)

data class GroupMemberDto(
    @JsonProperty("user_id") val userId: UUID,
    @JsonProperty("display_name") val displayName: String?,
    val role: GroupRole,
    @JsonProperty("joined_at") val joinedAt: Instant,
)

/** Hilfs-DTO für die Member-Abfrage mit JOIN über users.display_name. */
data class MemberWithDisplay(
    val userId: UUID,
    val displayName: String?,
    val role: String,
    val joinedAt: Instant,
)
