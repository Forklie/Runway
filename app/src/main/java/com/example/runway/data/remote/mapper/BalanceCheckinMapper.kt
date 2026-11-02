package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.BalanceCheckinDto
import com.example.runway.domain.model.BalanceCheckin
import com.example.runway.domain.model.GapResolution
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

/** An unknown resolution reads as UNACCOUNTED: Clarity is never flattered by a bad document. */
fun BalanceCheckinDto.toDomain(id: String): BalanceCheckin = BalanceCheckin(
    id = id,
    date = date.toInstantOr(),
    accountId = accountId,
    enteredBalanceMinor = enteredBalanceMinor,
    expectedBalanceMinor = expectedBalanceMinor,
    gapMinor = gapMinor,
    resolution = resolution.toEnumOr(GapResolution.UNACCOUNTED),
)

fun BalanceCheckin.toDto(): BalanceCheckinDto = BalanceCheckinDto(
    date = date.toTimestamp(),
    accountId = accountId,
    enteredBalanceMinor = enteredBalanceMinor,
    expectedBalanceMinor = expectedBalanceMinor,
    gapMinor = gapMinor,
    resolution = resolution.name,
)

fun DocumentSnapshot.toBalanceCheckinOrNull(): BalanceCheckin? =
    try {
        toObject(BalanceCheckinDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed check-in %s", id)
        null
    }
