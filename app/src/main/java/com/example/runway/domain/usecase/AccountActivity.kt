package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.model.MovementDirection
import com.example.runway.domain.model.Transaction
import com.example.runway.domain.model.TransactionType
import java.time.ZoneId

/** How much one account moved in a period. */
data class AccountActivity(
    val accountSuffix: String,
    val inMinor: Long,
    val outMinor: Long,
    val count: Int,
    val activeDays: Int,
    val countByChannel: Map<MovementChannel, Int>,
) {
    val netMinor: Long get() = inMinor - outMinor
}

/** One entry per account, busiest first. */
fun summarizeAccountActivity(movements: List<AccountMovement>, zone: ZoneId): List<AccountActivity> =
    movements.groupBy { it.accountSuffix }
        .map { (suffix, rows) ->
            AccountActivity(
                accountSuffix = suffix,
                inMinor = rows.filter { it.direction == MovementDirection.IN }.sumOf { it.amountMinor },
                outMinor = rows.filter { it.direction == MovementDirection.OUT }.sumOf { it.amountMinor },
                count = rows.size,
                activeDays = rows.map { it.at.atZone(zone).toLocalDate() }.distinct().size,
                countByChannel = rows.groupingBy { it.channel }.eachCount(),
            )
        }
        .sortedWith(compareByDescending<AccountActivity> { it.count }.thenBy { it.accountSuffix })

// ponytail: two debits of the same amount on the same day count as one, match on merchant if that bites
fun List<Transaction>.hasExpenseOf(minor: Long): Boolean =
    any { it.type == TransactionType.EXPENSE && it.amountLkrMinor == minor }

/** Debits with no expense of the same amount logged on the same day, newest first. */
fun unloggedDebits(movements: List<AccountMovement>, logged: List<Transaction>, zone: ZoneId): List<AccountMovement> {
    val loggedByDay = logged.groupBy { it.date.atZone(zone).toLocalDate() }
    return movements
        .filter { it.direction == MovementDirection.OUT }
        .filterNot { loggedByDay[it.at.atZone(zone).toLocalDate()].orEmpty().hasExpenseOf(it.amountMinor) }
        .sortedByDescending { it.at }
}
