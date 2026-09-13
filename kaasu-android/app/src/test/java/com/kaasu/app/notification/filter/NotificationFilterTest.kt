package com.kaasu.app.notification.filter

import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.IgnoredPatternDao
import com.kaasu.app.core.database.entity.AppSourceEntity
import com.kaasu.app.core.database.entity.IgnoredPatternEntity
import com.kaasu.app.notification.model.RawNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationFilterTest {

    private lateinit var filter: NotificationFilter

    @Before
    fun setUp() {
        // No user-added bank sources; rely on the built-in allowlist.
        filter = NotificationFilter(FakeAppSourceDao(), FakeIgnoredPatternDao())
    }

    private fun notif(pkg: String, text: String) =
        RawNotification(packageName = pkg, appName = null, title = null, text = text, subText = null)

    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val messages = "com.google.android.apps.messaging"
    private val swiggy = "in.swiggy.android"
    private val whatsapp = "com.whatsapp"

    // ── Payment apps are always processed (parser handles their marketing) ────

    @Test fun payment_app_processed() = runTest {
        assertTrue(filter.shouldProcess(notif(gpay, "₹500 paid to Swiggy")))
    }

    // ── Non-allowlisted apps are dropped regardless of content ────────────────

    @Test fun shopping_app_marketing_dropped() = runTest {
        assertFalse(filter.shouldProcess(notif(swiggy, "Get Doritos Nachos @ ₹70 and Coca-Cola @ ₹33")))
    }

    @Test fun shopping_app_even_with_verb_dropped() = runTest {
        // Even if a non-allowlisted app uses a transaction verb, it isn't a payment/SMS source.
        assertFalse(filter.shouldProcess(notif(swiggy, "₹500 debited for your order")))
    }

    @Test fun chat_app_dropped() = runTest {
        assertFalse(filter.shouldProcess(notif(whatsapp, "Hey, can you send me ₹500?")))
    }

    // ── Messaging app: only genuine bank transaction SMS pass ─────────────────

    @Test fun bank_sms_via_messages_processed() = runTest {
        assertTrue(filter.shouldProcess(notif(messages, "A/c XX1234 debited by Rs.5000. Avbl Bal: Rs.12,345.67")))
    }

    @Test fun promo_sms_via_messages_dropped() = runTest {
        assertFalse(filter.shouldProcess(notif(messages, "Sale! Flat ₹200 off on your next recharge")))
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeAppSourceDao : AppSourceDao {
        override suspend fun insert(appSource: AppSourceEntity): Long = 0
        override suspend fun update(appSource: AppSourceEntity) {}
        override fun getAll(): Flow<List<AppSourceEntity>> = flowOf(emptyList())
        override suspend fun getByPackage(packageName: String): AppSourceEntity? = null
        override suspend fun getKnownFinanceApps(): List<AppSourceEntity> = emptyList()
        override suspend fun updateLastSeen(packageName: String, time: Long) {}
        override suspend fun updateLastAccessibilityScrapeAttempt(packageName: String, time: Long) {}
        override suspend fun updateLastAccessibilityScrapeSuccess(packageName: String, time: Long) {}
    }

    private class FakeIgnoredPatternDao : IgnoredPatternDao {
        override suspend fun insert(pattern: IgnoredPatternEntity): Long = 0
        override suspend fun delete(id: Long) {}
        override fun getAll(): Flow<List<IgnoredPatternEntity>> = flowOf(emptyList())
        override suspend fun getAllPatterns(): List<String> = emptyList()
    }
}
