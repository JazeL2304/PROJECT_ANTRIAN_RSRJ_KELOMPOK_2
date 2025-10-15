package com.example.projectantrianrsrjkelompok2

// Data class sederhana untuk dokter
data class Doctor(
    val id: Int,
    val name: String,
    val specialization: String,
    val schedule: String
)

// Data class sederhana untuk poli
data class Specialization(
    val id: Int,
    val name: String,
    val description: String,
    val emoji: String
)

// Data class sederhana untuk antrian
data class Queue(
    val id: Int,
    val patientName: String,
    val doctorName: String,
    val specialization: String,
    val queueNumber: Int,
    val date: String,
    val time: String,
    val status: String
)

// ===== Booking & BookingStatus =====
data class Booking(
    val id: String,
    val queueNumber: Int,
    val patientName: String,
    val doctorName: String,
    val specialization: String,
    val date: String,
    val time: String,
    val complaint: String = "",
    val status: BookingStatus,
    val createdAt: Long = System.currentTimeMillis()
)

enum class BookingStatus {
    WAITING,
    CALLED,
    COMPLETED,
    CANCELLED,
    MISSED
}

fun BookingStatus.toDisplayString(): String {
    return when (this) {
        BookingStatus.WAITING -> "Menunggu"
        BookingStatus.CALLED -> "Dipanggil"
        BookingStatus.COMPLETED -> "Selesai"
        BookingStatus.CANCELLED -> "Dibatalkan"
        BookingStatus.MISSED -> "Terlewat"
    }
}

fun BookingStatus.getColorResource(): Int {
    return when (this) {
        BookingStatus.WAITING -> android.R.color.holo_orange_dark
        BookingStatus.CALLED -> android.R.color.holo_blue_dark
        BookingStatus.COMPLETED -> android.R.color.holo_green_dark
        BookingStatus.CANCELLED -> android.R.color.holo_red_dark
        BookingStatus.MISSED -> android.R.color.darker_gray
    }
}

object DataSource {

    private val bookingHistory = mutableListOf(
        Booking(
            id = "A001",
            queueNumber = 5,
            patientName = "John Doe",
            doctorName = "Dr. Ahmad Santoso",
            specialization = "Layanan Umum",
            date = "2025-10-10",
            time = "09:00",
            complaint = "Demam dan batuk",
            status = BookingStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 432000000
        ),
        Booking(
            id = "A002",
            queueNumber = 12,
            patientName = "John Doe",
            doctorName = "Dr. Budi Dental",
            specialization = "Layanan Gigi",
            date = "2025-10-12",
            time = "14:00",
            complaint = "Sakit gigi geraham kiri",
            status = BookingStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - 259200000
        ),
        Booking(
            id = "A003",
            queueNumber = 8,
            patientName = "John Doe",
            doctorName = "Dr. Indra Mata",
            specialization = "Layanan Mata",
            date = "2025-10-13",
            time = "10:30",
            complaint = "Mata merah dan gatal",
            status = BookingStatus.CANCELLED,
            createdAt = System.currentTimeMillis() - 172800000
        ),
        Booking(
            id = "A004",
            queueNumber = 15,
            patientName = "John Doe",
            doctorName = "Dr. Ani Pediatri",
            specialization = "Layanan Anak",
            date = "2025-10-14",
            time = "11:00",
            complaint = "Anak demam tinggi",
            status = BookingStatus.MISSED,
            createdAt = System.currentTimeMillis() - 86400000
        )
    )

    private var currentActiveBooking: Booking? = null

    fun getSpecializations(): List<Specialization> {
        return listOf(
            Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
            Specialization(2, "Layanan Gigi", "Kesehatan gigi dan mulut", "🦷"),
            Specialization(3, "Layanan Mata", "Kesehatan mata", "👁️"),
            Specialization(4, "Layanan Jantung", "Kesehatan jantung", "❤️"),
            Specialization(5, "Layanan Anak", "Kesehatan anak", "👶"),
            Specialization(6, "Layanan Kandungan", "Kesehatan ibu dan anak", "🤱")
        )
    }

