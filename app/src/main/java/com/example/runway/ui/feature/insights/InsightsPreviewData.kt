package com.example.runway.ui.feature.insights

import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.IncomeSource
import com.example.runway.domain.model.IncomeSourceKind
import com.example.runway.domain.model.Reliability
import com.example.runway.domain.usecase.AccountBalance
import com.example.runway.domain.usecase.CryptoPosition
import com.example.runway.domain.usecase.IncomeReality
import com.example.runway.domain.usecase.Insights
import com.example.runway.domain.usecase.SourceReality
import java.time.Instant
import java.time.LocalDate

/** Kavindu's three income channels against his own "160,000 to 210,000" guess. Preview only. */
internal fun previewInsights(newUser: Boolean = false): Insights {
    val opened = Instant.parse("2026-10-25T03:00:00Z")
    fun source(id: String, name: String, kind: IncomeSourceKind, reliability: Reliability, estimate: Long) =
        IncomeSource(id, name, kind, CurrencyCode.LKR, reliability, 0, null, estimate)
    fun balance(id: String, name: String, kind: AccountKind, minor: Long) =
        AccountBalance(Account(id, name, kind, 0, 0, opened, false, opened), minor)

    val reality = if (newUser) {
        IncomeReality(monthsCounted = 0, sources = emptyList(), totalAverageMinor = 0, ownEstimateMinor = 21_000_000)
    } else {
        IncomeReality(
            monthsCounted = 3,
            sources = listOf(
                SourceReality(source("salary", "Salary", IncomeSourceKind.SALARY, Reliability.RELIABLE, 14_000_000), 13_000_000, 12_000_000, 14_000_000),
                SourceReality(source("freelance", "Freelance", IncomeSourceKind.FREELANCE, Reliability.VARIABLE, 5_000_000), 3_000_000, 0, 9_000_000),
                SourceReality(source("adsense", "AdSense", IncomeSourceKind.ADSENSE, Reliability.VARIABLE, 2_000_000), 1_150_000, 350_000, 1_900_000),
            ),
            totalAverageMinor = 17_150_000,
            ownEstimateMinor = 21_000_000,
        )
    }
    return Insights(
        today = LocalDate.of(2026, 11, 10),
        incomeReality = reality,
        balances = listOf(
            balance("bank-main", "Main bank", AccountKind.BANK, if (newUser) 0 else 9_010_000),
            balance("cash", "Cash", AccountKind.CASH, if (newUser) 0 else 570_000),
            balance("goal-fund", "MacBook fund", AccountKind.GOAL_FUND, if (newUser) 0 else 18_640_000),
        ),
        lastCheckin = if (newUser) null else Instant.parse("2026-11-08T05:00:00Z"),
        daysSinceCheckin = if (newUser) null else 2,
        clarityPercent = if (newUser) null else 91,
        crypto = CryptoPosition(20_000_000, 5_000_000, 18_000_000, Instant.parse("2026-10-01T05:00:00Z"), 3_000_000),
        hasCryptoEntries = !newUser,
    )
}
