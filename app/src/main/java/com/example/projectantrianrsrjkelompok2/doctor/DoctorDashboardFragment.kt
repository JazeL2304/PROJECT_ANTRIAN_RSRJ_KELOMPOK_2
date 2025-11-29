package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.MainActivity
import com.example.projectantrianrsrjkelompok2.ProfileFragment
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.toDisplayString
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class DoctorDashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var ivProfileIcon: ImageView  // ✅ TAMBAHAN: Icon profile

    private lateinit var tvTotalPatientsToday: TextView
    private lateinit var tvActiveQueue: TextView
    private lateinit var tvCompletedToday: TextView
    private lateinit var tvRecentPatients: TextView

    private val DOCTOR_NAME = "Dr. Ahmad Santoso"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesHelper = PreferencesHelper(requireContext())

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvGreeting.text = "Selamat Datang, $DOCTOR_NAME! 👋"

        ivProfileIcon = view.findViewById(R.id.ivProfileIcon)  // ✅ TAMBAHAN

        tvTotalPatientsToday = view.findViewById(R.id.tv_total_patients_today)
        tvActiveQueue = view.findViewById(R.id.tv_active_queue)
        tvCompletedToday = view.findViewById(R.id.tv_completed_today)
        tvRecentPatients = view.findViewById(R.id.tv_recent_patients)

        // ✅ TAMBAHAN: Profile icon click listener
        ivProfileIcon.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ProfileFragment())
        }

        val btnViewQueue = view.findViewById<Button>(R.id.btnViewQueue)
        val btnPatientHistory = view.findViewById<Button>(R.id.btnPatientHistory)
        val btnUpdateStatus = view.findViewById<Button>(R.id.btnUpdateStatus)

        btnViewQueue.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DoctorQueueFragment())
            try {
                val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.nav_doctor_queue
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnPatientHistory.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DoctorPatientHistoryFragment())
            try {
                val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav?.selectedItemId = R.id.nav_patient_history
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        btnUpdateStatus.setOnClickListener {
            showStatusDialog()
        }

        loadStatistics()
        loadRecentPatients()
    }

    private fun loadStatistics() {
        try {
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val todayBookings = DataSource.getBookingHistory()
                .filter { it.date == today }
                .filter { it.doctorName == DOCTOR_NAME }

            val activeQueue = todayBookings.filter {
                it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
            }.size

            val completed = todayBookings.filter {
                it.status == BookingStatus.COMPLETED
            }.size

            tvTotalPatientsToday.text = todayBookings.size.toString()
            tvActiveQueue.text = activeQueue.toString()
            tvCompletedToday.text = completed.toString()

        } catch (e: Exception) {
            tvTotalPatientsToday.text = "0"
            tvActiveQueue.text = "0"
            tvCompletedToday.text = "0"
        }
    }

    private fun loadRecentPatients() {
        try {
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val todayBookings = DataSource.getBookingHistory()
                .filter { it.date == today }
                .filter { it.doctorName == DOCTOR_NAME }
                .filter { it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED }
                .sortedBy { it.queueNumber }

            val recentText = StringBuilder()
            recentText.append("📋 Pasien yang Menunggu:\n\n")

            if (todayBookings.isEmpty()) {
                recentText.append("Tidak ada pasien yang menunggu")
            } else {
                todayBookings.take(5).forEach { booking ->
                    recentText.append("• No. ${booking.queueNumber} - ${booking.patientName}\n")
                    recentText.append("  Keluhan: ${booking.complaint.ifEmpty { "-" }}\n")
                    recentText.append("  Waktu: ${booking.time}\n")
                    recentText.append("  Status: ${booking.status.toDisplayString()}\n\n")
                }

                if (todayBookings.size > 5) {
                    recentText.append("... dan ${todayBookings.size - 5} pasien lainnya")
                }
            }

            tvRecentPatients.text = recentText.toString()

        } catch (e: Exception) {
            tvRecentPatients.text = "Tidak ada data pasien"
        }
    }

    private fun showStatusDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚙️ Atur Status Praktek")
            .setMessage("Fitur pengaturan status praktek (Aktif/Istirahat/Selesai) akan segera tersedia.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
        loadRecentPatients()
    }
}