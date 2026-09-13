package com.kaasu.app.notification.classifier

import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.entity.RuleEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRuleEngine @Inject constructor(
    private val ruleDao: RuleDao
) {
    // Called by KaasuNotificationListenerService.process(); returned categoryId passed to TransactionRepository.insertParsed()
    // Returns null → stored as "Uncategorized" in UI without requiring a hardcoded category row ID
    suspend fun classify(merchantName: String?, sourceAppPackage: String?): Long? {
        val rules = ruleDao.getActiveRulesList()
        for (rule in rules) {
            if (rule.categoryId == null) continue

            // Source-app-only rule (empty matchText): matches any transaction from this app
            if (rule.matchText.isBlank()) {
                if (rule.sourceAppPackage != null && rule.sourceAppPackage == sourceAppPackage) {
                    return rule.categoryId
                }
                continue
            }

            // Source-app filter: skip if rule is app-specific and doesn't match
            if (rule.sourceAppPackage != null && rule.sourceAppPackage != sourceAppPackage) continue

            if (merchantName != null && matches(merchantName, rule)) return rule.categoryId
        }
        return null
    }

    private fun matches(merchantName: String, rule: RuleEntity): Boolean {
        val text = rule.matchText
        return when (rule.matchType) {
            "EQUALS"      -> merchantName.equals(text, ignoreCase = true)
            "CONTAINS"    -> merchantName.contains(text, ignoreCase = true)
            "STARTS_WITH" -> merchantName.startsWith(text, ignoreCase = true)
            "ENDS_WITH"   -> merchantName.endsWith(text, ignoreCase = true)
            "REGEX"       -> runCatching { Regex(text, RegexOption.IGNORE_CASE).containsMatchIn(merchantName) }.getOrDefault(false)
            else          -> false
        }
    }
}
