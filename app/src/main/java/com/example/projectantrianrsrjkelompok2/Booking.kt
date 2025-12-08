package com.example.projectantrianrsrjkelompok2

// ✅ UPDATED: Added userId field untuk user-specific bookings

data class Booking(
    val id: String = "",
    val queueNumber: Int = 0,
    val patientName: String = "",
    val doctorName: String = "",
    val specialization: String = "",
    val date: String = "",
    val time: String = "",
    val complaint: String = "-",
    val diagnosis: String = "",
    val prescription: String = "",
    val status: BookingStatus = BookingStatus.WAITING,
    val createdAt: Long = System.currentTimeMillis(),
    val firebaseId: String = "",

    // ✅ NEW: User ID field - CRITICAL untuk user isolation
    val userId: String = ""  // Stores: "user001", "user002", etc
)

enum class BookingStatus {
    WAITING,
    CALLED,
    COMPLETED,
    CANCELLED,
    MISSED;

    fun toDisplayString(): String {
        return when (this) {
            WAITING -> "Menunggu"
            CALLED -> "Dipanggil"
            COMPLETED -> "Selesai"
            CANCELLED -> "Dibatalkan"
            MISSED -> "Terlewat"
        }
    }

    fun getColorResource(): Int {
        return when (this) {
            WAITING -> android.R.color.holo_blue_light
            CALLED -> android.R.color.holo_orange_light
            COMPLETED -> android.R.color.holo_green_light
            CANCELLED -> android.R.color.holo_red_light
            MISSED -> android.R.color.darker_gray
        }
    }
}

// ✅ Extension function untuk String
// Ini diperlukan jika ada code yang call toDisplayString() pada String
fun String.toDisplayString(): String {
    return try {
        BookingStatus.valueOf(this).toDisplayString()
    } catch (e: Exception) {
        this // Return original string jika bukan BookingStatus
    }
}