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
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private lateinit var layoutHistoryContainer: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvHistoryCount: TextView

    private val historyList = mutableListOf<Booking>()
    private var userId = ""

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

        val pref = PreferencesHelper(requireContext())
        userId = pref.getUserId() ?: ""

        if (userId.isEmpty()) {
            Log.e("HistoryFragment", "User not logged in!")
            showEmptyState()
            Toast.makeText(requireContext(), "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        loadHistoryFromFirebase()
    }

    private fun initViews(view: View) {
        layoutHistoryContainer = view.findViewById(R.id.layoutHistoryContainer)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount)
    }

    override fun onResume() {
        super.onResume()
        Log.d("HistoryFragment", "onResume - Refreshing history data")
        if (userId.isNotEmpty()) {
            loadHistoryFromFirebase()
        }
    }

    private fun loadHistoryFromFirebase() {
        Log.d("HistoryFragment", "Loading history from Firebase for userId: $userId")

        BookingRepository.listenHistoryByUserId(userId) { bookings ->
            Log.d("HistoryFragment", "Received ${bookings.size} history items from Firebase")

            historyList.clear()
            historyList.addAll(bookings)

            if (historyList.isEmpty()) {
                showEmptyState()
            } else {
                showHistoryList(historyList)
            }
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

        bookings.forEachIndexed { index, booking ->
            val cardView = createHistoryCard(booking, index + 1)
            layoutHistoryContainer.addView(cardView)
        }
    }

    private fun createHistoryCard(booking: Booking, number: Int): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(4).toFloat()
            setCardBackgroundColor(Color.WHITE)

            isClickable = true
            isFocusable = true

            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            foreground = context.getDrawable(outValue.resourceId)

            setOnClickListener {
                showDetailDialog(booking)
            }
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(16)
            setPadding(padding, padding, padding, padding)
        }

        // ==================== HEADER ====================
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvNumber = TextView(requireContext()).apply {
            text = "No. $number" // Hapus #, ganti jadi "No." biar rapi
            textSize = 18f
            setTextColor(Color.parseColor("#1976D2"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvStatus = TextView(requireContext()).apply {
            text = "Selesai" // Hapus Icon Centang
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            val padding = dpToPx(8)
            setPadding(padding, padding/2, padding, padding/2)
            background = createRoundedBackground("#4CAF50")
        }

        header.addView(tvNumber)
        header.addView(tvStatus)

        // ==================== DOCTOR INFO ====================
        val doctorInfoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = dpToPx(12)
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(12)
            }
        }

        val tvDoctorLabel = TextView(requireContext()).apply {
            text = "Dokter" // Hapus Emoji Dokter
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
        }

        val tvDoctor = TextView(requireContext()).apply {
            text = booking.doctorName
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(4), 0, 0)
        }

        val tvSpec = TextView(requireContext()).apply {
            text = booking.specialization
            textSize = 14f
            setTextColor(Color.parseColor("#616161"))
        }

        val tvDateTime = TextView(requireContext()).apply {
            text = "${formatDate(booking.date)} • ${booking.time}" // Hapus Emoji Kalender
            textSize = 13f
            setTextColor(Color.parseColor("#757575"))
            setPadding(0, dpToPx(4), 0, 0)
        }

        doctorInfoLayout.addView(tvDoctorLabel)
        doctorInfoLayout.addView(tvDoctor)
        doctorInfoLayout.addView(tvSpec)
        doctorInfoLayout.addView(tvDateTime)

        // ==================== COMPLAINT ====================
        val tvComplaintLabel = TextView(requireContext()).apply {
            text = "Keluhan" // Hapus Emoji Chat
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(12), 0, dpToPx(4))
        }

        val tvComplaint = TextView(requireContext()).apply {
            text = booking.complaint.ifEmpty { "-" }
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
        }

        // ==================== DIVIDER ====================
        val divider = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        }

        // ==================== DIAGNOSIS ====================
        val tvDiagnosisLabel = TextView(requireContext()).apply {
            text = "Diagnosis" // Hapus Emoji Stetoskop
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dpToPx(4))
        }

        val tvDiagnosis = TextView(requireContext()).apply {
            text = if (booking.diagnosis.isNotEmpty()) {
                booking.diagnosis
            } else {
                "(Belum diisi dokter)"
            }
            textSize = 15f
            setTextColor(if (booking.diagnosis.isNotEmpty()) {
                Color.parseColor("#D32F2F")
            } else {
                Color.parseColor("#9E9E9E")
            })
            setTypeface(null, if (booking.diagnosis.isNotEmpty()) {
                android.graphics.Typeface.BOLD
            } else {
                android.graphics.Typeface.ITALIC
            })
        }

        // ==================== PRESCRIPTION ====================
        val tvPrescriptionLabel = TextView(requireContext()).apply {
            text = "Resep Obat" // Hapus Emoji Obat
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(12), 0, dpToPx(4))
        }

        val tvPrescription = TextView(requireContext()).apply {
            text = if (booking.prescription.isNotEmpty()) {
                booking.prescription
            } else {
                "(Belum diisi dokter)"
            }
            textSize = 14f
            setTextColor(if (booking.prescription.isNotEmpty()) {
                Color.parseColor("#424242")
            } else {
                Color.parseColor("#9E9E9E")
            })
            setTypeface(null, if (booking.prescription.isEmpty()) {
                android.graphics.Typeface.ITALIC
            } else {
                android.graphics.Typeface.NORMAL
            })
            setPadding(dpToPx(8), 0, 0, 0)
        }

        // ==================== TAP HINT ====================
        val tvTapHint = TextView(requireContext()).apply {
            text = "Ketuk untuk detail lengkap" // Hapus Emoji Jari
            textSize = 11f
            setTextColor(Color.parseColor("#9E9E9E"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, dpToPx(12), 0, 0)
        }

        // ==================== ADD ALL VIEWS ====================
        container.addView(header)
        container.addView(doctorInfoLayout)
        container.addView(tvComplaintLabel)
        container.addView(tvComplaint)
        container.addView(divider)
        container.addView(tvDiagnosisLabel)
        container.addView(tvDiagnosis)
        container.addView(tvPrescriptionLabel)
        container.addView(tvPrescription)
        container.addView(tvTapHint)

        cardView.addView(container)

        return cardView
    }

    private fun showDetailDialog(booking: Booking) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val tvTitle = TextView(requireContext()).apply {
            text = "Detail Riwayat Pemeriksaan" // Hapus Emoji Clipboard
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#212121"))
            setPadding(0, 0, 0, 24)
        }
        container.addView(tvTitle)

        val tvContent = TextView(requireContext()).apply {
            // Bersihkan emoji di Detail Dialog juga
            text = """
Dokter:
   ${booking.doctorName}
   ${booking.specialization}

Tanggal & Waktu:
   ${formatDate(booking.date)} • ${booking.time}

Keluhan Saya:
   ${booking.complaint.ifEmpty { "-" }}

Diagnosis Dokter:
   ${booking.diagnosis.ifEmpty { "(Belum diisi)" }}

Resep Obat:
${if (booking.prescription.isNotEmpty()) {
                booking.prescription.split("\n").joinToString("\n") { "   $it" }
            } else {
                "   (Belum diisi)"
            }}

Status: Pemeriksaan Selesai
            """.trimIndent()
            textSize = 14f
            setTextColor(Color.parseColor("#424242"))
            setLineSpacing(8f, 1f)
        }
        container.addView(tvContent)

        android.app.AlertDialog.Builder(requireContext())
            .setView(container)
            .setPositiveButton("Tutup", null)
            .show()
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

    private fun createRoundedBackground(colorHex: String): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = dpToPx(4).toFloat()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}