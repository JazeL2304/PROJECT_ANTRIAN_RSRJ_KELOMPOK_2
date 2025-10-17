package com.example.projectantrianrsrjkelompok2

// -------------------- MODEL BOOKING --------------------
data class Booking(
    val id: String,
    val queueNumber: Int,
    val patientName: String,
    val doctorName: String,
    val specialization: String,
    val date: String,
    val time: String,
    val complaint: String = "",
    val diagnosis: String = "",       // ✅ Tambahan
    val prescription: String = "",    // ✅ Tambahan
    val status: BookingStatus,
    val createdAt: Long = System.currentTimeMillis()
)

// -------------------- ENUM STATUS BOOKING --------------------
enum class BookingStatus {
    WAITING, CALLED, COMPLETED, CANCELLED, MISSED
}

// -------------------- EXTENSIONS --------------------
fun BookingStatus.toDisplayString(): String = when (this) {
    BookingStatus.WAITING -> "Menunggu"
    BookingStatus.CALLED -> "Dipanggil"
    BookingStatus.COMPLETED -> "Selesai"
    BookingStatus.CANCELLED -> "Dibatalkan"
    BookingStatus.MISSED -> "Terlewat"
}

fun BookingStatus.getColorResource(): Int = when (this) {
    BookingStatus.WAITING -> android.R.color.holo_orange_dark
    BookingStatus.CALLED -> android.R.color.holo_blue_dark
    BookingStatus.COMPLETED -> android.R.color.holo_green_dark
    BookingStatus.CANCELLED -> android.R.color.holo_red_dark
    BookingStatus.MISSED -> android.R.color.darker_gray
}
