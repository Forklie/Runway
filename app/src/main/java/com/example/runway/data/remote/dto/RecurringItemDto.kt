package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/** users/{uid}/recurringItems/{id} (Doc 2 §6.2). The document id is passed to the mapper separately. */
data class RecurringItemDto(
    var kind: String = "",
    var name: String = "",
    var amountMinor: Long = 0L,
    var currency: String = "LKR",
    var frequency: String = "",
    var nextDueDate: Timestamp? = null,
    var categoryId: String? = null,
    var accountId: String? = null,
    var active: Boolean = true,
)
