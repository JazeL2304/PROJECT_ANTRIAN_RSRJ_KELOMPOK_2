package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingHistoryFragment : Fragment() {

    private val TAG = "BookingHistoryFragment"
    private lateinit var repository: FirebaseRepository
    private lateinit var historyContainer: LinearLayout
    private lateinit var loadingProgress: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var filterSpinner: Spinner
    private lateinit var customDateButton: Button
    private lateinit var totalHistoryText: TextView

    private var allBookings: List<Map<String, Any?>> = emptyList()

    private val firebaseDateFormat1 = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val firebaseDateFormat2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking_history, container, false)

        repository = FirebaseRepository()

        historyContainer = view.findViewById(R.id.containerHistory)
        loadingProgress = view.findViewById(R.id.progressBar)
        emptyStateText = view.findViewById(R.id.tvEmptyHistory)
        filterSpinner = view.findViewById(R.id.spinnerFilter)
        customDateButton = view.findViewById(R.id.btnDateRange)
        totalHistoryText = view.findViewById(R.id.tvTotalHistory)

        setupFilterSpinner()
        setupCustomDateButton()

        loadBookingHistory()

        return view
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("Semua Waktu", "Hari Ini", "Minggu Ini", "Bulan Ini", "Custom")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                customDateButton.visibility = if (position == 4) View.VISIBLE else View.GONE
                if (position != 4) {
                    applyFilter(filterOptions[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCustomDateButton() {
        customDateButton.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day)
                applyCustomDateFilter(selectedDate.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null

        return try {
            firebaseDateFormat1.parse(dateStr)
        } catch (e1: Exception) {
            try {
                firebaseDateFormat2.parse(dateStr)
            } catch (e2: Exception) {
                Log.e(TAG, "Cannot parse date: $dateStr")
                null
            }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameWeek(date: Date, referenceDate: Date): Boolean {
        val cal1 = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            time = referenceDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(date: Date, referenceDate: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { time = referenceDate }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun loadBookingHistory() {
        Log.d(TAG, "Loading booking history...")
        loadingProgress.visibility = View.VISIBLE
        historyContainer.visibility = View.GONE
        emptyStateText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                allBookings = repository.getAllBookingsMap()
                Log.d(TAG, "Total bookings loaded: ${allBookings.size}")

                loadingProgress.visibility = View.GONE

                if (allBookings.isEmpty()) {
                    Log.w(TAG, "No bookings found in Firebase!")
                    showEmptyState()
                } else {
                    updateTotalCounter(allBookings.size)
                    applyFilter("Semua Waktu")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading booking history: ${e.message}", e)
                loadingProgress.visibility = View.GONE
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    "Error loading data: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun applyFilter(filterType: String) {
        historyContainer.removeAllViews()
        val today = Date()
        val filteredBookings = when (filterType) {
            "Semua Waktu" -> allBookings
            "Hari Ini" -> allBookings.filter { booking ->
                val dateStr = booking["date"] as? String
                val bookingDate = parseDate(dateStr)
                if (bookingDate != null) isSameDay(bookingDate, today) else false
            }
            "Minggu Ini" -> allBookings.filter { booking ->
                val dateStr = booking["date"] as? String
                val bookingDate = parseDate(dateStr)
                if (bookingDate != null) isSameWeek(bookingDate, today) else false
            }
            "Bulan Ini" -> allBookings.filter { booking ->
                val dateStr = booking["date"] as? String
                val bookingDate = parseDate(dateStr)
                if (bookingDate != null) isSameMonth(bookingDate, today) else false
            }
            else -> allBookings
        }

        updateTotalCounter(filteredBookings.size)

        if (filteredBookings.isEmpty()) {
            showEmptyState()
        } else {
            historyContainer.visibility = View.VISIBLE
            emptyStateText.visibility = View.VISIBLE
            totalHistoryText.visibility = View.VISIBLE

            val sortedBookings = filteredBookings.sortedByDescending { booking ->
                (booking["createdAt"] as? Long) ?: 0L
            }

            sortedBookings.forEach { booking ->
                val cardView = createBookingCard(booking)
                historyContainer.addView(cardView)
            }
        }
    }

    private fun applyCustomDateFilter(selectedDate: Date) {
        historyContainer.removeAllViews()

        val filteredBookings = allBookings.filter { booking ->
            val bookingDateStr = booking["date"] as? String
            val bookingDate = parseDate(bookingDateStr)

            if (bookingDate != null) {
                isSameDay(bookingDate, selectedDate)
            } else {
                false
            }
        }

        updateTotalCounter(filteredBookings.size)

        if (filteredBookings.isEmpty()) {
            showEmptyState()
        } else {
            historyContainer.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
            totalHistoryText.visibility = View.VISIBLE

            filteredBookings.forEach { booking ->
                val cardView = createBookingCard(booking)
                historyContainer.addView(cardView)
            }
        }
    }

    private fun createBookingCard(booking: Map<String, Any?>): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.WHITE)
            setPadding(24, 24, 24, 24)
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(24, 24, 24, 24)
        }

        // Queue Number & Status Row
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Queue Number
        val queueNumber = when (val qn = booking["queueNumber"]) {
            is Int -> qn.toString()
            is Long -> qn.toString()
            is String -> qn
            else -> "N/A"
        }

        val queueText = TextView(requireContext()).apply {
            text = "Queue #$queueNumber"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1976D2"))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Status Badge
        val status = booking["status"] as? String ?: "UNKNOWN"
        val statusBadge = TextView(requireContext()).apply {
            text = when (status) {
                "WAITING" -> "Menunggu"
                "CALLED" -> "Dipanggil"
                "COMPLETED" -> "Selesai"
                "CANCELLED" -> "Dibatalkan"
                else -> status
            }
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16, 8, 16, 8)

            val bgColor = when (status) {
                "WAITING" -> Color.parseColor("#FFA726")
                "CALLED" -> Color.parseColor("#42A5F5")
                "COMPLETED" -> Color.parseColor("#66BB6A")
                "CANCELLED" -> Color.parseColor("#EF5350")
                else -> Color.GRAY
            }
            setBackgroundColor(bgColor)
            setTextColor(Color.WHITE)
        }

        headerRow.addView(queueText)
        headerRow.addView(statusBadge)
        contentLayout.addView(headerRow)

        // Patient Name
        val patientName = booking["patientName"] as? String ?: "N/A"
        val patientText = TextView(requireContext()).apply {
            text = "Pasien: $patientName"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        contentLayout.addView(patientText)

        // Doctor Name
        val doctorName = booking["doctorName"] as? String ?: "N/A"
        val doctorText = TextView(requireContext()).apply {
            text = "Dokter: $doctorName"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 4, 0, 4)
        }
        contentLayout.addView(doctorText)

        // Date & Time (BERSIH DARI EMOJI)
        val dateStr = booking["date"] as? String ?: "N/A"
        val displayDate = parseDate(dateStr)?.let {
            displayDateFormat.format(it)
        } ?: dateStr

        val time = booking["time"] as? String ?: "N/A"
        val dateTimeText = TextView(requireContext()).apply {
            text = "$displayDate | $time" // EMOJI HAPUS
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 4, 0, 0)
        }
        contentLayout.addView(dateTimeText)

        cardView.addView(contentLayout)

        cardView.setOnClickListener {
            showBookingDetail(booking)
        }

        return cardView
    }

    private fun showBookingDetail(booking: Map<String, Any?>) {
        val queueNumber = when (val qn = booking["queueNumber"]) {
            is Int -> qn.toString()
            is Long -> qn.toString()
            is String -> qn
            else -> "N/A"
        }

        val status = booking["status"] as? String ?: "UNKNOWN"
        val statusText = when (status) {
            "WAITING" -> "Menunggu"
            "CALLED" -> "Dipanggil"
            "COMPLETED" -> "Selesai"
            "CANCELLED" -> "Dibatalkan"
            else -> status
        }

        val dateStr = booking["date"] as? String ?: "N/A"
        val displayDate = parseDate(dateStr)?.let {
            displayDateFormat.format(it)
        } ?: dateStr

        // DETAIL BERSIH TANPA EMOJI
        val details = """
            Queue Number: #$queueNumber
            Status: $statusText
            
            Pasien: ${booking["patientName"] ?: "N/A"}
            Dokter: ${booking["doctorName"] ?: "N/A"}
            
            Tanggal: $displayDate
            Waktu: ${booking["time"] ?: "N/A"}
            
            Spesialisasi: ${booking["specialization"] ?: "N/A"}
            
            ${if (status == "COMPLETED") {
            """
                Diagnosis: ${booking["diagnosis"] ?: "Belum ada"}
                Resep: ${booking["prescription"] ?: "Belum ada"}
                """.trimIndent()
        } else ""}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Booking")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEmptyState() {
        historyContainer.visibility = View.GONE
        emptyStateText.visibility = View.VISIBLE
        totalHistoryText.visibility = View.GONE
        Log.d(TAG, "Showing empty state") // Log message bersih
    }

    private fun updateTotalCounter(count: Int) {
        totalHistoryText.text = "Total: $count booking"
        totalHistoryText.visibility = if (count > 0) View.VISIBLE else View.GONE
    }
}