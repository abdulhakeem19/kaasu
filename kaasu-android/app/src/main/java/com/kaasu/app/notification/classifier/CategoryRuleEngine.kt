package com.kaasu.app.notification.classifier

import com.kaasu.app.core.database.dao.RuleDao
import com.kaasu.app.core.database.entity.RuleEntity
import com.kaasu.app.domain.model.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a matching rule decided.
 *
 * A rule used to be able to say only "file this under category X". The `transactionType` column
 * existed on [RuleEntity] but was never read, so a rule that says "this is not a purchase at all"
 * was impossible to express — which is why the stock "credit card" rule filed bill payments into
 * Bills, making them spend by definition and double-counting every card purchase.
 */
data class RuleOutcome(
    val categoryId: Long? = null,
    val typeOverride: TransactionType? = null,
) {
    val isEmpty: Boolean get() = categoryId == null && typeOverride == null

    companion object {
        val NONE = RuleOutcome()
    }
}

@Singleton
class CategoryRuleEngine @Inject constructor(
    private val ruleDao: RuleDao
) {
    /**
     * First matching rule wins, in the order the DAO returns (priority, then insertion).
     *
     * Rules carrying only a type override are no longer skipped — the old
     * `if (rule.categoryId == null) continue` guard made them invisible.
     */
    suspend fun classify(merchantName: String?, sourceAppPackage: String?): RuleOutcome {
        val rules = ruleDao.getActiveRulesList()
        for (rule in rules) {
            val outcome = rule.toOutcome()
            if (outcome.isEmpty) continue

            // Source-app-only rule (empty matchText): matches any transaction from this app
            if (rule.matchText.isBlank()) {
                if (rule.sourceAppPackage != null && rule.sourceAppPackage == sourceAppPackage) {
                    return outcome
                }
                continue
            }

            // Source-app filter: skip if rule is app-specific and doesn't match
            if (rule.sourceAppPackage != null && rule.sourceAppPackage != sourceAppPackage) continue

            if (merchantName != null && matches(merchantName, rule)) return outcome
        }
        return RuleOutcome.NONE
    }

    /** Convenience for the callers that only ever wanted the category. */
    suspend fun classifyCategory(merchantName: String?, sourceAppPackage: String?): Long? =
        classify(merchantName, sourceAppPackage).categoryId

    private fun RuleEntity.toOutcome() = RuleOutcome(
        categoryId = categoryId,
        typeOverride = transactionType
            ?.let { raw -> TransactionType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } },
    )

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
