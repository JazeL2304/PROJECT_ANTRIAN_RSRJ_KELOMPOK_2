package com.example.projectantrianrsrjkelompok2

object DataSource {

    // ===============================
    // 👨‍⚕️ DOKTER
    // ===============================
    private val doctorList = mutableListOf(
        Doctor(1, "Dr. Ahmad Santoso", "Dokter Umum", "Senin–Jumat 08:00–15:00"),
        Doctor(2, "Dr. Budi Dental", "Dokter Gigi", "Senin–Kamis 09:00–16:00"),
        Doctor(3, "Dr. Indra Mata", "Dokter Mata", "Senin–Jumat 08:00–14:00"),
        Doctor(4, "Dr. Ani Pediatri", "Dokter Anak", "Setiap Hari 24 Jam")
    )

    fun getAllDoctors(): List<Doctor> = doctorList.toList()

    fun addDoctor(doctor: Doctor) {
        val nextId = (doctorList.maxOfOrNull { it.id } ?: 0) + 1
        doctorList.add(doctor.copy(id = nextId))
        println("✅ Dokter baru ditambahkan: ${doctor.name} (ID $nextId)")
    }

    fun removeDoctor(doctor: Doctor) {
        val removed = doctorList.removeIf { it.id == doctor.id }
        if (removed) println("🗑️ Dokter '${doctor.name}' dihapus.")
        else println("⚠️ Dokter '${doctor.name}' tidak ditemukan.")
    }

