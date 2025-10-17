package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.MainActivity
import com.example.projectantrianrsrjkelompok2.R

class DoctorDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🩺 Tombol fitur dokter
        val btnViewQueue = view.findViewById<Button>(R.id.btnViewQueue)
        val btnPatientHistory = view.findViewById<Button>(R.id.btnPatientHistory)
        val btnPracticeStatus = view.findViewById<Button>(R.id.btnPracticeStatus)

        // 📋 Lihat antrian pasien
        btnViewQueue.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DoctorQueueFragment())
        }

        // 🕒 Riwayat pasien
        btnPatientHistory.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DoctorPatientHistoryFragment())
        }

        // ⚙️ Atur status praktek dokter
        btnPracticeStatus.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DoctorPracticeStatusFragment())
        }
    }
}
