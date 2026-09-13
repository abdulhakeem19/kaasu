package com.kaasu.app.core.backup

import android.content.Context
import android.net.Uri
import com.kaasu.app.core.database.dao.AccountDao
import com.kaasu.app.core.database.dao.CategoryDao
import com.kaasu.app.core.database.dao.MerchantAliasDao
import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.dao.TransactionDao
import com.kaasu.app.core.database.entity.AccountEntity
import com.kaasu.app.core.database.entity.CategoryEntity
import com.kaasu.app.core.database.entity.MerchantAliasEntity
import com.kaasu.app.core.database.entity.RuleEntity
import com.kaasu.app.core.database.entity.TransactionEntity
import com.kaasu.app.core.datastore.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full local JSON backup & restore of the user's data (transactions, categories, accounts, rules,
 * merchant aliases) plus the monthly-budget setting. The backup is a faithful snapshot: restore
 * wipes those tables and re-inserts every row preserving its primary key, so category/account
 * references in transactions stay valid. Stays entirely on-device; the user chooses where to save
 * or load the file.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val ruleDao: RuleDao,
    private val merchantAliasDao: MerchantAliasDao,
    private val settings: SettingsDataStore,
) {
    companion object {
        const val VERSION = 1
        const val FILE_NAME = "kaasu_backup.json"
    }

    // ── Export ──────────────────────────────────────────────────────────────

    suspend fun exportToFile(): File {
        val root = JSONObject().apply {
            put("version", VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("monthlyBudgetInPaise", settings.monthlyBudgetInPaise.first())
            put("transactions", transactionDao.getAllForBackup().toJsonArray(::txToJson))
            put("categories", categoryDao.getAllForBackup().toJsonArray(::catToJson))
            put("accounts", accountDao.getAllForBackup().toJsonArray(::accToJson))
            put("rules", ruleDao.getAllForBackup().toJsonArray(::ruleToJson))
            put("merchantAliases", merchantAliasDao.getAll().toJsonArray(::aliasToJson))
        }
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        return File(dir, FILE_NAME).apply { writeText(root.toString(2)) }
    }

    // ── Import ──────────────────────────────────────────────────────────────

    /** Restores from a backup uri. Returns the number of transactions restored, or throws. */
    suspend fun importFromUri(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IllegalArgumentException("Could not read the selected file")
        val root = JSONObject(text)
        require(root.optInt("version", -1) in 1..VERSION) { "Unsupported backup version" }

        val transactions = root.getJSONArray("transactions").map(::jsonToTx)
        val categories = root.getJSONArray("categories").map(::jsonToCat)
        val accounts = root.getJSONArray("accounts").map(::jsonToAcc)
        val rules = root.getJSONArray("rules").map(::jsonToRule)
        val aliases = root.optJSONArray("merchantAliases")?.map(::jsonToAlias) ?: emptyList()

        // Wipe then restore, preserving primary keys so references stay intact.
        transactionDao.deleteAll()
        ruleDao.deleteAllRules()
        merchantAliasDao.deleteAll()
        accountDao.deleteAll()
        categoryDao.deleteAllIncludingDefaults()

        categoryDao.insertAllReplace(categories)
        accountDao.insertAll(accounts)
        ruleDao.insertAll(rules)
        aliases.forEach { merchantAliasDao.upsert(it) }
        transactionDao.insertAll(transactions)

        if (root.has("monthlyBudgetInPaise")) {
            settings.setMonthlyBudget(root.getLong("monthlyBudgetInPaise"))
        }
        return transactions.size
    }

    // ── Entity ↔ JSON ───────────────────────────────────────────────────────

    private fun txToJson(t: TransactionEntity) = JSONObject().apply {
        put("id", t.id); put("amountInPaise", t.amountInPaise); put("currency", t.currency)
        put("type", t.type); putN("merchantName", t.merchantName); putN("categoryId", t.categoryId)
        putN("sourceAppPackage", t.sourceAppPackage); putN("sourceAppName", t.sourceAppName)
        putN("paymentMode", t.paymentMode); putN("rawText", t.rawText); putN("rawTextHash", t.rawTextHash)
        put("confidenceScore", t.confidenceScore); put("transactionTime", t.transactionTime)
        put("createdAt", t.createdAt); put("updatedAt", t.updatedAt)
        put("isManual", t.isManual); put("isTransfer", t.isTransfer); put("isRefund", t.isRefund)
        put("isIgnored", t.isIgnored); putN("note", t.note); putN("accountId", t.accountId)
        put("isRecurring", t.isRecurring); putN("parentId", t.parentId)
        put("isDuplicate", t.isDuplicate)
    }

    private fun jsonToTx(o: JSONObject) = TransactionEntity(
        id = o.getLong("id"), amountInPaise = o.getLong("amountInPaise"), currency = o.optString("currency", "INR"),
        type = o.getString("type"), merchantName = o.strOrNull("merchantName"), categoryId = o.longOrNull("categoryId"),
        sourceAppPackage = o.strOrNull("sourceAppPackage"), sourceAppName = o.strOrNull("sourceAppName"),
        paymentMode = o.strOrNull("paymentMode"), rawText = o.strOrNull("rawText"), rawTextHash = o.strOrNull("rawTextHash"),
        confidenceScore = o.optInt("confidenceScore", 0), transactionTime = o.getLong("transactionTime"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt"),
        isManual = o.optBoolean("isManual"), isTransfer = o.optBoolean("isTransfer"), isRefund = o.optBoolean("isRefund"),
        isIgnored = o.optBoolean("isIgnored"), note = o.strOrNull("note"), accountId = o.longOrNull("accountId"),
        isRecurring = o.optBoolean("isRecurring"), parentId = o.longOrNull("parentId"),
        isDuplicate = o.optBoolean("isDuplicate")
    )

    private fun catToJson(c: CategoryEntity) = JSONObject().apply {
        put("id", c.id); put("name", c.name); putN("icon", c.icon); putN("color", c.color); put("type", c.type)
        putN("monthlyBudgetInPaise", c.monthlyBudgetInPaise); put("isDefault", c.isDefault); put("isArchived", c.isArchived)
        put("createdAt", c.createdAt); put("updatedAt", c.updatedAt)
    }

    private fun jsonToCat(o: JSONObject) = CategoryEntity(
        id = o.getLong("id"), name = o.getString("name"), icon = o.strOrNull("icon"), color = o.strOrNull("color"),
        type = o.getString("type"), monthlyBudgetInPaise = o.longOrNull("monthlyBudgetInPaise"),
        isDefault = o.optBoolean("isDefault"), isArchived = o.optBoolean("isArchived"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    private fun accToJson(a: AccountEntity) = JSONObject().apply {
        put("id", a.id); put("displayName", a.displayName); putN("lastFourDigits", a.lastFourDigits)
        put("accountType", a.accountType); putN("colorArgb", a.colorArgb); put("isActive", a.isActive); put("createdAt", a.createdAt)
    }

    private fun jsonToAcc(o: JSONObject) = AccountEntity(
        id = o.getLong("id"), displayName = o.getString("displayName"), lastFourDigits = o.strOrNull("lastFourDigits"),
        accountType = o.getString("accountType"), colorArgb = o.intOrNull("colorArgb"),
        isActive = o.optBoolean("isActive", true), createdAt = o.getLong("createdAt")
    )

    private fun ruleToJson(r: RuleEntity) = JSONObject().apply {
        put("id", r.id); putN("name", r.name); put("matchText", r.matchText); put("matchType", r.matchType)
        putN("categoryId", r.categoryId); putN("transactionType", r.transactionType); putN("sourceAppPackage", r.sourceAppPackage)
        put("priority", r.priority); put("isSystem", r.isSystem); put("isActive", r.isActive)
        put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
    }

    private fun jsonToRule(o: JSONObject) = RuleEntity(
        id = o.getLong("id"), name = o.strOrNull("name"), matchText = o.getString("matchText"), matchType = o.getString("matchType"),
        categoryId = o.longOrNull("categoryId"), transactionType = o.strOrNull("transactionType"), sourceAppPackage = o.strOrNull("sourceAppPackage"),
        priority = o.optInt("priority", 0), isSystem = o.optBoolean("isSystem"), isActive = o.optBoolean("isActive", true),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    private fun aliasToJson(a: MerchantAliasEntity) = JSONObject().apply {
        put("originalLower", a.originalLower); put("displayName", a.displayName); put("createdAt", a.createdAt); put("updatedAt", a.updatedAt)
    }

    private fun jsonToAlias(o: JSONObject) = MerchantAliasEntity(
        originalLower = o.getString("originalLower"), displayName = o.getString("displayName"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun <T> List<T>.toJsonArray(map: (T) -> JSONObject) = JSONArray().also { arr -> forEach { arr.put(map(it)) } }
    private fun <T> JSONArray.map(map: (JSONObject) -> T): List<T> = (0 until length()).map { map(getJSONObject(it)) }
    private fun JSONObject.putN(key: String, v: Any?) { put(key, v ?: JSONObject.NULL) }
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
    private fun JSONObject.strOrNull(key: String): String? = if (isNull(key)) null else getString(key)
}
