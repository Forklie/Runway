package com.example.runway.domain.model

import java.time.Instant

/** BR-60: LKR put in, LKR taken out, and a manual "what my holdings are worth now". */
enum class CryptoEntryType { DEPOSIT, WITHDRAWAL, HOLDING_SNAPSHOT }

/**
 * users/{uid}/cryptoEntries/{id}. Values are in LKR: this is a net-position ledger, kept out of
 * spending power (BR-62), so there is no currency or FX rate here.
 */
data class CryptoEntry(
    val id: String,
    val type: CryptoEntryType,
    val asset: String, // free text: "USDT", "ETH", "alts"
    val amountLkrMinor: Long,
    val date: Instant,
    val note: String,
)
