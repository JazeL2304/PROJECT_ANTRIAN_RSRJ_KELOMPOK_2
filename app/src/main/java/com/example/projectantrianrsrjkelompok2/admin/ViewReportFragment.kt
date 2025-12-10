package com.example.projectantrianrsrjkelompok2.admin

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.R
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ ViewReportFragment UPDATED
 * - Tampilkan diagnosis dokter
 * - Tampilkan resep obat
 * - Click card untuk detail lengkap
 */
class ViewReportFragment : Fragment() {

    private lateinit var tvDoctorCount: TextView
    private lateinit var tvPatientCount: TextView
    private lateinit var tvBookingCount: TextView
    private lateinit var tvCurrentCalledPatient: TextView
    private lateinit var btnCallNextPatient: Button
    private lateinit var layoutBookingList: LinearLayout

    private val database = FirebaseDatabase.getInstance()
    private val bookingsRef = database.getReference("bookings")
    private val doctorsRef = database.getReference("doctors")

    private val TAG = "ViewReportFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init views
        tvDoctorCount = view.findViewById(R.id.tvDoctorCount)
        tvPatientCount = view.findViewById(R.id.tvPatientCount)
        tvBookingCount = view.findViewById(R.id.tvBookingCount)
        tvCurrentCalledPatient = view.findViewById(R.id.tvCurrentCalledPatientAdmin)
        btnCallNextPatient = view.findViewById(R.id.btnCallNextPatientAdmin)
        layoutBookingList = view.findViewById(R.id.layoutBookingList)

        // Button listener
        btnCallNextPatient.setOnClickListener {
            callNextPatient()
        }

