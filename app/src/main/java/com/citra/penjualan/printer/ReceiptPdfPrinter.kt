package com.citra.penjualan.printer

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
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

    // Ukuran struk thermal 80mm = ~226 pt lebar
    private val PAGE_W = 226
    private val PAGE_H = 520

    fun printToPdf(receipt: ReceiptData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Nota_${receipt.idTransaksi?.takeLast(6) ?: "Toko"}".trim()

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
                    callback?.onLayoutCancelled(); return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
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
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
                    val page = pdfDocument!!.startPage(pageInfo)
                    drawReceipt(page.canvas, receipt)
                    pdfDocument!!.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { pdfDocument!!.writeTo(it) }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange(0, 0)))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                } finally {
                    pdfDocument?.close()
                    pdfDocument = null
                }
            }
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, attributes)
    }

    private fun drawReceipt(canvas: Canvas, receipt: ReceiptData) {
        val W = PAGE_W.toFloat()
        val cx = W / 2f
        val ml = 14f
        val mr = W - 14f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // ─── HEADER: Background ungu gelap ───
        p.style = Paint.Style.FILL
        p.color = 0xFF4A2B66.toInt()
        canvas.drawRect(0f, 0f, W, 88f, p)

        // Accent diagonal strip di header
        p.color = 0xFF6A3D8F.toInt()
        val path = Path()
        path.moveTo(0f, 58f); path.lineTo(W, 44f)
        path.lineTo(W, 88f); path.lineTo(0f, 88f); path.close()
        canvas.drawPath(path, p)

        // Nama Toko (putih, bold, center)
        p.color = 0xFFFFFFFF.toInt()
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 18f
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(receipt.toko.ifBlank { "TOKO CITRA" }.uppercase(), cx, 32f, p)

        // Tagline
        p.color = 0xFFCE93D8.toInt()
        p.textSize = 7.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Terima kasih sudah berbelanja ♥", cx, 47f, p)

        // Badge "NOTA TRANSAKSI"
        p.color = 0xFFBA68C8.toInt()
        p.style = Paint.Style.FILL
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawRoundRect(RectF(cx - 40f, 64f, cx + 40f, 80f), 8f, 8f, p)
        p.color = 0xFFFFFFFF.toInt()
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("✦  NOTA TRANSAKSI  ✦", cx, 75f, p)

        var y = 102f

        // ─── INFO BARIS ───
        fun kv(label: String, value: String) {
            p.textAlign = Paint.Align.LEFT
            p.color = 0xFF9E9E9E.toInt()
            p.textSize = 7.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(label, ml, y, p)
            p.textAlign = Paint.Align.RIGHT
            p.color = 0xFF3E1E55.toInt()
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(value, mr, y, p)
            y += 13f
        }

        kv("Tanggal", receipt.tanggal)
        kv("No. Nota", "#${receipt.idTransaksi?.takeLast(8) ?: "-"}")
        kv("Kasir", receipt.toko.ifBlank { "Admin" })

        y += 4f
        drawDashed(canvas, ml, mr, y); y += 12f

        // ─── HEADER TABEL ───
        p.style = Paint.Style.FILL
        p.color = 0xFFF3E5F5.toInt()
        canvas.drawRoundRect(RectF(ml, y, mr, y + 16f), 4f, 4f, p)
        p.color = 0xFF5C3D75.toInt()
        p.textSize = 7.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.LEFT
        canvas.drawText("  PRODUK", ml + 4f, y + 11f, p)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText("QTY", cx, y + 11f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("HARGA  ", mr - 2f, y + 11f, p)
        y += 20f

        // ─── BARIS ITEM ───
        val perItem = if (receipt.jumlah > 0) receipt.totalHarga / receipt.jumlah else receipt.totalHarga

        // Background baris item
        p.style = Paint.Style.FILL
        p.color = 0xFFFAF5FF.toInt()
        canvas.drawRect(ml, y - 2f, mr, y + 26f, p)

        // Nama produk
        p.textAlign = Paint.Align.LEFT
        p.color = 0xFF222222.toInt()
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("  ${receipt.namaProduk}", ml + 4f, y + 10f, p)

        // Harga per pcs
        p.color = 0xFF888888.toInt()
        p.textSize = 6.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("  @ Rp ${fmt(perItem)}", ml + 4f, y + 22f, p)

        // QTY (tengah)
        p.textAlign = Paint.Align.CENTER
        p.color = 0xFF5C3D75.toInt()
        p.textSize = 10f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${receipt.jumlah}", cx, y + 15f, p)

        // Total kanan
        p.textAlign = Paint.Align.RIGHT
        p.color = 0xFF4A2B66.toInt()
        p.textSize = 9f
        canvas.drawText("Rp ${fmt(receipt.totalHarga)}  ", mr - 2f, y + 15f, p)
        y += 34f

        drawDashed(canvas, ml, mr, y); y += 12f

        // ─── SUMMARY ───
        // Subtotal baris
        p.textAlign = Paint.Align.LEFT
        p.color = 0xFF888888.toInt()
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Subtotal (${receipt.jumlah} item)", ml, y, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("Rp ${fmt(receipt.totalHarga)}", mr, y, p)
        y += 10f

        // Biaya layanan
        p.textAlign = Paint.Align.LEFT
        canvas.drawText("Biaya Layanan", ml, y, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("Rp 0", mr, y, p)
        y += 14f

        // ─── TOTAL BOX ───
        p.style = Paint.Style.FILL
        p.color = 0xFF4A2B66.toInt()
        canvas.drawRoundRect(RectF(ml, y, mr, y + 30f), 10f, 10f, p)

        p.textAlign = Paint.Align.LEFT
        p.color = 0xFFCE93D8.toInt()
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("  TOTAL PEMBAYARAN", ml + 4f, y + 19f, p)

        p.textAlign = Paint.Align.RIGHT
        p.color = 0xFFFFFFFF.toInt()
        p.textSize = 12f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rp ${fmt(receipt.totalHarga)}  ", mr - 4f, y + 20f, p)
        y += 42f

        // ─── LUNAS STAMP ───
        drawDashed(canvas, ml, mr, y); y += 14f

        p.style = Paint.Style.FILL
        p.color = 0xFFE8F5E9.toInt()
        canvas.drawRoundRect(RectF(cx - 34f, y - 2f, cx + 34f, y + 18f), 10f, 10f, p)
        p.style = Paint.Style.STROKE
        p.color = 0xFF4CAF50.toInt()
        p.strokeWidth = 1.5f
        canvas.drawRoundRect(RectF(cx - 34f, y - 2f, cx + 34f, y + 18f), 10f, 10f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFF388E3C.toInt()
        p.textAlign = Paint.Align.CENTER
        p.textSize = 11f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("✓  LUNAS", cx, y + 13f, p)
        y += 26f

        // ─── FOOTER TEXT ───
        p.style = Paint.Style.FILL
        p.color = 0xFFBDBDBD.toInt()
        p.textSize = 6f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText("Simpan nota ini sebagai bukti pembelian Anda.", cx, y, p); y += 9f
        canvas.drawText("Barang yang sudah dibeli tidak dapat dikembalikan.", cx, y, p); y += 9f
        canvas.drawText("Terima kasih atas kepercayaan Anda! ♥", cx, y, p)

        // Bottom bar
        p.color = 0xFF4A2B66.toInt()
        canvas.drawRect(0f, PAGE_H - 10f, W, PAGE_H.toFloat(), p)
    }

    private fun drawDashed(canvas: Canvas, startX: Float, endX: Float, y: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFFD1C4E9.toInt()
        p.strokeWidth = 1f
        p.style = Paint.Style.STROKE
        p.pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        canvas.drawLine(startX, y, endX, y, p)
    }

    private fun fmt(amount: Int): String {
        val s = amount.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            if (count > 0 && count % 3 == 0) sb.insert(0, ".")
            sb.insert(0, s[i])
            count++
        }
        return sb.toString()
    }
}