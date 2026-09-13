package com.kaasu.app.statement.parser.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around PdfBox-Android for on-device text extraction from a statement PDF.
 * Android's built-in PdfRenderer only rasterizes pages (no text), so PdfBox-Android is the only
 * practical on-device option for extracting selectable text.
 *
 * Scope: text-based (selectable-text) statements only — scanned/image PDFs are explicitly out
 * of scope (no OCR). A scanned statement will simply extract no/garbled text and its parser's
 * canParse() will fail to match, surfacing as "couldn't recognize this file's format".
 */
@Singleton
class PdfTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resourcesLoaded = AtomicBoolean(false)

    fun extractText(uri: Uri): String {
        if (resourcesLoaded.compareAndSet(false, true)) {
            PDFBoxResourceLoader.init(context)
        }
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open the selected PDF")
        return input.use { stream ->
            PDDocument.load(stream).use { document ->
                PDFTextStripper().getText(document)
            }
        }
    }
}
