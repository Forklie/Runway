package com.example.runway.domain.model

import java.time.Instant

enum class AccountKind { BANK, SECONDARY, CASH, GOAL_FUND }

/** Balance is derived: checkpoint + movements since checkpointDate (BR-31). Never store a running balance. */
data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val openingBalanceMinor: Long,
    val checkpointBalanceMinor: Long,
    val checkpointDate: Instant,
    val archived: Boolean,
    val createdAt: Instant,
)