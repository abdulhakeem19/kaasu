package com.kaasu.app.core.database.seed

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseSeeder : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val now = System.currentTimeMillis()
        seedCategories(db, now)
        seedAppSources(db, now)
        seedIgnoredPatterns(db, now)
        seedRules(db, now)
    }

    private fun seedCategories(db: SupportSQLiteDatabase, now: Long) {
        data class Seed(val name: String, val icon: String, val color: String, val type: String)

        val expense = "EXPENSE"
        val income = "INCOME"
        val system = "SYSTEM"

        val categories = listOf(
            Seed("Food",            "restaurant",           "#E53935", expense),
            Seed("Travel",          "directions_car",       "#1E88E5", expense),
            Seed("Shopping",        "shopping_bag",         "#8E24AA", expense),
            Seed("Bills",           "receipt_long",         "#F57C00", expense),
            Seed("Recharge",        "smartphone",           "#00ACC1", expense),
            Seed("Subscriptions",   "subscriptions",        "#6D4C41", expense),
            Seed("Groceries",       "local_grocery_store",  "#43A047", expense),
            Seed("Rent",            "home",                 "#5E35B1", expense),
            Seed("Health",          "local_hospital",       "#E91E63", expense),
            Seed("Education",       "school",               "#1976D2", expense),
            Seed("Entertainment",   "movie",                "#FB8C00", expense),
            Seed("Fuel",            "local_gas_station",    "#757575", expense),
            Seed("Family",          "family_restroom",      "#C0CA33", expense),
            Seed("Personal",        "person",               "#26A69A", expense),
            Seed("Cash",            "payments",             "#78909C", expense),
            Seed("Other",           "category",             "#90A4AE", expense),
            Seed("Uncategorized",   "help_outline",         "#BDBDBD", expense),
            Seed("Salary",          "account_balance_wallet","#2E7D32", income),
            Seed("Freelance",       "work",                 "#1565C0", income),
            Seed("Refund",          "undo",                 "#00695C", income),
            Seed("Cashback",        "redeem",               "#F9A825", income),
            Seed("Transfer In",     "south",                "#4527A0", income),
            Seed("Other Income",    "attach_money",         "#558B2F", income),
            Seed("Transfer",        "swap_horiz",           "#546E7A", system),
            Seed("Ignored",         "block",                "#9E9E9E", system),
        )

        categories.forEach { cat ->
            db.execSQL(
                "INSERT INTO categories (name, icon, color, type, monthlyBudgetInPaise, isDefault, isArchived, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, NULL, 1, 0, ?, ?)",
                arrayOf<Any?>(cat.name, cat.icon, cat.color, cat.type, now, now)
            )
        }
    }

    private fun seedAppSources(db: SupportSQLiteDatabase, now: Long) {
        data class Seed(val pkg: String, val name: String)

        val apps = listOf(
            Seed("com.google.android.apps.nbu.paisa.user", "Google Pay"),
            Seed("com.phonepe.app",                        "PhonePe"),
            Seed("net.one97.paytm",                        "Paytm"),
            Seed("in.org.npci.upiapp",                     "BHIM"),
            Seed("com.amazon.mShop.android.shopping",      "Amazon Pay"),
            Seed("com.csam.icici.bank.imobile",            "iMobile Pay"),
            Seed("com.sbi.lotusintouch",                   "YONO SBI"),
            Seed("com.axis.mobile",                        "Axis Mobile"),
            Seed("com.mgs.indusind",                       "IndusMobile"),
            Seed("com.idfcfirstbank.optimus",              "IDFC FIRST Bank"),
            Seed("com.sbi.card",                           "SBI Card"),
            Seed("com.slicepay",                           "Slice"),
            Seed("com.UnionBank.retail",                   "Union Bank"),
            Seed("com.dreamplug.androidapp",               "CRED"),
        )

        apps.forEach { app ->
            db.execSQL(
                "INSERT INTO app_sources (packageName, appName, isEnabled, isKnownFinanceApp, lastSeenAt, createdAt, updatedAt) " +
                "VALUES (?, ?, 1, 1, NULL, ?, ?)",
                arrayOf<Any?>(app.pkg, app.name, now, now)
            )
        }
    }

    private fun seedIgnoredPatterns(db: SupportSQLiteDatabase, now: Long) {
        val patterns = listOf(
            // "otp" alone is intentionally NOT used — many banks append "Never Share OTP/PIN/CVV"
            // to ALL transaction SMS, which would cause the broad "otp" pattern to filter out
            // legitimate credits. These specific phrases match only actual OTP delivery messages.
            "is the otp" to "OTP delivery message (e.g. '090523 is the OTP for Trxn.')",
            "otp is" to "OTP confirmation (e.g. 'OTP is valid for 10 mins')",
            "otp for" to "OTP delivery for a transaction",
            "your otp" to "OTP delivery (e.g. 'Your OTP is 123456')",
            "one time password" to "Spelled-out OTP delivery messages",
            "offer" to "Promotional offer notifications",
            "use code" to "Promotional coupon-code messages",
            "% off" to "Discount promotions",
            "cashback offer" to "Promotional cashback offers",
            "voucher" to "Promotional voucher messages",
            "apply now" to "Promotional call-to-action messages",
            "loan" to "Loan offer notifications",
            "pre-approved" to "Pre-approved credit offers",
            "reward points" to "Loyalty points updates",
            "statement generated" to "Monthly statement alerts",
            "due date" to "Bill due date reminders",
            "low balance" to "Balance alert notifications",
            "declined" to "Failed/declined transactions should not be recorded",
            "e-statement" to "Electronic statement delivery alerts",
            "bill due" to "Credit card bill due reminders",
            "bill generated" to "Credit card bill generation alerts",
            "min due" to "Minimum payment due reminders",
            "total due" to "Total amount due reminders",
            // Only the e-mandate SETUP reminders are ignored — an actual e-mandate execution
            // (a real debit) is allowed through and flagged recurring by RecurringDetector.
            "mandate registered" to "e-mandate setup confirmation (not a charge)",
            "mandate created" to "e-mandate setup confirmation (not a charge)",
            "will be debited" to "Upcoming auto-debit reminder (not a charge yet)",
        )

        patterns.forEach { (pattern, reason) ->
            db.execSQL(
                "INSERT INTO ignored_patterns (pattern, reason, sourceAppPackage, isSystem, createdAt, updatedAt) " +
                "VALUES (?, ?, NULL, 1, ?, ?)",
                arrayOf<Any?>(pattern, reason, now, now)
            )
        }
    }

    private fun seedRules(db: SupportSQLiteDatabase, now: Long) {
        // Uses a subquery to look up the category ID by name — no hardcoded IDs needed.
        // If the SELECT returns no rows (category missing), the INSERT is silently skipped.
        data class Seed(val name: String, val matchText: String, val category: String)

        val rules = listOf(
            // Food & Dining
            Seed("Swiggy",      "swiggy",       "Food"),
            Seed("Zomato",      "zomato",       "Food"),
            Seed("Domino's",    "domino",       "Food"),
            Seed("McDonald's",  "mcdonald",     "Food"),
            Seed("KFC",         "kfc",          "Food"),
            Seed("Subway",      "subway",       "Food"),
            Seed("Blinkit",     "blinkit",      "Groceries"),
            // Groceries
            Seed("BigBasket",   "bigbasket",    "Groceries"),
            Seed("Zepto",       "zepto",        "Groceries"),
            Seed("Dunzo",       "dunzo",        "Groceries"),
            Seed("JioMart",     "jiomart",      "Groceries"),
            Seed("DMart",       "dmart",        "Groceries"),
            // Shopping
            Seed("Amazon",      "amazon",       "Shopping"),
            Seed("Flipkart",    "flipkart",     "Shopping"),
            Seed("Myntra",      "myntra",       "Shopping"),
            Seed("Meesho",      "meesho",       "Shopping"),
            Seed("Ajio",        "ajio",         "Shopping"),
            Seed("Nykaa",       "nykaa",        "Shopping"),
            // Travel
            Seed("Uber",        "uber",         "Travel"),
            Seed("Ola",         "ola cabs",     "Travel"),
            Seed("Rapido",      "rapido",       "Travel"),
            Seed("IRCTC",       "irctc",        "Travel"),
            Seed("MakeMyTrip",  "makemytrip",   "Travel"),
            Seed("RedBus",      "redbus",       "Travel"),
            Seed("Ixigo",       "ixigo",        "Travel"),
            // Fuel
            Seed("Petrol",      "petrol",       "Fuel"),
            Seed("HP Petrol",   "hp petrol",    "Fuel"),
            Seed("Indian Oil",  "iocl",         "Fuel"),
            // Recharge
            Seed("Jio",         "jio",          "Recharge"),
            Seed("Airtel",      "airtel",       "Recharge"),
            Seed("Vi",          "vodafone",     "Recharge"),
            Seed("BSNL",        "bsnl",         "Recharge"),
            // Subscriptions
            Seed("Netflix",     "netflix",      "Subscriptions"),
            Seed("Spotify",     "spotify",      "Subscriptions"),
            Seed("Hotstar",     "hotstar",      "Subscriptions"),
            Seed("YouTube",     "youtube",      "Subscriptions"),
            Seed("Zee5",        "zee5",         "Subscriptions"),
            // Health
            Seed("Apollo",      "apollo",       "Health"),
            Seed("1mg",         "1mg",          "Health"),
            Seed("Pharmeasy",   "pharmeasy",    "Health"),
            Seed("Practo",      "practo",       "Health"),
            Seed("Netmeds",     "netmeds",      "Health"),
            // Bills
            Seed("Electricity", "electricity",  "Bills"),
            Seed("Broadband",   "broadband",    "Bills"),
            Seed("Insurance",   "insurance",    "Bills"),
            // Credit-card bill payments (merchant is typically "<Bank> Credit Card")
            Seed("Credit Card", "credit card",  "Bills"),
            Seed("Card Bill",   "card bill",     "Bills"),
            // Income keywords (match on merchantName from notification)
            Seed("Salary",      "salary",       "Salary"),
            Seed("Freelance",   "freelance",    "Freelance"),
        )

        rules.forEach { rule ->
            db.execSQL(
                """INSERT INTO rules (name, matchText, matchType, categoryId, transactionType,
                   sourceAppPackage, priority, isSystem, isActive, createdAt, updatedAt)
                   SELECT ?, ?, 'CONTAINS', id, NULL, NULL, 10, 1, 1, ?, ?
                   FROM categories WHERE name = ? LIMIT 1""",
                arrayOf<Any?>(rule.name, rule.matchText, now, now, rule.category)
            )
        }
    }
}
