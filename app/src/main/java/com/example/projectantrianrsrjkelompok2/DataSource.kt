package com.example.projectantrianrsrjkelompok2

import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ FIXED: DataSource sekarang menggunakan Firebase TANPA blocking Main Thread
 */
object DataSource {

    private val firebaseRepo = FirebaseRepository()
    private var activeBooking: Booking? = null

    // Cache untuk performa
    private var cachedDoctors: List<Doctor>? = null
    private var cachedPatients: List<Patient>? = null
    private var cachedBookings: List<Booking>? = null
    private var cachedSpecializations: List<Specialization>? = null
    private var lastCacheTime = 0L
    private const val CACHE_DURATION = 30_000L // 30 seconds

    // ✅ Scope untuk background operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ===============================
    // 👨‍⚕️ DOKTER
    // ===============================

    fun getAllDoctors(): List<Doctor> {
        // Return cache immediately if valid
        if (!shouldRefreshCache() && cachedDoctors != null) {
            return cachedDoctors!!
        }

        // If no cache, return empty and fetch in background
        if (cachedDoctors == null) {
            scope.launch {
                cachedDoctors = firebaseRepo.getAllDoctors()
                lastCacheTime = System.currentTimeMillis()
            }
            return emptyList()
        }

        return cachedDoctors ?: emptyList()
    }

    fun getDoctorsBySpecialization(specId: Int): List<Doctor> {
        val specName = when (specId) {
            1 -> "Dokter Umum"
            2 -> "Dokter Gigi"
            3 -> "Dokter Mata"
            4 -> "Dokter Anak"
            5 -> "Dokter Jantung"
            6 -> "Dokter Kandungan"
            else -> return emptyList()
        }

        // Filter from cache
        return cachedDoctors?.filter {
            it.specialization.contains(specName, true)
        } ?: emptyList()
    }

    fun addDoctor(doctor: Doctor) {
        scope.launch {
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
        return cachedDoctors?.find { it.id == id }
    }

    // ===============================
    // 🧍‍♀️ PASIEN
    // ===============================

    fun getAllPatients(): List<Patient> {
        if (!shouldRefreshCache() && cachedPatients != null) {
            return cachedPatients!!
        }

        if (cachedPatients == null) {
            scope.launch {
                cachedPatients = firebaseRepo.getAllPatients()
            }
            return emptyList()
        }

        return cachedPatients ?: emptyList()
    }

    fun addPatient(patient: Patient): Boolean {
        scope.launch {
            val success = firebaseRepo.addPatient(patient)
            if (success) cachedPatients = null
        }
        return true // Optimistic return
    }

    fun removePatient(patient: Patient) {
        println("⚠️ Remove patient not yet implemented in Firebase")
    }

    fun findPatientsByName(nameQuery: String): List<Patient> {
        return cachedPatients?.filter {
            it.name.contains(nameQuery, true)
        } ?: emptyList()
    }

    fun clearAndSetPatients(newList: List<Patient>) {
        println("⚠️ Bulk update not yet implemented in Firebase")
    }

    // ===============================
    // 🏥 SPESIALISASI
    // ===============================

    fun getSpecializations(): List<Specialization> {
        // Return cache if valid
        if (cachedSpecializations != null) {
            return cachedSpecializations!!
        }

        // Fetch in background if no cache
        scope.launch {
            cachedSpecializations = firebaseRepo.getSpecializations()
        }

        // Return default while loading
        return listOf(
            Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
            Specialization(2, "Layanan Gigi", "Perawatan gigi dan mulut", "🦷"),
            Specialization(3, "Layanan Mata", "Kesehatan mata dan penglihatan", "👁️"),
            Specialization(4, "Layanan Anak", "Kesehatan bayi dan anak-anak", "👶"),
            Specialization(5, "Layanan Jantung", "Kesehatan jantung dan pembuluh darah", "❤️"),
            Specialization(6, "Layanan Kandungan", "Kesehatan ibu dan anak", "🤰")
        )
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
        if (!shouldRefreshCache() && cachedBookings != null) {
            return cachedBookings!!
        }

        if (cachedBookings == null) {
            scope.launch {
                cachedBookings = firebaseRepo.getBookingHistory()
                lastCacheTime = System.currentTimeMillis()
            }
            return emptyList()
        }

        return cachedBookings ?: emptyList()
    }

    fun addToHistory(booking: Booking) {
        scope.launch {
            firebaseRepo.createBooking(booking)
            cachedBookings = null
        }
    }

    fun setActiveBooking(booking: Booking) {
        activeBooking = booking
        addToHistory(booking)
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
        scope.launch {
            firebaseRepo.updateBookingStatus(bookingId, newStatus)
            cachedBookings = null
        }
    }

    fun updateBookingDiagnosis(
        bookingId: String,
        diagnosis: String,
        prescription: String
    ) {
        scope.launch {
            firebaseRepo.updateBookingDiagnosis(bookingId, diagnosis, prescription)
            cachedBookings = null
        }
    }

    fun hasCalledPatient(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return cachedBookings?.any {
            it.date == today && it.status == BookingStatus.CALLED
        } ?: false
    }

    // ===============================
    // 📊 STATISTICS & REPORTS
    // ===============================

    fun getTotalPatients(): Int = cachedPatients?.size ?: 0

    fun getTotalDoctors(): Int = cachedDoctors?.size ?: 0

    fun getTodayBookings(): List<Booking> {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        return cachedBookings?.filter { it.date == today } ?: emptyList()
    }

    fun getActiveQueues(): List<Booking> {
        return cachedBookings?.filter {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        } ?: emptyList()
    }

    fun getNextQueueNumber(): Int {
        val maxQueue = cachedBookings?.maxOfOrNull { it.queueNumber } ?: 0
        return maxQueue + 1
    }

    fun getDoctors(): List<Doctor> = getAllDoctors()

    // ===============================
    // 🔄 CACHE MANAGEMENT
    // ===============================

    private fun shouldRefreshCache(): Boolean {
        return (System.currentTimeMillis() - lastCacheTime > CACHE_DURATION)
    }

    fun invalidateCache() {
        cachedDoctors = null
        cachedPatients = null
        cachedBookings = null
        cachedSpecializations = null
        lastCacheTime = 0L
    }

    /**
     * ✅ Force load data from Firebase (for initial load)
     * Call this ONCE after seed completes
     */
    suspend fun forceLoadFromFirebase() = withContext(Dispatchers.IO) {
        cachedDoctors = firebaseRepo.getAllDoctors()
        cachedPatients = firebaseRepo.getAllPatients()
        cachedSpecializations = firebaseRepo.getSpecializations()
        cachedBookings = firebaseRepo.getBookingHistory()
        lastCacheTime = System.currentTimeMillis()
    }
}