    fun getDoctorsBySpecialization(specId: Int): List<Doctor> {
        return when(specId) {
            1 -> listOf(
                Doctor(1, "Dr. Ahmad Santoso", "Dokter Umum", "Senin-Jumat 08:00-15:00"),
                Doctor(2, "Dr. Siti Nurhaliza", "Dokter Umum", "Selasa-Sabtu 13:00-20:00")
            )
            2 -> listOf(
                Doctor(3, "Dr. Budi Dental", "Dokter Gigi", "Senin-Kamis 09:00-16:00"),
                Doctor(4, "Dr. Maria Gigi", "Dokter Gigi", "Selasa-Jumat 14:00-21:00")
            )
            3 -> listOf(
                Doctor(5, "Dr. Indra Mata", "Dokter Mata", "Senin-Jumat 08:00-14:00")
            )
            4 -> listOf(
                Doctor(6, "Dr. Kartika Jantung", "Dokter Jantung", "Senin-Jumat 07:00-15:00")
            )
            5 -> listOf(
                Doctor(7, "Dr. Ani Pediatri", "Dokter Anak", "Setiap Hari 24 Jam")
            )
            6 -> listOf(
                Doctor(8, "Dr. Dewi ObGyn", "Dokter Kandungan", "Senin-Sabtu 08:00-17:00")
            )
            else -> emptyList()
        }
    }

    fun getTimeSlots(): List<String> {
        return listOf(
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
            "11:00", "11:30", "13:00", "13:30", "14:00", "14:30",
            "15:00", "15:30", "16:00", "16:30"
        )
    }

    fun getSampleQueues(): List<Queue> {
        return listOf(
            Queue(1, "John Doe", "Dr. Ahmad Santoso", "Poli Umum", 15, "2024-12-15", "09:30", "Menunggu"),
            Queue(2, "Jane Smith", "Dr. Budi Dental", "Poli Gigi", 3, "2024-12-14", "10:00", "Selesai")
        )
    }

    fun getBookingHistory(): List<Booking> {
        return bookingHistory.sortedByDescending { it.createdAt }
    }

    fun addBooking(booking: Booking) {
        bookingHistory.add(0, booking)
    }

    fun getBookingById(id: String): Booking? {
        return bookingHistory.find { it.id == id }
    }

    fun updateBookingStatus(id: String, newStatus: BookingStatus) {
        val booking = bookingHistory.find { it.id == id }
        booking?.let {
            val index = bookingHistory.indexOf(it)
            bookingHistory[index] = it.copy(status = newStatus)
        }
    }

    fun cancelBooking(id: String): Boolean {
        val booking = bookingHistory.find { it.id == id }
        return if (booking != null) {
            updateBookingStatus(id, BookingStatus.CANCELLED)
            true
        } else {
            false
        }
    }

    fun setActiveBooking(booking: Booking) {
        currentActiveBooking = booking
        addBooking(booking)
    }

    fun getActiveBooking(): Booking? {
        return currentActiveBooking
    }

    fun clearActiveBooking() {
        currentActiveBooking = null
    }

    fun hasActiveBooking(): Boolean {
        return currentActiveBooking != null
    }

    fun updateActiveBookingStatus(newStatus: BookingStatus) {
        currentActiveBooking = currentActiveBooking?.copy(status = newStatus)
        currentActiveBooking?.let {
            updateBookingStatus(it.id, newStatus)
        }
    }

    // ← TAMBAHAN BARU: Function untuk add/update history
    fun addToHistory(booking: Booking) {
        // Cek apakah sudah ada di history (by id)
        val existingIndex = bookingHistory.indexOfFirst { it.id == booking.id }
        if (existingIndex != -1) {
            // Update existing booking
            bookingHistory[existingIndex] = booking
        } else {
            // Add new booking
            bookingHistory.add(0, booking)
        }
    }
}
