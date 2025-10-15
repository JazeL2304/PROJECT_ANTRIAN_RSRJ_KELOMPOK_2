package com.example.projectantrianrsrjkelompok2

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var layoutHistoryContainer: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvHistoryCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadHistoryData()
    }

    private fun initViews(view: View) {
        layoutHistoryContainer = view.findViewById(R.id.layoutHistoryContainer)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount)
    }

    private fun loadHistoryData() {
        val bookings = DataSource.getBookingHistory()

        if (bookings.isEmpty()) {
            showEmptyState()
        } else {
            showHistoryList(bookings)
        }
    }

    private fun showEmptyState() {
        layoutHistoryContainer.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        tvHistoryCount.text = "Belum ada riwayat"
    }

    private fun showHistoryList(bookings: List<Booking>) {
        layoutHistoryContainer.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        tvHistoryCount.text = "${bookings.size} riwayat ditemukan"

        layoutHistoryContainer.removeAllViews()

        bookings.forEach { booking ->
            val cardView = createBookingCard(booking)
            layoutHistoryContainer.addView(cardView)
        }
    }

    private fun createBookingCard(booking: Booking): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(4).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(16)
            setPadding(padding, padding, padding, padding)
        }

        // Header
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvQueueNumber = TextView(requireContext()).apply {
            text = "No. ${booking.queueNumber}"
            textSize = 18f
            setTextColor(Color.parseColor("#2196F3"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvStatus = TextView(requireContext()).apply {
            text = booking.status.toDisplayString()
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            val padding = dpToPx(8)
            setPadding(padding, padding/2, padding, padding/2)
            setBackgroundColor(resources.getColor(booking.status.getColorResource()))
        }

        header.addView(tvQueueNumber)
        header.addView(tvStatus)

        // Separator
        val separator = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                val margin = dpToPx(12)
                topMargin = margin
                bottomMargin = margin
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        // Info
        val tvDoctor = TextView(requireContext()).apply {
            text = "👨‍⚕️ ${booking.doctorName}"
            textSize = 14f
            setTextColor(Color.BLACK)
        }

        val tvSpec = TextView(requireContext()).apply {
            text = "🏥 ${booking.specialization}"
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, dpToPx(4), 0, 0)
        }

        val tvDateTime = TextView(requireContext()).apply {
            text = "📅 ${formatDate(booking.date)} | 🕘 ${booking.time}"
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, dpToPx(4), 0, 0)
        }

        container.addView(header)
        container.addView(separator)
        container.addView(tvDoctor)
        container.addView(tvSpec)
        container.addView(tvDateTime)

        cardView.addView(container)

        return cardView
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
