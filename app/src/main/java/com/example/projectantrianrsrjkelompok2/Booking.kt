package com.example.projectantrianrsrjkelompok2

// -------------------- MODEL BOOKING --------------------
data class Booking(

    // ID unik Firebase (wajib var supaya bisa di-set setelah getValue)
    var firebaseId: String = "",

    // ID internal aplikasi (opsional)
    var id: String = "",

    var queueNumber: Int = 0,
    var patientName: String = "",
    var doctorName: String = "",
    var specialization: String = "",
    var date: String = "",
    var time: String = "",
    var complaint: String = "",
    var diagnosis: String = "",
    var prescription: String = "",

    var status: BookingStatus = BookingStatus.WAITING,

    var createdAt: Long = System.currentTimeMillis()
)

// -------------------- ENUM STATUS BOOKING --------------------
enum class BookingStatus {
    WAITING,
    CALLED,
    COMPLETED,
    CANCELLED,
    MISSED
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
