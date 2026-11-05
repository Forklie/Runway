package com.example.runway.ui.feature.quickadd

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.runway.MainActivity
import com.example.runway.R
import com.example.runway.core.di.DispatcherProvider
import com.example.runway.core.event.AppEvent
import com.example.runway.core.event.AppEvents
import com.example.runway.core.money.formatLkr
import com.example.runway.core.time.TimeProvider
import com.example.runway.domain.model.AccountMovement
import com.example.runway.domain.model.MovementDirection
import com.example.runway.domain.repository.TransactionRepository
import com.example.runway.domain.usecase.hasExpenseOf
import com.example.runway.domain.usecase.parseSampathSms
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

const val EXTRA_IMPORT_MINOR = "import_minor"
const val EXTRA_IMPORT_AT = "import_at"
const val EXTRA_IMPORT_NOTE = "import_note"
private const val CHANNEL_ID = "bank_sms"
private const val LOOKUP_MILLIS = 3_000L // goAsync gets about 10 seconds. Asking twice beats never asking.

/**
 * A Sampath debit that isn't logged yet asks to be added: in the app if it's open, otherwise as a
 * notification. Nothing is saved until the user saves it in Quick Add.
 */
@AndroidEntryPoint
class BankSmsReceiver : BroadcastReceiver() {

    @Inject lateinit var transactions: TransactionRepository
    @Inject lateinit var time: TimeProvider
    @Inject lateinit var dispatchers: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        // A long message arrives in parts
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent).filterNotNull()
        val sender = parts.firstOrNull()?.originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val movement = parseSampathSms(sender, body, Instant.now()) ?: return
        // ponytail: credits aren't offered because Quick Add only saves expenses, send them to Log income if wanted
        if (movement.direction != MovementDirection.OUT) return

        val pending = goAsync()
        CoroutineScope(dispatchers.io).launch {
            try {
                if (!alreadyLogged(movement.amountMinor)) ask(context, movement)
            } finally {
                pending.finish()
            }
        }
    }

    /** False if the lookup fails or is slow, for example when signed out. */
    private suspend fun alreadyLogged(minor: Long): Boolean = runCatching {
        val today = time.today().atStartOfDay(time.zone).toInstant()
        withTimeoutOrNull(LOOKUP_MILLIS) { transactions.observeSince(today).first() }.orEmpty().hasExpenseOf(minor)
    }.getOrDefault(false)

    private fun ask(context: Context, movement: AccountMovement) {
        val state = ActivityManager.RunningAppProcessInfo().also(ActivityManager::getMyMemoryState)
        if (state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            AppEvents.emit(AppEvent.BankDebit(movement.amountMinor, movement.at, movement.description))
        } else {
            notify(context, movement)
        }
    }

    @SuppressLint("MissingPermission") // areNotificationsEnabled covers POST_NOTIFICATIONS
    private fun notify(context: Context, movement: AccountMovement) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.bank_sms_channel))
                .build(),
        )
        val id = movement.at.toEpochMilli().toInt()
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_IMPORT_MINOR, movement.amountMinor)
            .putExtra(EXTRA_IMPORT_AT, movement.at.toEpochMilli())
            .putExtra(EXTRA_IMPORT_NOTE, movement.description)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val amount = movement.amountMinor.formatLkr(cents = movement.amountMinor % 100 != 0L)
        manager.notify(
            id,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash)
                .setContentTitle(context.getString(R.string.bank_sms_title, amount))
                .setContentText(
                    if (movement.description.isBlank()) context.getString(R.string.bank_sms_body, movement.accountSuffix)
                    else context.getString(R.string.bank_sms_body_at, movement.description, movement.accountSuffix),
                )
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // no amounts on the lock screen
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
                .build(),
        )
    }
}
