package com.example.runway.domain.usecase

import com.example.runway.domain.model.CryptoEntry
import com.example.runway.domain.model.CryptoEntryType
import java.time.Instant
import javax.inject.Inject

data class CryptoPosition(
    val depositsMinor: Long,
    val withdrawalsMinor: Long,
    val holdingsMinor: Long?,   // latest snapshot, null until he adds one
    val holdingsDate: Instant?,
    val netMinor: Long,         // positive is a gain, negative a loss
)

/** BR-61: net position = withdrawals + latest holdings snapshot − deposits. Never part of Safe to Spend. */
class ComputeCryptoPosition @Inject constructor() {

    operator fun invoke(entries: List<CryptoEntry>): CryptoPosition {
        val deposits = entries.filter { it.type == CryptoEntryType.DEPOSIT }.sumOf { it.amountLkrMinor }
        val withdrawals = entries.filter { it.type == CryptoEntryType.WITHDRAWAL }.sumOf { it.amountLkrMinor }
        val latest = entries.filter { it.type == CryptoEntryType.HOLDING_SNAPSHOT }.maxByOrNull { it.date }
        return CryptoPosition(
            depositsMinor = deposits,
            withdrawalsMinor = withdrawals,
            holdingsMinor = latest?.amountLkrMinor,
            holdingsDate = latest?.date,
            netMinor = withdrawals + (latest?.amountLkrMinor ?: 0L) - deposits,
        )
    }
}
