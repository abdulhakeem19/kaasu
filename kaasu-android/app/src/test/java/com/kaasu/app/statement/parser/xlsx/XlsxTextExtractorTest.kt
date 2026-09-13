package com.kaasu.app.statement.parser.xlsx

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds one minimal, synthetic, in-memory `.xlsx` (a zip of a handful of small XML parts) and
 * verifies [XlsxParsing] (the pure logic behind [XlsxTextExtractor]) resolves the named sheet,
 * reads shared strings, and reconstructs sparse rows (including entirely-absent cells) into the
 * pipe-delimited text [IdfcFirstSavingsXlsxParser] consumes. Exercises [XlsxParsing] directly
 * rather than [XlsxTextExtractor] itself, since the latter needs a real/mocked Android Context
 * (only used for its one-line ContentResolver read) that this module's plain-JUnit test setup
 * has no way to supply — see [XlsxTextExtractor]'s doc comment.
 */
class XlsxTextExtractorTest {

    // Shared-string table: index -> text. Order matters — it's what <v> indices reference.
    private val sharedStrings = listOf(
        "Transaction Date", "Value Date", "Particulars", "Cheque No.", "Debit", "Credit", "Balance", // 0-6
        "01-Jun-2026", "UPI/DR/111111111111/TEST NAME/CNRB/test/note" // 7-8
    )

    private val workbookXml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Account Statement" r:id="rId3" sheetId="1"/>
            <sheet name="Important Message and Safety Ti" r:id="rId4" sheetId="2"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private val relsXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Target="sharedStrings.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings"/>
          <Relationship Id="rId3" Target="worksheets/sheet1.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"/>
          <Relationship Id="rId4" Target="worksheets/sheet2.xml" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"/>
        </Relationships>
    """.trimIndent()

    // Built by plain concatenation (not a trimIndent() template) because the embedded
    // joinToString block's lines have no common leading indent with the rest of the template —
    // trimIndent() would then compute a common indent of zero and leave the <?xml ...?>
    // declaration with stray leading whitespace, which XML parsers reject outright.
    private val sharedStringsXml =
        """<?xml version="1.0" encoding="UTF-8"?>""" +
            """<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" """ +
            """count="${sharedStrings.size}" uniqueCount="${sharedStrings.size}">""" +
            sharedStrings.joinToString("") { "<si><t>${it}</t></si>" } +
            "</sst>"

    // Row 1: header (all shared-string refs, indices 0..6). Row 2: one data row with a genuinely
    // absent cell for Cheque No. (D2, no <v> at all) — mirrors the real file's sparse rows.
    private val sheet1Xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData>
            <row r="1">
              <c r="A1" t="s"><v>0</v></c>
              <c r="B1" t="s"><v>1</v></c>
              <c r="C1" t="s"><v>2</v></c>
              <c r="D1" t="s"><v>3</v></c>
              <c r="E1" t="s"><v>4</v></c>
              <c r="F1" t="s"><v>5</v></c>
              <c r="G1" t="s"><v>6</v></c>
            </row>
            <row r="2">
              <c r="A2" t="s"><v>7</v></c>
              <c r="B2" t="s"><v>7</v></c>
              <c r="C2" t="s"><v>8</v></c>
              <c r="D2" s="3"/>
              <c r="E2" t="n"><v>250.0</v></c>
              <c r="F2" s="2"/>
              <c r="G2" t="n"><v>999.49</v></c>
            </row>
          </sheetData>
        </worksheet>
    """.trimIndent()

    // The second sheet (Important Message and Safety Tips) — deliberately never referenced by
    // the extractor once name-resolution picks "Account Statement" instead; present only to
    // prove the resolver doesn't just grab the first worksheet part it finds.
    private val sheet2Xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <sheetData>
            <row r="1"><c r="A1" t="s"><v>0</v></c></row>
          </sheetData>
        </worksheet>
    """.trimIndent()

    private fun buildXlsxBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun writePart(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            writePart("xl/workbook.xml", workbookXml)
            writePart("xl/_rels/workbook.xml.rels", relsXml)
            writePart("xl/sharedStrings.xml", sharedStringsXml)
            writePart("xl/worksheets/sheet1.xml", sheet1Xml)
            writePart("xl/worksheets/sheet2.xml", sheet2Xml)
        }
        return out.toByteArray()
    }

    @Test fun extractText_resolvesNamedSheetAndProducesPipeDelimitedRows() {
        val text = XlsxParsing.extractText(buildXlsxBytes())
        val lines = text.lines()

        assertEquals("Transaction Date|Value Date|Particulars|Cheque No.|Debit|Credit|Balance", lines[0])
        assertEquals(
            "01-Jun-2026|01-Jun-2026|UPI/DR/111111111111/TEST NAME/CNRB/test/note||250.0||999.49",
            lines[1]
        )
    }
}
