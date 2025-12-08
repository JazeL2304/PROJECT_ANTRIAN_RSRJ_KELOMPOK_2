package com.example.projectantrianrsrjkelompok2

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
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

    override fun onResume() {
        super.onResume()
        Log.d("HistoryFragment", "🔄 onResume - Refreshing history data")
        loadHistoryData()
    }

    private fun loadHistoryData() {
        try {
            // ✅ STEP 1: Get current logged in user ID
            val currentUserId = PreferencesHelper.getUserId(requireContext())

            if (currentUserId.isNullOrEmpty()) {
                Log.e("HistoryFragment", "❌ User not logged in!")
                showEmptyState()
                Toast.makeText(
                    requireContext(),
                    "Silakan login terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            Log.d("HistoryFragment", "📊 Loading history for userId: $currentUserId")

            // ✅ STEP 2: Get ALL bookings from database
            val allBookings = DataSource.getBookingHistory()

            Log.d("HistoryFragment", "  - Total bookings in database: ${allBookings.size}")

            // ✅ STEP 3: Filter by current user
            val userBookings = allBookings.filter { booking ->
                val belongsToUser = booking.userId == currentUserId

                // Debug log
                if (belongsToUser) {
                    Log.d("HistoryFragment", "  ✅ ${booking.id} belongs to $currentUserId")
                } else {
                    Log.d("HistoryFragment", "  ❌ ${booking.id} belongs to ${booking.userId}")
                }

                belongsToUser
            }

            Log.d("HistoryFragment", "  - Current user's bookings: ${userBookings.size}")

            // ✅ STEP 4: Filter by status (COMPLETED or CANCELLED)
            val historyBookings = userBookings.filter { booking ->
                booking.status == BookingStatus.COMPLETED ||
                        booking.status == BookingStatus.CANCELLED
            }

            Log.d("HistoryFragment", "  - History bookings (COMPLETED + CANCELLED): ${historyBookings.size}")

            // ✅ STEP 5: Remove duplicates
            val uniqueBookings = historyBookings
                .distinctBy {
                    "${it.patientName}|${it.queueNumber}|${it.date}|${it.doctorName}"
                }
                .sortedByDescending { it.createdAt }

            Log.d("HistoryFragment", "  - Unique history: ${uniqueBookings.size}")

            // Debug: Log duplicates if any
            if (historyBookings.size != uniqueBookings.size) {
                Log.w("HistoryFragment", "⚠️ Duplicates removed: ${historyBookings.size - uniqueBookings.size}")
            }

            // Log final result
            val completedCount = uniqueBookings.count { it.status == BookingStatus.COMPLETED }
            val cancelledCount = uniqueBookings.count { it.status == BookingStatus.CANCELLED }

            Log.d("HistoryFragment", "✅ History for user $currentUserId:")
            Log.d("HistoryFragment", "  - Completed: $completedCount")
            Log.d("HistoryFragment", "  - Cancelled: $cancelledCount")
            Log.d("HistoryFragment", "  - Total: ${uniqueBookings.size}")

            uniqueBookings.forEachIndexed { index, booking ->
                val statusIcon = when (booking.status) {
                    BookingStatus.COMPLETED -> "✅"
                    BookingStatus.CANCELLED -> "❌"
                    else -> "📋"
                }
                Log.d("HistoryFragment", "  $statusIcon ${index + 1}. ${booking.patientName} (Q${booking.queueNumber})")
            }

            if (uniqueBookings.isEmpty()) {
                Log.w("HistoryFragment", "⚠️ No history for user: $currentUserId")
                showEmptyState()
            } else {
                Log.d("HistoryFragment", "✅ Displaying ${uniqueBookings.size} items")
                showHistoryList(uniqueBookings)
            }

        } catch (e: Exception) {
            Log.e("HistoryFragment", "❌ Error loading history: ${e.message}", e)
            e.printStackTrace()
            showEmptyState()
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

        val completedCount = bookings.count { it.status == BookingStatus.COMPLETED }
        val cancelledCount = bookings.count { it.status == BookingStatus.CANCELLED }

        tvHistoryCount.text = when {
            completedCount > 0 && cancelledCount > 0 ->
                "${bookings.size} riwayat ($completedCount selesai, $cancelledCount dibatalkan)"
            completedCount > 0 ->
                "${bookings.size} riwayat selesai"
            cancelledCount > 0 ->
                "${bookings.size} riwayat dibatalkan"
            else ->
                "${bookings.size} riwayat"
        }

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

            val bgColor = when (booking.status) {
                BookingStatus.COMPLETED -> Color.parseColor("#E8F5E9")
                BookingStatus.CANCELLED -> Color.parseColor("#F5F5F5")
                else -> Color.WHITE
            }
            setCardBackgroundColor(bgColor)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(16)
            setPadding(padding, padding, padding, padding)
        }

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
            val (statusText, statusColor) = when (booking.status) {
                BookingStatus.COMPLETED -> "✅ Selesai" to "#4CAF50"
                BookingStatus.CANCELLED -> "❌ Dibatalkan" to "#9E9E9E"
                BookingStatus.MISSED -> "⚠️ Terlewat" to "#F44336"
                BookingStatus.CALLED -> "🔔 Dipanggil" to "#FF9800"
                BookingStatus.WAITING -> "⏳ Menunggu" to "#2196F3"
            }

            text = statusText
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            val padding = dpToPx(8)
            setPadding(padding, padding/2, padding, padding/2)

            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(statusColor))
                cornerRadius = dpToPx(4).toFloat()
            }
            background = shape
        }

        header.addView(tvQueueNumber)
        header.addView(tvStatus)

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

        if (booking.status == BookingStatus.CANCELLED) {
            val tvCancelledNote = TextView(requireContext()).apply {
                text = "💬 Antrian ini telah dibatalkan"
                textSize = 12f
                setTextColor(Color.parseColor("#9E9E9E"))
                setTypeface(null, android.graphics.Typeface.ITALIC)
                setPadding(0, dpToPx(8), 0, 0)
            }
            container.addView(tvCancelledNote)
        }

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