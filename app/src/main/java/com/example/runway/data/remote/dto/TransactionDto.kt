package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/**
 * users/{uid}/transactions/{id} (Doc 2 section 6.2). Every field name here must be in the rules' hasOnly
 * list, or the write is rejected with PERMISSION_DENIED. note is never null: the rules require a string.
 */
data class TransactionDto(
    var type: String = "",
    var amountMinor: Long = 0L,
    var currency: String = "LKR",
    var fxRate: Double = 1.0,
    var amountLkrMinor: Long = 0L,
    var categoryId: String? = null,
    var accountId: String? = null,
    var toAccountId: String? = null,
    var incomeSourceId: String? = null,
    var relatedTxId: String? = null,
    var recurringId: String? = null,
    var note: String = "",
    var date: Timestamp? = null,
    var monthKey: String = "",
    var archived: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)
