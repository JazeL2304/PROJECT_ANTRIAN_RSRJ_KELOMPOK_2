package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper

class DoctorQueueFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var pref: PreferencesHelper

    private var doctorName = "Unknown Doctor"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_doctor_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listDoctorQueue)
        emptyLayout = view.findViewById(R.id.tvEmptyQueue)
        pref = PreferencesHelper(requireContext())

        doctorName = pref.getDoctorName() ?: "Dr. Ahmad Santoso"

        // 🔥 Ambil semua antrian dokter (tanpa filter tanggal)
        BookingRepository.listenQueueByDoctor(
            doctorName,
            null
        ) { list ->
            updateUI(list)
        }
    }

    private fun updateUI(list: List<Booking>) {

        if (list.isEmpty()) {
            listView.visibility = View.GONE
            emptyLayout.visibility = View.VISIBLE
            return
        }

        emptyLayout.visibility = View.GONE
        listView.visibility = View.VISIBLE

        // =====================================================
        // 🔥 SORT & REMOVE DUPLICATE DATA
        // =====================================================

        val sorted = list
            .distinctBy { "${it.patientName}|${it.time}" } // anti duplikat
            .sortedWith(
                compareBy<Booking> {

                    // Urutkan status: Called → Waiting → Completed
                    when (it.status) {
                        BookingStatus.CALLED -> 0
                        BookingStatus.WAITING -> 1
                        BookingStatus.COMPLETED -> 2
                        else -> 3
                    }
                }.thenBy { it.time }  // urut waktu
                    .thenBy { it.patientName.lowercase() } // fallback
            )

        // =====================================================
        // 🔥 AUTO NUMBERING (No.1, No.2, No.3,…)
        // =====================================================

        val displayList = sorted.mapIndexed { index, it ->

            val autoNo = index + 1

            val icon = when (it.status) {
                BookingStatus.CALLED -> "📢"
                BookingStatus.WAITING -> "⏱"
                BookingStatus.COMPLETED -> "✔"
                else -> "❓"
            }

            "$icon No.$autoNo - ${it.patientName}\n" +
                    "Keluhan: ${it.complaint.ifEmpty { "-" }}\n" +
                    "Waktu: ${it.time}\n" +
                    "Status: ${it.status.toDisplayString()}"
        }

        listView.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            displayList
        )

        listView.setOnItemClickListener { _, _, pos, _ ->
            val booking = sorted[pos]
            handleStatus(booking)
        }
    }

    private fun handleStatus(b: Booking) {

        when (b.status) {

            BookingStatus.WAITING -> {
                BookingRepository.updateStatus(
                    b.firebaseId,
                    BookingStatus.CALLED
                )

                Toast.makeText(
                    requireContext(),
                    "📢 ${b.patientName} dipanggil!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            BookingStatus.CALLED -> {
                BookingRepository.updateStatus(
                    b.firebaseId,
                    BookingStatus.COMPLETED
                )

                Toast.makeText(
                    requireContext(),
                    "✔ ${b.patientName} selesai!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    "Status tidak dapat diubah",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}
