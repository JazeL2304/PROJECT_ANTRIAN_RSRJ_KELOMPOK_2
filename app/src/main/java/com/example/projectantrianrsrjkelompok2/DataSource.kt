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

    /** ✅ Ubah jadwal dokter berdasarkan ID */
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

    /** ✅ Ambil dokter berdasarkan ID */
    fun getDoctorById(id: Int): Doctor? = doctorList.find { it.id == id }

    /** ✅ Ambil daftar dokter berdasarkan spesialisasi */
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

    /** ✅ Tambah pasien baru, return true jika berhasil */
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

    /** 🔹 Ganti seluruh daftar pasien (digunakan setelah penghapusan manual) */
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
    // ===============================
    private val bookingHistory = mutableListOf(
        Booking(
            id = "B001",
            queueNumber = 1,
            patientName = "Rizky Amalia",
            doctorName = "Dr. Ahmad Santoso",
            specialization = "Dokter Umum",
            date = "17/10/2025",
            time = "08:30",
            complaint = "Demam tinggi dan batuk kering",
            diagnosis = "Infeksi saluran pernapasan ringan",
            prescription = "Paracetamol 500mg, Amoxicillin 250mg",
            status = BookingStatus.COMPLETED
        ),
        Booking(
            id = "B002",
            queueNumber = 2,
            patientName = "Dewi Lestari",
            doctorName = "Dr. Ahmad Santoso",
            specialization = "Dokter Umum",
            date = "17/10/2025",
            time = "09:15",
            complaint = "Sakit kepala dan pusing",
            diagnosis = "Migrain ringan",
            prescription = "Ibuprofen 200mg, istirahat cukup",
            status = BookingStatus.COMPLETED
        )
    )

    private var activeBooking: Booking? = null

    fun getBookingHistory(): List<Booking> = bookingHistory.toList()

    fun addToHistory(booking: Booking) {
        val idx = bookingHistory.indexOfFirst { it.id == booking.id }
        if (idx != -1) bookingHistory[idx] = booking
        else bookingHistory.add(0, booking)
    }

    fun setActiveBooking(booking: Booking) {
        activeBooking = booking
        println("📌 Booking aktif diset ke: ${booking.id}")
    }

    fun getActiveBooking(): Booking? = activeBooking

    fun clearActiveBooking() {
        activeBooking = null
        println("🗑️ Booking aktif dihapus.")
    }

    fun hasActiveBooking(): Boolean = activeBooking != null
}
