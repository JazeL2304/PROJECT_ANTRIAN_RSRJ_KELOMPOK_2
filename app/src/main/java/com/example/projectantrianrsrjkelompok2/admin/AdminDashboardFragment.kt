package com.example.projectantrianrsrjkelompok2.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.ProfileFragment
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalPatients: TextView
    private lateinit var tvTotalDoctors: TextView
    private lateinit var tvTodayBookings: TextView
    private lateinit var tvActiveQueues: TextView
    private lateinit var tvRecentBookings: TextView

    private lateinit var pref: PreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_admin_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = PreferencesHelper(requireContext())

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvTotalPatients = view.findViewById(R.id.tv_total_patients)
        tvTotalDoctors = view.findViewById(R.id.tv_total_doctors)
        tvTodayBookings = view.findViewById(R.id.tv_today_bookings)
        tvActiveQueues = view.findViewById(R.id.tv_active_queues)
        tvRecentBookings = view.findViewById(R.id.tv_recent_bookings)

        tvGreeting.text = "Selamat Datang, ${pref.getUsername()}"

        // =============================
        // PROFILE
        // =============================
        val ivProfile = view.findViewById<ImageView>(R.id.ivProfileIcon)
        ivProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // =============================
        // BUTTON NAVIGATION
        // =============================
        view.findViewById<Button>(R.id.btnManageDoctor).setOnClickListener {
            navigateTo(ManageDoctorFragment())
        }

        view.findViewById<Button>(R.id.btnManagePatient).setOnClickListener {
            navigateTo(ManagePatientFragment())
        }

        view.findViewById<Button>(R.id.btnViewReports).setOnClickListener {
            navigateTo(ViewReportFragment())
        }

        // =============================
        // FIX DOCTOR NAME (Hidden Feature)
        // =============================
        view.findViewById<Button>(R.id.btnManageDoctor).setOnLongClickListener {

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Fix Nama Dokter") // HAPUS EMOJI OBENG
                .setMessage(
                    "Update nama di Firebase:\n\n" +
                            "users/doc001/fullName\n\n" +
                            "DARI:\n" +
                            "\"Dr. Ahmad Susanto\"\n\n" +
                            "MENJADI:\n" +
                            "\"Dr. Ahmad Santoso\"\n\n" +
                            "Lanjutkan?"
                )
                .setPositiveButton("Ya, Update!") { _, _ ->

                    com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("users/doc001/fullName")
                        .setValue("Dr. Ahmad Santoso")
                        .addOnSuccessListener {

                            android.widget.Toast.makeText(
                                requireContext(),
                                "BERHASIL!\n\n" +
                                        "Nama berhasil diubah:\n" +
                                        "Susanto → Santoso\n\n" +
                                        "DOKTER HARUS LOGOUT & LOGIN ULANG!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            android.util.Log.d("ADMIN_FIX", "SUCCESS: users/doc001/fullName → Dr. Ahmad Santoso")
                        }
                        .addOnFailureListener { e ->

                            android.widget.Toast.makeText(
                                requireContext(),
                                "GAGAL!\n\n" + // HAPUS EMOJI SILANG
                                        "Error: ${e.message}\n\n" +
                                        "Cek Firebase Rules!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            android.util.Log.e("ADMIN_FIX", "FAILED: ${e.message}")
                        }
                }
                .setNegativeButton("Batal", null)
                .show()

            true
        }

        startRealtimeAdmin()
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // =====================================================
    // REALTIME ADMIN DASHBOARD
    // =====================================================
    private fun startRealtimeAdmin() {
        BookingRepository.clearListeners()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())

        android.util.Log.d("AdminDashboard", "Fetching stats for date: $today") // HAPUS EMOJI KALENDER

        BookingRepository.listenDashboardStats(today) {
                totalPatients,
                totalDoctors,
                todayBookings,
                activeQueues,
                waitingList
            ->
            android.util.Log.d("AdminDashboard", "Stats received:") // HAPUS EMOJI CHART
            android.util.Log.d("AdminDashboard", "   - Total Patients: $totalPatients")
            android.util.Log.d("AdminDashboard", "   - Total Doctors: $totalDoctors")
            android.util.Log.d("AdminDashboard", "   - Today Bookings: $todayBookings")
            android.util.Log.d("AdminDashboard", "   - Active Queues: $activeQueues")

            tvTotalPatients.text = totalPatients.toString()
            tvTotalDoctors.text = totalDoctors.toString()
            tvTodayBookings.text = todayBookings.toString()
            tvActiveQueues.text = activeQueues.toString()

            val sb = StringBuilder()
            sb.append("Pasien yang Menunggu:\n\n") // HAPUS EMOJI CLIPBOARD

            if (waitingList.isEmpty()) {
                sb.append("Belum ada pasien yang menunggu")
            } else {
                waitingList.take(5).forEachIndexed { i, b ->
                    sb.append("${i + 1}. ${b.patientName}\n")
                    sb.append("   Dokter: ${b.doctorName}\n")
                    sb.append("   Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("   Antrian: No. ${b.queueNumber}\n")
                    sb.append("   Waktu: ${b.time}\n")
                    sb.append("   Status: ${b.status.toDisplayString()}\n\n")
                }
            }

            tvRecentBookings.text = sb.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}