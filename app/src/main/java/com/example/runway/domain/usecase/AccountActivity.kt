package com.example.runway.domain.usecase

import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.model.MovementChannel
import com.example.runway.domain.model.MovementDirection
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
