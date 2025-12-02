package com.example.projectantrianrsrjkelompok2.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ Time Helper - Mengelola format waktu dan timezone WIB
 * Memastikan semua waktu konsisten menggunakan WIB (GMT+7)
 */
object TimeHelper {

    // Timezone WIB (GMT+7)
    private val WIB_TIMEZONE = TimeZone.getTimeZone("Asia/Jakarta")

    /**
     * Get current time in WIB
     */
    fun getCurrentTimeWIB(): Calendar {
        return Calendar.getInstance(WIB_TIMEZONE)
    }

    /**
     * Format date to Indonesian format
     * Example: "Jumat, 06 Desember 2024"
     */
    fun formatDateIndonesian(date: Date): String {
        val format = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        format.timeZone = WIB_TIMEZONE
        return format.format(date)
    }

    /**
     * Format date to standard format
     * Example: "2024-12-06"
     */
    fun formatDateStandard(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.timeZone = WIB_TIMEZONE
        return format.format(date)
    }

    /**
     * Format time to HH:mm WIB
     * Example: "14:30 WIB"
     */
    fun formatTimeWIB(date: Date): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.timeZone = WIB_TIMEZONE
        return "${format.format(date)} WIB"
    }

    /**
     * Format time without WIB suffix
     * Example: "14:30"
     */
    fun formatTime(date: Date): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.timeZone = WIB_TIMEZONE
        return format.format(date)
    }

    /**
     * Get current time string (HH:mm)
     */
    fun getCurrentTimeString(): String {
        return formatTime(Date())
    }

    /**
     * Get current date string (yyyy-MM-dd)
     */
    fun getCurrentDateString(): String {
        return formatDateStandard(Date())
    }

    /**
     * Parse time string to Calendar
     * Example: "14:30" → Calendar object
     */
    fun parseTime(timeString: String): Calendar? {
        return try {
            val parts = timeString.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null

            Calendar.getInstance(WIB_TIMEZONE).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse date string to Calendar
     * Example: "2024-12-06" → Calendar object
     */
    fun parseDate(dateString: String): Calendar? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.timeZone = WIB_TIMEZONE
            val date = format.parse(dateString) ?: return null

            Calendar.getInstance(WIB_TIMEZONE).apply {
                time = date
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate booking time based on queue number
     * Clinic opens at 08:00, 8 minutes per patient
     *
     * Example:
     * - Queue 1  → 08:00 WIB
     * - Queue 2  → 08:08 WIB
     * - Queue 3  → 08:16 WIB
     * - Queue 10 → 09:12 WIB
     */
    fun generateBookingTime(queueNumber: Int): String {
        val clinicOpenHour = 8
        val clinicOpenMinute = 0
        val minutesPerPatient = 8

        val totalMinutes = (queueNumber - 1) * minutesPerPatient
        val bookingHour = clinicOpenHour + (totalMinutes / 60)
        val bookingMinute = clinicOpenMinute + (totalMinutes % 60)

        return String.format("%02d:%02d", bookingHour, bookingMinute)
    }

    /**
     * Calculate estimated wait time
     * @param patientsAhead Number of patients ahead in queue
     * @param avgServiceTime Average service time per patient (default 8 minutes)
     * @return Wait time in minutes
     */
    fun calculateWaitTime(
        patientsAhead: Int,
        avgServiceTime: Int = 8
    ): Int {
        return patientsAhead * avgServiceTime
    }

    /**
     * Format wait time to human readable
     * Example:
     * - 5 min   → "5 menit"
     * - 65 min  → "1 jam 5 menit"
     * - 125 min → "2 jam 5 menit"
     */
    fun formatWaitTime(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes menit"
            minutes % 60 == 0 -> "${minutes / 60} jam"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                "$hours jam $mins menit"
            }
        }
    }

    /**
     * Calculate appointment time
     * Current time + wait time = appointment time
     */
    fun calculateAppointmentTime(waitMinutes: Int): String {
        val calendar = getCurrentTimeWIB()
        calendar.add(Calendar.MINUTE, waitMinutes)
        return formatTime(calendar.time)
    }

    /**
     * Check if time is in clinic hours
     * Clinic hours: 08:00 - 17:00
     */
    fun isClinicHours(timeString: String): Boolean {
        val time = parseTime(timeString) ?: return false
        val hour = time.get(Calendar.HOUR_OF_DAY)
        return hour in 8..16 // 08:00 - 17:00
    }

    /**
     * Get time difference in minutes
     */
    fun getTimeDifferenceMinutes(time1: String, time2: String): Int {
        val t1 = parseTime(time1) ?: return 0
        val t2 = parseTime(time2) ?: return 0

        val diffMillis = t2.timeInMillis - t1.timeInMillis
        return (diffMillis / (1000 * 60)).toInt()
    }

    /**
     * Check if appointment time is near (within 15 minutes)
     */
    fun isAppointmentNear(appointmentTime: String): Boolean {
        val now = getCurrentTimeString()
        val diff = getTimeDifferenceMinutes(now, appointmentTime)
        return diff in 0..15
    }

    /**
     * Check if appointment is late
     */
    fun isAppointmentLate(appointmentTime: String): Boolean {
        val now = getCurrentTimeString()
        val diff = getTimeDifferenceMinutes(appointmentTime, now)
        return diff > 0
    }

    /**
     * Get day name in Indonesian
     */
    fun getDayNameIndonesian(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Unknown"
        }
    }

    /**
     * Format full datetime for display
     * Example: "Jumat, 06 Desember 2024 | 14:30 WIB"
     */
    fun formatFullDateTime(date: Date): String {
        val dateStr = formatDateIndonesian(date)
        val timeStr = formatTimeWIB(date)
        return "$dateStr | $timeStr"
    }

    /**
     * Format datetime for booking display
     * Example: "Jumat, 06 Des 2024 | 14:30 WIB"
     */
    fun formatBookingDateTime(dateString: String, timeString: String): String {
        val calendar = parseDate(dateString) ?: return "$dateString | $timeString WIB"

        val format = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        format.timeZone = WIB_TIMEZONE
        val dateStr = format.format(calendar.time)

        return "$dateStr | $timeString WIB"
    }
}