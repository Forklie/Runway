package com.example.runway.domain.model

import java.time.Instant

enum class MovementDirection { IN, OUT }

enum class MovementChannel { CARD, ATM, ONLINE, OTHER }

/** One debit or credit a bank reported by SMS. Read on demand and never stored. */
data class AccountMovement(
    val accountSuffix: String, // the digits after ** in the message
    val direction: MovementDirection,
    val channel: MovementChannel,
    val amountMinor: Long,
    val at: Instant,
    val description: String = "", // merchant, payee or reason, as the bank wrote it
)
