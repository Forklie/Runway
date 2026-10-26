package com.example.runway.domain.model

import java.time.Instant

/** users/{uid} (Doc 2 §6.2). The mapper supplies safe defaults for missing fields. */
data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val homeCurrency: String = "LKR",
    val timezone: String = "Asia/Colombo",
    val onboardingComplete: Boolean = false,
    val salaryDay: Int = 25,
    val pyfPercent: Double = 0.0,
    val defaultAccountId: String? = null,
    val lastFxRates: Map<String, Double> = emptyMap(),
    val createdAt: Instant,
)