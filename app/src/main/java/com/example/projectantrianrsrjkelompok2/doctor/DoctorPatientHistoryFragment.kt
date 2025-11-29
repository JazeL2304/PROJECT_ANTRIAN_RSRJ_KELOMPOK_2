package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.toDisplayString

class DoctorPatientHistoryFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyStateLayout: ViewGroup  // ✅ CHANGED: LinearLayout → ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_patient_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ FIXED: Ambil sebagai ViewGroup (kompatibel dengan LinearLayout di layout XML)
        listView = view.findViewById(R.id.listPatientHistory)
        emptyStateLayout = view.findViewById(R.id.tvEmptyHistory)  // ✅ Ini LinearLayout di ui-design

        loadPatientHistory()
    }

    private fun loadPatientHistory() {
        // ✅ Ambil HANYA riwayat yang COMPLETED untuk Dr. Ahmad Santoso
        val completedBookings = DataSource.getBookingHistory()
            .filter { it.doctorName == "Dr. Ahmad Santoso" }
            .filter { it.status == BookingStatus.COMPLETED }
            .sortedByDescending { it.date } // Urutkan dari terbaru

        if (completedBookings.isEmpty()) {
            // ✅ FIXED: Show empty state layout (LinearLayout)
            emptyStateLayout.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            // ✅ FIXED: Show list
            emptyStateLayout.visibility = View.GONE
            listView.visibility = View.VISIBLE

            // ✅ Format tampilan yang RAPI & CLEAN
            val displayList = completedBookings.map { booking ->
                """
                👤 ${booking.patientName}
                📅 ${booking.date} • ${booking.time}
                💬 Keluhan: ${booking.complaint.ifEmpty { "-" }}
                📋 Diagnosis: ${booking.diagnosis.ifEmpty { "-" }}
                💊 Resep: ${booking.prescription.ifEmpty { "-" }}
                📌 Status: ${booking.status.toDisplayString()}
                """.trimIndent()
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                displayList
            )
            listView.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()
        loadPatientHistory()
    }
}