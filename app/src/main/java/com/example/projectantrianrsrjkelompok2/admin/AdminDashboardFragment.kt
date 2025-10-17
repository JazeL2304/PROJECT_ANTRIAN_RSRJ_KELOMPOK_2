package com.example.projectantrianrsrjkelompok2.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.MainActivity
import com.example.projectantrianrsrjkelompok2.R

class AdminDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Tombol-tombol dashboard
        val btnManageDoctor = view.findViewById<Button>(R.id.btnManageDoctor)
        val btnManagePatient = view.findViewById<Button>(R.id.btnManagePatient)
        val btnManageSchedule = view.findViewById<Button>(R.id.btnManageSchedule)
        val btnViewReports = view.findViewById<Button>(R.id.btnViewReports)

        btnManageDoctor.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ManageDoctorFragment())
        }

        btnManagePatient.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ManagePatientFragment())
        }

        btnManageSchedule.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ManageScheduleFragment())
        }

        btnViewReports.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ViewReportFragment())
        }
    }
}
