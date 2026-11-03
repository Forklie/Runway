package com.example.runway.data.remote.dto

/** users/{uid}/incomeSources/{id} (Doc 2 section 6.2). */
data class IncomeSourceDto(
    var name: String = "",
    var kind: String = "",
    var currency: String = "LKR",
    var reliability: String = "",
    var expectedAmountMinor: Long = 0L,
    var payDay: Int? = null,
    var ownMonthlyEstimateMinor: Long? = null,
)
