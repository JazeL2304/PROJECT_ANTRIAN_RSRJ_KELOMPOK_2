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

        view.findViewById<Button>(R.id.btnViewQueue).setOnClickListener {

            (activity as? MainActivity)
                ?.navigateToFragment(DoctorQueueFragment())

            try {
                requireActivity()
                    .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .selectedItemId = R.id.nav_doctor_queue
            } catch (_: Exception) {}
        }

        view.findViewById<Button>(R.id.btnPatientHistory).setOnClickListener {

            (activity as? MainActivity)
                ?.navigateToFragment(DoctorPatientHistoryFragment())

            try {
                requireActivity()
                    .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .selectedItemId = R.id.nav_patient_history
            } catch (_: Exception) {}
        }

        view.findViewById<Button>(R.id.btnUpdateStatus)
            .setOnClickListener { showStatusDialog() }

        startRealtime()
    }

    // ==========================================================
    // ✅ REALTIME SAFE + AUTO NUMBER
    // ==========================================================
    private fun startRealtime() {

        BookingRepository.clearListeners()

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

        val waiting =
            list.filter {
                it.status == BookingStatus.WAITING ||
                        it.status == BookingStatus.CALLED
            }.sortedBy { it.queueNumber }

        tvTotalPatientsToday.text = list.size.toString()
        tvActiveQueue.text = waiting.size.toString()

        tvCompletedToday.text =
            list.count { it.status == BookingStatus.COMPLETED }.toString()

        val sb = StringBuilder()
        sb.append("📋 Pasien yang Menunggu:\n\n")

        if (waiting.isEmpty()) {
            sb.append("Tidak ada pasien yang menunggu")
        } else {

            waiting.take(5)
                .forEachIndexed { index, b ->

                    val noDisplay = index + 1   // ✅ AUTO NUMBER

                    sb.append("• No. $noDisplay - ${b.patientName}\n")
                    sb.append("  Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("  Waktu: ${b.time}\n")
                    sb.append("  Status: ${b.status.toDisplayString()}\n\n")
                }
        }

        tvRecentPatients.text = sb.toString()
    }

    private fun showStatusDialog() {

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚙️ Atur Status Praktek")
            .setMessage(
                "Fitur pengaturan status praktek (Aktif/Istirahat/Selesai) akan segera tersedia."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}
