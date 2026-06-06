package de.healthforge.admin

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApkReleaseRepo : JpaRepository<ApkRelease, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<ApkRelease>
    fun findFirstByOrderByCreatedAtDesc(): ApkRelease?
}
