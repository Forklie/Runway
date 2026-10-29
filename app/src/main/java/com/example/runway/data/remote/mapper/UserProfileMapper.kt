package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.UserProfileDto
import com.example.runway.domain.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

fun UserProfileDto.toDomain(uid: String): UserProfile = UserProfile(
    uid = uid,
    displayName = displayName,
    email = email,
    homeCurrency = homeCurrency.ifBlank { "LKR" },
    timezone = timezone.ifBlank { "Asia/Colombo" },
    onboardingComplete = onboardingComplete,
    salaryDay = salaryDay.coerceIn(1, 31),
    pyfPercent = pyfPercent,
    defaultAccountId = defaultAccountId,
    lastFxRates = lastFxRates,
    createdAt = createdAt.toInstantOr(),
)

fun UserProfile.toDto(): UserProfileDto = UserProfileDto(
    displayName = displayName,
    email = email,
    homeCurrency = homeCurrency,
    timezone = timezone,
    onboardingComplete = onboardingComplete,
    salaryDay = salaryDay,
    pyfPercent = pyfPercent,
    defaultAccountId = defaultAccountId,
    lastFxRates = lastFxRates,
    createdAt = createdAt.toTimestamp(),
)

/** Null when the document does not exist yet or cannot be read. */
fun DocumentSnapshot.toUserProfileOrNull(): UserProfile? {
    if (!exists()) return null
    return try {
        toObject(UserProfileDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Malformed user profile")
        null
    }
}
