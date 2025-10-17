package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.*
import java.text.SimpleDateFormat
import java.util.*

class DoctorAddHistoryFragment : Fragment() {

    private lateinit var etPatientName: EditText
    private lateinit var etComplaint: EditText
    private lateinit var etDiagnosis: EditText
    private lateinit var etPrescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_doctor_add_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etPatientName = view.findViewById(R.id.etPatientName)
        etComplaint = view.findViewById(R.id.etComplaint)
        etDiagnosis = view.findViewById(R.id.etDiagnosis)
        etPrescription = view.findViewById(R.id.etPrescription)
        etDate = view.findViewById(R.id.etDate)
        etTime = view.findViewById(R.id.etTime)
        btnSave = view.findViewById(R.id.btnSaveHistory)

        // Set default date & time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        etDate.setText(dateFormat.format(Date()))
        etTime.setText(timeFormat.format(Date()))

        btnSave.setOnClickListener { savePatientHistory() }
    }

    private fun savePatientHistory() {
        val patientName = etPatientName.text.toString().trim()
        val complaint = etComplaint.text.toString().trim()
        val diagnosis = etDiagnosis.text.toString().trim()
        val prescription = etPrescription.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()

        if (patientName.isEmpty() || complaint.isEmpty() || diagnosis.isEmpty() || prescription.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Semua field harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val doctorName = "Dr. Ahmad Santoso" // bisa diganti sesuai login dokter
        val specialization = "Dokter Umum"

        val newBooking = Booking(
            id = UUID.randomUUID().toString(),
            queueNumber = (1..100).random(),
            patientName = patientName,
            doctorName = doctorName,
            specialization = specialization,
            date = date,
            time = time,
            complaint = complaint,
            diagnosis = diagnosis,
            prescription = prescription,
            status = BookingStatus.COMPLETED
        )

        DataSource.addToHistory(newBooking)
        Toast.makeText(requireContext(), "✅ Riwayat pasien berhasil disimpan", Toast.LENGTH_SHORT).show()

        // Kosongkan form
        etPatientName.text.clear()
        etComplaint.text.clear()
        etDiagnosis.text.clear()
        etPrescription.text.clear()

        // Kembali ke daftar riwayat pasien
        (activity as MainActivity).navigateToFragment(DoctorPatientHistoryFragment())
    }
}
