package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.CryptoEntryDto
import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

/** Null for an unknown type: a deposit read as a withdrawal would flip the net position. */
fun CryptoEntryDto.toDomainOrNull(id: String): CryptoEntry? {
    val type = enumValues<CryptoEntryType>().firstOrNull { it.name == type } ?: return null
    return CryptoEntry(
        id = id,
        type = type,
        asset = asset,
        amountLkrMinor = amountLkrMinor.coerceAtLeast(0L),
        date = date.toInstantOr(),
        note = note,
    )
}

fun CryptoEntry.toDto(): CryptoEntryDto = CryptoEntryDto(
    type = type.name,
    asset = asset,
    amountLkrMinor = amountLkrMinor,
    date = date.toTimestamp(),
    note = note,
)

fun DocumentSnapshot.toCryptoEntryOrNull(): CryptoEntry? =
    try {
        toObject(CryptoEntryDto::class.java)?.toDomainOrNull(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed crypto entry %s", id)
        null
    }
