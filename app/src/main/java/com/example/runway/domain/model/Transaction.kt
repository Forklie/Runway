package com.example.runway.domain.model

import java.time.Instant

/**
 * users/{uid}/transactions/{id} (Doc 2 section 5.5, section 6.2). Contract owned by A; completed by C as A's buddy so
 * Home, Goal and Check-in can read live data. All sums and formulas use [amountLkrMinor] (BR-04).
 * [recurringId] comes last with a default, so code that builds a transaction without it still compiles.
 */
data class Transaction(
    val id: String,               // client generated: a retry rewrites the same document (BR-19)
    val type: TransactionType,
    val amountMinor: Long,
    val currency: CurrencyCode,
    val fxRate: Double,           // LKR per 1 unit, 1.0 for LKR. Fixed at entry, never recomputed (BR-05)
    val amountLkrMinor: Long,
    val categoryId: String?,
    val accountId: String?,       // source account
    val toAccountId: String?,     // transfers only
    val incomeSourceId: String?,
    val relatedTxId: String?,     // refunds point at the original expense
    val note: String,
    val date: Instant,            // when it happened
    val monthKey: String,         // "2026-10", device zone (BR-20)
    val archived: Boolean,        // soft delete (BR-16)
    val createdAt: Instant,
    val updatedAt: Instant,
    val recurringId: String? = null, // set when a recurring item generated it (BR-91)
)
