package com.kaasu.app.accessibility.scraper

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.kaasu.app.accessibility.extraction.TransactionRowTextBuilder
import com.kaasu.app.accessibility.model.ScrapedTransactionCandidate

/**
 * Shared node-tree-walking mechanics for the two [ScreenScraper] implementations (GPay, PhonePe).
 * Both apps render their transaction-history screen the same conceptual way — a scrollable list of
 * rows, each row a handful of `TextView`s (a verb+name line, an amount, a date) — so the walk/group
 * logic is shared here and only the per-app resource-id substrings that identify the screen
 * ([screenResourceIdHints]) are overridden. This deliberately trades a small amount of the brief's
 * "each scraper does its own layout-specific work" for not duplicating the same bounded-depth
 * tree-walk twice; if GPay/PhonePe's actual layouts turn out to need genuinely different row
 * extraction later, that logic can be overridden per subclass without touching [ScreenScraper]'s
 * contract.
 *
 * Both [isTransactionScreen] and [extractCandidates] are bounded-depth walks (never full-tree,
 * unbounded recursion) since this runs on every `typeWindowContentChanged` event while the target
 * app is foregrounded — see [com.kaasu.app.accessibility.service.KaasuAccessibilityService].
 */
abstract class BaseTransactionScreenScraper : ScreenScraper {

    /** Resource-id SUBSTRINGS (case-insensitive) that mark a screen as a transaction/history screen. */
    protected abstract val screenResourceIdHints: List<String>

    override fun isTransactionScreen(root: AccessibilityNodeInfo): Boolean =
        containsMatchingResourceId(root, depth = 0)

    private fun containsMatchingResourceId(node: AccessibilityNodeInfo, depth: Int): Boolean {
        if (depth > MAX_SCREEN_CHECK_DEPTH) return false
        val resId = node.viewIdResourceName
        if (resId != null && screenResourceIdHints.any { resId.contains(it, ignoreCase = true) }) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (containsMatchingResourceId(child, depth + 1)) return true
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
        return false
    }

    override fun extractCandidates(root: AccessibilityNodeInfo): List<ScrapedTransactionCandidate> {
        val leaves = mutableListOf<Pair<Rect, String>>()
        collectLeafTexts(root, depth = 0, out = leaves)
        val rows = groupIntoRows(leaves)
        val now = System.currentTimeMillis()

        return rows.mapNotNull { fragments ->
            val reconstructed = TransactionRowTextBuilder.build(fragments) ?: return@mapNotNull null
            ScrapedTransactionCandidate(
                amountText = reconstructed.amountText,
                merchantText = reconstructed.merchantText,
                dateText = reconstructed.dateText,
                directionHint = reconstructed.directionHint,
                rawNodeText = reconstructed.sentence,
                sourcePackage = packageName,
                scrapedAt = now
            )
        }
    }

    private fun collectLeafTexts(node: AccessibilityNodeInfo, depth: Int, out: MutableList<Pair<Rect, String>>) {
        if (depth > MAX_WALK_DEPTH || out.size >= MAX_COLLECTED_FRAGMENTS) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && node.childCount == 0) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            out += bounds to text
        }
        for (i in 0 until node.childCount) {
            if (out.size >= MAX_COLLECTED_FRAGMENTS) break
            val child = node.getChild(i) ?: continue
            try {
                collectLeafTexts(child, depth + 1, out)
            } finally {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    // Groups leaf text nodes into "rows" by on-screen vertical proximity — a cheap stand-in for
    // reading the real RecyclerView row structure that works because a single history row's
    // fragments (verb+name, amount, date) always share (near enough) the same top-Y coordinate.
    private fun groupIntoRows(items: List<Pair<Rect, String>>): List<List<String>> {
        if (items.isEmpty()) return emptyList()
        val sorted = items.sortedWith(compareBy({ it.first.top }, { it.first.left }))
        val rows = mutableListOf<MutableList<Pair<Rect, String>>>()
        for (item in sorted) {
            val currentRow = rows.lastOrNull()
            val rowAnchorTop = currentRow?.first()?.first?.top
            if (currentRow != null && rowAnchorTop != null &&
                kotlin.math.abs(item.first.top - rowAnchorTop) <= ROW_Y_TOLERANCE_PX
            ) {
                currentRow += item
            } else {
                rows += mutableListOf(item)
            }
        }
        return rows.map { row -> row.sortedBy { it.first.left }.map { it.second } }
    }

    private companion object {
        const val MAX_SCREEN_CHECK_DEPTH = 20
        const val MAX_WALK_DEPTH = 25
        const val MAX_COLLECTED_FRAGMENTS = 400
        const val ROW_Y_TOLERANCE_PX = 40
    }
}
