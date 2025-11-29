package com.example.projectantrianrsrjkelompok2

import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ UPDATED: DataSource sekarang menggunakan Firebase sebagai backend
 * Tetapi tetap compatible dengan kode lama (backward compatible)
 */
object DataSource {

    private val firebaseRepo = FirebaseRepository()
    private var activeBooking: Booking? = null

    // Cache untuk performa (optional)
    private var cachedDoctors: List<Doctor>? = null
    private var cachedPatients: List<Patient>? = null
    private var cachedBookings: List<Booking>? = null
    private var lastCacheTime = 0L
    private const val CACHE_DURATION = 30_000L // 30 seconds

    // ===============================
    // 👨‍⚕️ DOKTER
    // ===============================

    fun getAllDoctors(): List<Doctor> {
        if (shouldRefreshCache()) {
            cachedDoctors = runBlocking { firebaseRepo.getAllDoctors() }
        }
        return cachedDoctors ?: emptyList()
    }

    fun getDoctorsBySpecialization(specId: Int): List<Doctor> {
        val specName = when (specId) {
            1 -> "Dokter Umum"
            2 -> "Dokter Gigi"
            3 -> "Dokter Mata"
            4 -> "Dokter Anak"
            else -> return emptyList()
        }

        return runBlocking {
            firebaseRepo.getDoctorsBySpecialization(specName)
        }
    }

    fun addDoctor(doctor: Doctor) {
        runBlocking {
            firebaseRepo.addDoctor(doctor)
            cachedDoctors = null // Invalidate cache
        }
    }

    fun removeDoctor(doctor: Doctor) {
        // TODO: Implement in FirebaseRepository
        println("⚠️ Remove doctor not yet implemented in Firebase")
    }

    fun updateDoctorSchedule(id: Int, newSchedule: String) {
        // TODO: Implement in FirebaseRepository
        println("⚠️ Update schedule not yet implemented in Firebase")
    }

    fun getDoctorById(id: Int): Doctor? {
        return getAllDoctors().find { it.id == id }
    }

    // ===============================
    // 🧍‍♀️ PASIEN
    // ===============================

    fun getAllPatients(): List<Patient> {
        if (shouldRefreshCache()) {
            cachedPatients = runBlocking { firebaseRepo.getAllPatients() }
        }
        return cachedPatients ?: emptyList()
    }

    fun addPatient(patient: Patient): Boolean {
        return runBlocking {
            val success = firebaseRepo.addPatient(patient)
            if (success) cachedPatients = null // Invalidate cache
            success
        }
    }

    fun removePatient(patient: Patient) {
        // TODO: Implement in FirebaseRepository
        println("⚠️ Remove patient not yet implemented in Firebase")
    }

    fun findPatientsByName(nameQuery: String): List<Patient> {
        return getAllPatients().filter { it.name.contains(nameQuery, true) }
    }

    fun clearAndSetPatients(newList: List<Patient>) {
        // TODO: Batch update in Firebase
        println("⚠️ Bulk update not yet implemented in Firebase")
    }

    // ===============================
    // 🏥 SPESIALISASI
    // ===============================

    fun getSpecializations(): List<Specialization> {
        return runBlocking {
            firebaseRepo.getSpecializations()
        }
    }

    // ===============================
    // ⏰ JAM PRAKTIK
    // ===============================

    fun getTimeSlots(): List<String> = listOf(
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
        "11:00", "13:00", "13:30", "14:00", "14:30", "15:00"
    )

    // ===============================
    // 📋 BOOKING / ANTRIAN
    // ===============================

    fun getBookingHistory(): List<Booking> {
        if (shouldRefreshCache()) {
            cachedBookings = runBlocking { firebaseRepo.getBookingHistory() }
            lastCacheTime = System.currentTimeMillis()
        }
        return cachedBookings ?: emptyList()
    }

    fun addToHistory(booking: Booking) {
        runBlocking {
            firebaseRepo.createBooking(booking)
            cachedBookings = null // Invalidate cache
        }
    }

    fun setActiveBooking(booking: Booking) {
        activeBooking = booking
        addToHistory(booking) // Also save to Firebase
    }

    fun getActiveBooking(): Booking? = activeBooking

    fun clearActiveBooking() {
        activeBooking = null
    }

    fun clearActiveBookingOnly() {
        activeBooking = null
    }

    fun hasActiveBooking(): Boolean = activeBooking != null

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        runBlocking {
            firebaseRepo.updateBookingStatus(bookingId, newStatus)
            cachedBookings = null // Invalidate cache
        }
    }

    fun updateBookingDiagnosis(
        bookingId: String,
        diagnosis: String,
        prescription: String
    ) {
        runBlocking {
            firebaseRepo.updateBookingDiagnosis(bookingId, diagnosis, prescription)
            cachedBookings = null // Invalidate cache
        }
    }

    fun hasCalledPatient(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getBookingHistory().any {
            it.date == today && it.status == BookingStatus.CALLED
        }
    }

    // ===============================
    // 📊 STATISTICS & REPORTS
    // ===============================

    fun getTotalPatients(): Int = getAllPatients().size

    fun getTotalDoctors(): Int = getAllDoctors().size

    fun getTodayBookings(): List<Booking> {
        return runBlocking {
            firebaseRepo.getTodayBookings()
        }
    }

    fun getActiveQueues(): List<Booking> {
        return runBlocking {
            firebaseRepo.getActiveQueues()
        }
    }

    fun getNextQueueNumber(): Int {
        return runBlocking {
            firebaseRepo.getNextQueueNumber()
        }
    }

    fun getDoctors(): List<Doctor> = getAllDoctors()

    // ===============================
    // 🔄 CACHE MANAGEMENT
    // ===============================

    private fun shouldRefreshCache(): Boolean {
        return cachedDoctors == null ||
                cachedPatients == null ||
                cachedBookings == null ||
                (System.currentTimeMillis() - lastCacheTime > CACHE_DURATION)
    }

    fun invalidateCache() {
        cachedDoctors = null
        cachedPatients = null
        cachedBookings = null
        lastCacheTime = 0L
    }
}