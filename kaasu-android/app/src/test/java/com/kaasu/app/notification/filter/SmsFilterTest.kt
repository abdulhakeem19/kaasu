package com.kaasu.app.notification.filter

import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.core.database.dao.SmsSenderDao
import com.kaasu.app.core.database.entity.IgnoredPatternEntity
import com.kaasu.app.core.database.entity.SmsSenderEntity
import com.kaasu.app.notification.model.RawNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SmsFilterTest {

    private lateinit var senderDao: FakeSmsSenderDao
    private lateinit var patternDao: FakeIgnoredPatternDao
    private lateinit var filter: SmsFilter

    @Before
    fun setUp() {
        senderDao = FakeSmsSenderDao()
        patternDao = FakeIgnoredPatternDao()
        filter = SmsFilter(senderDao, patternDao)
    }

    private fun sms(sender: String, text: String) =
        RawNotification(packageName = "${SmsFilter.SMS_PACKAGE_PREFIX}$sender", appName = null, title = null, text = text, subText = null)

    // ── Mandatory content gate ─────────────────────────────────────────────

    @Test fun blank_text_dropped() = runTest {
        assertFalse(filter.shouldProcess(sms("HD-HDFCBK", "")))
    }

    @Test fun genuine_bank_sms_processed() = runTest {
        assertTrue(filter.shouldProcess(sms("HD-HDFCBK", "A/c XX1234 debited by Rs.5000. Avbl Bal: Rs.12,345.67")))
    }

    @Test fun promo_sms_dropped_even_from_bank_looking_sender() = runTest {
        // Sender format alone is never a hard gate — content still has to look financial.
        assertFalse(filter.shouldProcess(sms("HD-HDFCBK", "Sale! Flat Rs.200 off on your next recharge")))
    }

    @Test fun sender_with_non_dlt_format_still_processed_if_content_qualifies() = runTest {
        // Content is the mandatory gate; sender format is only ever a soft nudge in the parser.
        assertTrue(filter.shouldProcess(sms("VM-RANDOM1", "You sent to John Rs.500 from A/c XX9012")))
    }

    // ── Sender registry: auto-create on first sight, respect user-disabled senders ─────

    @Test fun first_sight_auto_creates_enabled_sender_row() = runTest {
        assertTrue(filter.shouldProcess(sms("HD-HDFCBK", "A/c XX1234 debited by Rs.5000")))
        val created = senderDao.getBySenderId("HD-HDFCBK")
        assertTrue(created != null && created.isEnabled)
    }

    @Test fun disabled_sender_dropped() = runTest {
        val now = System.currentTimeMillis()
        senderDao.insert(
            SmsSenderEntity(
                senderId = "HD-HDFCBK", displayLabel = null, isEnabled = false,
                isKnownFinanceSender = false, lastSeenAt = null, createdAt = now, updatedAt = now
            )
        )
        assertFalse(filter.shouldProcess(sms("HD-HDFCBK", "A/c XX1234 debited by Rs.5000")))
    }

    // ── Ignored patterns (same list NotificationFilter applies) ────────────

    @Test fun ignored_pattern_dropped() = runTest {
        patternDao.patterns.add("recharge")
        assertFalse(filter.shouldProcess(sms("HD-HDFCBK", "You sent to John Rs.500 for recharge from A/c XX9012")))
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeSmsSenderDao : SmsSenderDao {
        private val rows = mutableMapOf<String, SmsSenderEntity>()
        private var nextId = 1L

        override suspend fun insert(sender: SmsSenderEntity): Long {
            if (rows.containsKey(sender.senderId)) return -1
            rows[sender.senderId] = sender.copy(id = nextId++)
            return rows[sender.senderId]!!.id
        }

        override suspend fun update(sender: SmsSenderEntity) {
            rows[sender.senderId] = sender
        }

        override fun getAll(): Flow<List<SmsSenderEntity>> = flowOf(rows.values.toList())

        override suspend fun getBySenderId(senderId: String): SmsSenderEntity? = rows[senderId]

        override suspend fun updateLastSeen(senderId: String, time: Long) {
            rows[senderId]?.let { rows[senderId] = it.copy(lastSeenAt = time, updatedAt = time) }
        }
    }

    private class FakeIgnoredPatternDao : IgnoredPatternDao {
        val patterns = mutableListOf<String>()
        override suspend fun insert(pattern: IgnoredPatternEntity): Long = 0
        override suspend fun delete(id: Long) {}
        override fun getAll(): Flow<List<IgnoredPatternEntity>> = flowOf(emptyList())
        override suspend fun getAllPatterns(): List<String> = patterns
    }
}
