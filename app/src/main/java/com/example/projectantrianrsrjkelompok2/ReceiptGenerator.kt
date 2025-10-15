package com.example.projectantrianrsrjkelompok2.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.toDisplayString // TAMBAHKAN INI
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReceiptGenerator {

    fun generateAndSaveReceipt(context: Context, booking: Booking): Boolean {
        try {
            // Create PDF document
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Draw receipt content
            drawReceiptContent(canvas, paint, booking)

            pdfDocument.finishPage(page)

            // Save to Downloads folder
            val fileName = "Struk_${booking.id}_${System.currentTimeMillis()}.pdf"

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                saveToDownloadsQ(context, pdfDocument, fileName)
            } else {
                // Android 9 and below
                saveToDownloadsLegacy(context, pdfDocument, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun drawReceiptContent(canvas: Canvas, paint: Paint, booking: Booking) {
        var yPos = 50f
        val leftMargin = 40f

        // Header
        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("RUMAH SAKIT RAWAT JALAN", leftMargin, yPos, paint)
        yPos += 30f

        paint.textSize = 12f
        paint.typeface = android.graphics.Typeface.DEFAULT
        canvas.drawText("Jl. Kesehatan No. 123, Jakarta", leftMargin, yPos, paint)
        yPos += 20f
        canvas.drawText("Telp: (021) 1234-5678", leftMargin, yPos, paint)
        yPos += 40f

        // Line separator
        paint.strokeWidth = 2f
        canvas.drawLine(leftMargin, yPos, 555f, yPos, paint)
        yPos += 30f

        // Title
        paint.textSize = 18f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("BUKTI PENDAFTARAN", leftMargin, yPos, paint)
        yPos += 40f

        // Booking details
        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.DEFAULT

        canvas.drawText("Nomor Antrian:", leftMargin, yPos, paint)
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = 28f
        paint.color = Color.parseColor("#2196F3")
        canvas.drawText("${booking.queueNumber}", leftMargin + 200f, yPos, paint)
        yPos += 50f

        // Reset style
        paint.color = Color.BLACK
        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.DEFAULT

        canvas.drawText("ID Booking:", leftMargin, yPos, paint)
        canvas.drawText(booking.id, leftMargin + 200f, yPos, paint)
        yPos += 30f

        canvas.drawText("Nama Pasien:", leftMargin, yPos, paint)
        canvas.drawText(booking.patientName, leftMargin + 200f, yPos, paint)
        yPos += 30f

        canvas.drawText("Dokter:", leftMargin, yPos, paint)
        canvas.drawText(booking.doctorName, leftMargin + 200f, yPos, paint)
        yPos += 30f

        canvas.drawText("Spesialisasi:", leftMargin, yPos, paint)
        canvas.drawText(booking.specialization, leftMargin + 200f, yPos, paint)
        yPos += 30f

        canvas.drawText("Tanggal:", leftMargin, yPos, paint)
        canvas.drawText(formatDate(booking.date), leftMargin + 200f, yPos, paint)
        yPos += 30f

        canvas.drawText("Jam:", leftMargin, yPos, paint)
        canvas.drawText(booking.time, leftMargin + 200f, yPos, paint)
        yPos += 30f

        canvas.drawText("Status:", leftMargin, yPos, paint)
        canvas.drawText(booking.status.toDisplayString(), leftMargin + 200f, yPos, paint)
        yPos += 50f

        // Line separator
        canvas.drawLine(leftMargin, yPos, 555f, yPos, paint)
        yPos += 30f

        // Footer
        paint.textSize = 12f
        paint.color = Color.parseColor("#666666")
        canvas.drawText("Harap datang 15 menit sebelum jadwal", leftMargin, yPos, paint)
        yPos += 20f
        canvas.drawText("Tunjukkan bukti ini kepada petugas pendaftaran", leftMargin, yPos, paint)
        yPos += 40f

        val currentDate = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
            .format(Date())
        canvas.drawText("Dicetak pada: $currentDate", leftMargin, yPos, paint)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun saveToDownloadsQ(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        return uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
                Toast.makeText(
                    context,
                    "Struk berhasil disimpan di Downloads",
                    Toast.LENGTH_LONG
                ).show()
                true
            } ?: false
        } ?: false
    }

    private fun saveToDownloadsLegacy(
        context: Context,
        pdfDocument: PdfDocument,
        fileName: String
    ): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadsDir, fileName)

        return try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
                pdfDocument.close()
                Toast.makeText(
                    context,
                    "Struk berhasil disimpan di Downloads/$fileName",
                    Toast.LENGTH_LONG
                ).show()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
