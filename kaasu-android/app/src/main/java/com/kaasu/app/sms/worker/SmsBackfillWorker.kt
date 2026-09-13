package com.kaasu.app.sms.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kaasu.app.capture.TransactionCapturePipeline
import com.kaasu.app.core.util.Hashing
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.notification.filter.SmsFilter
import com.kaasu.app.sms.SmsRawNotificationFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-time historical backfill of the device's SMS inbox, run once after the user grants
 * READ_SMS during onboarding (or on-demand via "Re-scan SMS inbox" in Settings). Idempotent:
 * re-running skips any row whose rawTextHash already exists in the transactions table, so it's
 * safe to invoke via [rescan] as often as the user likes.
 */
@HiltWorker
class SmsBackfillWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val smsFilter: SmsFilter,
    private val transactionCapturePipeline: TransactionCapturePipeline,
    private val transactionRepository: TransactionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val resolver = applicationContext.contentResolver
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        var inserted = 0
        var skipped = 0
        var processed = 0

        resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} ASC"
        )?.use { cursor ->
            val total = cursor.count
            val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIdx)
                val body = cursor.getString(bodyIdx) ?: ""
                val timestamp = cursor.getLong(dateIdx)

                val raw = SmsRawNotificationFactory.build(address, body, timestamp)
                if (raw != null && smsFilter.shouldProcess(raw)) {
                    val hash = Hashing.sha256Prefix(raw.fullText())
                    if (transactionRepository.getByHash(hash) == null) {
                        if (transactionCapturePipeline.process(raw)) inserted++ else skipped++
                    } else {
                        skipped++
                    }
                } else {
                    skipped++
                }

                processed++
                if (processed % PROGRESS_STEP == 0) {
                    setProgress(
                        workDataOf(
                            KEY_PROCESSED to processed,
                            KEY_TOTAL to total,
                            KEY_INSERTED to inserted
                        )
                    )
                }
            }

            return Result.success(
                workDataOf(
                    KEY_INSERTED to inserted,
                    KEY_SKIPPED to skipped,
                    KEY_TOTAL to total
                )
            )
        }

        // Query returned null (shouldn't normally happen once permission is granted).
        return Result.success(workDataOf(KEY_INSERTED to 0, KEY_SKIPPED to 0, KEY_TOTAL to 0))
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sms_backfill"

        const val KEY_PROCESSED = "processed"
        const val KEY_TOTAL = "total"
        const val KEY_INSERTED = "inserted"
        const val KEY_SKIPPED = "skipped"

        private const val PROGRESS_STEP = 200

        // First run — only enqueues if no backfill has run/is running yet.
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SmsBackfillWorker>().build()
            )
        }

        // User-triggered re-scan (Settings → "Re-scan SMS inbox") — always replaces any prior run.
        fun rescan(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SmsBackfillWorker>().build()
            )
        }
    }
}
