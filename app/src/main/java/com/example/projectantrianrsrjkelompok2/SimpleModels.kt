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
    val status: String // "Menunggu", "Sedang Dilayani", "Selesai"
)

// Static data untuk simulasi
object DataSource {

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

    // Simulasi data antrian
    fun getSampleQueues(): List<Queue> {
        return listOf(
            Queue(1, "John Doe", "Dr. Ahmad Santoso", "Poli Umum", 15, "2024-12-15", "09:30", "Menunggu"),
            Queue(2, "Jane Smith", "Dr. Budi Dental", "Poli Gigi", 3, "2024-12-14", "10:00", "Selesai")
        )
    }
}