package com.kaasu.app.statement.parser.xlsx

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Hand-rolled `.xlsx` (Office Open XML spreadsheet) text extractor for the one bank export this
 * app supports in that format (IDFC FIRST Bank savings statement). Deliberately NOT using Apache
 * POI or any other XLSX library — an .xlsx is just a zip of small XML parts, and this app only
 * ever needs one sheet's cell grid, so `java.util.zip` + a standard DOM parser is enough and
 * keeps the dependency footprint at zero.
 *
 * Thin Context-owning wrapper — [uri] reading is the only thing here that needs a
 * ContentResolver. The actual zip/XML parsing lives in [XlsxParsing], a plain object with no
 * Android dependency, so it can be exercised directly in a local JVM unit test without a real or
 * mocked [Context] (see [XlsxTextExtractorTest]).
 */
@Singleton
class XlsxTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun extractText(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Could not open the selected file")
        return XlsxParsing.extractText(bytes)
    }

    // Byte-array entry point, exposed separately so callers/tests can feed an in-memory .xlsx
    // zip directly without needing a Context/content Uri.
    fun extractText(bytes: ByteArray): String = XlsxParsing.extractText(bytes)
}

/**
 * Pure zip+XML parsing logic for the `.xlsx` format, with zero Android dependency — kept separate
 * from [XlsxTextExtractor] purely so it's directly unit-testable without a Context.
 *
 * Uses `javax.xml.parsers.DocumentBuilderFactory` (part of the JDK/Android standard library, same
 * tier as `android.util.Xml`'s own pull parser) rather than `android.util.Xml` itself — that class
 * is stubbed out ("not mocked") in this module's local JVM unit tests since Robolectric isn't set
 * up here, which would leave this logic unable to be genuinely exercised by a test.
 * `javax.xml.parsers` runs identically on-device and in a plain JVM unit test, so this keeps the
 * "zero new dependency, built-in only" intent while staying testable.
 *
 * Resolves the "Account Statement" sheet by NAME via workbook.xml → workbook.xml.rels (never a
 * hardcoded "sheet1.xml"), falling back to the first declared sheet / first worksheet part found
 * if no name match — reasonable for a personal app with one bank's stable export format.
 */
internal object XlsxParsing {

    private const val TARGET_SHEET_NAME = "Account Statement"
    // This bank's statement only ever uses columns A..G (Transaction Date .. Balance).
    private const val MAX_COLUMN_INDEX = 6

