package de.healthforge.group

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/groups")
class GroupController(
    private val service: GroupService,
) {
    private fun require(principal: AuthPrincipal?): AuthPrincipal =
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")

    @GetMapping
    fun myGroups(@AuthenticationPrincipal principal: AuthPrincipal?): List<GroupSummaryDto> {
        val p = require(principal)
        return service.myGroups(p.userId)
    }

    @GetMapping("/discover")
    fun discover(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("q", required = false) q: String? = null,
        @RequestParam("limit", defaultValue = "20") limit: Int,
        @RequestParam("offset", defaultValue = "0") offset: Int,
    ): List<GroupSummaryDto> {
        val p = require(principal)
        return service.discover(q, limit, offset, p.userId)
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @Valid @RequestBody req: GroupCreateRequest,
    ): ResponseEntity<GroupSummaryDto> {
        val p = require(principal)
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, p.userId))
    }

    @GetMapping("/{id}")
    fun detail(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): GroupSummaryDto {
        val p = require(principal)
        return service.get(id, p.userId)
    }

    @GetMapping("/{id}/members")
    fun members(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): List<GroupMemberDto> {
        val p = require(principal)
        return service.members(id, p.userId)
    }

    @PostMapping("/join")
    fun joinByCode(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @Valid @RequestBody req: GroupJoinByCodeRequest,
    ): GroupSummaryDto {
        val p = require(principal)
        return service.joinByCode(req.inviteCode, p.userId)
    }

    @PostMapping("/{id}/join")
    fun joinPublic(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): GroupSummaryDto {
        val p = require(principal)
        return service.joinPublic(id, p.userId)
    }

    @PostMapping("/{id}/leave")
    fun leave(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.leave(id, p.userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/members/{userId}")
    fun removeMember(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @PathVariable userId: UUID,
    ): ResponseEntity<Void> {
        val p = require(principal)
        service.removeMember(id, userId, p.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/transfer-ownership")
    fun transferOwnership(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @PathVariable id: UUID,
        @RequestParam("new_owner_id") newOwnerId: UUID,
    ): GroupSummaryDto {
        val p = require(principal)
        service.transferOwnership(id, newOwnerId, p.userId)
        return service.get(id, p.userId)
    }
}
