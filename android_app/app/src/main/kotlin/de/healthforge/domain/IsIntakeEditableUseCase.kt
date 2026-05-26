package de.healthforge.domain

import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * REQ-INTAKE-004: intake entries logged within the past 7 days are editable.
 * Older entries are read-only.
 */
class IsIntakeEditableUseCase @Inject constructor() {

    operator fun invoke(loggedAtEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val age = Duration.between(Instant.ofEpochMilli(loggedAtEpochMs), Instant.ofEpochMilli(nowEpochMs))
        return age.toDays() < 7
    }
}
