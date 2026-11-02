package com.example.runway.testutil

import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Small builders so formula tests read like Doc 1's worked examples. Amounts are LKR minor units. */
object Fixtures {
    val colombo: ZoneId = ZoneId.of("Asia/Colombo")

    /** Noon in Colombo, so a date never slips into a neighbouring day or month. */
    fun at(date: String): Instant = LocalDate.parse(date).atTime(12, 0).atZone(colombo).toInstant()

    fun lkr(rupees: Long): Long = rupees * 100

    fun account(
        id: String,
        kind: AccountKind = AccountKind.BANK,
        checkpoint: Long = 0L,
        checkpointDate: Instant = at("2026-10-01"),
    ) = Account(
        id = id,
        name = id,
        kind = kind,
        openingBalanceMinor = checkpoint,
        checkpointBalanceMinor = checkpoint,
        checkpointDate = checkpointDate,
        archived = false,
        createdAt = checkpointDate,
    )

    fun tx(
        type: TransactionType,
        amount: Long,
        date: Instant,
        account: String? = "bank-main",
        to: String? = null,
        category: String? = null,
        source: String? = null,
        related: String? = null,
        id: String = "tx-${type.name.lowercase()}-$amount-${date.epochSecond}",
        archived: Boolean = false,
        createdAt: Instant = date,
    ) = Transaction(
        id = id,
        type = type,
        amountMinor = amount,
        currency = CurrencyCode.LKR,
        fxRate = 1.0,
        amountLkrMinor = amount,
        categoryId = category,
        accountId = account,
        toAccountId = to,
        incomeSourceId = source,
        relatedTxId = related,
        recurringId = null,
        note = "",
        date = date,
        monthKey = date.atZone(colombo).toLocalDate().toString().take(7),
        archived = archived,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    fun category(id: String, kind: CategoryKind, name: String = id) = Category(
        id = id,
        name = name,
        iconKey = "more_horiz",
        kind = kind,
        sortOrder = 0,
        archived = false,
    )

    fun source(
        id: String,
        kind: IncomeSourceKind,
        reliability: Reliability,
        expected: Long = 0L,
        ownEstimate: Long? = null,
    ) = IncomeSource(
        id = id,
        name = id,
        kind = kind,
        currency = CurrencyCode.LKR,
        reliability = reliability,
        expectedAmountMinor = expected,
        payDay = null,
        ownMonthlyEstimateMinor = ownEstimate,
    )
}
