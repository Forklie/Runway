package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/** users/{uid}/cryptoEntries/{id} (Doc 2 section 6.2). */
data class CryptoEntryDto(
    var type: String = "",
    var asset: String = "",
    var amountLkrMinor: Long = 0L,
    var date: Timestamp? = null,
    var note: String = "",
)
