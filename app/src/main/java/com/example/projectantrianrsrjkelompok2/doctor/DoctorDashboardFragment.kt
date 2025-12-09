package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.R  // ← TAMBAHKAN INI!
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DoctorDashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var ivProfileIcon: ImageView

    private lateinit var tvTotalPatientsToday: TextView
    private lateinit var tvActiveQueue: TextView
    private lateinit var tvCompletedToday: TextView
    private lateinit var tvRecentPatients: TextView

    private lateinit var pref: PreferencesHelper
    private var doctorName = ""

    private val database = FirebaseDatabase.getInstance()
    private val bookingsRef = database.getReference("bookings")
    private var bookingsListener: ValueEventListener? = null

    companion object {
        private const val TAG = "DoctorDashboard"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_doctor_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = PreferencesHelper(requireContext())

        // ✅ Get nama dokter dari login
        doctorName = pref.getUserFullName() ?: pref.getDoctorName() ?: "Dokter"

        Log.d(TAG, "👨‍⚕️ Doctor logged in: $doctorName")

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvGreeting.text = "Selamat Datang, $doctorName! 👋"

        ivProfileIcon = view.findViewById(R.id.ivProfileIcon)

        tvTotalPatientsToday = view.findViewById(R.id.tv_total_patients_today)
        tvActiveQueue = view.findViewById(R.id.tv_active_queue)
        tvCompletedToday = view.findViewById(R.id.tv_completed_today)
        tvRecentPatients = view.findViewById(R.id.tv_recent_patients)

        ivProfileIcon.setOnClickListener {
            (activity as? MainActivity)
                ?.navigateToFragment(ProfileFragment())
        }

        view.findViewById<Button>(R.id.btnViewQueue).setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_doctor_queue
        }

        view.findViewById<Button>(R.id.btnPatientHistory).setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_patient_history
        }

        startRealtime()
    }

    /**
     * ✅ Helper: Normalisasi nama dokter
     * Menghilangkan "Dr.", "dr.", trim, lowercase
     */
    private fun normalizeName(name: String): String {
        return name
            .replace("Dr.", "", ignoreCase = true)
            .replace("dr.", "", ignoreCase = true)
            .trim()
            .lowercase()
    }

    /**
     * ✅ REALTIME LISTENER - MANUAL FILTER
     */
    private fun startRealtime() {

        Log.d(TAG, "📡 Starting realtime listener for doctor: $doctorName")

        if (doctorName.isEmpty() || doctorName == "Dokter") {
            Log.e(TAG, "❌ Doctor name is empty or default!")
            tvRecentPatients.text = "❌ Error: Nama dokter tidak ditemukan.\nSilakan login ulang."
            return
        }

        // ✅ Normalize nama dokter yang login
        val normalizedLoggedDoctor = normalizeName(doctorName)
        Log.d(TAG, "🔍 Normalized logged doctor: '$normalizedLoggedDoctor'")

        // ✅ Listen to ALL bookings
        bookingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                Log.d(TAG, "📥 Received ${snapshot.childrenCount} total bookings")

                val allBookings = mutableListOf<Booking>()

                for (child in snapshot.children) {
                    try {
                        val map = child.value as? Map<String, Any> ?: continue

                        val bookingDoctorName = map["doctorName"] as? String ?: ""

                        // ✅ Normalize nama dokter di booking
                        val normalizedBookingDoctor = normalizeName(bookingDoctorName)

                        Log.d(TAG, "🔍 Comparing: '$normalizedBookingDoctor' vs '$normalizedLoggedDoctor'")

                        // ✅ Filter: hanya booking untuk dokter ini
                        if (normalizedBookingDoctor == normalizedLoggedDoctor) {

                            val booking = Booking(
                                firebaseId = child.key ?: "",
                                id = map["id"] as? String ?: "",
                                patientName = map["patientName"] as? String ?: "",
                                doctorName = bookingDoctorName,
                                specialization = map["specialization"] as? String ?: "",
                                date = map["date"] as? String ?: "",
                                time = map["time"] as? String ?: "",
                                queueNumber = (map["queueNumber"] as? Long)?.toInt() ?: 0,
                                complaint = map["complaint"] as? String ?: "",
                                status = try {
                                    BookingStatus.valueOf(
                                        (map["status"] as? String ?: "WAITING").uppercase()
                                    )
                                } catch (e: Exception) {
                                    BookingStatus.WAITING
                                },
                                diagnosis = map["diagnosis"] as? String ?: "",
                                prescription = map["prescription"] as? String ?: "",
                                createdAt = (map["createdAt"] as? Long) ?: 0L
                            )

                            allBookings.add(booking)
                            Log.d(TAG, "✅ Match: ${booking.patientName} (${booking.date})")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Error parsing booking: ${e.message}")
                    }
                }

                Log.d(TAG, "📊 Total bookings for this doctor: ${allBookings.size}")

                // ✅ Remove duplicates
                val unique = allBookings.distinctBy {
                    "${it.patientName}|${it.queueNumber}|${it.time}"
                }

                updateDashboardUI(unique)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Database error: ${error.message}")
                tvRecentPatients.text = "❌ Error loading data: ${error.message}"
            }
        }

        bookingsRef.addValueEventListener(bookingsListener!!)
    }

    private fun updateDashboardUI(list: List<Booking>) {

        Log.d(TAG, "📊 Updating dashboard with ${list.size} bookings")

        // ✅ Filter pasien yang menunggu/dipanggil
        val waiting = list.filter {
            it.status == BookingStatus.WAITING ||
                    it.status == BookingStatus.CALLED
        }.sortedBy { it.queueNumber }

        // ✅ Filter pasien yang sudah selesai
        val completed = list.filter {
            it.status == BookingStatus.COMPLETED
        }

        Log.d(TAG, "  - Total: ${list.size}")
        Log.d(TAG, "  - Waiting/Called: ${waiting.size}")
        Log.d(TAG, "  - Completed: ${completed.size}")

        // ✅ UPDATE STATISTIK
        tvTotalPatientsToday.text = list.size.toString()
        tvActiveQueue.text = waiting.size.toString()
        tvCompletedToday.text = completed.size.toString()

        // ✅ TAMPILKAN SEMUA PASIEN
        val sb = StringBuilder()

        if (list.isEmpty()) {
            sb.append("✅ Tidak ada pasien hari ini\n\n")
            sb.append("Klik tombol 'Lihat Antrian Pasien' untuk melihat riwayat lengkap")
        } else {
            // Section 1: Pasien yang Menunggu
            if (waiting.isNotEmpty()) {
                sb.append("⏱️ Pasien yang Menunggu:\n\n")

                waiting.take(3).forEachIndexed { index, b ->
                    val noDisplay = index + 1

                    val statusIcon = when(b.status) {
                        BookingStatus.CALLED -> "📢"
                        else -> "⏳"
                    }

                    sb.append("$statusIcon No. $noDisplay - ${b.patientName}\n")
                    sb.append("   Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("   Waktu: ${b.time}\n")
                    sb.append("   Status: ${b.status.toDisplayString()}\n\n")
                }

                if (waiting.size > 3) {
                    sb.append("... dan ${waiting.size - 3} pasien lainnya menunggu\n\n")
                }
            }

            // Section 2: Pasien yang Selesai
            if (completed.isNotEmpty()) {
                sb.append("✅ Pasien Selesai Hari Ini:\n\n")

                completed.take(3).forEachIndexed { index, b ->
                    val noDisplay = index + 1

                    sb.append("✅ No. $noDisplay - ${b.patientName}\n")
                    sb.append("   Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("   Waktu: ${b.time}\n")
                    sb.append("   Status: Selesai ✓\n\n")
                }

                if (completed.size > 3) {
                    sb.append("... dan ${completed.size - 3} pasien lainnya selesai\n\n")
                }
            }

            if (waiting.isEmpty() && completed.isNotEmpty()) {
                sb.append("\n🎉 Semua pasien sudah selesai!")
            }
        }

        tvRecentPatients.text = sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Remove listener
        bookingsListener?.let {
            bookingsRef.removeEventListener(it)
        }
    }
}