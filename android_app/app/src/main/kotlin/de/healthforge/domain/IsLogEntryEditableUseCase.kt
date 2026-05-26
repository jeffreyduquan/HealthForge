package de.healthforge.domain

import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * REQ-LOG-006: a symptom-diary entry is editable only within the 7-day window
 * after `occurredAt`. Older entries become read-only.
 */
class IsLogEntryEditableUseCase @Inject constructor() {

    operator fun invoke(occurredAtEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val age = Duration.between(Instant.ofEpochMilli(occurredAtEpochMs), Instant.ofEpochMilli(nowEpochMs))
        return age.toDays() < 7
    }
}
