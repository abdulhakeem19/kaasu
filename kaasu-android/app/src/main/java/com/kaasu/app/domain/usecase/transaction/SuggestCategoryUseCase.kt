package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.model.Category
import com.kaasu.app.notification.classifier.CategoryRuleEngine
import javax.inject.Inject

/**
 * Suggests a category for an uncategorized transaction, for the "Needs a tag" review queue.
 *
 * Strategy: first ask the rule engine (covers seeded merchant rules + user rules the listener may
 * have missed at capture time). If that returns nothing, fall back to a light keyword scan of the
 * merchant name against a small map, resolved to a real category that exists in the DB.
 */
class SuggestCategoryUseCase @Inject constructor(
    private val ruleEngine: CategoryRuleEngine
) {
    suspend operator fun invoke(
        merchantName: String?,
        sourceAppPackage: String?,
        categories: List<Category>
    ): Long? {
        ruleEngine.classify(merchantName, sourceAppPackage)?.let { return it }

        val name = merchantName?.lowercase() ?: return null
        val byName = categories.associateBy { it.name.lowercase() }
        for ((keyword, categoryName) in KEYWORDS) {
            if (name.contains(keyword)) {
                byName[categoryName.lowercase()]?.let { return it.id }
            }
        }
        return null
    }

    companion object {
        // keyword found in merchant name → target category name (must exist in seeded categories)
        private val KEYWORDS = listOf(
            "swiggy" to "Food", "zomato" to "Food", "dominos" to "Food", "restaurant" to "Food",
            "cafe" to "Food", "coffee" to "Food", "bakery" to "Food",
            "uber" to "Travel", "ola" to "Travel", "rapido" to "Travel", "irctc" to "Travel",
            "metro" to "Travel", "fuel" to "Fuel", "petrol" to "Fuel", "hpcl" to "Fuel", "iocl" to "Fuel",
            "zepto" to "Groceries", "blinkit" to "Groceries", "bigbasket" to "Groceries",
            "dmart" to "Groceries", "grocery" to "Groceries", "mart" to "Groceries",
            "amazon" to "Shopping", "flipkart" to "Shopping", "myntra" to "Shopping", "ajio" to "Shopping",
            "netflix" to "Subscriptions", "spotify" to "Subscriptions", "hotstar" to "Subscriptions",
            "prime" to "Subscriptions", "youtube" to "Subscriptions",
            "electricity" to "Bills", "recharge" to "Recharge", "airtel" to "Recharge",
            "jio" to "Recharge", "broadband" to "Bills", "gas" to "Bills",
            "pharmacy" to "Health", "hospital" to "Health", "apollo" to "Health", "1mg" to "Health",
            "salary" to "Salary", "rent" to "Rent"
        )
    }
}
