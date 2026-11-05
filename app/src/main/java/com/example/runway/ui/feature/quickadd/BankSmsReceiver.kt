package com.example.runway.ui.feature.quickadd

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.runway.MainActivity
import com.example.runway.R
import com.example.runway.core.money.formatLkr
import com.example.runway.domain.model.MovementDirection
import com.example.runway.domain.usecase.parseSampathSms
import java.math.BigDecimal
import java.time.Instant

const val EXTRA_IMPORT_AMOUNT = "import_amount"
private const val CHANNEL_ID = "bank_sms"
private const val ASK_FOR_MILLIS = 60 * 60 * 1000L // an ongoing prompt must not stay forever

/** A Sampath debit asks to be added. Nothing is saved until the user saves it in Quick Add. */
class BankSmsReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // areNotificationsEnabled covers POST_NOTIFICATIONS
    override fun onReceive(context: Context, intent: Intent) {
        // A long message arrives in parts
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent).filterNotNull()
        val sender = parts.firstOrNull()?.originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val movement = parseSampathSms(sender, body, Instant.now()) ?: return
        // ponytail: credits aren't offered because Quick Add only saves expenses, send them to Log income if wanted
        if (movement.direction != MovementDirection.OUT) return

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.bank_sms_channel))
                .build(),
        )
        val id = movement.at.toEpochMilli().toInt()
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_IMPORT_AMOUNT, BigDecimal.valueOf(movement.amountMinor, 2).toPlainString())
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val amount = movement.amountMinor.formatLkr(cents = movement.amountMinor % 100 != 0L)
        manager.notify(
            id,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash)
                .setContentTitle(context.getString(R.string.bank_sms_title, amount))
                .setContentText(context.getString(R.string.bank_sms_body, movement.accountSuffix))
                .setStyle(NotificationCompat.BigTextStyle())
                // Live Update on Android 16+. Older versions can't swipe away ongoing, so they get a plain one.
                .setOngoing(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(amount)
                .setTimeoutAfter(ASK_FOR_MILLIS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // no amounts on the lock screen
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
                .build(),
        )
    }
}
