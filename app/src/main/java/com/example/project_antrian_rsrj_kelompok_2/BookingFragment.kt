package com.example.project_antrian_rsrj_kelompok_2

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.project_antrian_rsrj_kelompok_2.MainActivity
import com.example.project_antrian_rsrj_kelompok_2.R
import com.example.project_antrian_rsrj_kelompok_2.DataSource
import com.example.project_antrian_rsrj_kelompok_2.Doctor
import java.text.SimpleDateFormat
import java.util.*

class BookingFragment : Fragment() {

    private lateinit var spinnerSpecialization: Spinner
    private lateinit var spinnerDoctor: Spinner
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var spinnerTime: Spinner
    private lateinit var etPatientName: EditText
    private lateinit var btnConfirmBooking: Button

    private var selectedDate: String = ""
    private var selectedSpecializationId: Int = 0
    private val doctors = mutableListOf<Doctor>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinners()
        setupDatePicker()
        setupBookingButton()

        // Cek apakah ada spesialisasi yang sudah dipilih dari dashboard
        arguments?.getInt("selected_specialization_id")?.let { specId ->
            if (specId > 0) {
                selectedSpecializationId = specId
                spinnerSpecialization.setSelection(specId) // Set spinner ke posisi yang benar
                loadDoctors(specId)
            }
        }
    }

    private fun initViews(view: View) {
        spinnerSpecialization = view.findViewById(R.id.spinner_specialization)
        spinnerDoctor = view.findViewById(R.id.spinner_doctor)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        spinnerTime = view.findViewById(R.id.spinner_time)
        etPatientName = view.findViewById(R.id.et_patient_name)
        btnConfirmBooking = view.findViewById(R.id.btn_confirm_booking)
    }

    private fun setupSpinners() {
        // Setup Specialization Spinner
        val specializations = DataSource.getSpecializations()
        val specNames = mutableListOf("Pilih Layanan Klinik")
        specNames.addAll(specializations.map { "${it.emoji} ${it.name}" })

        val specAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, specNames)
        specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecialization.adapter = specAdapter

        spinnerSpecialization.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    selectedSpecializationId = specializations[position - 1].id
                    loadDoctors(selectedSpecializationId)
                } else {
                    clearDoctorSpinner()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup Time Spinner
        val timeSlots = mutableListOf("Pilih Jam")
        timeSlots.addAll(DataSource.getTimeSlots())

        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeSlots)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTime.adapter = timeAdapter
    }

    private fun loadDoctors(specializationId: Int) {
        doctors.clear()
        doctors.addAll(DataSource.getDoctorsBySpecialization(specializationId))

        val doctorNames = mutableListOf("Pilih Dokter")
        doctorNames.addAll(doctors.map { "${it.name} (${it.schedule})" })

        val doctorAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, doctorNames)
        doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDoctor.adapter = doctorAdapter
    }

    private fun clearDoctorSpinner() {
        doctors.clear()
        val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Pilih Dokter"))
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDoctor.adapter = emptyAdapter
    }

    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    val displayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(calendar.time)
                    tvSelectedDate.text = displayDate
                    tvSelectedDate.visibility = View.VISIBLE
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set minimum date ke hari ini
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            // Set maximum date 30 hari ke depan
            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.DAY_OF_MONTH, 30)
            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

            datePickerDialog.show()
        }
    }

    private fun setupBookingButton() {
        btnConfirmBooking.setOnClickListener {
            if (validateBookingData()) {
                createBooking()
            }
        }
    }

    private fun validateBookingData(): Boolean {
        // Validasi nama pasien
        val patientName = etPatientName.text.toString().trim()
        if (patientName.isEmpty()) {
            etPatientName.error = "Nama pasien harus diisi"
            return false
        }

        // Validasi layanan
        if (spinnerSpecialization.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Pilih layanan klinik terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validasi dokter
        if (spinnerDoctor.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Pilih dokter terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validasi tanggal
        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), "Pilih tanggal terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validasi jam
        if (spinnerTime.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "Pilih jam terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun createBooking() {
        // Generate nomor antrian random (1-50)
        val queueNumber = (1..50).random()

        val patientName = etPatientName.text.toString().trim()
        val selectedDoctor = doctors[spinnerDoctor.selectedItemPosition - 1]
        val selectedTime = DataSource.getTimeSlots()[spinnerTime.selectedItemPosition - 1]
        val specialization = DataSource.getSpecializations().find { it.id == selectedSpecializationId }

        // Simulasi menyimpan data booking
        // Dalam implementasi nyata, data ini akan disimpan ke database

        // Tampilkan konfirmasi
        val message = """
            Booking Berhasil!
            
            Nomor Antrian: $queueNumber
            Nama: $patientName
            Layanan: ${specialization?.name}
            Dokter: ${selectedDoctor.name}
            Tanggal: ${tvSelectedDate.text}
            Jam: $selectedTime
            
            Silakan datang 15 menit sebelum jadwal.
        """.trimIndent()

        Toast.makeText(requireContext(), "Booking berhasil! Nomor antrian: $queueNumber", Toast.LENGTH_LONG).show()

        // Navigate ke Queue Fragment
        (activity as MainActivity).navigateToFragment(QueueFragment())
    }
}