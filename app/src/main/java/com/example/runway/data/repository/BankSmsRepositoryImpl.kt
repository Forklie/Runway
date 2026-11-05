package com.example.runway.data.repository

import android.content.Context
import android.provider.Telephony
import com.example.runway.core.di.DispatcherProvider
import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.repository.BankSmsRepository
import com.example.runway.domain.usecase.SAMPATH_SENDERS
import com.example.runway.domain.usecase.parseSampathSms
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.withContext

/** Reads only the supported banks' messages from the inbox. Nothing is stored, logged or uploaded. */
class BankSmsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : BankSmsRepository {

    override suspend fun movements(from: Instant, until: Instant): List<AccountMovement> =
        withContext(dispatchers.io) {
            val senders = SAMPATH_SENDERS.toList()
            val marks = senders.joinToString(",") { "?" }
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.ADDRESS} COLLATE NOCASE IN ($marks) AND ${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} < ?",
                (senders + from.toEpochMilli().toString() + until.toEpochMilli().toString()).toTypedArray(),
                null,
            )?.use { rows ->
                buildList {
                    while (rows.moveToNext()) {
                        val body = rows.getString(1) ?: continue
                        parseSampathSms(rows.getString(0).orEmpty(), body, Instant.ofEpochMilli(rows.getLong(2)))?.let(::add)
                    }
                }
            }.orEmpty() // null on a device with no SMS provider
        }
}
