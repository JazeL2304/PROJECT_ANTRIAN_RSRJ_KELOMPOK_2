package com.example.projectantrianrsrjkelompok2

import java.text.SimpleDateFormat
import java.util.*

data class Doctor(
    val id: Int = 0,
    val name: String = "",
    val specialization: String = "",
    val schedule: String = "" // Format: "Senin–Jumat 08:00–20:00"
) {
    /**
     * Check apakah dokter bekerja pada hari tertentu
     */
    fun isWorkingOn(date: String): Boolean {
        val dayName = getDayNameFromDate(date)
        val workingDays = getWorkingDays()
        return workingDays.any { it.equals(dayName, ignoreCase = true) }
    }

    /**
     * Parse hari kerja dari schedule string
     * Format: "Senin–Jumat", "Senin–Kamis", "Senin–Sabtu", "Senin–Minggu"
     */
    fun getWorkingDays(): List<String> {
        return when {
            schedule.contains("Senin–Minggu", ignoreCase = true) ||
                    schedule.contains("Senin-Minggu", ignoreCase = true) ->
                listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

            schedule.contains("Senin–Sabtu", ignoreCase = true) ||
                    schedule.contains("Senin-Sabtu", ignoreCase = true) ->
                listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

            schedule.contains("Senin–Jumat", ignoreCase = true) ||
                    schedule.contains("Senin-Jumat", ignoreCase = true) ->
                listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")

            schedule.contains("Senin–Kamis", ignoreCase = true) ||
                    schedule.contains("Senin-Kamis", ignoreCase = true) ->
                listOf("Senin", "Selasa", "Rabu", "Kamis")

            else -> emptyList()
        }
    }

    /**
     * Get jam kerja dalam format readable
     */
    fun getWorkingHours(): String {
        // Extract time dari schedule: "08:00–20:00" atau "20:00–08:00"
        val timePattern = Regex("(\\d{2}:\\d{2})\\s*[–-]\\s*(\\d{2}:\\d{2})")
        val match = timePattern.find(schedule)

        return if (match != null) {
            val start = match.groupValues[1]
            val end = match.groupValues[2]
            "$start - $end"
        } else {
            "Jam tidak tersedia"
        }
    }

    /**
     * Get hari kerja dalam format readable
     */
    fun getWorkingDaysString(): String {
        return when {
            schedule.contains("Senin–Minggu") || schedule.contains("Senin-Minggu") ->
                "Setiap Hari (Senin - Minggu)"
            schedule.contains("Senin–Sabtu") || schedule.contains("Senin-Sabtu") ->
                "Senin - Sabtu"
            schedule.contains("Senin–Jumat") || schedule.contains("Senin-Jumat") ->
                "Senin - Jumat"
            schedule.contains("Senin–Kamis") || schedule.contains("Senin-Kamis") ->
                "Senin - Kamis"
            else -> "Tidak tersedia"
        }
    }

    /**
     * Check apakah dokter shift malam (cross-midnight)
     * Contoh: "20:00–08:00" adalah shift malam
     */
    fun isNightShift(): Boolean {
        val times = extractWorkingTimes()
        if (times.first.isEmpty() || times.second.isEmpty()) return false

        val start = parseTimeToMinutes(times.first)
        val end = parseTimeToMinutes(times.second)

        return start > end // Jika start > end, berarti cross midnight
    }

    /**
     * Get available time slots untuk hari tertentu
     */
    fun getAvailableTimeSlots(date: String): List<String> {
        if (!isWorkingOn(date)) return emptyList()

        val (startTime, endTime) = extractWorkingTimes()
        if (startTime.isEmpty() || endTime.isEmpty()) return emptyList()

        val slots = mutableListOf<String>()
        val start = parseTimeToMinutes(startTime)
        var end = parseTimeToMinutes(endTime)

        // Handle night shift (cross-midnight)
        if (start > end) {
            // Shift malam: 20:00-08:00
            // Generate dari start sampai 23:30
            var current = start
            while (current <= 23 * 60 + 30) {
                slots.add(formatMinutesToTime(current))
                current += 30
            }

            // Lanjut dari 00:00 sampai end
            current = 0
            while (current <= end) {
                slots.add(formatMinutesToTime(current))
                current += 30
            }
        } else {
            // Shift normal: 08:00-20:00
            var current = start
            while (current <= end) {
                slots.add(formatMinutesToTime(current))
                current += 30
            }
        }

        return slots
    }

    /**
     * Validasi apakah waktu tertentu valid untuk tanggal tertentu
     */
    fun isTimeValid(time: String, date: String): Boolean {
        if (!isWorkingOn(date)) return false
        val availableSlots = getAvailableTimeSlots(date)
        return availableSlots.contains(time)
    }

    // Helper functions
    private fun getDayNameFromDate(date: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
            val calendar = Calendar.getInstance()
            calendar.time = sdf.parse(date) ?: Date()

            val dayFormat = SimpleDateFormat("EEEE", Locale("id", "ID"))
            dayFormat.format(calendar.time)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Extract start time dan end time dari schedule
     * Return: Pair(startTime, endTime) contoh: Pair("08:00", "20:00")
     */
    private fun extractWorkingTimes(): Pair<String, String> {
        val timePattern = Regex("(\\d{2}:\\d{2})\\s*[–-]\\s*(\\d{2}:\\d{2})")
        val match = timePattern.find(schedule)

        return if (match != null) {
            Pair(match.groupValues[1], match.groupValues[2])
        } else {
            Pair("", "")
        }
    }

    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun formatMinutesToTime(minutes: Int): String {
        return String.format("%02d:%02d", minutes / 60, minutes % 60)
    }
}