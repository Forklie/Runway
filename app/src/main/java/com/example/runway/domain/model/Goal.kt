package com.example.runway.domain.model

import java.time.Instant

/**
 * users/{uid}/goals/{id}. v1 has one active goal (BR-70): the MacBook, LKR 490,000, 12 months.
 * "Saved" is never stored here: it is the balance of [fundAccountId] (BR-71), derived like every balance.
 */
data class Goal(
    val id: String,
    val name: String,
    val targetMinor: Long,
    val startDate: Instant,
    val deadline: Instant,
    val fundAccountId: String,
    val active: Boolean,
) {
    companion object {
        /** v1 keeps its one goal at a fixed id, so onboarding and the goal form write the same document. */
        const val MAIN_ID = "main"
    }
}
