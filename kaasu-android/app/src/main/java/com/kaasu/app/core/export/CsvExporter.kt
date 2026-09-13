package com.kaasu.app.core.export

import android.content.Context
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Called by ReportsViewModel.exportCsv(); writes to cacheDir/exports/ and returns the file for FileProvider
    fun export(transactions: List<Transaction>, categoryMap: Map<Long, Category>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "kaasu_transactions.csv")

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("Date,Time,Type,Amount (INR),Merchant,Category,Note,Source App,Manual\n")
            transactions
                .sortedByDescending { it.transactionTime }
                .forEach { t ->
                    val date = Date(t.transactionTime)
                    val category = t.categoryId?.let { categoryMap[it] }?.name ?: ""
                    val amount = "%.2f".format(t.amountInPaise / 100.0)
                    writer.write(
                        "${dateFmt.format(date)}," +
                        "${timeFmt.format(date)}," +
                        "${t.type.name}," +
                        "$amount," +
                        "${(t.merchantName ?: "").escapeCsv()}," +
                        "${category.escapeCsv()}," +
                        "${(t.note ?: "").escapeCsv()}," +
                        "${(t.sourceAppName ?: "").escapeCsv()}," +
                        "${if (t.isManual) "Yes" else "No"}\n"
                    )
                }
        }

        return file
    }

    private fun String.escapeCsv(): String =
        if (contains(',') || contains('"') || contains('\n'))
            "\"${replace("\"", "\"\"")}\""
        else this
}