        // Load data
        loadStatistics()
        loadTodayBookings()
    }

    /**
     * ✅ Load statistik
     */
    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔍 Loading statistics...")

                val doctorCount = countDoctors()
                val patientCount = countUniquePatients()
                val bookingCount = countBookings()

                tvDoctorCount.text = doctorCount.toString()
                tvPatientCount.text = patientCount.toString()
                tvBookingCount.text = bookingCount.toString()

                Log.d(TAG, "✅ Statistics: Doctors=$doctorCount, Patients=$patientCount, Bookings=$bookingCount")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading statistics: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ Hitung total dokter dari doctors table
     */
    private suspend fun countDoctors(): Int {
        return try {
            val snapshot = doctorsRef.get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error counting doctors: ${e.message}")
            0
        }
    }

    /**
     * ✅ Hitung pasien UNIK dari bookings
     */
    private suspend fun countUniquePatients(): Int {
        return try {
            val snapshot = bookingsRef.get().await()

            val uniquePatients = mutableSetOf<String>()

            for (child in snapshot.children) {
                val patientName = child.child("patientName").value as? String
                if (!patientName.isNullOrEmpty()) {
                    uniquePatients.add(patientName.trim().lowercase())
                }
            }

            Log.d(TAG, "📊 Unique patients: ${uniquePatients.size}")
            uniquePatients.size

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error counting patients: ${e.message}")
            0
        }
    }

    /**
     * ✅ Hitung total bookings
     */
    private suspend fun countBookings(): Int {
        return try {
            val snapshot = bookingsRef.get().await()
            snapshot.childrenCount.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error counting bookings: ${e.message}")
            0
        }
    }

    /**
     * ✅ Load booking HARI INI saja dengan diagnosis & resep
     */
    private fun loadTodayBookings() {
        lifecycleScope.launch {
            try {
                val snapshot = bookingsRef.get().await()

                // ✅ Get today's date in format YYYY-MM-DD
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                Log.d(TAG, "📅 Today's date: $todayDate")

                val bookings = mutableListOf<BookingInfo>()

                for (child in snapshot.children) {
                    try {
                        val map = child.value as? Map<String, Any> ?: continue

                        val bookingDate = map["date"] as? String ?: ""

                        // ✅ Filter: hanya booking dengan date = hari ini
                        if (bookingDate == todayDate) {
                            val booking = BookingInfo(
                                id = child.key ?: "",
                                patientName = map["patientName"] as? String ?: "",
                                doctorName = map["doctorName"] as? String ?: "",
                                date = bookingDate,
                                time = map["time"] as? String ?: "",
                                queueNumber = (map["queueNumber"] as? Long)?.toInt() ?: 0,
                                status = map["status"]?.toString() ?: "WAITING",
                                complaint = map["complaint"] as? String ?: "",
                                diagnosis = map["diagnosis"] as? String ?: "",
                                prescription = map["prescription"] as? String ?: "",
                                createdAt = (map["createdAt"] as? Long) ?: 0L
                            )

                            bookings.add(booking)
                            Log.d(TAG, "✅ Today's booking: ${booking.patientName} - Queue #${booking.queueNumber}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Error parsing booking: ${e.message}")
                    }
                }

                // Sort by queueNumber
                bookings.sortBy { it.queueNumber }

                Log.d(TAG, "📊 Total bookings today: ${bookings.size}")
                displayBookings(bookings)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading bookings: ${e.message}")
            }
        }
    }

    /**
     * ✅ Tampilkan daftar booking dengan diagnosis & resep
     */
    private fun displayBookings(bookings: List<BookingInfo>) {
        layoutBookingList.removeAllViews()

        if (bookings.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "Belum ada booking hari ini"
                textSize = 14f
                setTextColor(Color.parseColor("#757575"))
                setPadding(0, dpToPx(20), 0, dpToPx(20))
                gravity = android.view.Gravity.CENTER
            }
            layoutBookingList.addView(emptyView)
            return
        }

        bookings.forEach { booking ->
            val card = createBookingCard(booking)
            layoutBookingList.addView(card)
        }
    }

    /**
     * ✅ Buat card booking dengan diagnosis & resep
     */
    private fun createBookingCard(booking: BookingInfo): CardView {
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

        val tvQueue = TextView(requireContext()).apply {
            text = "🎫 #${booking.queueNumber}"
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
            text = getStatusEmoji(booking.status)
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            val padding = dpToPx(8)
            setPadding(padding, padding/2, padding, padding/2)
            background = createStatusBackground(booking.status)
        }

        header.addView(tvQueue)
        header.addView(tvStatus)

        // ==================== PATIENT INFO ====================
        val patientLayout = LinearLayout(requireContext()).apply {
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

        val tvPatientLabel = TextView(requireContext()).apply {
            text = "👤 Pasien"
            textSize = 12f
            setTextColor(Color.parseColor("#757575"))
        }

        val tvPatient = TextView(requireContext()).apply {
            text = booking.patientName
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dpToPx(4), 0, 0)
        }

        val tvDoctor = TextView(requireContext()).apply {
            text = "👨‍⚕️ ${booking.doctorName}"
            textSize = 14f
            setTextColor(Color.parseColor("#616161"))
        }

        val tvDateTime = TextView(requireContext()).apply {
            text = "📅 ${booking.date} ⏰ ${booking.time}"
            textSize = 13f
            setTextColor(Color.parseColor("#757575"))
            setPadding(0, dpToPx(4), 0, 0)
        }

        patientLayout.addView(tvPatientLabel)
        patientLayout.addView(tvPatient)
        patientLayout.addView(tvDoctor)
        patientLayout.addView(tvDateTime)

        // ==================== COMPLAINT ====================
        if (booking.complaint.isNotEmpty()) {
            val tvComplaintLabel = TextView(requireContext()).apply {
                text = "💬 Keluhan"
                textSize = 12f
                setTextColor(Color.parseColor("#757575"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dpToPx(12), 0, dpToPx(4))
            }

            val tvComplaint = TextView(requireContext()).apply {
                text = booking.complaint
                textSize = 14f
                setTextColor(Color.parseColor("#424242"))
            }

            container.addView(header)
            container.addView(patientLayout)
            container.addView(tvComplaintLabel)
            container.addView(tvComplaint)
        } else {
            container.addView(header)
            container.addView(patientLayout)
        }

        // ==================== DIAGNOSIS ====================
        if (booking.status == "COMPLETED") {
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
            container.addView(divider)

            val tvDiagnosisLabel = TextView(requireContext()).apply {
                text = "🩺 Diagnosis"
                textSize = 12f
                setTextColor(Color.parseColor("#757575"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, dpToPx(4))
            }

            val tvDiagnosis = TextView(requireContext()).apply {
                text = if (booking.diagnosis.isNotEmpty()) {
                    booking.diagnosis
                } else {
                    "(Belum diisi)"
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

            val tvPrescriptionLabel = TextView(requireContext()).apply {
                text = "💊 Resep Obat"
                textSize = 12f
                setTextColor(Color.parseColor("#757575"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, dpToPx(12), 0, dpToPx(4))
            }

            val tvPrescription = TextView(requireContext()).apply {
                text = if (booking.prescription.isNotEmpty()) {
                    booking.prescription
                } else {
                    "(Belum diisi)"
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

            container.addView(tvDiagnosisLabel)
            container.addView(tvDiagnosis)
            container.addView(tvPrescriptionLabel)
            container.addView(tvPrescription)
        }

        // ==================== TAP HINT ====================
        val tvTapHint = TextView(requireContext()).apply {
            text = "👆 Ketuk untuk detail lengkap"
            textSize = 11f
            setTextColor(Color.parseColor("#9E9E9E"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, dpToPx(12), 0, 0)
        }
        container.addView(tvTapHint)

        cardView.addView(container)
        return cardView
    }

    /**
     * ✅ Show detail dialog
     */
    private fun showDetailDialog(booking: BookingInfo) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        val tvTitle = TextView(requireContext()).apply {
            text = "📋 Detail Booking"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#212121"))
            setPadding(0, 0, 0, 24)
        }
        container.addView(tvTitle)

        val tvContent = TextView(requireContext()).apply {
            text = buildString {
                append("🎫 Nomor Antrian: #${booking.queueNumber}\n\n")
                append("👤 Pasien: ${booking.patientName}\n")
                append("👨‍⚕️ Dokter: ${booking.doctorName}\n")
                append("📅 Tanggal: ${booking.date}\n")
                append("⏰ Waktu: ${booking.time}\n\n")

                if (booking.complaint.isNotEmpty()) {
                    append("💬 Keluhan:\n   ${booking.complaint}\n\n")
                }

                if (booking.status == "COMPLETED") {
                    append("🩺 Diagnosis:\n   ${booking.diagnosis.ifEmpty { "(Belum diisi)" }}\n\n")
                    append("💊 Resep Obat:\n")
                    if (booking.prescription.isNotEmpty()) {
                        booking.prescription.split("\n").forEach {
                            append("   $it\n")
                        }
                    } else {
                        append("   (Belum diisi)\n")
                    }
                    append("\n")
                }

                append("✅ Status: ${getStatusText(booking.status)}")
            }
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

    /**
     * ✅ Panggil pasien berikutnya (HARI INI saja)
     */
    private fun callNextPatient() {
        lifecycleScope.launch {
            try {
                val snapshot = bookingsRef.get().await()

                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                var nextPatient: BookingInfo? = null

                for (child in snapshot.children) {
                    val map = child.value as? Map<String, Any> ?: continue
                    val status = map["status"]?.toString() ?: ""
                    val bookingDate = map["date"] as? String ?: ""

                    if (status == "WAITING" && bookingDate == todayDate) {
                        nextPatient = BookingInfo(
                            id = child.key ?: "",
                            patientName = map["patientName"] as? String ?: "",
                            doctorName = map["doctorName"] as? String ?: "",
                            date = bookingDate,
                            time = map["time"] as? String ?: "",
                            queueNumber = (map["queueNumber"] as? Long)?.toInt() ?: 0,
                            status = status,
                            complaint = map["complaint"] as? String ?: "",
                            diagnosis = map["diagnosis"] as? String ?: "",
                            prescription = map["prescription"] as? String ?: "",
                            createdAt = (map["createdAt"] as? Long) ?: 0L
                        )
                        break
                    }
                }

                if (nextPatient != null) {
                    bookingsRef.child(nextPatient.id).child("status").setValue("CALLED").await()

                    tvCurrentCalledPatient.text = buildString {
                        append("🔔 PASIEN DIPANGGIL:\n\n")
                        append("Antrian #${nextPatient.queueNumber}\n")
                        append("Nama: ${nextPatient.patientName}\n")
                        append("Dokter: ${nextPatient.doctorName}\n")
                        append("Waktu: ${nextPatient.date} ${nextPatient.time}")
                    }

                    Toast.makeText(
                        requireContext(),
                        "✅ Pasien ${nextPatient.patientName} dipanggil!",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadTodayBookings()

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Tidak ada pasien yang menunggu hari ini",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error calling patient: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Data class untuk booking dengan diagnosis & resep
     */
    data class BookingInfo(
        val id: String,
        val patientName: String,
        val doctorName: String,
        val date: String,
        val time: String,
        val queueNumber: Int,
        val status: String,
        val complaint: String,
        val diagnosis: String,
        val prescription: String,
        val createdAt: Long
    )

    private fun getStatusText(status: String): String {
        return when (status.uppercase()) {
            "WAITING" -> "⏳ Menunggu"
            "CALLED" -> "🔔 Dipanggil"
            "COMPLETED" -> "✅ Selesai"
            "CANCELLED" -> "❌ Dibatalkan"
            else -> status
        }
    }

    private fun getStatusEmoji(status: String): String {
        return when (status.uppercase()) {
            "WAITING" -> "⏳ Menunggu"
            "CALLED" -> "🔔 Dipanggil"
            "COMPLETED" -> "✅ Selesai"
            "CANCELLED" -> "❌ Dibatalkan"
            else -> status
        }
    }

    private fun createStatusBackground(status: String): android.graphics.drawable.GradientDrawable {
        val color = when (status.uppercase()) {
            "WAITING" -> "#FF9800"
            "CALLED" -> "#2196F3"
            "COMPLETED" -> "#4CAF50"
            "CANCELLED" -> "#F44336"
            else -> "#9E9E9E"
        }

        return android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = dpToPx(8).toFloat()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 onResume, reload data")
        loadStatistics()
        loadTodayBookings()
    }
}