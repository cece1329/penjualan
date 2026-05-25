package com.citra.penjualan.printer

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color
import android.os.Build
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.ContextCompat
import com.citra.penjualan.R
import android.os.Parcelable
import android.widget.Toast
import kotlinx.parcelize.Parcelize
import java.io.FileOutputStream
import java.io.OutputStream

@Parcelize
data class ReceiptItem(
    val nama: String,
    val qty: Int,
    val harga: Int
) : Parcelable

@Parcelize
data class ReceiptData(
    val toko: String,
    val alamat: String? = null,
    val cabang: String? = null,
    val kasir: String? = null,
    val tanggal: String,
    val idTransaksi: String?,
    val items: List<ReceiptItem> = emptyList(),
    val namaProduk: String = "",
    val jumlah: Int,
    val totalHarga: Int,
    val metodePembayaran: String? = null,
    val uangDiterima: Int? = null,
    val kembalian: Int? = null,
    val noGopay: String? = null,
    val namaPelanggan: String? = null,
    val jenisPelanggan: String? = null
) : Parcelable

data class ReportTransaction(
    val id: String,
    val date: String,
    val total: Int,
    val method: String,
    val items: String
)

data class ReportSummary(
    val title: String,
    val period: String,
    val omzet: Int,
    val laba: Int,
    val topSelling: List<Pair<String, Int>>,
    val paymentSummary: List<Pair<String, Int>>,
    val transactions: List<ReportTransaction>
)

class ReceiptPdfPrinter(private val context: Context) {

    // Resolusi dasar untuk kalkulasi (Logical Width)
    private val BASE_W = 226f
    // Resolusi HD untuk Gambar & Sharing
    val PAGE_W = 576 

    fun getRequiredHeight(receipt: ReceiptData, currentBaseW: Float = BASE_W): Int {
        var headerExtra = 20f // Ruang dasar untuk logo
        if (!receipt.cabang.isNullOrBlank()) headerExtra += 15f
        if (!receipt.alamat.isNullOrBlank()) headerExtra += 15f

        var y = 145f + headerExtra // Penyesuaian starting Y karena adanya logo dan info header tambahan
        // Baris Info (Tanggal, Invoice, Kasir)
        y += 13f * 3
        if (!receipt.namaPelanggan.isNullOrBlank()) y += 13f
        if (!receipt.metodePembayaran.isNullOrBlank()) y += 13f
        if (receipt.uangDiterima != null) y += 13f
        if (receipt.kembalian != null) y += 13f
        if (!receipt.noGopay.isNullOrBlank()) y += 13f

        y += 4f + 12f // Spasi dashed
        y += 20f // Header tabel

        // Baris Produk
        val itemCount = if (receipt.items.isNotEmpty()) receipt.items.size else 1
        y += itemCount * 34f

        y += 12f // Dashed total
        y += 10f + 14f // Summary
        y += 42f // Total box
        y += 14f + 26f // Lunas stamp
        y += 9f + 9f + 9f // Footer text
        y += 35f // Extra margin bawah
        return y.toInt()
    }

    fun printToPdf(receipt: ReceiptData, isThermal: Boolean = true) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Nota_${receipt.idTransaksi?.takeLast(6) ?: "Toko"}".trim()

        val adapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PdfDocument? = null
            private var currentAttributes: PrintAttributes? = null

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
                currentAttributes = newAttributes
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
                    
                    // Ambil lebar kertas asli dari atribut printer
                    val paperWidth = currentAttributes?.mediaSize?.widthMils?.let { it * 72 / 1000 } ?: PAGE_W
                    // Hitung tinggi yang proporsional dengan lebar kertas baru
                    val scaleFactor = paperWidth.toFloat() / (if (isThermal) BASE_W else 595f)
                    val dynamicHeight = (getRequiredHeight(receipt, if (isThermal) BASE_W else 595f) * scaleFactor).toInt()

                    val pageInfo = PdfDocument.PageInfo.Builder(paperWidth, dynamicHeight.coerceAtLeast(100), 1).create()
                    val page = pdfDocument!!.startPage(pageInfo)
                    drawReceipt(page.canvas, receipt, paperWidth.toFloat())
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

