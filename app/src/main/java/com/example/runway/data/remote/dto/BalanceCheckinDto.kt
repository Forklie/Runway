package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/** users/{uid}/balanceCheckins/{id} (Doc 2 section 6.2). */
data class BalanceCheckinDto(
    var date: Timestamp? = null,
    var accountId: String = "",
    var enteredBalanceMinor: Long = 0L,
    var expectedBalanceMinor: Long = 0L,
    var gapMinor: Long = 0L,
    var resolution: String = "",
)
