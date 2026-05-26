package de.healthforge.data.repository

import de.healthforge.data.network.GroupApi
import de.healthforge.data.network.GroupCreateRequest
import de.healthforge.data.network.GroupJoinByCodeRequest
import de.healthforge.data.network.GroupMemberDto
import de.healthforge.data.network.GroupSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Group-Client (P3.S1b). Wraps [GroupApi] in [Result] for ViewModel error handling.
 *
 * REQ-GROUP-001..006.
 */
@Singleton
class GroupRepository @Inject constructor(
    private val api: GroupApi,
) {
    suspend fun myGroups(): Result<List<GroupSummaryDto>> = runCatching { api.myGroups() }

    suspend fun discover(
        q: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Result<List<GroupSummaryDto>> = runCatching {
        api.discover(q = q?.takeIf { it.isNotBlank() }, limit = limit, offset = offset)
    }

    suspend fun create(
        name: String,
        description: String?,
        visibility: String,
    ): Result<GroupSummaryDto> = runCatching {
        api.create(GroupCreateRequest(name = name, description = description, visibility = visibility))
    }

    suspend fun detail(id: String): Result<GroupSummaryDto> = runCatching { api.detail(id) }

    suspend fun members(id: String): Result<List<GroupMemberDto>> = runCatching { api.members(id) }

    suspend fun joinByCode(inviteCode: String): Result<GroupSummaryDto> = runCatching {
        api.joinByCode(GroupJoinByCodeRequest(inviteCode = inviteCode.trim().uppercase()))
    }

    suspend fun joinPublic(id: String): Result<GroupSummaryDto> = runCatching { api.joinPublic(id) }

    suspend fun leave(id: String): Result<Unit> = runCatching { api.leave(id) }

    suspend fun removeMember(id: String, userId: String): Result<Unit> = runCatching {
        api.removeMember(id, userId)
    }

    suspend fun transferOwnership(id: String, newOwnerId: String): Result<GroupSummaryDto> = runCatching {
        api.transferOwnership(id, newOwnerId)
    }
}
