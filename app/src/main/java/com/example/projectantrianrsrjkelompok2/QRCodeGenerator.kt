package com.example.projectantrianrsrjkelompok2.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {

    fun generateQRCode(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateBookingQRContent(booking: com.example.projectantrianrsrjkelompok2.Booking): String {
        return buildString {
            append("BOOKING_ID:${booking.id}\n")
            append("QUEUE_NUMBER:${booking.queueNumber}\n")
            append("PATIENT:${booking.patientName}\n")
            append("DOCTOR:${booking.doctorName}\n")
            append("SPECIALIZATION:${booking.specialization}\n")
            append("DATE:${booking.date}\n")
            append("TIME:${booking.time}\n")
            append("STATUS:${booking.status.name}")
        }
    }
}
