package com.example.runway.ui.feature.home

import com.example.runway.domain.model.Account
import com.example.runway.domain.model.AccountKind
import com.example.runway.domain.model.Category
import com.example.runway.domain.model.CategoryKind
import com.example.runway.domain.model.CurrencyCode
import com.example.runway.domain.model.Goal
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import com.example.runway.domain.usecase.ComputeAccountBalances
import com.example.runway.domain.usecase.ComputeMonthSpending
import com.example.runway.domain.usecase.ComputeSafeToSpend
import com.example.runway.domain.usecase.HomePrompt
import com.example.runway.domain.usecase.HomeSummary
import com.example.runway.domain.usecase.ProjectGoal
import com.example.runway.domain.usecase.SafeToSpendInput
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val zone: ZoneId = ZoneId.of("Asia/Colombo")
private val today: LocalDate = LocalDate.of(2026, 11, 10)

internal fun previewHomeActions() = HomeActions(
    onAddExpense = {},
    onCheckin = {},
    onIncome = {},
    onTransfer = {},
    onSettings = {},
)

/**
 * Kavindu on Tue 10 Nov, built with the real formulas so previews (and the wireframe screenshots
 * taken from them) show numbers that add up. Preview only: never used at runtime.
 */
internal fun previewHomeSummary(dayZero: Boolean = false): HomeSummary {
    val categories = listOf(
        Category("rent", "Rent", "home", CategoryKind.COMMITTED, 0, false),
        Category("utilities", "Utilities", "bolt", CategoryKind.COMMITTED, 1, false),
        Category("gym", "Gym", "fitness_center", CategoryKind.COMMITTED, 3, false),
        Category("food-coffee", "Food and Coffee", "restaurant", CategoryKind.DISCRETIONARY, 4, false),
        Category("transport", "Transport", "directions_car", CategoryKind.DISCRETIONARY, 5, false),
    )
    val accounts = listOf(
        account("bank-main", "Main bank", AccountKind.BANK),
        account("cash", "Cash", AccountKind.CASH),
        account("goal-fund", "MacBook fund", AccountKind.GOAL_FUND),
    )
    val november = if (dayZero) emptyList() else listOf(
        tx("rent", TransactionType.EXPENSE, 3_400_000, at(1), "rent", "bank-main"),
        tx("dialog", TransactionType.EXPENSE, 499_000, at(2), "utilities", "bank-main", note = "Dialog Fibre"),
        tx("gym", TransactionType.EXPENSE, 500_000, at(3), "gym", "bank-main"),
        tx("pickme", TransactionType.EXPENSE, 64_000, at(8), "transport", "cash", note = "PickMe to work"),
        tx("coffee", TransactionType.EXPENSE, 85_000, at(10, hour = 9), "food-coffee", "bank-main", note = "Cappuccino + croissant"),
        tx("kottu", TransactionType.EXPENSE, 120_000, at(10, hour = 20), "food-coffee", "cash", note = "Kottu at Pilawoos"),
    )
    // Pay Yourself First on the 25th of Aug, Sep and Oct: the goal fund's pace.
    val payYourselfFirst = if (dayZero) emptyList() else listOf(8, 9, 10).map { month ->
        tx("pyf-$month", TransactionType.TRANSFER, 3_990_000, LocalDate.of(2026, month, 25).atTime(12, 0).atZone(zone).toInstant(), null, "bank-main", to = "goal-fund")
    }

    val goal = Goal(
        id = Goal.MAIN_ID,
        name = "MacBook Pro M4",
        targetMinor = 49_000_000,
        startDate = (if (dayZero) today else LocalDate.of(2026, 6, 1)).atStartOfDay(zone).toInstant(),
        deadline = (if (dayZero) today.plusYears(1) else LocalDate.of(2027, 5, 1)).atStartOfDay(zone).toInstant(),
        fundAccountId = "goal-fund",
        active = true,
    )
    val saved = if (dayZero) 1_120_000L else 18_640_000L
    val projection = ProjectGoal(ComputeAccountBalances())(goal, saved, november + payYourselfFirst, today, zone)
    val month = ComputeMonthSpending()(november, categories)
    val safeToSpend = ComputeSafeToSpend()
    val input = SafeToSpendInput(
        reliableIncomeExpected = 12_000_000,
        variableIncomeReceived = if (dayZero) 0 else 800_000,
        committedCosts = safeToSpend.committedCosts(plannedMinor = 4_500_000, actualCommittedMinor = month.committedMinor),
        goalContribution = projection.requiredPerMonthMinor,
        discretionarySpent = month.discretionaryMinor,
        today = today,
    )
    val result = safeToSpend(input)

    return HomeSummary(
        firstName = "Kavindu",
        today = today,
        zone = zone,
        safeToSpend = result,
        safeToSpendInput = input,
        pace = safeToSpend.pace(result, month.discretionaryMinor, today),
        goal = goal,
        projection = projection,
        month = month,
        clarityPercent = if (dayZero) null else 91,
        prompt = if (dayZero) null else HomePrompt.WelcomeBack(daysAway = 4),
        todayActivity = november.filter { it.date.atZone(zone).toLocalDate() == today }.sortedByDescending { it.date },
        accountsById = accounts.associateBy { it.id },
        categoriesById = categories.associateBy { it.id },
        sourcesById = emptyMap(),
        isEmpty = false,
    )
}

private fun at(day: Int, hour: Int = 12): Instant = LocalDate.of(2026, 11, day).atTime(hour, 0).atZone(zone).toInstant()

private fun account(id: String, name: String, kind: AccountKind) = Account(
    id = id,
    name = name,
    kind = kind,
    openingBalanceMinor = 0,
    checkpointBalanceMinor = 0,
    checkpointDate = at(1),
    archived = false,
    createdAt = at(1),
)

private fun tx(
    id: String,
    type: TransactionType,
    amount: Long,
    date: Instant,
    category: String?,
    account: String?,
    to: String? = null,
    note: String = "",
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
    incomeSourceId = null,
    relatedTxId = null,
    recurringId = null,
    note = note,
    date = date,
    monthKey = date.atZone(zone).toLocalDate().toString().take(7),
    archived = false,
    createdAt = date,
    updatedAt = date,
)
