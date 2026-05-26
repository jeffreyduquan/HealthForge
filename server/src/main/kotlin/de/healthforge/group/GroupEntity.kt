package de.healthforge.group

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant
import java.util.UUID

enum class GroupVisibility { PUBLIC, PRIVATE }
enum class GroupRole { OWNER, ADMIN, MEMBER }

@Entity
@Table(name = "groups")
class GroupEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "visibility", nullable = false)
    var visibility: String = GroupVisibility.PRIVATE.name,

    @Column(name = "invite_code")
    var inviteCode: String? = null,

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    var ownerId: UUID,

    @Column(name = "member_count", nullable = false)
    var memberCount: Int = 1,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberKey::class)
class GroupMemberEntity(
    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    var groupId: UUID = UUID.randomUUID(),

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "role", nullable = false)
    var role: String = GroupRole.MEMBER.name,

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now(),
)

data class GroupMemberKey(
    var groupId: UUID = UUID.randomUUID(),
    var userId: UUID = UUID.randomUUID(),
) : Serializable
