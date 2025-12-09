package com.example.projectantrianrsrjkelompok2.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.R
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ ViewReportFragment FINAL
 * - Sesuai dengan layout asli (tvDoctorCount, tvPatientCount, tvBookingCount)
 * - Fitur panggil pasien berikutnya
 * - Daftar booking hari ini
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
     * ✅ Load booking HARI INI saja
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
     * ✅ Tampilkan daftar booking
     */
    private fun displayBookings(bookings: List<BookingInfo>) {
        layoutBookingList.removeAllViews()

        if (bookings.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "Belum ada booking hari ini"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(0, 20, 0, 20)
            }
            layoutBookingList.addView(emptyView)
            return
        }

        bookings.forEach { booking ->
            // ✅ Buat TextView langsung (tanpa inflate layout)
            val bookingText = TextView(requireContext()).apply {
                text = buildString {
                    append("🎫 #${booking.queueNumber} - ${booking.patientName}\n")
                    append("👨‍⚕️ ${booking.doctorName}\n")
                    append("📅 ${booking.date} ⏰ ${booking.time}\n")
                    append("Status: ${getStatusText(booking.status)}")
                }
                textSize = 13f
                setPadding(0, 12, 0, 12)
                setTextColor(resources.getColor(android.R.color.black, null))
            }

            layoutBookingList.addView(bookingText)

            // Divider
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }
            layoutBookingList.addView(divider)
        }
    }

    /**
     * ✅ Panggil pasien berikutnya (HARI INI saja)
     */
    private fun callNextPatient() {
        lifecycleScope.launch {
            try {
                val snapshot = bookingsRef.get().await()

                // ✅ Get today's date
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                var nextPatient: BookingInfo? = null

                for (child in snapshot.children) {
                    val map = child.value as? Map<String, Any> ?: continue
                    val status = map["status"]?.toString() ?: ""
                    val bookingDate = map["date"] as? String ?: ""

                    // ✅ Filter: hanya booking hari ini dengan status WAITING
                    if (status == "WAITING" && bookingDate == todayDate) {
                        nextPatient = BookingInfo(
                            id = child.key ?: "",
                            patientName = map["patientName"] as? String ?: "",
                            doctorName = map["doctorName"] as? String ?: "",
                            date = bookingDate,
                            time = map["time"] as? String ?: "",
                            queueNumber = (map["queueNumber"] as? Long)?.toInt() ?: 0,
                            status = status,
                            createdAt = (map["createdAt"] as? Long) ?: 0L
                        )
                        break
                    }
                }

                if (nextPatient != null) {
                    // Update status ke CALLED
                    bookingsRef.child(nextPatient.id).child("status").setValue("CALLED").await()

                    // Update UI
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

                    // Refresh list
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
     * Data class untuk booking
     */
    data class BookingInfo(
        val id: String,
        val patientName: String,
        val doctorName: String,
        val date: String,
        val time: String,
        val queueNumber: Int,
        val status: String,
        val createdAt: Long
    )

    /**
     * Format status
     */
    private fun getStatusText(status: String): String {
        return when (status.uppercase()) {
            "WAITING" -> "⏳ Menunggu"
            "CALLED" -> "🔔 Dipanggil"
            "COMPLETED" -> "✅ Selesai"
            "CANCELLED" -> "❌ Dibatalkan"
            else -> status
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 onResume, reload data")
        loadStatistics()
        loadTodayBookings()
    }
}