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

        // ✅ Filter booking hari ini untuk Dr. Ahmad Santoso
        val bookingsToday = DataSource.getBookingHistory()
            .filter { it.date == today }
            .filter { it.doctorName == "Dr. Ahmad Santoso" }
            .sortedWith(compareBy<Booking> {
                // ✅ SORTING: CALLED (0) → WAITING (1) → COMPLETED (2) → LAINNYA (3)
                when (it.status) {
                    BookingStatus.CALLED -> 0
                    BookingStatus.WAITING -> 1
                    BookingStatus.COMPLETED -> 2
                    BookingStatus.CANCELLED -> 3
                    BookingStatus.MISSED -> 3
                    else -> 4  // ← TAMBAHAN untuk case lainnya
                }
            }.thenBy { it.queueNumber })

        if (bookingsToday.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listViewQueue.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listViewQueue.visibility = View.VISIBLE

            // ✅ Tampilkan daftar booking
            val displayList = bookingsToday.map { booking ->
                val statusEmoji = when (booking.status) {
                    BookingStatus.CALLED -> "📢"
                    BookingStatus.WAITING -> "⏱️"
                    BookingStatus.COMPLETED -> "✅"
                    BookingStatus.CANCELLED -> "❌"
                    BookingStatus.MISSED -> "⚠️"
                    else -> "❓"
                }

                """
                $statusEmoji No. ${booking.queueNumber} - ${booking.patientName}
                🕒 Jam: ${booking.time}
                🏥 Dokter: ${booking.doctorName}
                💬 Keluhan: ${booking.complaint.ifEmpty { "-" }}
                📌 Status: ${booking.status.toDisplayString()}
                """.trimIndent()
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
            listViewQueue.adapter = adapter

            // ✅ Klik item untuk ubah status dengan VALIDASI
            listViewQueue.setOnItemClickListener { _, _, position, _ ->
                val selected = bookingsToday[position]
                handleStatusChange(selected)
            }
        }
    }

    private fun handleStatusChange(booking: Booking) {
        when (booking.status) {
            BookingStatus.WAITING -> {
                // ✅ VALIDASI: Cek apakah ada pasien yang sedang dipanggil
                if (DataSource.hasCalledPatient()) {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ Selesaikan pasien yang sedang dipanggil terlebih dahulu!",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // ✅ PANGGIL PASIEN: WAITING → CALLED
                DataSource.updateBookingStatus(booking.id, BookingStatus.CALLED)
                Toast.makeText(
                    requireContext(),
                    "✅ ${booking.patientName} dipanggil!",
                    Toast.LENGTH_SHORT
                ).show()
                loadTodayQueue() // Refresh
            }

            BookingStatus.CALLED -> {
                // ✅ SELESAIKAN PASIEN: CALLED → COMPLETED
                DataSource.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
                Toast.makeText(
                    requireContext(),
                    "✅ ${booking.patientName} selesai diperiksa!",
                    Toast.LENGTH_SHORT
                ).show()
                loadTodayQueue() // Refresh
            }

            BookingStatus.COMPLETED -> {
                // ✅ Sudah selesai, tidak bisa diubah lagi
                Toast.makeText(
                    requireContext(),
                    "ℹ️ Pasien ini sudah selesai diperiksa",
                    Toast.LENGTH_SHORT
                ).show()
            }

            BookingStatus.CANCELLED -> {
                // ✅ Booking dibatalkan
                Toast.makeText(
                    requireContext(),
                    "ℹ️ Booking ini sudah dibatalkan",
                    Toast.LENGTH_SHORT
                ).show()
            }

            BookingStatus.MISSED -> {
                // ✅ Pasien tidak datang
                Toast.makeText(
                    requireContext(),
                    "ℹ️ Pasien tidak datang",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadTodayQueue() // Refresh saat kembali ke fragment
    }
}