        // Gunakan ukuran 58mm (Roll) jika isThermal true
        val mediaSize = if (isThermal) {
            PrintAttributes.MediaSize("Roll_58mm", "Printer Kasir", 2260, 8000)
        } else {
            PrintAttributes.MediaSize.ISO_A4
        }

        val attributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, attributes)
    }

    fun printReport(report: ReportSummary) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Laporan_${report.period.replace(" ", "_")}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: android.os.Bundle?) {
                val info = PrintDocumentInfo.Builder(jobName).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(pages: Array<out android.print.PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Ukuran A4
                val page = pdfDocument.startPage(pageInfo)
                drawReportDoc(page.canvas, report)
                pdfDocument.finishPage(page)
                try {
                    pdfDocument.writeTo(FileOutputStream(destination.fileDescriptor))
                    callback?.onWriteFinished(arrayOf(android.print.PageRange(0, 0)))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                } finally {
                    pdfDocument.close()
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    private fun drawReportDoc(canvas: Canvas, report: ReportSummary) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val width = 595f
        val margin = 40f
        val contentWidth = width - (margin * 2)

        // HEADER BACKGROUND (Purple Bar)
        p.color = 0xFF4A2B66.toInt()
        canvas.drawRect(0f, 0f, width, 130f, p)

        // JUDUL LAPORAN (White)
        p.color = Color.WHITE
        p.textSize = 22f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(report.title.uppercase(), margin, 65f, p)

        // PERIODE (Soft Purple)
        p.textSize = 11f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        p.color = 0xFFCE93D8.toInt()
        canvas.drawText("Periode: ${report.period}", margin, 90f, p)

        var y = 160f

        // FINANCIAL CARDS (Dua Kolom: Omzet & Laba)
        val cardW = (contentWidth - 15f) / 2f
        
        // Omzet Card
        p.color = 0xFFF3E5F5.toInt() // Light Purple BG
        canvas.drawRoundRect(RectF(margin, y, margin + cardW, y + 70f), 12f, 12f, p)
        p.color = 0xFF4A2B66.toInt()
        p.textSize = 9f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL OMZET", margin + 15f, y + 25f, p)
        p.textSize = 16f
        canvas.drawText("Rp ${fmt(report.omzet)}", margin + 15f, y + 50f, p)

        // Laba Card
        p.color = 0xFFE8F5E9.toInt() // Light Green BG
        canvas.drawRoundRect(RectF(margin + cardW + 15f, y, margin + (cardW * 2) + 15f, y + 70f), 12f, 12f, p)
        p.color = 0xFF2E7D32.toInt() // Green Text
        p.textSize = 9f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ESTIMASI LABA", margin + cardW + 30f, y + 25f, p)
        p.textSize = 16f
        canvas.drawText("Rp ${fmt(report.laba)}", margin + cardW + 30f, y + 50f, p)

        y += 105f

        // SECTION: TOP SELLING
        p.color = Color.BLACK
        p.textSize = 14f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("5 Produk Terlaris", margin, y, p)
        
        y += 15f
        p.color = 0xFFEEEEEE.toInt()
        canvas.drawRect(margin, y, width - margin, y + 1.5f, p) // Divider
        
        y += 25f
        p.textSize = 11f
        p.color = 0xFF424242.toInt()
        report.topSelling.forEachIndexed { index, pair ->
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${index + 1}. ${pair.first}", margin + 10f, y, p)
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText("${pair.second} unit", width - margin - 80f, y, p)
            p.textAlign = Paint.Align.LEFT
            y += 20f
        }

        y += 25f
        p.textSize = 14f
        p.color = Color.BLACK
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Daftar Transaksi", margin, y, p)
        
        y += 15f
        p.color = 0xFFEEEEEE.toInt()
        canvas.drawRect(margin, y, width - margin, y + 1.5f, p) // Divider
        y += 20f

        // LIST TRANSAKSI (Modern Card Style)
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        report.transactions.forEach { trans ->
            if (y > 780) return@forEach // Batasan satu halaman A4

            // Background Card
            p.color = 0xFFF9F9F9.toInt()
            p.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(margin, y, width - margin, y + 50f), 10f, 10f, p)
            
            // Aksen Ungu di samping kiri card
            p.color = 0xFFBA68C8.toInt()
            canvas.drawRoundRect(RectF(margin, y, margin + 5f, y + 50f), 10f, 10f, p)

            // Isi Card
            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            p.textSize = 10.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(trans.date, margin + 15f, y + 20f, p)
            
            p.textSize = 9.5f
            p.color = Color.GRAY
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("ID: ${trans.id.takeLast(8)} • ${trans.method}", margin + 15f, y + 38f, p)
            
            p.color = Color.BLACK
            p.textSize = 12f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText("Rp ${fmt(trans.total)}", width - margin - 15f, y + 30f, p)
            p.textAlign = Paint.Align.LEFT
            y += 62f
        }
    }

    fun shareAsPdf(receipt: ReceiptData) {
        val pdfDocument = PdfDocument()
        val paperWidth = 595 // A4 Width in points
        val scaleFactor = paperWidth.toFloat() / BASE_W
        val dynamicHeight = (getRequiredHeight(receipt) * scaleFactor).toInt()

        val pageInfo = PdfDocument.PageInfo.Builder(paperWidth, dynamicHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        drawReceipt(page.canvas, receipt, paperWidth.toFloat())
        pdfDocument.finishPage(page)

        val fileName = "Nota_${receipt.idTransaksi?.takeLast(6) ?: System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/PenjualanNota")
            }
        }

        val contentResolver = context.contentResolver
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            null // Implementasi folder biasa untuk versi lama jika diperlukan
        }

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Nota PDF..."))
        }
    }

    fun shareAsImage(receipt: ReceiptData) {
        val scale = PAGE_W.toFloat() / BASE_W
        val dynamicHeight = (getRequiredHeight(receipt) * scale).toInt()
        val bitmap = Bitmap.createBitmap(PAGE_W, dynamicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        drawReceipt(canvas, receipt, PAGE_W.toFloat())

        val fileName = "Nota_${System.currentTimeMillis()}.png"
        var uri: Uri? = null

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PenjualanNota")
            }
        }

        val contentResolver = context.contentResolver
        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Nota via..."))
        }
    }

    @SuppressLint("MissingPermission")
    fun printToBluetooth(receipt: ReceiptData) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Izin Bluetooth Connect diperlukan", Toast.LENGTH_SHORT).show()
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth tidak aktif atau tidak didukung", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.isNullOrEmpty()) {
            Toast.makeText(context, "Pasangkan (Pair) printer Bluetooth Anda terlebih dahulu di pengaturan HP", Toast.LENGTH_LONG).show()
            return
        }

        // Mencari device yang namanya mengandung "Printer", "Thermal", atau "MPT"
        val device = pairedDevices.firstOrNull { 
            val name = it.name.lowercase()
            name.contains("printer") || name.contains("thermal") || name.contains("mpt") 
        } ?: pairedDevices.first()

        Toast.makeText(context, "Menghubungkan ke ${device.name}...", Toast.LENGTH_SHORT).show()

        Thread {
            var socket: android.bluetooth.BluetoothSocket? = null
            try {
                val uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                val outputStream = socket.outputStream
                
                outputStream.write(byteArrayOf(0x1B, 0x40)) // Initialize Printer

                // Cetak Logo Baked Love
                try {
                    val logo = BitmapFactory.decodeResource(context.resources, R.drawable.bakedlove)
                    if (logo != null) {
                        outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center align
                        outputStream.write(decodeBitmapToEscPos(logo))
                        outputStream.write(byteArrayOf(0x0A)) // Feed sedikit setelah logo
                    }
                } catch (e: Exception) { e.printStackTrace() }

                val text = buildTextReceipt(receipt)
                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Reset ke Left align
                outputStream.write(text.toByteArray(java.nio.charset.Charset.forName("GBK")))
                outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)) // Feed lines
                
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Sedang mencetak ke ${device.name}...", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Gagal mencetak: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
        }.start()
    }

    internal fun drawReceipt(canvas: Canvas, receipt: ReceiptData, targetWidth: Float = BASE_W) {
        val scaleFactor = targetWidth / BASE_W
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)

        // Gunakan BASE_W (226f) sebagai patokan menggambar agar logika koordinat tidak berubah
        val W = BASE_W
        val cx = W / 2f
        val ml = 14f
        val mr = W - 14f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Hitung tinggi header dinamis agar muat Logo + Cabang + Alamat + Nama Toko
        var dynamicHeadHeight = 110f
        if (!receipt.cabang.isNullOrBlank()) dynamicHeadHeight += 15f
        if (!receipt.alamat.isNullOrBlank()) dynamicHeadHeight += 15f
        val yHead = dynamicHeadHeight + 30f 

        // HEADER: Background ungu gelap
        p.style = Paint.Style.FILL
        p.color = 0xFF4A2B66.toInt()
        canvas.drawRect(0f, 0f, W, yHead, p)

        // Accent diagonal strip di header
        p.color = 0xFF6A3D8F.toInt()
        val path = Path()
        path.moveTo(0f, 58f); path.lineTo(W, 44f)
        path.lineTo(W, yHead); path.lineTo(0f, yHead); path.close()
        canvas.drawPath(path, p)

        // Gambar Logo Baked Love di Header
        try {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.bakedlove)
            if (logo != null) {
                val logoW = 44f
                val logoH = (logo.height * logoW / logo.width)
                val logoRect = RectF(cx - (logoW / 2), 10f, cx + (logoW / 2), 10f + logoH)
                canvas.drawBitmap(logo, null, logoRect, p)
            }
        } catch (e: Exception) {}

        var curY = 66f

        // Menampilkan Info Cabang & Alamat di Header
        if (!receipt.cabang.isNullOrBlank()) {
            p.color = Color.WHITE
            p.textSize = 9.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Cabang: ${receipt.cabang}", cx, curY, p)
            curY += 15f
        }
        if (!receipt.alamat.isNullOrBlank()) {
            p.color = Color.WHITE // Fix: Gunakan Putih agar terbaca printer
            p.textSize = 8f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            // Manual wrap sederhana untuk alamat panjang
            if (p.measureText(receipt.alamat) > (W - 28f)) {
                val mid = receipt.alamat.length / 2
                val splitIdx = receipt.alamat.lastIndexOf(" ", mid).takeIf { it != -1 } ?: mid
                canvas.drawText(receipt.alamat.substring(0, splitIdx), cx, curY, p)
                curY += 11f
                canvas.drawText(receipt.alamat.substring(splitIdx).trim(), cx, curY, p)
                curY += 15f
            } else {
                canvas.drawText(receipt.alamat, cx, curY, p)
                curY += 15f
            }
        }

        // Nama Toko (Sekarang diposisikan di bawah Alamat/Cabang)
        p.color = 0xFFFFFFFF.toInt()
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 18f
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(receipt.toko.ifBlank { "BAKED LOVE" }.uppercase(), cx, curY + 12f, p)

        curY += 28f

        // Tagline
        p.color = 0xFFCE93D8.toInt()
        p.textSize = 7.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText(context.getString(R.string.receipt_thanks), cx, curY, p)


        // Badge "NOTA TRANSAKSI"
        p.color = 0xFFBA68C8.toInt()
        p.style = Paint.Style.FILL
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawRoundRect(RectF(cx - 40f, curY + 14f, cx + 40f, curY + 30f), 8f, 8f, p)
        p.color = 0xFFFFFFFF.toInt()
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(context.getString(R.string.receipt_title), cx, curY + 25f, p)


        var y = curY + 55f

        // INFO BARIS
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

        kv(context.getString(R.string.receipt_date_label), receipt.tanggal)
        kv(context.getString(R.string.receipt_invoice_label), "#${receipt.idTransaksi?.takeLast(8) ?: "-"}")
        kv(context.getString(R.string.receipt_cashier_label), receipt.kasir?.takeIf { it.isNotBlank() } ?: "Admin")
        receipt.namaPelanggan?.takeIf { it.isNotBlank() }?.let {
            val jenis = receipt.jenisPelanggan?.takeIf { value -> value.isNotBlank() } ?: "Umum"
            kv("Pelanggan", "$it ($jenis)")
        }

        // METODE PEMBAYARAN
        receipt.metodePembayaran?.takeIf { it.isNotBlank() }?.let {
            kv(context.getString(R.string.receipt_payment_method_label), it)
        }

        // UANG DITERIMA & KEMBALIAN (khusus cash)
        if (receipt.uangDiterima != null) {
            kv(context.getString(R.string.receipt_received_amount_label), "Rp ${fmt(receipt.uangDiterima)}")
        }
        if (receipt.kembalian != null) {
            kv(context.getString(R.string.receipt_change_label), "Rp ${fmt(receipt.kembalian)}")
        }

        // NO GOPAY
        if (!receipt.noGopay.isNullOrBlank()) {
            kv(context.getString(R.string.receipt_gopay_number_label), receipt.noGopay)
        }


        y += 4f
        drawDashed(canvas, ml, mr, y); y += 12f


        // HEADER TABEL
        p.style = Paint.Style.FILL
        p.color = 0xFFF3E5F5.toInt()
        canvas.drawRoundRect(RectF(ml, y, mr, y + 16f), 4f, 4f, p)
        p.color = 0xFF5C3D75.toInt()
        p.textSize = 7.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.LEFT
        canvas.drawText("  ${context.getString(R.string.receipt_product_header)}", ml + 4f, y + 11f, p)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.receipt_qty_header), cx, y + 11f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("${context.getString(R.string.receipt_price_header)}  ", mr - 2f, y + 11f, p)
        y += 20f

        // LOOP ITEM: Agar nota "lengkap" dan tidak menumpuk satu sama lain
        val drawItems = if (receipt.items.isNotEmpty()) receipt.items else {
            listOf(ReceiptItem(receipt.namaProduk, receipt.jumlah, if(receipt.jumlah > 0) receipt.totalHarga/receipt.jumlah else receipt.totalHarga))
        }

        drawItems.forEach { item ->
            p.style = Paint.Style.FILL
            p.color = 0xFFFAF5FF.toInt()
            canvas.drawRect(ml, y - 2f, mr, y + 26f, p)

            p.textAlign = Paint.Align.LEFT
            p.color = 0xFF222222.toInt()
            p.textSize = 8.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("  ${item.nama}", ml + 4f, y + 10f, p)

            p.color = 0xFF888888.toInt()
            p.textSize = 6.5f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("  @ Rp ${fmt(item.harga)}", ml + 4f, y + 22f, p)

            p.textAlign = Paint.Align.CENTER
            p.color = 0xFF5C3D75.toInt()
            p.textSize = 10f
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${item.qty}", cx, y + 15f, p)

            p.textAlign = Paint.Align.RIGHT
            p.color = 0xFF4A2B66.toInt()
            p.textSize = 9f
            canvas.drawText("Rp ${fmt(item.qty * item.harga)}  ", mr - 2f, y + 15f, p)
            y += 34f
        }

        drawDashed(canvas, ml, mr, y); y += 12f

        // SUMMARY
        p.textAlign = Paint.Align.LEFT
        p.color = 0xFF888888.toInt()
        p.textSize = 8f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(context.getString(R.string.receipt_subtotal, receipt.jumlah), ml, y, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("Rp ${fmt(receipt.totalHarga)}", mr, y, p)
        y += 10f

        canvas.drawText(context.getString(R.string.receipt_service_fee), ml, y, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText("Rp 0", mr, y, p)
        y += 14f

        // TOTAL BOX
        p.style = Paint.Style.FILL
        p.color = 0xFF4A2B66.toInt()
        canvas.drawRoundRect(RectF(ml, y, mr, y + 30f), 10f, 10f, p)

        p.textAlign = Paint.Align.LEFT
        p.color = 0xFFCE93D8.toInt()
        p.textSize = 8.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("  ${context.getString(R.string.receipt_total_payment)}", ml + 4f, y + 19f, p)

        p.textAlign = Paint.Align.RIGHT
        p.color = 0xFFFFFFFF.toInt()
        p.textSize = 12f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rp ${fmt(receipt.totalHarga)}  ", mr - 4f, y + 20f, p)
        y += 42f

        // LUNAS STAMP
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
        canvas.drawText(context.getString(R.string.receipt_paid), cx, y + 13f, p)
        y += 26f

        // FOOTER TEXT
        p.style = Paint.Style.FILL
        p.color = 0xFFBDBDBD.toInt()
        p.textSize = 6f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(context.getString(R.string.receipt_footer_1), cx, y, p); y += 9f
        canvas.drawText(context.getString(R.string.receipt_footer_2), cx, y, p); y += 9f
        canvas.drawText(context.getString(R.string.receipt_footer_3), cx, y, p)

        p.color = 0xFF4A2B66.toInt()
        canvas.drawRect(0f, y + 15f, W, y + 25f, p) // Footer bar menempel di bawah teks, bukan di ujung kertas
        
        canvas.restore()
    }

    fun buildTextReceipt(receipt: ReceiptData): String {
        val sb = StringBuilder()
        val width = 32 // Standar karakter printer thermal 58mm
        val line = "-".repeat(width) + "\n"
        val boldLine = "=".repeat(width) + "\n"

        fun center(text: String): String {
            val spaces = (width - text.length) / 2
            return if (spaces <= 0) text + "\n" else " ".repeat(spaces) + text + "\n"
        }

        fun leftRight(left: String, right: String): String {
            val spaceCount = width - left.length - right.length
            return left + " ".repeat(spaceCount.coerceAtLeast(1)) + right + "\n"
        }

        sb.append(boldLine)
        if (!receipt.cabang.isNullOrBlank()) sb.append(center("Cabang: ${receipt.cabang}"))
        if (!receipt.alamat.isNullOrBlank()) sb.append(center(receipt.alamat))
        sb.append(center(receipt.toko.ifBlank { "BAKED LOVE" }.uppercase()))
        sb.append(boldLine)
        
        sb.append("Tgl   : ${receipt.tanggal}\n")
        sb.append("Kasir : ${receipt.kasir ?: "Admin"}\n")
        sb.append("Inv   : #${receipt.idTransaksi?.takeLast(8)}\n")
        if (!receipt.namaPelanggan.isNullOrBlank()) {
            sb.append("Plg   : ${receipt.namaPelanggan}\n")
        }
        sb.append(line)
        sb.append(leftRight("PRODUK (QTY)", "SUBTOTAL"))
        sb.append(line)

        receipt.items.forEach { item ->
            val nameText = if (item.nama.length > 20) item.nama.take(17) + "..." else item.nama
            sb.append("$nameText\n")
            val subtotal = "Rp ${fmt(item.qty * item.harga)}"
            sb.append(leftRight("  ${item.qty} x ${fmt(item.harga)}", subtotal))
        }

        sb.append(line)
        sb.append(leftRight("TOTAL", "Rp ${fmt(receipt.totalHarga)}"))
        
        if (!receipt.metodePembayaran.isNullOrBlank()) {
            sb.append(leftRight("Bayar (${receipt.metodePembayaran})", "Rp ${fmt(receipt.uangDiterima ?: 0)}"))
            if (receipt.kembalian != null) {
                sb.append(leftRight("Kembali", "Rp ${fmt(receipt.kembalian)}"))
            }
        }
        
        sb.append(boldLine)
        sb.append(center("Terima Kasih Atas"))
        sb.append(center("Kunjungan Anda"))
        sb.append(boldLine)
        
        return sb.toString()
    }

    private fun decodeBitmapToEscPos(bmp: Bitmap): ByteArray {
        val width = 200 // Ukuran logo yang proporsional untuk printer 58mm
        val height = (bmp.height * width / bmp.width)
        val scaledBmp = Bitmap.createScaledBitmap(bmp, width, height, true)
        
        val bwBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bwBmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE) // FIX: Memberi background putih agar area transparan tidak jadi hitam pekat
        val colorMatrix = android.graphics.ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(scaledBmp, 0f, 0f, paint)

        val widthBytes = (width + 7) / 8
        val data = ByteArray(widthBytes * height + 8)
        
        data[0] = 0x1D; data[1] = 0x76; data[2] = 0x30; data[3] = 0 
        data[4] = (widthBytes % 256).toByte()
        data[5] = (widthBytes / 256).toByte()
        data[6] = (height % 256).toByte()
        data[7] = (height / 256).toByte()

        var k = 8
        for (i in 0 until height) {
            for (j in 0 until widthBytes) {
                var slice = 0
                for (b in 0 until 8) {
                    val x = j * 8 + b
                    if (x < width) {
                        val pixel = bwBmp.getPixel(x, i)
                        val grey = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        if (grey < 128) slice = slice or (1 shl (7 - b))
                    }
                }
                data[k++] = slice.toByte()
            }
        }
        return data
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
