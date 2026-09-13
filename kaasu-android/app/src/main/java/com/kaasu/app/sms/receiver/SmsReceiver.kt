package com.kaasu.app.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.kaasu.app.capture.TransactionCapturePipeline
import com.kaasu.app.di.ApplicationScope
import com.kaasu.app.notification.filter.SmsFilter
import com.kaasu.app.sms.SmsRawNotificationFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Live SMS capture. Cannot be `@AndroidEntryPoint` — broadcast receivers aren't Hilt-injectable
 * that way — so it uses the same `@EntryPoint`-via-`EntryPointAccessors.fromApplication` pattern
 * as [com.kaasu.app.notification.listener.KaasuNotificationListenerService].
 */
class SmsReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun smsFilter(): SmsFilter
        fun transactionCapturePipeline(): TransactionCapturePipeline
        @ApplicationScope fun applicationScope(): CoroutineScope
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        // Reassemble multi-part messages arriving in the same broadcast.
        val fullBody = messages.joinToString("") { it.messageBody }
        val address = messages.first().originatingAddress
        val timestampMillis = messages.first().timestampMillis

        val raw = SmsRawNotificationFactory.build(address, fullBody, timestampMillis) ?: return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsReceiverEntryPoint::class.java
        )

        val pendingResult = goAsync()
        entryPoint.applicationScope().launch {
            try {
                if (entryPoint.smsFilter().shouldProcess(raw)) {
                    entryPoint.transactionCapturePipeline().process(raw)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
