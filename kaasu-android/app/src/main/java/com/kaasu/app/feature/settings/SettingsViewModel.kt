package com.kaasu.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kaasu.app.accessibility.scraper.ScraperRegistry
import com.kaasu.app.backfill.TransactionBackfillManager
import com.kaasu.app.core.backup.BackupManager
import com.kaasu.app.core.database.dao.AppSourceDao
import com.kaasu.app.core.database.dao.SmsSenderDao
import com.kaasu.app.core.database.entity.AppSourceEntity
import com.kaasu.app.core.datastore.SettingsDataStore
import com.kaasu.app.core.export.CsvExporter
import com.kaasu.app.domain.repository.AccountRepository
import com.kaasu.app.domain.repository.CategoryRepository
import com.kaasu.app.domain.repository.RuleRepository
import com.kaasu.app.domain.repository.TransactionRepository
import com.kaasu.app.sms.worker.SmsBackfillWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: RuleRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val appSourceDao: AppSourceDao,
    private val smsSenderDao: SmsSenderDao,
    private val backupManager: BackupManager,
    private val backfillManager: TransactionBackfillManager,
    private val scraperRegistry: ScraperRegistry,
    private val csvExporter: CsvExporter,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    // One-shot events: a file to share, or a toast/snackbar message.
    private val _shareFile = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val shareFile = _shareFile.asSharedFlow()
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message = _message.asSharedFlow()

    private fun uriFor(context: Context, file: java.io.File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Writes the backup into a file the owner picked via the system "save to files" dialog.
     *
     * Sharing was the only route out before, and a share sheet offers apps to send the file *to* —
     * on a device with no file-manager target listed there is simply no way to keep a copy. A
     * backup you cannot save anywhere is not a backup.
     */
    fun backupDataTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val file = backupManager.exportToFile()
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } ?: error("Could not open the selected location")
            }
                .onSuccess { _message.emit("Backup saved") }
                .onFailure { _message.emit("Backup failed: ${it.message}") }
        }
    }

    fun exportCsvTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val txns = transactionRepository.getAll().first()
                val cats = categoryRepository.getAllActive().first().associateBy { it.id }
                val file = csvExporter.export(txns, cats)
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } ?: error("Could not open the selected location")
            }
                .onSuccess { _message.emit("CSV saved") }
                .onFailure { _message.emit("Export failed: ${it.message}") }
        }
    }

    fun backupData(context: Context) {
        viewModelScope.launch {
            runCatching { backupManager.exportToFile() }
                .onSuccess { _shareFile.emit(uriFor(context, it)) }
                .onFailure { _message.emit("Backup failed: ${it.message}") }
        }
    }

    fun restoreData(uri: Uri) {
        viewModelScope.launch {
            runCatching { backupManager.importFromUri(uri) }
                .onSuccess { _message.emit("Restored $it transactions") }
                .onFailure { _message.emit("Restore failed: ${it.message}") }
        }
    }

    // Re-runs the current parser over transactions already stored, using the raw text each one
    // kept. Parser fixes otherwise only help future captures, leaving old rows stuck on whatever
    // the parser of the day produced. Enrich-only: never overwrites a name or category already set.
    private val _isRescanning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRescanning = _isRescanning.asStateFlow()

    fun rescanSavedTransactions() {
        if (_isRescanning.value) return
        viewModelScope.launch {
            _isRescanning.value = true
            runCatching { backfillManager.run() }
                .onSuccess { r ->
                    val parts = buildList {
                        if (r.merchantsFilled > 0) add("${r.merchantsFilled} name${if (r.merchantsFilled == 1) "" else "s"}")
                        if (r.categoriesFilled > 0) add("${r.categoriesFilled} categor${if (r.categoriesFilled == 1) "y" else "ies"}")
                        if (r.notesFilled > 0) add("${r.notesFilled} note${if (r.notesFilled == 1) "" else "s"}")
                    }
                    _message.emit(
                        if (parts.isEmpty()) "Re-scanned ${r.scanned} transactions — nothing new to fill"
                        else "Filled in " + parts.joinToString(", ") + " across ${r.scanned} transactions"
                    )
                }
                .onFailure { _message.emit("Re-scan failed: ${it.message}") }
            _isRescanning.value = false
        }
    }

    // Rows the current parser would now reject — legacy promo notifications and mandate pre-debit
    // notices that inflate spend totals. Marked ignored, never deleted, so this stays reversible.
    fun ignoreNonTransactions() {
        viewModelScope.launch {
            runCatching { backfillManager.ignoreNonTransactions() }
                .onSuccess { n ->
                    _message.emit(
                        if (n == 0) "No non-transactions found"
                        else "Excluded $n non-transaction${if (n == 1) "" else "s"} from your totals"
                    )
                }
                .onFailure { _message.emit("Cleanup failed: ${it.message}") }
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            runCatching {
                val txns = transactionRepository.getAll().first()
                val cats = categoryRepository.getAllActive().first().associateBy { it.id }
                csvExporter.export(txns, cats)
            }.onSuccess { _shareFile.emit(uriFor(context, it)) }
                .onFailure { _message.emit("Export failed: ${it.message}") }
        }
    }

    // Monthly budget — displayed in the budget row and edit dialog
    val monthlyBudgetInPaise = settingsDataStore.monthlyBudgetInPaise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    // App theme — "system" | "light" | "dark"
    val appTheme = settingsDataStore.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsDataStore.Theme.SYSTEM)

    val displayName = settingsDataStore.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // Count of active categories for the Categories row badge
    val categoryCount = categoryRepository.getAllActive()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Count of active rules for the Merchant rules row
    val ruleCount = ruleRepository.getActiveByPriority()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Account count for the profile row
    val accountCount = accountRepository.getAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // App sources (UPI/bank apps) for the Sources section
    /**
     * Whether the screen-reading channel has ever actually captured anything.
     *
     * This signal already existed per-app, three taps deep inside Bank Sources, and sat at "never
     * attempted" for the entire life of the channel without anyone noticing. A capture channel
     * that silently does nothing is the failure mode worth surfacing, so it is hoisted to the top
     * level of Settings.
     */
    val screenReadingHealth = appSourceDao.getAll()
        .map { sources ->
            val scraped = sources.filter { it.packageName in scraperRegistry.supportedPackages }
            ScreenReadingHealth(
                lastSuccessAt = scraped.mapNotNull { it.lastAccessibilityScrapeSuccessAt }.maxOrNull(),
                lastAttemptAt = scraped.mapNotNull { it.lastAccessibilityScrapeAttemptAt }.maxOrNull(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenReadingHealth())

    val appSources = appSourceDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // SMS senders (auto-discovered) for the SMS capture section
    val smsSenders = smsSenderDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Latest SMS backfill/re-scan WorkInfo, for the "Re-scan SMS inbox" row's progress display
    val smsBackfillWorkInfos = WorkManager.getInstance(appContext)
        .getWorkInfosForUniqueWorkFlow(SmsBackfillWorker.UNIQUE_WORK_NAME)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<WorkInfo>())

    fun rescanSms() {
        SmsBackfillWorker.rescan(appContext)
    }

    // Persisted preference toggles
    val hideAmountsOnLock = settingsDataStore.hideAmountsOnLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val appLockEnabled = settingsDataStore.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val biometricEnabled = settingsDataStore.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val budgetAlertsEnabled = settingsDataStore.budgetAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val subscriptionRenewalsEnabled = settingsDataStore.subscriptionRenewalsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val dailyNudgeEnabled = settingsDataStore.dailyNudgeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val weeklySummaryEnabled = settingsDataStore.weeklySummaryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleAppSource(entity: AppSourceEntity, enabled: Boolean) {
        viewModelScope.launch { appSourceDao.update(entity.copy(isEnabled = enabled)) }
    }

    fun addAppSource(packageName: String, appName: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            appSourceDao.insert(
                AppSourceEntity(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = true,
                    isKnownFinanceApp = true,
                    lastSeenAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun setHideAmountsOnLock(v: Boolean) { viewModelScope.launch { settingsDataStore.setHideAmountsOnLock(v) } }
    fun setBudgetAlerts(v: Boolean) { viewModelScope.launch { settingsDataStore.setBudgetAlertsEnabled(v) } }
    fun setSubscriptionRenewals(v: Boolean) { viewModelScope.launch { settingsDataStore.setSubscriptionRenewalsEnabled(v) } }
    fun setDailyNudge(v: Boolean) { viewModelScope.launch { settingsDataStore.setDailyNudgeEnabled(v) } }
    fun setWeeklySummary(v: Boolean) { viewModelScope.launch { settingsDataStore.setWeeklySummaryEnabled(v) } }

    fun setBudget(amountInPaise: Long) {
        viewModelScope.launch { settingsDataStore.setMonthlyBudget(amountInPaise) }
    }

    fun clearBudget() {
        viewModelScope.launch { settingsDataStore.setMonthlyBudget(0) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsDataStore.setAppTheme(theme) }
    }
}
