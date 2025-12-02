package com.example.projectantrianrsrjkelompok2.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.DataSource
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ Real-Time Queue Manager
 * Mengelola antrian berdasarkan waktu real HP
 */
object RealTimeQueueManager {

    private const val TAG = "RealTimeQueueManager"
    private val handler = Handler(Looper.getMainLooper())
    private var updateCallback: ((QueueUpdate) -> Unit)? = null
    private var isRunning = false

    /**
     * ✅ Start real-time monitoring
     */
    fun startMonitoring(callback: (QueueUpdate) -> Unit) {
        updateCallback = callback
        isRunning = true

        Log.d(TAG, "✅ Starting real-time queue monitoring")
        scheduleNextUpdate()
    }

    /**
     * ✅ Stop monitoring
     */
    fun stopMonitoring() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        updateCallback = null
        Log.d(TAG, "⏹️ Stopped real-time monitoring")
    }

    /**
     * Schedule next update (setiap 5 detik)
     */
    private fun scheduleNextUpdate() {
        if (!isRunning) return

        handler.postDelayed({
            updateQueueStatus()
            scheduleNextUpdate()
        }, 5000) // Update setiap 5 detik
    }

    /**
     * ✅ Update queue status berdasarkan waktu real
     */
    private fun updateQueueStatus() {
        try {
            val now = Calendar.getInstance()
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)

            Log.d(TAG, "🔄 Updating queue at $currentTime")

            // Get today's bookings
            val todayBookings = DataSource.getBookingHistory()
                .filter { it.date == today }
                .filter { it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED }
                .sortedBy { it.queueNumber }

            if (todayBookings.isEmpty()) {
                Log.d(TAG, "ℹ️ No active bookings for today")
                return
            }

            // Check which bookings should be called based on time
            todayBookings.forEach { booking ->
                checkAndUpdateBookingStatus(booking, currentTime)
            }

            // Get active booking
            val activeBooking = DataSource.getActiveBooking()
            if (activeBooking != null) {
                val update = createQueueUpdate(activeBooking, todayBookings)
                updateCallback?.invoke(update)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating queue: ${e.message}", e)
        }
    }

    /**
     * ✅ Check dan update booking status berdasarkan waktu
     */
    private fun checkAndUpdateBookingStatus(booking: Booking, currentTime: String) {
        try {
            val bookingTime = parseTime(booking.time)
            val current = parseTime(currentTime)

            if (current == null || bookingTime == null) return

            // Calculate time difference in minutes
            val diffMinutes = (current.timeInMillis - bookingTime.timeInMillis) / 60000

            when {
                // Jika sudah lewat 30 menit dari jadwal
                diffMinutes > 30 && booking.status == BookingStatus.WAITING -> {
                    Log.w(TAG, "⚠️ Booking ${booking.id} is late (${diffMinutes}m)")
                    DataSource.updateBookingStatus(booking.id, BookingStatus.MISSED)
                }

                // Jika sudah lewat 5 menit dari jadwal, tapi belum dipanggil
                diffMinutes > 5 && booking.status == BookingStatus.WAITING -> {
                    Log.i(TAG, "📢 Auto-calling booking ${booking.id}")
                    DataSource.updateBookingStatus(booking.id, BookingStatus.CALLED)
                }

                // Jika masih dalam waktu
                diffMinutes in -15..5 && booking.status == BookingStatus.WAITING -> {
                    Log.d(TAG, "⏱️ Booking ${booking.id} is on time")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking booking time: ${e.message}", e)
        }
    }

    /**
     * ✅ Parse time string to Calendar
     */
    private fun parseTime(timeString: String): Calendar? {
        return try {
            val parts = timeString.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null

            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeString", e)
            null
        }
    }

    /**
     * ✅ Create queue update object
     */
    private fun createQueueUpdate(
        activeBooking: Booking,
        allBookings: List<Booking>
    ): QueueUpdate {
        val myQueueNumber = activeBooking.queueNumber

        // Find current queue number (booking yang sedang dilayani)
        val currentQueue = allBookings
            .filter { it.status == BookingStatus.CALLED }
            .minByOrNull { it.queueNumber }
            ?.queueNumber ?: 0

        // Calculate patients ahead
        val patientsAhead = allBookings
            .count { it.queueNumber < myQueueNumber && it.status == BookingStatus.WAITING }

        // Get status
        val status = when {
            myQueueNumber < currentQueue -> QueueStatus.MISSED
            myQueueNumber == currentQueue -> QueueStatus.CALLED
            myQueueNumber == currentQueue + 1 -> QueueStatus.READY
            else -> QueueStatus.WAITING
        }

        return QueueUpdate(
            currentQueueNumber = currentQueue,
            myQueueNumber = myQueueNumber,
            patientsAhead = patientsAhead,
            status = status,
            booking = activeBooking,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * ✅ Get time until appointment
     */
    fun getTimeUntilAppointment(appointmentTime: String): TimeDifference? {
        try {
            val now = Calendar.getInstance()
            val appointment = parseTime(appointmentTime) ?: return null

            val diffMillis = appointment.timeInMillis - now.timeInMillis

            if (diffMillis < 0) {
                return TimeDifference(
                    hours = 0,
                    minutes = 0,
                    isPast = true
                )
            }

            val hours = (diffMillis / (1000 * 60 * 60)).toInt()
            val minutes = ((diffMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()

            return TimeDifference(
                hours = hours,
                minutes = minutes,
                isPast = false
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating time difference", e)
            return null
        }
    }

    /**
     * ✅ Check if booking time is near (15 menit sebelum)
     */
    fun isAppointmentNear(appointmentTime: String): Boolean {
        val diff = getTimeUntilAppointment(appointmentTime) ?: return false
        if (diff.isPast) return false

        val totalMinutes = diff.hours * 60 + diff.minutes
        return totalMinutes <= 15
    }

    /**
     * ✅ Check if booking is late
     */
    fun isAppointmentLate(appointmentTime: String): Boolean {
        val diff = getTimeUntilAppointment(appointmentTime)
        return diff?.isPast == true
    }

    /**
     * Data classes
     */
    data class QueueUpdate(
        val currentQueueNumber: Int,
        val myQueueNumber: Int,
        val patientsAhead: Int,
        val status: QueueStatus,
        val booking: Booking,
        val timestamp: Long
    )

    enum class QueueStatus {
        WAITING,    // Masih menunggu
        READY,      // Siap-siap (antrian berikutnya)
        CALLED,     // Dipanggil sekarang
        MISSED      // Terlewat
    }

    data class TimeDifference(
        val hours: Int,
        val minutes: Int,
        val isPast: Boolean
    ) {
        fun getFormattedTime(): String {
            return when {
                isPast -> "Sudah lewat"
                hours > 0 -> "$hours jam $minutes menit lagi"
                else -> "$minutes menit lagi"
            }
        }
    }
}