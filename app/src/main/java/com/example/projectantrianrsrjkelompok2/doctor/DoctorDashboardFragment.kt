package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_doctor_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = PreferencesHelper(requireContext())
        doctorName = pref.getDoctorName() ?: "Dr. Ahmad Santoso"

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
    // ✅ REALTIME SAFE + AUTO NUMBER + FIX COMPLETED COUNT
    // ==========================================================
    private fun startRealtime() {

        BookingRepository.clearListeners()

        // ✅ Ambil SEMUA booking dokter ini (tidak hanya hari ini)
        BookingRepository.listenQueueByDoctor(
            doctorName,
            null
        ) { bookings ->

            val unique = bookings.distinctBy {
                "${it.patientName}|${it.queueNumber}|${it.time}"
            }

            updateDashboardUI(unique)
        }
    }

    private fun updateDashboardUI(list: List<Booking>) {

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

        // ✅ UPDATE STATISTIK
        tvTotalPatientsToday.text = list.size.toString()
        tvActiveQueue.text = waiting.size.toString()
        tvCompletedToday.text = completed.size.toString()  // ✅ FIX: Hitung dari list completed

        // ✅ TAMPILKAN PASIEN MENUNGGU
        val sb = StringBuilder()

        if (waiting.isEmpty()) {
            sb.append("✅ Tidak ada pasien yang menunggu\n\n")
            sb.append("Klik tombol 'Lihat Antrian Pasien' untuk melihat riwayat lengkap")
        } else {
            sb.append("📋 Pasien yang Menunggu:\n\n")

            // ✅ Tampilkan max 3 pasien di dashboard (lebih ringkas)
            waiting.take(3)
                .forEachIndexed { index, b ->

                    val noDisplay = index + 1   // ✅ AUTO NUMBER

                    sb.append("• No. $noDisplay - ${b.patientName}\n")
                    sb.append("  Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("  Waktu: ${b.time}\n")
                    sb.append("  Status: ${b.status.toDisplayString()}\n\n")
                }

            // ✅ Tampilkan info kalau masih ada pasien lagi
            if (waiting.size > 3) {
                sb.append("... dan ${waiting.size - 3} pasien lainnya\n")
                sb.append("Klik 'Lihat Antrian Pasien' untuk melihat semua")
            }
        }

        tvRecentPatients.text = sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}