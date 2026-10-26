package com.example.runway.domain.model

import java.time.Instant

enum class RecurringKind { RENT, BILL, SUBSCRIPTION }
enum class RecurringFrequency { MONTHLY, YEARLY }

data class RecurringItem(
    val id: String,
    val kind: RecurringKind,
    val name: String,
    val amountMinor: Long,
    val currency: CurrencyCode,
    val frequency: RecurringFrequency,
    val nextDueDate: Instant,
    val categoryId: String?,
    val accountId: String?,
    val active: Boolean,
)