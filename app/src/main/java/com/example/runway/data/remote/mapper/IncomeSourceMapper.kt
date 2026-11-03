package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.IncomeSourceDto
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.Reliability
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

/** An unknown reliability reads as VARIABLE: unsure money is never planned on (BR-43). */
fun IncomeSourceDto.toDomain(id: String): IncomeSource = IncomeSource(
    id = id,
    name = name.ifBlank { "Income" },
    kind = kind.toEnumOr(IncomeSourceKind.OTHER),
    currency = currency.toEnumOr(CurrencyCode.LKR),
    reliability = reliability.toEnumOr(Reliability.VARIABLE),
    expectedAmountMinor = expectedAmountMinor.coerceAtLeast(0L),
    payDay = payDay?.coerceIn(1, 31),
    ownMonthlyEstimateMinor = ownMonthlyEstimateMinor,
)

fun IncomeSource.toDto(): IncomeSourceDto = IncomeSourceDto(
    name = name,
    kind = kind.name,
    currency = currency.name,
    reliability = reliability.name,
    expectedAmountMinor = expectedAmountMinor,
    payDay = payDay,
    ownMonthlyEstimateMinor = ownMonthlyEstimateMinor,
)

fun DocumentSnapshot.toIncomeSourceOrNull(): IncomeSource? =
    try {
        toObject(IncomeSourceDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed income source %s", id)
        null
    }
