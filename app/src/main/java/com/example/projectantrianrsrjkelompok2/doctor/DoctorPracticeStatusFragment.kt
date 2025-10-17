package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.R
import java.text.SimpleDateFormat
import java.util.*

class DoctorPracticeStatusFragment : Fragment() {

    private lateinit var tvPracticeStatus: TextView
    private lateinit var switchPracticeStatus: Switch
    private lateinit var btnCallNextPatient: Button
    private lateinit var tvCurrentPatient: TextView

    private var isActive = false
    private var currentIndex = -1
    private var todayQueue: List<Booking> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_practice_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvPracticeStatus = view.findViewById(R.id.tvPracticeStatus)
        switchPracticeStatus = view.findViewById(R.id.switchPracticeStatus)
        btnCallNextPatient = view.findViewById(R.id.btnCallNextPatient)
        tvCurrentPatient = view.findViewById(R.id.tvCurrentPatient)

        setupSwitch()
        setupCallButton()
    }

    private fun setupSwitch() {
        switchPracticeStatus.setOnCheckedChangeListener { _, isChecked ->
            isActive = isChecked
            if (isActive) {
                tvPracticeStatus.text = "🟢 Dokter sedang praktek"
                btnCallNextPatient.visibility = View.VISIBLE
                loadTodayQueue()
                Toast.makeText(requireContext(), "Praktek diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                tvPracticeStatus.text = "🔴 Dokter tidak aktif"
                btnCallNextPatient.visibility = View.GONE
                tvCurrentPatient.text = "Belum ada pasien yang dipanggil."
                currentIndex = -1
                Toast.makeText(requireContext(), "Praktek dimatikan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCallButton() {
        btnCallNextPatient.setOnClickListener {
            if (!isActive) {
                Toast.makeText(requireContext(), "Aktifkan praktek dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (todayQueue.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada pasien hari ini.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentIndex + 1 < todayQueue.size) {
                currentIndex++
                val nextPatient = todayQueue[currentIndex]

                // Update status booking
                val updatedBooking = nextPatient.copy(status = BookingStatus.CALLED)
                DataSource.addToHistory(updatedBooking)

                tvCurrentPatient.text = "📣 Pasien: ${nextPatient.patientName}\n" +
                        "🕒 Jam: ${nextPatient.time}\n" +
                        "💬 Keluhan: ${nextPatient.complaint}"

                Toast.makeText(
                    requireContext(),
                    "Memanggil ${nextPatient.patientName}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(requireContext(), "Semua pasien sudah dipanggil.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTodayQueue() {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        todayQueue = DataSource.getBookingHistory().filter {
            it.date == today && it.status == BookingStatus.COMPLETED
        }.sortedBy { it.queueNumber }

        if (todayQueue.isEmpty()) {
            tvCurrentPatient.text = "Tidak ada antrian pasien hari ini."
        }
    }
}
