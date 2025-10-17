package com.example.projectantrianrsrjkelompok2.admin

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

class ViewReportFragment : Fragment() {

    private lateinit var tvDoctorCount: TextView
    private lateinit var tvPatientCount: TextView
    private lateinit var tvBookingCount: TextView
    private lateinit var layoutBookingList: LinearLayout
    private lateinit var btnCallNextPatient: Button
    private lateinit var tvCurrentPatient: TextView

    private var todayBookings: List<Booking> = listOf()
    private var currentIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDoctorCount = view.findViewById(R.id.tvDoctorCount)
        tvPatientCount = view.findViewById(R.id.tvPatientCount)
        tvBookingCount = view.findViewById(R.id.tvBookingCount)
        layoutBookingList = view.findViewById(R.id.layoutBookingList)
        btnCallNextPatient = view.findViewById(R.id.btnCallNextPatientAdmin)
        tvCurrentPatient = view.findViewById(R.id.tvCurrentCalledPatientAdmin)

        loadReportData()
        setupCallButton()
    }

    private fun loadReportData() {
        val doctors = DataSource.getAllDoctors()
        val patients = DataSource.getAllPatients()
        val bookings = DataSource.getBookingHistory()

        tvDoctorCount.text = doctors.size.toString()
        tvPatientCount.text = patients.size.toString()
        tvBookingCount.text = bookings.size.toString()

        layoutBookingList.removeAllViews()

        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        todayBookings = bookings.filter { it.date == today }.sortedBy { it.queueNumber }

        if (todayBookings.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "Belum ada data booking hari ini."
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            layoutBookingList.addView(emptyText)
        } else {
            todayBookings.forEachIndexed { index, booking ->
                val textView = TextView(requireContext()).apply {
                    val statusLabel = when (booking.status) {
                        BookingStatus.CALLED -> "✅ Sudah dipanggil"
                        BookingStatus.WAITING -> "⏳ Menunggu"
                        else -> "✔️ ${booking.status.name.lowercase().replaceFirstChar { it.uppercase() }}"
                    }
                    text = "${index + 1}. ${booking.patientName} → ${booking.doctorName}\n" +
                            "Waktu: ${booking.time}  |  Status: $statusLabel"
                    textSize = 14f
                    setPadding(8, 8, 8, 12)
                }
                layoutBookingList.addView(textView)
            }
        }
    }

    private fun setupCallButton() {
        btnCallNextPatient.setOnClickListener {
            if (todayBookings.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada antrian hari ini.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentIndex + 1 < todayBookings.size) {
                currentIndex++
                val next = todayBookings[currentIndex]

                val updated = next.copy(status = BookingStatus.CALLED)
                DataSource.addToHistory(updated)

                tvCurrentPatient.text =
                    "📣 Pasien: ${next.patientName}\n👨‍⚕️ Dokter: ${next.doctorName}\n🕒 Waktu: ${next.time}"

                Toast.makeText(
                    requireContext(),
                    "Memanggil ${next.patientName} untuk ${next.specialization}",
                    Toast.LENGTH_SHORT
                ).show()

                loadReportData()
            } else {
                Toast.makeText(requireContext(), "Semua pasien sudah dipanggil.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
