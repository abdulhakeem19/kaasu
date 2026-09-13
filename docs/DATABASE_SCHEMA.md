# Kaasu Database Schema

## Database

Use Room with SQLite.

Suggested database name:

```text
kaasu.db
```

## Schema Versioning

Start with:

```text
version = 1
```

Add migrations from the beginning even if the MVP is small.

---

# 1. transactions

Stores captured and manually added transactions.

## Fields

```text
id: Long, primary key, auto-generated
amount: Decimal/String
currency: String
type: String
merchantName: String?
categoryId: Long?
sourceAppPackage: String?
sourceAppName: String?
paymentMode: String?
rawText: String?
rawTextHash: String?
confidenceScore: Int
transactionTime: Long
createdAt: Long
updatedAt: Long
isManual: Boolean
isTransfer: Boolean
isRefund: Boolean
isIgnored: Boolean
note: String?
```

## Type Values

```text
EXPENSE
INCOME
TRANSFER
REFUND
CASHBACK
UNKNOWN
```

## Notes

- Store amount as integer paise or string decimal to avoid floating-point errors.
- Avoid logging raw text in release logs.
- `rawTextHash` helps duplicate detection.
- `rawText` can be optional or user-configurable if privacy concerns are strong.

---

# 2. categories

Stores default and user-defined categories.

## Fields

```text
id: Long, primary key, auto-generated
name: String
icon: String?
color: String?
type: String
monthlyBudget: Long?
isDefault: Boolean
isArchived: Boolean
createdAt: Long
updatedAt: Long
```

## Type Values

```text
EXPENSE
INCOME
SYSTEM
```

## Default Expense Categories

```text
Food
Travel
Shopping
Bills
Recharge
Subscriptions
Groceries
Rent
Health
Education
Entertainment
Fuel
Family
Personal
Cash
Other
Uncategorized
```

## Default Income Categories

```text
Salary
Freelance
Refund
Cashback
Transfer In
Other Income
```

## System Categories

```text
Transfer
Ignored
```

---

# 3. rules

Stores user-created and system categorization rules.

## Fields

```text
id: Long, primary key, auto-generated
name: String?
matchText: String
matchType: String
categoryId: Long?
transactionType: String?
sourceAppPackage: String?
priority: Int
isSystem: Boolean
isActive: Boolean
createdAt: Long
updatedAt: Long
```

## Match Type Values

```text
CONTAINS
STARTS_WITH
ENDS_WITH
EQUALS
REGEX
SOURCE_APP
```

## Example Rules

```text
matchText = "swiggy", matchType = CONTAINS, category = Food
matchText = "zomato", matchType = CONTAINS, category = Food
matchText = "uber", matchType = CONTAINS, category = Travel
matchText = "credited", matchType = CONTAINS, transactionType = INCOME
```

---

# 4. app_sources

Stores known notification source apps.

## Fields

```text
id: Long, primary key, auto-generated
packageName: String
appName: String
isEnabled: Boolean
isKnownFinanceApp: Boolean
lastSeenAt: Long?
createdAt: Long
updatedAt: Long
```

## Examples

```text
com.google.android.apps.nbu.paisa.user     Google Pay
com.phonepe.app                            PhonePe
net.one97.paytm                            Paytm
in.org.npci.upiapp                         BHIM
```

Package names must be verified during implementation because apps can change package names or behavior.

---

# 5. monthly_budgets

Stores monthly budget configuration.

## Fields

```text
id: Long, primary key, auto-generated
month: Int
year: Int
amount: Long
createdAt: Long
updatedAt: Long
```

## Notes

- Amount can be stored in paise.
- Future: per-category budgets can live in categories or a separate table.

---

# 6. ignored_patterns

Stores patterns the user/system wants to ignore.

## Fields

```text
id: Long, primary key, auto-generated
pattern: String
reason: String?
sourceAppPackage: String?
isSystem: Boolean
createdAt: Long
updatedAt: Long
```

## Examples

```text
OTP
offer
loan
pre-approved
reward points
statement generated
```

---

# 7. parser_logs - Optional Debug Only

Use only in debug builds or beta builds.

## Fields

```text
id: Long, primary key, auto-generated
sourceAppPackage: String
rawTextHash: String
parseStatus: String
errorMessage: String?
createdAt: Long
```

## Do Not

- Store full raw notification text in release parser logs.
- Upload logs automatically.
- Include personal financial data in crash reports.

---

# Suggested Room Entities

## TransactionEntity

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountInPaise: Long,
    val currency: String = "INR",
    val type: String,
    val merchantName: String?,
    val categoryId: Long?,
    val sourceAppPackage: String?,
    val sourceAppName: String?,
    val paymentMode: String?,
    val rawText: String?,
    val rawTextHash: String?,
    val confidenceScore: Int,
    val transactionTime: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isManual: Boolean,
    val isTransfer: Boolean,
    val isRefund: Boolean,
    val isIgnored: Boolean,
    val note: String?
)
```

## CategoryEntity

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String?,
    val color: String?,
    val type: String,
    val monthlyBudgetInPaise: Long?,
    val isDefault: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

## RuleEntity

```kotlin
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val matchText: String,
    val matchType: String,
    val categoryId: Long?,
    val transactionType: String?,
    val sourceAppPackage: String?,
    val priority: Int,
    val isSystem: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

---

# Important Database Rules

- Do not use floating point for money.
- Use indexes for transaction time, category, source app, and raw hash.
- Use soft delete only if needed; otherwise hard delete for privacy.
- Provide full wipe option.
- Do not upload transaction data in MVP.
- Keep migrations clean from version 1.
