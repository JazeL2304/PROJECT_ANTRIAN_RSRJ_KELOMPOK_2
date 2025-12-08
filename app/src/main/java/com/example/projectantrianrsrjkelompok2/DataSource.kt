package com.example.projectantrianrsrjkelompok2

import android.util.Log
import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ FIXED: DataSource sekarang load active booking dari Firebase per-user
 * Tidak lagi mengandalkan single activeBooking variable
 */
object DataSource {

    private const val TAG = "DataSource"

    private val firebaseRepo = FirebaseRepository()

    // ✅ CHANGED: activeBooking sekarang adalah Map per userId
    // Key = userId, Value = Booking
    private var activeBookingMap: MutableMap<String, Booking> = mutableMapOf()

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
        // Return cache if available
        if (cachedDoctors != null && cachedDoctors!!.isNotEmpty()) {
            return cachedDoctors!!
        }

        // If no cache, return empty (will be loaded async)
        return emptyList()
    }

    fun getDoctorsBySpecialization(specId: Int): List<Doctor> {
        // Mapping ID ke nama spesialisasi yang EXACT seperti di Firebase
        val specName = when (specId) {
            1 -> "Dokter Umum"
            2 -> "Dokter Gigi"
            3 -> "Dokter Mata"
            4 -> "Dokter Anak"
            5 -> "Dokter Jantung"
            6 -> "Dokter Kandungan"
            else -> return emptyList()
        }

        Log.d(TAG, "=== getDoctorsBySpecialization ===")
        Log.d(TAG, "Spec ID: $specId -> Spec Name: $specName")

        // Get all doctors (with cache)
        val allDoctors = getAllDoctors()
        Log.d(TAG, "Total doctors in cache: ${allDoctors.size}")

        // If cache empty, return empty list
        if (allDoctors.isEmpty()) {
            Log.w(TAG, "⚠️ Cache is empty!")
            return emptyList()
        }

        // Filter by specialization (case insensitive)
        val filteredDoctors = allDoctors.filter { doctor ->
            val matches = doctor.specialization.equals(specName, ignoreCase = true) ||
                    doctor.specialization.contains(specName, ignoreCase = true)

            if (matches) {
                Log.d(TAG, "✅ MATCH: ${doctor.name} - ${doctor.specialization}")
            }

            matches
        }

        Log.d(TAG, "Filtered doctors: ${filteredDoctors.size}")

        return filteredDoctors
    }

    fun addDoctor(doctor: Doctor) {
        scope.launch {
            firebaseRepo.addDoctor(doctor)
            cachedDoctors = null // Invalidate cache
        }
    }

    fun removeDoctor(doctor: Doctor) {
        scope.launch {
            firebaseRepo.deleteDoctor(doctor.id)
            cachedDoctors = null
        }
    }

    fun updateDoctorSchedule(id: Int, newSchedule: String) {
        scope.launch {
            val allDoctors = cachedDoctors ?: return@launch
            val doctor = allDoctors.find { it.id == id } ?: return@launch
            val updatedDoctor = doctor.copy(schedule = newSchedule)
            firebaseRepo.updateDoctor(updatedDoctor)
            cachedDoctors = null
        }
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
        scope.launch {
            firebaseRepo.deletePatient(patient.id)
            cachedPatients = null
        }
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
        if (cachedSpecializations != null && cachedSpecializations!!.isNotEmpty()) {
            return cachedSpecializations!!
        }

        // Return empty while loading
        return emptyList()
    }

    // ===============================
    // ⏰ JAM PRAKTIK - ✅ FIXED: Generate berdasarkan jadwal dokter
    // ===============================

    /**
     * ✅ Generate time slots berdasarkan jadwal dokter yang dipilih
     * Support untuk shift malam (20:00-08:00)
     */
    fun getTimeSlotsForDoctor(doctor: Doctor): List<String> {
        val schedule = doctor.schedule

        Log.d(TAG, "🕐 Generating time slots for ${doctor.name}: $schedule")

        // Extract jam dari schedule (format: "Hari 08:00–20:00")
        val timePattern = "(\\d{2}:\\d{2})".toRegex()
        val times = timePattern.findAll(schedule).map { it.value }.toList()

        if (times.size < 2) {
            Log.w(TAG, "⚠️ Cannot parse schedule, using default slots")
            return getDefaultTimeSlots()
        }

        val startTime = times[0] // e.g., "08:00" or "20:00"
        val endTime = times[1]   // e.g., "20:00" or "08:00"

        Log.d(TAG, "  Start: $startTime, End: $endTime")

        return generateTimeSlots(startTime, endTime)
    }

    /**
     * ✅ Generate time slots dari jam mulai sampai jam selesai
     * Support untuk shift malam (melewati tengah malam)
     */
    private fun generateTimeSlots(startTime: String, endTime: String): List<String> {
        val timeSlots = mutableListOf<String>()

        try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val calendar = Calendar.getInstance()

            val start = format.parse(startTime)
            val end = format.parse(endTime)

            if (start == null || end == null) {
                return getDefaultTimeSlots()
            }

            calendar.time = start
            val endCalendar = Calendar.getInstance()
            endCalendar.time = end

            // ✅ DETECT SHIFT MALAM (end time lebih kecil dari start time)
            val isNightShift = endCalendar.before(calendar) || endCalendar.time == calendar.time

            if (isNightShift) {
                Log.d(TAG, "🌙 Night shift detected: $startTime to $endTime (next day)")

                // Generate slots dari start time sampai 23:30
                while (calendar.get(Calendar.HOUR_OF_DAY) < 24) {
                    timeSlots.add(format.format(calendar.time))
                    calendar.add(Calendar.MINUTE, 30)

                    // Stop at 23:30
                    if (calendar.get(Calendar.HOUR_OF_DAY) == 23 && calendar.get(Calendar.MINUTE) > 30) {
                        break
                    }
                }

                // Reset ke 00:00 untuk hari berikutnya
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)

                // Generate slots dari 00:00 sampai end time
                while (calendar.time.before(endCalendar.time) || calendar.time == endCalendar.time) {
                    timeSlots.add(format.format(calendar.time))
                    calendar.add(Calendar.MINUTE, 30)
                }

            } else {
                // ✅ NORMAL SHIFT (day shift)
                Log.d(TAG, "☀️ Day shift: $startTime to $endTime")

                while (calendar.time.before(endCalendar.time) || calendar.time == endCalendar.time) {
                    timeSlots.add(format.format(calendar.time))
                    calendar.add(Calendar.MINUTE, 30)
                }
            }

            Log.d(TAG, "✅ Generated ${timeSlots.size} time slots")
            Log.d(TAG, "   First slot: ${timeSlots.firstOrNull()}")
            Log.d(TAG, "   Last slot: ${timeSlots.lastOrNull()}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating time slots: ${e.message}")
            return getDefaultTimeSlots()
        }

        return timeSlots
    }

    /**
     * ✅ Default time slots (jika parsing gagal)
     */
    private fun getDefaultTimeSlots(): List<String> {
        return listOf(
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
            "11:00", "13:00", "13:30", "14:00", "14:30", "15:00"
        )
    }

    /**
     * ✅ DEPRECATED: Jangan pakai ini lagi, gunakan getTimeSlotsForDoctor()
     */
    @Deprecated("Use getTimeSlotsForDoctor(doctor) instead")
    fun getTimeSlots(): List<String> {
        return getDefaultTimeSlots()
    }

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

    /**
     * ✅ FIXED: Set active booking untuk USER TERTENTU
     */
    fun setActiveBooking(booking: Booking) {
        val userId = booking.userId
        if (userId.isNotEmpty()) {
            activeBookingMap[userId] = booking
            Log.d(TAG, "✅ Set active booking for user $userId: ${booking.id}")
        }
        addToHistory(booking)
    }

    /**
     * ✅ FIXED: Get active booking untuk USER TERTENTU
     * Jika tidak ada di memory, cari dari cache/Firebase
     */
    fun getActiveBooking(): Booking? {
        // Legacy support - return first active if any
        return activeBookingMap.values.firstOrNull()
    }

    /**
     * ✅ NEW: Get active booking untuk USER TERTENTU berdasarkan userId
     */
    fun getActiveBookingForUser(userId: String): Booking? {
        // 1. Cek di memory map dulu
        val memoryBooking = activeBookingMap[userId]
        if (memoryBooking != null) {
            Log.d(TAG, "✅ Found active booking in memory for user $userId")
            return memoryBooking
        }

        // 2. Cek di cached bookings
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val activeFromCache = cachedBookings?.find { booking ->
            booking.userId == userId &&
                    booking.date == today &&
                    (booking.status == BookingStatus.WAITING || booking.status == BookingStatus.CALLED)
        }

        if (activeFromCache != null) {
            // Simpan ke memory untuk akses cepat berikutnya
            activeBookingMap[userId] = activeFromCache
            Log.d(TAG, "✅ Found active booking in cache for user $userId: ${activeFromCache.id}")
            return activeFromCache
        }

        Log.d(TAG, "⚠️ No active booking found for user $userId")
        return null
    }

    /**
     * ✅ NEW: Check apakah user tertentu punya active booking
     */
    fun hasActiveBookingForUser(userId: String): Boolean {
        return getActiveBookingForUser(userId) != null
    }

    /**
     * ✅ FIXED: Clear active booking untuk USER TERTENTU dan mark as COMPLETED
     */
    fun clearActiveBooking() {
        // Legacy - clear semua
        activeBookingMap.values.forEach { booking ->
            scope.launch {
                try {
                    firebaseRepo.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
                    Log.d(TAG, "✅ Booking ${booking.id} marked as COMPLETED")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error updating booking status: ${e.message}", e)
                }
            }
        }
        activeBookingMap.clear()
        Log.d(TAG, "✅ All active bookings cleared and marked as completed")
    }

    /**
     * ✅ NEW: Clear active booking untuk USER TERTENTU
     */
    fun clearActiveBookingForUser(userId: String) {
        val booking = activeBookingMap[userId]
        if (booking != null) {
            scope.launch {
                try {
                    firebaseRepo.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
                    Log.d(TAG, "✅ Booking ${booking.id} for user $userId marked as COMPLETED")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error updating booking status: ${e.message}", e)
                }
            }
            activeBookingMap.remove(userId)
        }
        Log.d(TAG, "✅ Active booking cleared for user $userId")
    }

    /**
     * ✅ OPTIMIZED: Complete active booking untuk USER TERTENTU
     */
    fun completeActiveBooking() {
        // Legacy - complete first active
        val booking = activeBookingMap.values.firstOrNull() ?: return
        completeActiveBookingForUser(booking.userId)
    }

    /**
     * ✅ NEW: Complete active booking untuk USER TERTENTU
     */
    fun completeActiveBookingForUser(userId: String) {
        val booking = activeBookingMap[userId] ?: return

        Log.d(TAG, "🔄 Completing booking for user $userId: ${booking.id}")

        // ✅ STEP 1: Update status menjadi COMPLETED
        val completedBooking = booking.copy(status = BookingStatus.COMPLETED)

        // ✅ STEP 2: Update cache lokal LANGSUNG
        cachedBookings = cachedBookings?.map { cachedBooking ->
            if (cachedBooking.id == booking.id) {
                Log.d(TAG, "  ✅ Updated booking ${booking.id} in cache to COMPLETED")
                completedBooking
            } else {
                cachedBooking
            }
        }

        // ✅ STEP 3: Clear dari memory map
        activeBookingMap.remove(userId)

        // ✅ STEP 4: Update di Firebase (async)
        scope.launch {
            try {
                firebaseRepo.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
                Log.d(TAG, "  ✅ Booking ${booking.id} updated to COMPLETED in Firebase")

                delay(500)
                forceLoadFromFirebase()

            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Error updating booking in Firebase: ${e.message}", e)
            }
        }

        Log.d(TAG, "✅ Booking ${booking.id} completed successfully for user $userId")
    }

    /**
     * ✅ Clear active booking WITHOUT updating status
     */
    fun clearActiveBookingOnly() {
        activeBookingMap.clear()
        Log.d(TAG, "✅ Active booking map cleared (status already updated)")
    }

    /**
     * ✅ NEW: Clear active booking untuk user tertentu tanpa update status
     */
    fun clearActiveBookingOnlyForUser(userId: String) {
        activeBookingMap.remove(userId)
        Log.d(TAG, "✅ Active booking cleared for user $userId (status already updated)")
    }

    fun hasActiveBooking(): Boolean = activeBookingMap.isNotEmpty()

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

    /**
     * ✅ FIXED: Get next queue number untuk DOKTER + TANGGAL tertentu
     */
    fun getNextQueueNumber(): Int {
        val maxQueue = cachedBookings?.maxOfOrNull { it.queueNumber } ?: 0
        return maxQueue + 1
    }

    /**
     * ✅ NEW: Get next queue number untuk DOKTER + TANGGAL tertentu
     */
    fun getNextQueueNumberForDoctor(doctorName: String, date: String): Int {
        val maxQueue = cachedBookings
            ?.filter { it.doctorName == doctorName && it.date == date }
            ?.maxOfOrNull { it.queueNumber } ?: 0
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
        try {
            Log.d(TAG, "📥 Force loading data from Firebase...")

            cachedDoctors = firebaseRepo.getAllDoctors()
            cachedPatients = firebaseRepo.getAllPatients()
            cachedSpecializations = firebaseRepo.getSpecializations()
            cachedBookings = firebaseRepo.getBookingHistory()
            lastCacheTime = System.currentTimeMillis()

            Log.d(TAG, "✅ DataSource cache loaded:")
            Log.d(TAG, "  - Doctors: ${cachedDoctors?.size ?: 0}")
            Log.d(TAG, "  - Patients: ${cachedPatients?.size ?: 0}")
            Log.d(TAG, "  - Specializations: ${cachedSpecializations?.size ?: 0}")
            Log.d(TAG, "  - Bookings: ${cachedBookings?.size ?: 0}")

            // ✅ NEW: Rebuild activeBookingMap dari cache
            rebuildActiveBookingMap()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading cache: ${e.message}", e)
        }
    }

    /**
     * ✅ NEW: Rebuild activeBookingMap dari cached bookings
     * Dipanggil setelah forceLoadFromFirebase
     */
    private fun rebuildActiveBookingMap() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        cachedBookings?.filter { booking ->
            booking.date == today &&
                    booking.userId.isNotEmpty() &&
                    (booking.status == BookingStatus.WAITING || booking.status == BookingStatus.CALLED)
        }?.forEach { booking ->
            activeBookingMap[booking.userId] = booking
            Log.d(TAG, "  ✅ Restored active booking for user ${booking.userId}: ${booking.id}")
        }

        Log.d(TAG, "✅ Active booking map rebuilt: ${activeBookingMap.size} active bookings")
    }
}