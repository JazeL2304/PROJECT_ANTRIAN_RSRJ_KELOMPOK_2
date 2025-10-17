package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.MainActivity
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.toDisplayString

class DoctorPatientHistoryFragment : Fragment() {

    private lateinit var listViewHistory: ListView
    private lateinit var emptyText: TextView
    private lateinit var btnAddHistory: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_patient_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewHistory = view.findViewById(R.id.listDoctorPatientHistory)
        emptyText = view.findViewById(R.id.tvEmptyHistory)
        btnAddHistory = view.findViewById(R.id.btnAddHistory)

        btnAddHistory.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DoctorAddHistoryFragment())
        }

        loadPatientHistory()
    }

    private fun loadPatientHistory() {
        val completedBookings = DataSource.getBookingHistory().filter {
            it.status == BookingStatus.COMPLETED
        }

        if (completedBookings.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listViewHistory.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listViewHistory.visibility = View.VISIBLE

            val displayList = completedBookings.map { booking ->
                """
                👤 ${booking.patientName}
                🗓️ ${booking.date} • 🕒 ${booking.time}
                💬 Keluhan: ${booking.complaint.ifEmpty { "-" }}
                🩺 Diagnosis: ${booking.diagnosis.ifEmpty { "-" }}
                💊 Resep: ${booking.prescription.ifEmpty { "-" }}
                📌 Status: ${booking.status.toDisplayString()}
                """.trimIndent()
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                displayList
            )
            listViewHistory.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data jika kembali dari form tambah riwayat
        loadPatientHistory()
    }
}
