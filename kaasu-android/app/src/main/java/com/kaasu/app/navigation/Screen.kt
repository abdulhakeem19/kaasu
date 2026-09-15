package com.kaasu.app.navigation

sealed class Screen(val route: String) {
    // Onboarding graph
    // Onboarding is a single screen. Three more routes were declared here — privacy_promise,
    // notification_permission and budget_setup — with no composable() registered against any of
    // them, so navigating to one would have thrown rather than shown anything.
    data object Onboarding : Screen("onboarding")

    // Main graph (bottom nav)
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object Reports : Screen("reports")
    data object Settings : Screen("settings")

    // Transaction detail and add/edit — no bottom bar
    data object TransactionDetail : Screen("transaction/{transactionId}") {
        fun createRoute(id: Long) = "transaction/$id"
    }
    data object AddEditTransaction : Screen("transaction/add_edit?transactionId={transactionId}") {
        fun createRoute(id: Long? = null) =
            if (id != null) "transaction/add_edit?transactionId=$id"
            else "transaction/add_edit"
    }

    // Category management — no bottom bar
    data object Categories : Screen("categories")

    // Account management — no bottom bar
    data object Accounts : Screen("accounts")
    data object AddEditAccount : Screen("account/add_edit?accountId={accountId}") {
        fun createRoute(id: Long? = null) =
            if (id != null) "account/add_edit?accountId=$id"
            else "account/add_edit"
    }

    // Privacy & data — no bottom bar
    data object PrivacyData : Screen("privacy_data")
    data object AddEditCategory : Screen("category/add_edit?categoryId={categoryId}") {
        fun createRoute(id: Long? = null) =
            if (id != null) "category/add_edit?categoryId=$id"
            else "category/add_edit"
    }

    // Profile — no bottom bar
    data object Profile : Screen("profile")

    // Budgets — no bottom bar
    data object Budgets : Screen("budgets")

    // Bank sources — no bottom bar
    data object BankSources : Screen("bank_sources")

    // SMS sources — no bottom bar
    data object SmsSources : Screen("sms_sources")

    // Subscriptions — no bottom bar
    data object Subscriptions : Screen("subscriptions")
    data object SubscriptionDetail : Screen("subscription/{merchant}") {
        fun createRoute(merchant: String) = "subscription/${android.net.Uri.encode(merchant)}"
    }

    // Needs-a-tag review queue — no bottom bar
    data object NeedsTag : Screen("needs_tag")

    // Duplicates manager — no bottom bar
    data object Duplicates : Screen("duplicates")

    // Settings sub-screens — no bottom bar
    data object AppLock : Screen("app_lock")
    data object Help : Screen("help")
    data object About : Screen("about")
    data object MerchantRules : Screen("merchant_rules")
    data object Legal : Screen("legal/{doc}") {
        fun createRoute(doc: String) = "legal/$doc"
    }

    // Statement import (Settings → "Import bank statement") — no bottom bar. uri/mimeType are
    // Uri.encode()'d since they carry arbitrary content-resolver URI/MIME-type strings.
    data object ImportStatement : Screen("import_statement/{uri}/{mimeType}") {
        fun createRoute(uri: String, mimeType: String) =
            "import_statement/${android.net.Uri.encode(uri)}/${android.net.Uri.encode(mimeType)}"
    }
}
