package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
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

        // ✅ FIX: Gunakan getUserFullName() bukan getDoctorName()
        // Karena LoginFragment menyimpan ke KEY_USER_FULL_NAME
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

        // ✅ PERBAIKAN: Langsung trigger bottom nav untuk Antrian
        view.findViewById<Button>(R.id.btnViewQueue).setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_doctor_queue
        }

        // ✅ PERBAIKAN: Langsung trigger bottom nav untuk Riwayat
        view.findViewById<Button>(R.id.btnPatientHistory).setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_patient_history
        }

        startRealtime()
    }

    // ==========================================================
    // ✅ REALTIME SAFE + AUTO NUMBER + FIX COMPLETED COUNT + ICON CENTANG
    // ==========================================================
    private fun startRealtime() {

        BookingRepository.clearListeners()

        Log.d(TAG, "📡 Starting realtime listener for doctor: $doctorName")

        // ✅ FIXED: Pastikan doctorName tidak kosong
        if (doctorName.isEmpty() || doctorName == "Dokter") {
            Log.e(TAG, "❌ Doctor name is empty or default!")
            tvRecentPatients.text = "❌ Error: Nama dokter tidak ditemukan.\nSilakan login ulang."
            return
        }

        // ✅ Ambil SEMUA booking dokter ini (tidak hanya hari ini)
        // PENTING: BookingRepository.listenQueueByDoctor harus diubah dulu
        // agar mengambil SEMUA status (WAITING, CALLED, COMPLETED)
        BookingRepository.listenQueueByDoctor(
            doctorName,
            null  // null = ambil semua tanggal
        ) { bookings ->

            Log.d(TAG, "📥 Received ${bookings.size} bookings for $doctorName")

            val unique = bookings.distinctBy {
                "${it.patientName}|${it.queueNumber}|${it.time}"
            }

            updateDashboardUI(unique)
        }
    }

    private fun updateDashboardUI(list: List<Booking>) {

        Log.d(TAG, "📊 Updating dashboard with ${list.size} bookings")

        // ✅ Filter pasien yang menunggu/dipanggil (WAITING atau CALLED)
        val waiting =
            list.filter {
                it.status == BookingStatus.WAITING ||
                        it.status == BookingStatus.CALLED
            }.sortedBy { it.queueNumber }

        // ✅ Filter pasien yang sudah selesai (COMPLETED)
        val completed = list.filter {
            it.status == BookingStatus.COMPLETED
        }

        Log.d(TAG, "  - Total: ${list.size}")
        Log.d(TAG, "  - Waiting/Called: ${waiting.size}")
        Log.d(TAG, "  - Completed: ${completed.size}")

        // ✅ UPDATE STATISTIK - SEKARANG AKAN BENAR!
        tvTotalPatientsToday.text = list.size.toString()
        tvActiveQueue.text = waiting.size.toString()
        tvCompletedToday.text = completed.size.toString()  // ← INI SEKARANG AKAN BERTAMBAH!

        // ✅ TAMPILKAN SEMUA PASIEN (MENUNGGU + SELESAI) DENGAN ICON
        val sb = StringBuilder()

        if (list.isEmpty()) {
            sb.append("✅ Tidak ada pasien hari ini\n\n")
            sb.append("Klik tombol 'Lihat Antrian Pasien' untuk melihat riwayat lengkap")
        } else {
            // ✅ SECTION 1: Pasien yang Menunggu
            if (waiting.isNotEmpty()) {
                sb.append("⏱️ Pasien yang Menunggu:\n\n")

                waiting.take(3).forEachIndexed { index, b ->
                    val noDisplay = index + 1

                    // Icon berdasarkan status
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

            // ✅ SECTION 2: Pasien yang Selesai (dengan icon centang ✅)
            if (completed.isNotEmpty()) {
                sb.append("✅ Pasien Selesai Hari Ini:\n\n")

                completed.take(3).forEachIndexed { index, b ->
                    val noDisplay = index + 1

                    // ✅ ICON CENTANG untuk yang selesai
                    sb.append("✅ No. $noDisplay - ${b.patientName}\n")
                    sb.append("   Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("   Waktu: ${b.time}\n")
                    sb.append("   Status: Selesai ✓\n\n")
                }

                if (completed.size > 3) {
                    sb.append("... dan ${completed.size - 3} pasien lainnya selesai\n\n")
                }
            }

            // ✅ Info tambahan
            if (waiting.isEmpty() && completed.isNotEmpty()) {
                sb.append("\n🎉 Semua pasien sudah selesai!")
            }
        }

        tvRecentPatients.text = sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}