    fun extractText(bytes: ByteArray): String {
        val parts = readZipParts(bytes)
        val workbookXml = parts["xl/workbook.xml"]
            ?: throw IllegalArgumentException("Not a valid .xlsx file (missing workbook.xml)")

        val sheetPath = resolveSheetPath(workbookXml, parts["xl/_rels/workbook.xml.rels"], parts)
        val sheetXml = parts[sheetPath]
            ?: throw IllegalArgumentException("Not a valid .xlsx file (missing worksheet part)")

        val sharedStrings = parts["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val rows = parseSheetRows(sheetXml, sharedStrings)
        return rows.joinToString("\n") { row -> row.joinToString("|") }
    }

    /** Reads every zip entry into memory, keyed by its path within the archive. */
    private fun readZipParts(bytes: ByteArray): Map<String, String> {
        val parts = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var read = zip.read(buffer)
                    while (read >= 0) {
                        out.write(buffer, 0, read)
                        read = zip.read(buffer)
                    }
                    parts[entry.name] = out.toString(Charsets.UTF_8.name())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return parts
    }

    private fun resolveSheetPath(workbookXml: String, relsXml: String?, parts: Map<String, String>): String {
        val workbookDoc = parseXml(workbookXml)
        val sheetElements = workbookDoc.getElementsByTagName("sheet")

        var targetRid: String? = null
        for (i in 0 until sheetElements.length) {
            val el = sheetElements.item(i) as? Element ?: continue
            if (el.getAttribute("name").equals(TARGET_SHEET_NAME, ignoreCase = true)) {
                targetRid = el.getAttribute("r:id")
                break
            }
        }
        // Fall back to the first declared sheet if no name match.
        if (targetRid == null && sheetElements.length > 0) {
            targetRid = (sheetElements.item(0) as? Element)?.getAttribute("r:id")
        }

        val target = targetRid?.let { rid -> resolveRelationshipTarget(relsXml, rid) }
        if (target != null) {
            return normalizeXlPath(target)
        }

        // Last-resort fallback: first worksheet part present in the archive at all.
        return parts.keys.filter { it.startsWith("xl/worksheets/") }.sorted().firstOrNull()
            ?: "xl/worksheets/sheet1.xml"
    }

    private fun resolveRelationshipTarget(relsXml: String?, rid: String): String? {
        if (relsXml == null) return null
        val relsDoc = parseXml(relsXml)
        val relElements = relsDoc.getElementsByTagName("Relationship")
        for (i in 0 until relElements.length) {
            val el = relElements.item(i) as? Element ?: continue
            if (el.getAttribute("Id") == rid) return el.getAttribute("Target")
        }
        return null
    }

    // Relationship Targets are relative to the "xl/" directory (e.g. "worksheets/sheet1.xml"),
    // occasionally given package-absolute (e.g. "/xl/worksheets/sheet1.xml") — normalize both.
    private fun normalizeXlPath(target: String): String = when {
        target.startsWith("/xl/") -> target.removePrefix("/")
        target.startsWith("xl/") -> target
        else -> "xl/$target"
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val doc = parseXml(xml)
        val siNodes = doc.getElementsByTagName("si")
        val result = ArrayList<String>(siNodes.length)
        for (i in 0 until siNodes.length) {
            val si = siNodes.item(i) as? Element ?: continue
            // getElementsByTagName walks all descendants, so this concatenates every <t> text
            // run whether the string is a plain <si><t> or rich-text <si><r><t>...</t></r>...</si>.
            val tNodes = si.getElementsByTagName("t")
            val sb = StringBuilder()
            for (j in 0 until tNodes.length) {
                sb.append(tNodes.item(j).textContent ?: "")
            }
            result += sb.toString()
        }
        return result
    }

    private fun parseSheetRows(sheetXml: String, sharedStrings: List<String>): List<List<String>> {
        val doc = parseXml(sheetXml)
        val rowNodes = doc.getElementsByTagName("row")
        val rows = ArrayList<List<String>>(rowNodes.length)
        for (i in 0 until rowNodes.length) {
            val rowEl = rowNodes.item(i) as? Element ?: continue
            val cells = arrayOfNulls<String>(MAX_COLUMN_INDEX + 1)
            val cellNodes = rowEl.getElementsByTagName("c")
            for (j in 0 until cellNodes.length) {
                val cellEl = cellNodes.item(j) as? Element ?: continue
                val colIndex = columnIndexOf(cellEl.getAttribute("r"))
                if (colIndex < 0 || colIndex > MAX_COLUMN_INDEX) continue

                val vNodes = cellEl.getElementsByTagName("v")
                // No <v> child at all = a genuinely blank cell (e.g. <c r="D23" s="3"/>).
                val rawValue = if (vNodes.length > 0) vNodes.item(0).textContent ?: "" else ""

                cells[colIndex] = if (cellEl.getAttribute("t") == "s") {
                    // Shared-string reference: <v> is an index into the shared-string table.
                    rawValue.toIntOrNull()?.let { idx -> sharedStrings.getOrNull(idx) } ?: ""
                } else {
                    // t="n" or absent: plain numeric (or otherwise literal) value.
                    rawValue
                }
            }
            rows += cells.map { it ?: "" }
        }
        return rows
    }

    // Converts a cell reference's column letters ("A22" -> "A", "AB3" -> "AB") to a 0-based
    // column index (A=0, B=1, ..., Z=25, AA=26, ...). This statement only reaches column G, but
    // the conversion is general so a wider future export wouldn't silently misalign.
    private fun columnIndexOf(cellRef: String): Int {
        var index = 0
        for (c in cellRef) {
            if (c in 'A'..'Z') {
                index = index * 26 + (c - 'A' + 1)
            } else if (c in 'a'..'z') {
                index = index * 26 + (c - 'a' + 1)
            } else {
                break
            }
        }
        return index - 1
    }

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        // XXE hardening: this app only ever parses parts from a zip the user themselves picked,
        // but disabling external entity/DTD resolution is a cheap, standard precaution.
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.isExpandEntityReferences = false }
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }
}
