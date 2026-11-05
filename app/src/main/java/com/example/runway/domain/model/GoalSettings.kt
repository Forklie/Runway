package com.example.runway.domain.model

import java.time.Instant

/**
 * The fields of goals/main that Settings reads and edits (BR-70, Doc 2 §6.2). Kept separate from C's
 * Goal model so Settings does not depend on it. Both read the same document.
 */
data class GoalSettings(
    val name: String,
    val targetMinor: Long,
    val startDate: Instant,
    val deadline: Instant,
    val fundAccountId: String,
    val active: Boolean,
)
