package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper

class DoctorPatientHistoryFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyState: ViewGroup

    private lateinit var pref: PreferencesHelper
    private var doctorName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_doctor_patient_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listPatientHistory)
        emptyState = view.findViewById(R.id.tvEmptyHistory)

        pref = PreferencesHelper(requireContext())
        doctorName = pref.getDoctorName() ?: "Dr. Ahmad Santoso"

        startRealtimeHistory()
    }

    private fun startRealtimeHistory() {

        BookingRepository.listenHistoryByDoctor(doctorName) { list ->

            if (list.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                listView.visibility = View.GONE
                return@listenHistoryByDoctor
            }

            emptyState.visibility = View.GONE
            listView.visibility = View.VISIBLE

            val show = list.map { b ->

                """
                👤 ${b.patientName}
                📅 ${b.date} • ${b.time}
                💬 Keluhan: ${b.complaint.ifEmpty { "-" }}
                📋 Diagnosis: ${b.diagnosis.ifEmpty { "-" }}
                💊 Resep: ${b.prescription.ifEmpty { "-" }}
                ✅ Status: ${b.status.toDisplayString()}
                """.trimIndent()
            }

            listView.adapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    show
                )

            // ✅ LONG CLICK DELETE
            listView.setOnItemLongClickListener { _, _, pos, _ ->

                val data = list[pos]

                confirmDelete(data)

                true
            }
        }
    }

    private fun confirmDelete(b: Booking) {

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus Riwayat")
            .setMessage("Hapus data pasien ${b.patientName}?")
            .setPositiveButton("HAPUS") { _, _ ->

                BookingRepository.deleteBooking(b.firebaseId)

                Toast.makeText(
                    requireContext(),
                    "Riwayat berhasil dihapus",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}
