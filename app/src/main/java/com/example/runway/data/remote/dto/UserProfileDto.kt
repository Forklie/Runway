package com.example.runway.data.remote.dto

import com.google.firebase.Timestamp

/** users/{uid} (Doc 2 §6.2). Field names must match exactly: they are the Firestore keys. */
data class UserProfileDto(
    var displayName: String = "",
    var email: String = "",
    var homeCurrency: String = "LKR",
    var timezone: String = "Asia/Colombo",
    var onboardingComplete: Boolean = false,
    var salaryDay: Int = 25,
    var pyfPercent: Double = 0.0,
    var defaultAccountId: String? = null,
    var lastFxRates: Map<String, Double> = emptyMap(),
    var createdAt: Timestamp? = null,
)
