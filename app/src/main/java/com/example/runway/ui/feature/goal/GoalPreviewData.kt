package com.example.runway.ui.feature.goal

import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.usecase.AccountBalance
import com.example.runway.domain.usecase.ComputeAccountBalances
import com.example.runway.domain.usecase.GoalRunway
import com.example.runway.domain.usecase.ProjectGoal
import java.time.LocalDate
import java.time.ZoneId

/** Kavindu's MacBook runway on 10 Nov, from the real ProjectGoal. Preview only. */
internal fun previewGoalRunway(dayZero: Boolean = false): GoalRunway {
    val zone = ZoneId.of("Asia/Colombo")
    val today = LocalDate.of(2026, 11, 10)
    val start = if (dayZero) today else LocalDate.of(2026, 6, 1)
    val goal = Goal(
        id = Goal.MAIN_ID,
        name = "MacBook Pro M4",
        targetMinor = 49_000_000,
        startDate = start.atStartOfDay(zone).toInstant(),
        deadline = (if (dayZero) today.plusYears(1) else LocalDate.of(2027, 5, 1)).atStartOfDay(zone).toInstant(),
        fundAccountId = "goal-fund",
        active = true,
    )
    val payYourselfFirst = if (dayZero) emptyList() else listOf(8, 9, 10).map { month ->
        val date = LocalDate.of(2026, month, 25).atTime(12, 0).atZone(zone).toInstant()
        Transaction(
            id = "pyf-$month", type = TransactionType.TRANSFER, amountMinor = 3_990_000, currency = CurrencyCode.LKR,
            fxRate = 1.0, amountLkrMinor = 3_990_000, categoryId = null, accountId = "bank-main", toAccountId = "goal-fund",
            incomeSourceId = null, relatedTxId = null, recurringId = null, note = "", date = date,
            monthKey = "2026-%02d".format(month), archived = false, createdAt = date, updatedAt = date,
        )
    }
    val saved = if (dayZero) 1_120_000L else 18_640_000L
    val opened = start.atStartOfDay(zone).toInstant()
    fun account(id: String, name: String, kind: AccountKind) = Account(id, name, kind, 0, 0, opened, false, opened)

    return GoalRunway(
        goal = goal,
        projection = ProjectGoal(ComputeAccountBalances())(goal, saved, payYourselfFirst, today, zone),
        fundName = "MacBook fund",
        fromAccounts = listOf(
            AccountBalance(account("bank-main", "Main bank", AccountKind.BANK), 9_010_000),
            AccountBalance(account("cash", "Cash", AccountKind.CASH), 570_000),
        ),
    )
}
