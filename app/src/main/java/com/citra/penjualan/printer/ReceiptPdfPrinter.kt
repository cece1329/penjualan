package com.citra.penjualan.printer

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfDocument
import android.util.TypedValue
import java.io.FileOutputStream

class ReceiptPdfPrinter(private val context: Context) {

    data class ReceiptData(
        val toko: String,
        val tanggal: String,
        val idTransaksi: String?,
        val namaProduk: String,
        val jumlah: Int,
        val totalHarga: Int
    )

    fun printToPdf(receipt: ReceiptData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Struk_${receipt.idTransaksi ?: ""}".trim()

        val adapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                // Perbaikan Utama: Membuat informasi dokumen (PrintDocumentInfo)
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()

                // Memberikan info ke callback beserta flag 'changed = true'
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                try {
                    pdfDocument = PdfDocument()

                    val pageInfo = PdfDocument.PageInfo.Builder(
                        595,
                        842,
                        1
                    ).create()

                    val page = pdfDocument!!.startPage(pageInfo)
                    val canvas = page.canvas
                    drawReceipt(canvas, receipt)
                    pdfDocument!!.finishPage(page)

                    // Menulis PDF menggunakan FileOutputStream dari descriptor tujuan
                    FileOutputStream(destination.fileDescriptor).use { outputStream ->
                        pdfDocument!!.writeTo(outputStream)
                    }

                    callback?.onWriteFinished(arrayOf(android.print.PageRange(0, 0)))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                } finally {
                    pdfDocument?.close()
                    pdfDocument = null
                }
            }
        }

        // Setting dasar untuk printer adapter
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, attributes)
    }

    private fun drawReceipt(canvas: android.graphics.Canvas, receipt: ReceiptData) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun textSizeSp(sp: Float): Float {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                context.resources.displayMetrics
            )
        }

        val marginLeft = 48f
        var y = 72f

        // Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = textSizeSp(18f)
        paint.color = 0xFF5C3D75.toInt()
        canvas.drawText(receipt.toko.ifBlank { "" }, marginLeft, y, paint)

        y += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = textSizeSp(16f)
        paint.color = 0xFF4A2B66.toInt()
        canvas.drawText("STRUK TRANSAKSI", marginLeft, y, paint)

        y += 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = textSizeSp(12.5f)
        paint.color = 0xFF8E74A6.toInt()
        canvas.drawText("Tanggal : ${receipt.tanggal}", marginLeft, y, paint)
        y += 20f
        canvas.drawText("ID       : ${receipt.idTransaksi ?: "-"}", marginLeft, y, paint)

        y += 18f

        // Separator Line
        paint.color = 0xFFBA68C8.toInt()
        paint.strokeWidth = 2f
        canvas.drawLine(marginLeft, y, 547f, y, paint)

        y += 20f

        // Items
        paint.color = 0xFF5C3D75.toInt()
        paint.textSize = textSizeSp(14.5f)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(receipt.namaProduk, marginLeft, y, paint)
        y += 18f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = textSizeSp(12.5f)
        canvas.drawText("Jumlah : ${receipt.jumlah}", marginLeft, y, paint)
        y += 18f

        // Total Line
        y += 12f
        paint.strokeWidth = 2f
        canvas.drawLine(marginLeft, y, 547f, y, paint)
        y += 26f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = textSizeSp(18f)
        paint.color = 0xFF4A2B66.toInt()
        canvas.drawText("TOTAL", marginLeft, y, paint)

        val totalText = "Rp ${receipt.totalHarga}"
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(totalText, 547f, y, paint)

        // Footer
        paint.textAlign = Paint.Align.LEFT
        y += 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = textSizeSp(11.5f)
        paint.color = 0xFF8E74A6.toInt()
        canvas.drawText("Terima kasih telah berbelanja.", marginLeft, y, paint)
    }
}