    fun updateDoctorSchedule(id: Int, newSchedule: String) {
        val index = doctorList.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = doctorList[index]
            doctorList[index] = old.copy(schedule = newSchedule)
            println("🕐 Jadwal ${old.name} diperbarui ke $newSchedule")
        } else {
            println("⚠️ Dokter dengan ID $id tidak ditemukan.")
        }
    }

    fun getDoctorById(id: Int): Doctor? = doctorList.find { it.id == id }

    fun getDoctorsBySpecialization(specId: Int): List<Doctor> = when (specId) {
        1 -> doctorList.filter { it.specialization.contains("Umum", true) }
        2 -> doctorList.filter { it.specialization.contains("Gigi", true) }
        3 -> doctorList.filter { it.specialization.contains("Mata", true) }
        4 -> doctorList.filter { it.specialization.contains("Anak", true) }
        else -> doctorList
    }

    // ===============================
    // 🧍‍♀️ PASIEN
    // ===============================
    private val patientList = mutableListOf(
        Patient(1, "Atila Falah", "Laki-laki", 21, "Jl. Anggrek No. 12"),
        Patient(2, "Rizky Amalia", "Perempuan", 23, "Jl. Melati No. 8"),
        Patient(3, "Dewi Lestari", "Perempuan", 20, "Jl. Mawar No. 5")
    )

    fun getAllPatients(): List<Patient> = patientList.toList()

    fun addPatient(patient: Patient): Boolean {
        val duplicate = patientList.any {
            it.name.equals(patient.name, true) && it.address.equals(patient.address, true)
        }

        return if (duplicate) {
            println("⚠️ Pasien '${patient.name}' sudah terdaftar di alamat ${patient.address}")
            false
        } else {
            val nextId = (patientList.maxOfOrNull { it.id } ?: 0) + 1
            patientList.add(patient.copy(id = nextId))
            println("✅ Pasien baru ditambahkan: ${patient.name} (ID: $nextId)")
            true
        }
    }

    fun removePatient(patient: Patient) {
        val removed = patientList.removeIf {
            it.name.equals(patient.name, true) &&
                    it.gender.equals(patient.gender, true) &&
                    it.age == patient.age &&
                    it.address.equals(patient.address, true)
        }

        if (removed) println("🗑️ Pasien '${patient.name}' berhasil dihapus.")
        else println("⚠️ Gagal menghapus pasien '${patient.name}' — tidak ditemukan.")
    }

    fun findPatientsByName(nameQuery: String): List<Patient> =
        patientList.filter { it.name.contains(nameQuery, true) }

    fun clearAndSetPatients(newList: List<Patient>) {
        patientList.clear()
        patientList.addAll(newList)
        println("✅ Data pasien diperbarui. Total sekarang: ${patientList.size}")
    }

    // ===============================
    // 🏥 SPESIALISASI
    // ===============================
    fun getSpecializations(): List<Specialization> = listOf(
        Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
        Specialization(2, "Layanan Gigi", "Perawatan gigi dan mulut", "🦷"),
        Specialization(3, "Layanan Mata", "Kesehatan mata dan penglihatan", "👁️"),
        Specialization(4, "Layanan Anak", "Kesehatan bayi dan anak-anak", "👶")
    )

    // ===============================
    // ⏰ JAM PRAKTIK
    // ===============================
    fun getTimeSlots(): List<String> = listOf(
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
        "11:00", "13:00", "13:30", "14:00", "14:30", "15:00"
    )

    // ===============================
    // 📋 BOOKING / ANTRIAN & RIWAYAT PASIEN
    // ✅ FIXED: Data antrian hari ini + history
    // ===============================
    private val bookingHistory = mutableListOf(
        // ✅ Data lama (COMPLETED)
        Booking(
            id = "B001",
            queueNumber = 7,
            patientName = "Ahmad Santoso",
            doctorName = "Dr. Budi Dental",
            specialization = "Layanan Gigi",
            date = "14/10/2025",  // ✅ GANTI FORMAT!
            time = "08:30",
            complaint = "Pemeriksaan rutin gigi dan pembersihan karang gigi",
            diagnosis = "Karang gigi ringan",
            prescription = "Scaling gigi, sikat gigi lebih teratur",
            status = BookingStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        ),

        Booking(
            id = "B002",
            queueNumber = 12,
            patientName = "Ahmad Santoso",
            doctorName = "Dr. Indra Mata",
            specialization = "Layanan Mata",
            date = "16/10/2025",  // ✅ GANTI FORMAT!
            time = "09:00",
            complaint = "Mata perih dan penglihatan kabur",
            diagnosis = "Mata kering dan lelah akibat terlalu lama menatap layar",
            prescription = "Tetes mata artifisial, istirahat mata setiap 20 menit",
            status = BookingStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L)
        ),

        Booking(
            id = "B003",
            queueNumber = 5,
            patientName = "Ahmad Santoso",
            doctorName = "Dr. Ani Pediatri",
            specialization = "Layanan Anak",
            date = "17/10/2025",  // ✅ GANTI FORMAT!
            time = "10:30",
            complaint = "Imunisasi anak umur 2 tahun",
            diagnosis = "Anak sehat, imunisasi lengkap",
            prescription = "Vaksin DPT, Vitamin A",
            status = BookingStatus.COMPLETED,
            createdAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        ),

        // ✅ DATA BARU: Antrian HARI INI (17 Okt) - WAITING
        Booking(
            id = "B004",
            queueNumber = 1,
            patientName = "Rizky Amalia",
            doctorName = "Dr. Ahmad Santoso",
            specialization = "Layanan Umum",
            date = "17/10/2025",  // ✅ GANTI FORMAT!
            time = "14:00",
            complaint = "Demam dan batuk",
            diagnosis = "",
            prescription = "",
            status = BookingStatus.WAITING,
            createdAt = System.currentTimeMillis()
        ),

        Booking(
            id = "B005",
            queueNumber = 2,
            patientName = "Dewi Lestari",
            doctorName = "Dr. Ahmad Santoso",
            specialization = "Layanan Umum",
            date = "17/10/2025",  // ✅ GANTI FORMAT!
            time = "14:30",
            complaint = "Sakit kepala",
            diagnosis = "",
            prescription = "",
            status = BookingStatus.WAITING,
            createdAt = System.currentTimeMillis()
        ),

        Booking(
            id = "B006",
            queueNumber = 3,
            patientName = "Atila Falah",
            doctorName = "Dr. Ahmad Santoso",
            specialization = "Layanan Umum",
            date = "17/10/2025",  // ✅ GANTI FORMAT!
            time = "15:00",
            complaint = "Kontrol tekanan darah",
            diagnosis = "",
            prescription = "",
            status = BookingStatus.CALLED,
            createdAt = System.currentTimeMillis()
        )
    )

    private var activeBooking: Booking? = null

    fun getBookingHistory(): List<Booking> = bookingHistory.sortedByDescending { it.createdAt }

    fun addToHistory(booking: Booking) {
        // ✅ Cek duplikat berdasarkan ID
        val idx = bookingHistory.indexOfFirst { it.id == booking.id }
        if (idx != -1) {
            bookingHistory[idx] = booking
        } else {
            bookingHistory.add(booking)
        }
    }

    fun setActiveBooking(booking: Booking) {
        activeBooking = booking
        println("📌 Booking aktif diset ke: ${booking.id}")
    }

    fun getActiveBooking(): Booking? = activeBooking

    // ✅ FIXED: Jangan hapus bookingHistory!
    fun clearActiveBooking() {
        activeBooking = null
        // ❌ DIHAPUS: bookingHistory.clear()
        println("🗑️ Booking aktif dihapus (history tetap ada).")
    }

    // ✅ NEW METHOD: Clear active booking saja (keep history)
    fun clearActiveBookingOnly() {
        activeBooking = null
        println("🗑️ Active booking dihapus (history tetap ada).")
    }

    fun hasActiveBooking(): Boolean = activeBooking != null

    // ===============================
    // 📊 FUNGSI UNTUK ADMIN DASHBOARD & REPORTS
    // ===============================

    /** ✅ Total jumlah pasien terdaftar */
    fun getTotalPatients(): Int = patientList.size

    /** ✅ Total jumlah dokter */
    fun getTotalDoctors(): Int = doctorList.size

    /** ✅ Ambil booking hari ini berdasarkan tanggal */
    fun getTodayBookings(): List<Booking> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        return bookingHistory.filter { it.date == today }
    }

    /** ✅ Ambil semua antrian yang masih aktif (WAITING atau CALLED) */
    fun getActiveQueues(): List<Booking> {
        return bookingHistory.filter {
            it.status == BookingStatus.WAITING ||
                    it.status == BookingStatus.CALLED
        }
    }

    /** ✅ Generate nomor antrian berikutnya */
    fun getNextQueueNumber(): Int {
        val maxQueue = bookingHistory.maxOfOrNull { it.queueNumber } ?: 0
        return maxQueue + 1
    }

    /** ✅ Ambil list dokter (alias untuk compatibility) */
    fun getDoctors(): List<Doctor> = getAllDoctors()

    // ===============================
    // ✅ FUNGSI BARU UNTUK VALIDASI ANTRIAN
    // ===============================

    /**
     * ✅ Cek apakah ada pasien yang sedang dipanggil (CALLED) hari ini
     * Return true jika ada pasien dengan status CALLED
     */
    fun hasCalledPatient(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        return bookingHistory.any {
            it.date == today && it.status == BookingStatus.CALLED
        }
    }

    /**
     * ✅ Update status booking berdasarkan ID
     * Digunakan untuk mengubah status WAITING → CALLED → COMPLETED
     */
    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        val index = bookingHistory.indexOfFirst { it.id == bookingId }
        if (index != -1) {
            val booking = bookingHistory[index]
            bookingHistory[index] = booking.copy(status = newStatus)
            println("✅ Status booking ${booking.patientName} diubah menjadi ${newStatus.name}")
        } else {
            println("⚠️ Booking dengan ID $bookingId tidak ditemukan.")
        }
    }

    /**
     * ✅ Update diagnosis dan resep untuk booking (saat selesai pemeriksaan)
     */
    fun updateBookingDiagnosis(
        bookingId: String,
        diagnosis: String,
        prescription: String
    ) {
        val index = bookingHistory.indexOfFirst { it.id == bookingId }
        if (index != -1) {
            val booking = bookingHistory[index]
            bookingHistory[index] = booking.copy(
                diagnosis = diagnosis,
                prescription = prescription,
                status = BookingStatus.COMPLETED
            )
            println("✅ Diagnosis dan resep untuk ${booking.patientName} berhasil disimpan")
        }
    }
}
