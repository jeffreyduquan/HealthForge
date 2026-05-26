package de.healthforge.group

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

interface GroupRepo : JpaRepository<GroupEntity, UUID> {
    fun findByInviteCode(inviteCode: String): GroupEntity?
}

interface GroupMemberRepo : JpaRepository<GroupMemberEntity, GroupMemberKey> {
    fun findByGroupId(groupId: UUID): List<GroupMemberEntity>
    fun findByUserId(userId: UUID): List<GroupMemberEntity>
    fun existsByGroupIdAndUserId(groupId: UUID, userId: UUID): Boolean
    fun findByGroupIdAndUserId(groupId: UUID, userId: UUID): GroupMemberEntity?
    fun deleteByGroupIdAndUserId(groupId: UUID, userId: UUID)
    fun countByGroupId(groupId: UUID): Long

    @Query("SELECT m.groupId FROM GroupMemberEntity m WHERE m.userId = :userId")
    fun groupIdsForUser(@Param("userId") userId: UUID): List<UUID>
}

@Repository
class GroupSearchRepo(
    @jakarta.persistence.PersistenceContext private val em: jakarta.persistence.EntityManager,
) {
    fun searchPublic(q: String?, limit: Int, offset: Int): List<UUID> {
        val safeLimit = limit.coerceIn(1, 50)
        val safeOffset = offset.coerceAtLeast(0)
        val params = mutableMapOf<String, Any>("lim" to safeLimit, "off" to safeOffset)
        val where = StringBuilder("g.visibility = 'PUBLIC'")
        if (!q.isNullOrBlank()) {
            where.append(
                " AND (hf_immutable_unaccent(lower(g.name)) ILIKE hf_immutable_unaccent(lower(:q))" +
                    " OR hf_immutable_unaccent(lower(coalesce(g.description,''))) ILIKE hf_immutable_unaccent(lower(:q)))"
            )
            params["q"] = "%${q.trim()}%"
        }
        val sql = """
            SELECT g.id FROM groups g
            WHERE $where
            ORDER BY g.member_count DESC, g.created_at DESC, g.id
            LIMIT :lim OFFSET :off
        """.trimIndent()
        val nq = em.createNativeQuery(sql)
        params.forEach { (k, v) -> nq.setParameter(k, v) }
        @Suppress("UNCHECKED_CAST")
        val rows = nq.resultList as List<Any>
        return rows.map {
            when (it) {
                is UUID -> it
                is String -> UUID.fromString(it)
                else -> UUID.fromString(it.toString())
            }
        }
    }
}
