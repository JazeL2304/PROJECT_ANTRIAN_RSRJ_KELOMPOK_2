package com.example.projectantrianrsrjkelompok2.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.MainActivity
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.toDisplayString
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView  // ✅ IMPORT
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardFragment : Fragment() {

    // ← TextView untuk statistik
    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvTotalDoctors: TextView
    private lateinit var tvTodayBookings: TextView
    private lateinit var tvActiveQueues: TextView
    private lateinit var tvRecentBookings: TextView

    // ← TAMBAHAN: PreferencesHelper
    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ← INITIALIZE PreferencesHelper
        preferencesHelper = PreferencesHelper(requireContext())

        // ← INISIALISASI TextView statistik
        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvTotalPatients = view.findViewById(R.id.tv_total_patients)
        tvTotalDoctors = view.findViewById(R.id.tv_total_doctors)
        tvTodayBookings = view.findViewById(R.id.tv_today_bookings)
        tvActiveQueues = view.findViewById(R.id.tv_active_queues)
        tvRecentBookings = view.findViewById(R.id.tv_recent_bookings)

        // ← SET GREETING dengan nama user
        val username = preferencesHelper.getUsername()
        tvGreeting.text = "Selamat Datang, $username! 👋"

        // 🔹 Tombol-tombol dashboard
        val btnManageDoctor = view.findViewById<Button>(R.id.btnManageDoctor)
        val btnManagePatient = view.findViewById<Button>(R.id.btnManagePatient)
        val btnManageSchedule = view.findViewById<Button>(R.id.btnManageSchedule)
        val btnViewReports = view.findViewById<Button>(R.id.btnViewReports)

        // ✅ Setup navigasi (TETAP - tidak berubah)
        btnManageDoctor.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ManageDoctorFragment())
        }

        btnManagePatient.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ManagePatientFragment())
        }

        btnManageSchedule.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ManageScheduleFragment())
        }

        // ✅ FIXED: Button laporan - ganti loadFragment jadi navigateToFragment
        btnViewReports.setOnClickListener {
            (activity as? MainActivity)?.apply {
                // ✅ FIXED: Gunakan navigateToFragment (public method)
                navigateToFragment(ViewReportFragment())

                // ✅ Update bottom navigation selection
                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav.selectedItemId = R.id.nav_reports
            }
        }

        // ← LOAD DATA STATISTIK
        loadStatistics()
        loadRecentActivity()
    }

    // ← FUNCTION: Load statistik dashboard
    private fun loadStatistics() {
        try {
            // Get data dari DataSource
            val totalPatients = DataSource.getTotalPatients()
            val totalDoctors = DataSource.getTotalDoctors()
            val todayBookings = DataSource.getTodayBookings()
            val activeQueues = DataSource.getActiveQueues()

            // Update UI
            tvTotalPatients.text = totalPatients.toString()
            tvTotalDoctors.text = totalDoctors.toString()
            tvTodayBookings.text = todayBookings.size.toString()
            tvActiveQueues.text = activeQueues.size.toString()

        } catch (e: Exception) {
            // Fallback jika DataSource belum punya function
            tvTotalPatients.text = "0"
            tvTotalDoctors.text = "0"
            tvTodayBookings.text = "0"
            tvActiveQueues.text = "0"
        }
    }

    // ← FUNCTION: Load aktivitas terbaru
    private fun loadRecentActivity() {
        try {
            val todayBookings = DataSource.getTodayBookings()

            val recentText = StringBuilder()
            recentText.append("📋 Booking Hari Ini:\n\n")

            if (todayBookings.isEmpty()) {
                recentText.append("Tidak ada booking hari ini")
            } else {
                // Ambil 5 booking terbaru
                todayBookings.take(5).forEach { booking ->
                    recentText.append("• ${booking.patientName}\n")
                    recentText.append("  Dokter: ${booking.doctorName}\n")
                    recentText.append("  Waktu: ${booking.time}\n")
                    recentText.append("  Status: ${booking.status.toDisplayString()}\n\n")
                }

                if (todayBookings.size > 5) {
                    recentText.append("... dan ${todayBookings.size - 5} booking lainnya")
                }
            }

            tvRecentBookings.text = recentText.toString()

        } catch (e: Exception) {
            tvRecentBookings.text = "Tidak ada aktivitas terbaru"
        }
    }

    // ← FUNCTION HELPER: Format tanggal
    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    // ← FUNCTION HELPER: Format waktu
    private fun formatTime(timeString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = inputFormat.parse(timeString)
            outputFormat.format(time ?: Date())
        } catch (e: Exception) {
            timeString
        }
    }

    // ← Refresh data saat fragment kembali visible
    override fun onResume() {
        super.onResume()
        loadStatistics()
        loadRecentActivity()
    }
}
