package com.kaasu.app.notification.listener

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kaasu.app.capture.TransactionCapturePipeline
import com.kaasu.app.di.ApplicationScope
import com.kaasu.app.notification.filter.NotificationFilter
import com.kaasu.app.notification.model.RawNotification
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class KaasuNotificationListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationServiceEntryPoint {
        fun notificationFilter(): NotificationFilter
        fun transactionCapturePipeline(): TransactionCapturePipeline
        @ApplicationScope fun applicationScope(): CoroutineScope
    }

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationServiceEntryPoint::class.java
        )
    }

    // Android system callback — fires for every new notification on the device; wraps it into RawNotification and hands off to process()
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification?.extras ?: return

        val raw = RawNotification(
            packageName = sbn.packageName,
            appName = null,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            postedAt = sbn.postTime
        )

        entryPoint.applicationScope().launch { process(raw) }
    }

    // Gate: NotificationFilter → delegate to the shared TransactionCapturePipeline
    private suspend fun process(raw: RawNotification) {
        val ep = entryPoint
        if (!ep.notificationFilter().shouldProcess(raw)) return
        ep.transactionCapturePipeline().process(raw)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit
}
