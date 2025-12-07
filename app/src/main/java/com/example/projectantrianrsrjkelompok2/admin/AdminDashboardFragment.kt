package com.example.projectantrianrsrjkelompok2.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.ProfileFragment
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.toDisplayString
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper

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

        tvGreeting.text = "Selamat Datang, ${pref.getUsername()} 👋"

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
        // ✅ BUTTON NAVIGATION — FIX
        // =============================
        view.findViewById<Button>(R.id.btnManageDoctor).setOnClickListener {
            navigateTo(ManageDoctorFragment())
        }

        view.findViewById<Button>(R.id.btnManagePatient).setOnClickListener {
            navigateTo(ManagePatientFragment())
        }

        view.findViewById<Button>(R.id.btnManageSchedule).setOnClickListener {
            navigateTo(ManageScheduleFragment())
        }

        view.findViewById<Button>(R.id.btnViewReports).setOnClickListener {
            navigateTo(ViewReportFragment())
        }

        // =============================
        // REALTIME DASHBOARD
        // =============================
        startRealtimeAdmin()
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // =====================================================
    // ✅ REALTIME ADMIN DASHBOARD
    // =====================================================
    private fun startRealtimeAdmin() {

        BookingRepository.clearListeners()

        BookingRepository.listenActiveQueue { bookings ->

            val unique = bookings.distinctBy {
                "${it.patientName}|${it.time}|${it.queueNumber}"
            }

            tvTotalPatients.text =
                unique.map { it.patientName }
                    .distinct()
                    .size
                    .toString()

            tvTotalDoctors.text =
                unique.map { it.doctorName }
                    .distinct()
                    .size
                    .toString()

            tvTodayBookings.text = unique.size.toString()

            val waiting = unique.filter {
                it.status == BookingStatus.WAITING ||
                        it.status == BookingStatus.CALLED
            }.sortedBy { it.queueNumber }

            tvActiveQueues.text = waiting.size.toString()

            val sb = StringBuilder()
            sb.append("📋 Pasien yang Menunggu:\n\n")

            if (waiting.isEmpty()) {
                sb.append("Tidak ada pasien")
            } else {
                waiting.take(5).forEachIndexed { i, b ->
                    sb.append("• No. ${i + 1} - ${b.patientName}\n")
                    sb.append("  Keluhan: ${b.complaint.ifEmpty { "-" }}\n")
                    sb.append("  Waktu: ${b.time}\n")
                    sb.append("  Status: ${b.status.toDisplayString()}\n\n")
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
