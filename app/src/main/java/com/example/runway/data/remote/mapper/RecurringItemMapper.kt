package com.example.runway.data.remote.mapper

import com.example.runway.data.remote.dto.RecurringItemDto
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.RecurringFrequency
import com.example.runway.domain.model.RecurringItem
import com.example.runway.domain.model.RecurringKind
import com.google.firebase.firestore.DocumentSnapshot
import timber.log.Timber

fun RecurringItemDto.toDomain(id: String): RecurringItem = RecurringItem(
    id = id,
    kind = kind.toEnumOr(RecurringKind.BILL),
    name = name.ifBlank { "Recurring item" },
    amountMinor = amountMinor,
    currency = currency.toEnumOr(CurrencyCode.LKR),
    frequency = frequency.toEnumOr(RecurringFrequency.MONTHLY),
    nextDueDate = nextDueDate.toInstantOr(),
    categoryId = categoryId,
    accountId = accountId,
    active = active,
)

fun RecurringItem.toDto(): RecurringItemDto = RecurringItemDto(
    kind = kind.name,
    name = name,
    amountMinor = amountMinor,
    currency = currency.name,
    frequency = frequency.name,
    nextDueDate = nextDueDate.toTimestamp(),
    categoryId = categoryId,
    accountId = accountId,
    active = active,
)

/** One malformed document returns null (and is logged) instead of crashing the whole list. */
fun DocumentSnapshot.toRecurringItemOrNull(): RecurringItem? =
    try {
        toObject(RecurringItemDto::class.java)?.toDomain(id)
    } catch (e: RuntimeException) {
        Timber.w(e, "Skipping malformed recurring item %s", id)
        null
    }
