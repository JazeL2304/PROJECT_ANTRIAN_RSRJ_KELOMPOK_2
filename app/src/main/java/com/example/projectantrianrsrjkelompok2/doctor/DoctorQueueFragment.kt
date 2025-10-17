package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.toDisplayString
import java.text.SimpleDateFormat
import java.util.*

class DoctorQueueFragment : Fragment() {

    private lateinit var listViewQueue: ListView
    private lateinit var emptyText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listViewQueue = view.findViewById(R.id.listDoctorQueue)
        emptyText = view.findViewById(R.id.tvEmptyQueue)

        loadTodayQueue()
    }

    private fun loadTodayQueue() {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // 🔹 Filter booking hari ini
        val bookingsToday = DataSource.getBookingHistory().filter {
            it.date == today
        }

        if (bookingsToday.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listViewQueue.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listViewQueue.visibility = View.VISIBLE

            // 🔹 Tampilkan daftar booking
            val displayList = bookingsToday.map { booking ->
                """
                👤 ${booking.patientName}
                🕒 Jam: ${booking.time}
                🏥 Dokter: ${booking.doctorName}
                💬 Keluhan: ${booking.complaint.ifEmpty { "-" }}
                📌 Status: ${booking.status.toDisplayString()}
                """.trimIndent()
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
            listViewQueue.adapter = adapter

            // 🔹 Klik item untuk ubah status
            listViewQueue.setOnItemClickListener { _, _, position, _ ->
                val selected = bookingsToday[position]
                val nextStatus = when (selected.status) {
                    BookingStatus.WAITING -> BookingStatus.CALLED
                    BookingStatus.CALLED -> BookingStatus.COMPLETED
                    BookingStatus.COMPLETED -> BookingStatus.COMPLETED
                    else -> BookingStatus.WAITING
                }

                val updatedBooking = selected.copy(status = nextStatus)
                DataSource.addToHistory(updatedBooking) // update di datasource

                Toast.makeText(
                    requireContext(),
                    "Status ${selected.patientName} → ${nextStatus.toDisplayString()}",
                    Toast.LENGTH_SHORT
                ).show()

                loadTodayQueue() // refresh tampilan
            }
        }
    }
}
