package de.healthforge.group

import de.healthforge.common.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

/**
 * Group service (REQ-GROUP-001..006).
 *
 * Invite-codes: 8-char alphanumeric (Base32-ish, no I/O/0/1 to avoid confusion).
 * Generated for PRIVATE groups only; PUBLIC groups never carry an invite_code (DB CHECK enforces it).
 */
@Service
class GroupService(
    private val groupRepo: GroupRepo,
    private val memberRepo: GroupMemberRepo,
    private val searchRepo: GroupSearchRepo,
) {

    @Transactional
    fun create(req: GroupCreateRequest, ownerId: UUID): GroupSummaryDto {
        val now = Instant.now()
        val code = if (req.visibility == GroupVisibility.PRIVATE) generateInviteCode() else null
        val g = GroupEntity(
            id = UUID.randomUUID(),
            name = req.name.trim(),
            description = req.description?.trim()?.ifEmpty { null },
            visibility = req.visibility.name,
            inviteCode = code,
            ownerId = ownerId,
            memberCount = 1,
            createdAt = now,
            updatedAt = now,
        )
        groupRepo.save(g)
        memberRepo.save(
            GroupMemberEntity(
                groupId = g.id,
                userId = ownerId,
                role = GroupRole.OWNER.name,
                joinedAt = now,
            ),
        )
        return toSummary(g, GroupRole.OWNER)
    }

    @Transactional(readOnly = true)
    fun get(groupId: UUID, viewerId: UUID): GroupSummaryDto {
        val g = groupRepo.findById(groupId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group $groupId not found")
        }
        val role = memberRepo.findByGroupIdAndUserId(groupId, viewerId)?.role?.let { GroupRole.valueOf(it) }
        if (GroupVisibility.valueOf(g.visibility) == GroupVisibility.PRIVATE && role == null) {
            throw ApiException(HttpStatus.FORBIDDEN, "PRIVATE_GROUP", "join via invite_code first")
        }
        return toSummary(g, role)
    }

    @Transactional(readOnly = true)
    fun myGroups(userId: UUID): List<GroupSummaryDto> {
        val memberships = memberRepo.findByUserId(userId)
        if (memberships.isEmpty()) return emptyList()
        val byId = groupRepo.findAllById(memberships.map { it.groupId }).associateBy { it.id }
        return memberships.mapNotNull { m ->
            byId[m.groupId]?.let { toSummary(it, GroupRole.valueOf(m.role)) }
        }
    }

    @Transactional(readOnly = true)
    fun discover(q: String?, limit: Int, offset: Int, viewerId: UUID): List<GroupSummaryDto> {
        val ids = searchRepo.searchPublic(q, limit, offset)
        if (ids.isEmpty()) return emptyList()
        val byId = groupRepo.findAllById(ids).associateBy { it.id }
        val myRoles = memberRepo.findByUserId(viewerId)
            .filter { it.groupId in ids }
            .associate { it.groupId to GroupRole.valueOf(it.role) }
        return ids.mapNotNull { byId[it] }.map { toSummary(it, myRoles[it.id]) }
    }

    @Transactional
    fun joinByCode(code: String, userId: UUID): GroupSummaryDto {
        val g = groupRepo.findByInviteCode(code.trim())
            ?: throw ApiException(HttpStatus.NOT_FOUND, "INVALID_INVITE_CODE", "no group for invite code")
        return joinInternal(g, userId)
    }

    @Transactional
    fun joinPublic(groupId: UUID, userId: UUID): GroupSummaryDto {
        val g = groupRepo.findById(groupId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group $groupId not found")
        }
        if (GroupVisibility.valueOf(g.visibility) != GroupVisibility.PUBLIC) {
            throw ApiException(HttpStatus.FORBIDDEN, "NOT_PUBLIC", "use invite_code to join private group")
        }
        return joinInternal(g, userId)
    }

    private fun joinInternal(g: GroupEntity, userId: UUID): GroupSummaryDto {
        if (memberRepo.existsByGroupIdAndUserId(g.id, userId)) {
            val role = memberRepo.findByGroupIdAndUserId(g.id, userId)!!.role
            return toSummary(g, GroupRole.valueOf(role))
        }
        memberRepo.save(
            GroupMemberEntity(
                groupId = g.id,
                userId = userId,
                role = GroupRole.MEMBER.name,
                joinedAt = Instant.now(),
            ),
        )
        g.memberCount = (memberRepo.countByGroupId(g.id)).toInt()
        g.updatedAt = Instant.now()
        groupRepo.save(g)
        return toSummary(g, GroupRole.MEMBER)
    }

    @Transactional
    fun leave(groupId: UUID, userId: UUID) {
        val membership = memberRepo.findByGroupIdAndUserId(groupId, userId)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "NOT_A_MEMBER", "not a member")
        if (membership.role == GroupRole.OWNER.name) {
            throw ApiException(
                HttpStatus.CONFLICT,
                "OWNER_CANNOT_LEAVE",
                "transfer ownership before leaving the group",
            )
        }
        memberRepo.deleteByGroupIdAndUserId(groupId, userId)
        val g = groupRepo.findById(groupId).orElse(null) ?: return
        g.memberCount = (memberRepo.countByGroupId(groupId)).toInt()
        g.updatedAt = Instant.now()
        groupRepo.save(g)
    }

    @Transactional
    fun removeMember(groupId: UUID, targetUserId: UUID, callerId: UUID) {
        val g = groupRepo.findById(groupId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group $groupId not found")
        }
        if (g.ownerId != callerId) {
            throw ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "only group owner may remove members")
        }
        if (targetUserId == callerId) {
            throw ApiException(HttpStatus.CONFLICT, "OWNER_CANNOT_LEAVE", "transfer ownership first")
        }
        val target = memberRepo.findByGroupIdAndUserId(groupId, targetUserId)
            ?: throw ApiException(HttpStatus.NOT_FOUND, "NOT_A_MEMBER", "user is not a member")
        memberRepo.delete(target)
        g.memberCount = (memberRepo.countByGroupId(groupId)).toInt()
        g.updatedAt = Instant.now()
        groupRepo.save(g)
    }

    @Transactional
    fun transferOwnership(groupId: UUID, newOwnerId: UUID, callerId: UUID) {
        val g = groupRepo.findById(groupId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group $groupId not found")
        }
        if (g.ownerId != callerId) {
            throw ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "only owner may transfer ownership")
        }
        if (newOwnerId == callerId) return
        val newOwnerMembership = memberRepo.findByGroupIdAndUserId(groupId, newOwnerId)
            ?: throw ApiException(HttpStatus.CONFLICT, "TARGET_NOT_MEMBER", "new owner must be a member")
        val oldOwnerMembership = memberRepo.findByGroupIdAndUserId(groupId, callerId)!!
        // Step 1: demote current owner to MEMBER (avoids partial-unique-index conflict)
        oldOwnerMembership.role = GroupRole.MEMBER.name
        memberRepo.save(oldOwnerMembership)
        memberRepo.flush()
        // Step 2: promote new owner
        newOwnerMembership.role = GroupRole.OWNER.name
        memberRepo.save(newOwnerMembership)
        g.ownerId = newOwnerId
        g.updatedAt = Instant.now()
        groupRepo.save(g)
    }

    @Transactional(readOnly = true)
    fun members(groupId: UUID, viewerId: UUID): List<GroupMemberDto> {
        val g = groupRepo.findById(groupId).orElseThrow {
            ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group $groupId not found")
        }
        val viewerRole = memberRepo.findByGroupIdAndUserId(groupId, viewerId)
        if (GroupVisibility.valueOf(g.visibility) == GroupVisibility.PRIVATE && viewerRole == null) {
            throw ApiException(HttpStatus.FORBIDDEN, "PRIVATE_GROUP", "join group first")
        }
        return memberRepo.findByGroupId(groupId).map {
            GroupMemberDto(it.userId, GroupRole.valueOf(it.role), it.joinedAt)
        }
    }

    /** Check called by recipe service for visibility=GROUP filter. */
    @Transactional(readOnly = true)
    fun isMember(userId: UUID, groupId: UUID): Boolean =
        memberRepo.existsByGroupIdAndUserId(groupId, userId)

    /** Returns all group ids that the user is a member of (used by recipe browse). */
    @Transactional(readOnly = true)
    fun groupIdsForUser(userId: UUID): List<UUID> = memberRepo.groupIdsForUser(userId)

    private fun toSummary(g: GroupEntity, role: GroupRole?): GroupSummaryDto = GroupSummaryDto(
        id = g.id,
        name = g.name,
        description = g.description,
        visibility = GroupVisibility.valueOf(g.visibility),
        // invite_code only returned to MEMBERS (not leaked in PUBLIC discovery).
        inviteCode = if (role != null) g.inviteCode else null,
        ownerId = g.ownerId,
        memberCount = g.memberCount,
        myRole = role,
        createdAt = g.createdAt,
    )

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no I,O,0,1
        val random = SecureRandom()
        repeat(8) {
            val code = (1..8).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
            if (groupRepo.findByInviteCode(code) == null) return code
        }
        // Vanishingly small chance — fall through with one more attempt
        return (1..8).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }
}
