# Kaasu Technical Architecture

## 1. Architecture Goal

Build a reliable, local-first Android app that captures transaction notifications, parses them locally, stores them securely on-device, and presents simple spending insights.

## 2. Recommended Stack

```text
Language: Kotlin
UI: Jetpack Compose
Design System: Material 3
Architecture: MVVM or MVI
Database: Room
Preferences: DataStore
Async: Coroutines + Flow
Dependency Injection: Hilt
Background Tasks: WorkManager
Notification Capture: NotificationListenerService
Charts: Vico or Compose-compatible chart library
Export: Local CSV writer
```

## 3. Why Native Android

Kaasu depends on Android-specific behavior:

- Notification listener access
- Permission education
- Background behavior
- OEM-specific notification handling
- Local storage
- Play Store policy control
- Battery optimization testing

Native Kotlin gives better control than a cross-platform wrapper for this particular product.

## 4. High-Level Data Flow

```text
Payment/Bank Notification
    ↓
NotificationListenerService
    ↓
Notification Filter
    ↓
Transaction Parser
    ↓
Duplicate Checker
    ↓
Rule-Based Categorizer
    ↓
Room Database
    ↓
Repository
    ↓
Use Cases
    ↓
ViewModel
    ↓
Jetpack Compose UI
```

## 5. App Layers

## UI Layer

Responsible for screens, state rendering, and user interactions.

```text
feature/onboarding
feature/dashboard
feature/transactions
feature/reports
feature/categories
feature/rules
feature/settings
```

## Domain Layer

Responsible for business logic.

```text
domain/model
domain/usecase
domain/rule
domain/parser
```

## Data Layer

Responsible for database, repositories, mappers, and local data sources.

```text
data/repository
data/local
data/mapper
```

## Core Layer

Shared utilities.

```text
core/database
core/datastore
core/permissions
core/security
core/utils
```

## Notification Layer

Responsible for notification capture and parsing.

```text
notification/listener
notification/parser
notification/filter
notification/classifier
notification/duplicate
```

## 6. Suggested Package Structure

```text
com.kaasu.app/
  MainActivity.kt

  core/
    database/
      KaasuDatabase.kt
      dao/
      entity/
    datastore/
      SettingsDataStore.kt
    permissions/
      NotificationPermissionManager.kt
    security/
      LocalDataSecurity.kt
    utils/
      DateUtils.kt
      CurrencyUtils.kt
      TextNormalizer.kt

  data/
    repository/
      TransactionRepositoryImpl.kt
      CategoryRepositoryImpl.kt
      RuleRepositoryImpl.kt
    mapper/
      TransactionMapper.kt

  domain/
    model/
      Transaction.kt
      Category.kt
      Rule.kt
    usecase/
      CaptureTransactionUseCase.kt
      GetDashboardSummaryUseCase.kt
      AddManualExpenseUseCase.kt
      UpdateTransactionUseCase.kt
      ExportCsvUseCase.kt

  notification/
    listener/
      KaasuNotificationListenerService.kt
    filter/
      NotificationFilter.kt
    parser/
      TransactionParser.kt
      AmountParser.kt
      MerchantParser.kt
      TransactionTypeParser.kt
    duplicate/
      DuplicateTransactionChecker.kt
    classifier/
      CategoryRuleEngine.kt

  feature/
    onboarding/
    dashboard/
    transactions/
    reports/
    categories/
    rules/
    settings/

  ui/
    components/
    theme/
```

## 7. Notification Capture Design

Use `NotificationListenerService` to receive posted notifications after the user manually enables notification access from Android settings.

### Responsibilities

- Listen to posted notifications
- Read package name and notification text
- Ignore non-finance apps when possible
- Pass relevant notifications to parser
- Avoid heavy work in the service
- Save parsed transactions via repository/use case

### Avoid

- Reading or storing unrelated personal notifications
- Sending notification content to any server
- Long-running expensive work inside listener
- Requesting SMS permission in MVP

## 8. Parser Design

### Parser Pipeline

```text
RawNotification
  → NormalizeText
  → IsFinancialNotification
  → ExtractAmount
  → DetectTransactionType
  → ExtractMerchant
  → CalculateConfidence
  → CheckDuplicate
  → Categorize
  → Save
```

### Parser Output

```kotlin
data class ParsedTransaction(
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val merchantName: String?,
    val sourceAppPackage: String,
    val sourceAppName: String?,
    val rawText: String,
    val confidenceScore: Int,
    val transactionTime: Long
)
```

## 9. Duplicate Detection

Duplicates can happen when:

- Payment app and bank app both send notifications
- Notification updates are posted multiple times
- Same notification is restored after reboot
- Grouped notifications update

### Duplicate Heuristics

Compare:

- Amount
- Transaction type
- Merchant/person
- Source app
- Time window
- Raw text hash

### Suggested Rule

If amount, type, and similar merchant appear within 2 minutes, flag as possible duplicate.

## 10. Categorization

### Rule Priority

1. User custom rules
2. Known merchant rules
3. Source app hints
4. Keyword-based rules
5. Uncategorized

### Example Rule

```text
IF merchant contains "swiggy" THEN category = Food
IF text contains "credited" THEN type = Income
IF text contains "refund" THEN type = Refund
```

## 11. Local Storage

Use Room for structured data:

- Transactions
- Categories
- Rules
- Supported app sources
- Budgets
- Ignored patterns

Use DataStore for:

- Onboarding complete
- Permission explanation shown
- Monthly budget settings
- Theme
- Currency
- App lock setting later

## 12. Background Work

Use WorkManager only for deferrable tasks:

- Weekly/monthly report generation
- Export preparation
- Local cleanup
- Backup later

Do not use WorkManager for live notification capture. Notification capture belongs to `NotificationListenerService`.

## 13. Security

MVP security direction:

- Store data locally
- Do not upload transaction data
- Do not integrate analytics that capture transaction content
- Do not log raw notification text in release builds
- Delete local data fully when requested

Future:

- Encrypted backup
- App lock
- Optional biometric unlock
- Optional cloud sync with clear consent

## 14. Error Handling

### Parser Errors

- Save as uncategorized if valid amount exists
- Ignore low-confidence promotional notifications
- Allow user to report wrong parse

### Permission Errors

- Show disabled state
- Provide button to open Android settings
- Explain impact clearly

### Database Errors

- Avoid crash
- Show generic error
- Keep local backup/export path later

## 15. Testing Strategy

- Unit tests for parser
- Unit tests for rules
- Unit tests for duplicate detection
- DAO tests
- ViewModel tests
- Manual device tests for notification access
- OEM testing: Samsung, Redmi, Realme, OnePlus, Pixel/Motorola

## 16. Official References

- NotificationListenerService: https://developer.android.com/reference/kotlin/android/service/notification/NotificationListenerService
- Room: https://developer.android.com/jetpack/androidx/releases/room
- DataStore: https://developer.android.com/topic/libraries/architecture/datastore
- Background work: https://developer.android.com/develop/background-work/background-tasks
