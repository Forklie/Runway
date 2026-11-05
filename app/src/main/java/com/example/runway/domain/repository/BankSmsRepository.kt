package com.example.runway.domain.repository

import com.example.runway.domain.model.AccountMovement
import java.time.Instant

/** The caller checks READ_SMS first. A missing permission throws SecurityException. */
interface BankSmsRepository {
    /** Supported banks' debits and credits received from [from] up to, not including, [until]. */
    suspend fun movements(from: Instant, until: Instant): List<AccountMovement>
}
