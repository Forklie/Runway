package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/**
 * users/{uid}/accounts/{id}. Firestore needs `var` and defaults for every field (no-arg constructor).
 * The document id is not a field: the mapper receives it separately.
 */
data class AccountDto(
    var name: String = "",
    var kind: String = "",
    var openingBalanceMinor: Long = 0L,
    var checkpointBalanceMinor: Long = 0L,
    var checkpointDate: Timestamp? = null,
    var archived: Boolean = false,
    var createdAt: Timestamp? = null,
)
