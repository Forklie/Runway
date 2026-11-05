package com.example.runway.domain.model

import java.time.Instant

/** What the user chose to do with a gap (BR-84). Both choices are valid. */
enum class GapResolution {
    /** Entered balance matched the expected one. */
    NONE,

    /** Logged as one "Unlogged spending" expense, so it now counts as logged. */
    LOGGED,

    /** Left as Unaccounted. Counts against Clarity (BR-82). */
    UNACCOUNTED,
}

/**
 * users/{uid}/balanceCheckins/{id}: one account in one weekly check-in (BR-80 to BR-84).
 * gapMinor = expected − entered. Positive is unlogged spending, negative is unlogged income or a refund (BR-81).
 */
data class BalanceCheckin(
    val id: String,
    val date: Instant,
    val accountId: String,
    val enteredBalanceMinor: Long,
    val expectedBalanceMinor: Long,
    val gapMinor: Long,
    val resolution: GapResolution,
)
