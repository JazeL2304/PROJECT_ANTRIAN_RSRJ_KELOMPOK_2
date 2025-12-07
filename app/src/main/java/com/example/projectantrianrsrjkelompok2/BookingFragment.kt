package com.example.projectantrianrsrjkelompok2

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BookingFragment : Fragment() {

    private lateinit var spinnerSpecialization: Spinner
    private lateinit var spinnerDoctor: Spinner
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var spinnerTime: Spinner
    private lateinit var etPatientName: EditText
    private lateinit var etComplaint: EditText
    private lateinit var btnConfirmBooking: Button
    private lateinit var progressBar: ProgressBar

    private var selectedDate = ""
    private var selectedSpecializationId = 0
    private var selectedDoctor: Doctor? = null

    private val doctors = mutableListOf<Doctor>()
    private val currentTimeSlots = mutableListOf<String>()

    private var isNightShiftDoctor = false
    private var isProcessing = false
    private var isDataLoaded = false

    companion object {
        private const val TAG = "BookingFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_booking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadDataAndSetupUI()
    }

    private fun initViews(view: View) {
        spinnerSpecialization = view.findViewById(R.id.spinner_specialization)
        spinnerDoctor = view.findViewById(R.id.spinner_doctor)
        btnSelectDate = view.findViewById(R.id.btn_select_date)
        tvSelectedDate = view.findViewById(R.id.tv_selected_date)
        spinnerTime = view.findViewById(R.id.spinner_time)
        etPatientName = view.findViewById(R.id.et_patient_name)
        etComplaint = view.findViewById(R.id.et_complaint)
        btnConfirmBooking = view.findViewById(R.id.btn_confirm_booking)
        progressBar = view.findViewById(R.id.progress_bar)
    }

    private fun loadDataAndSetupUI() {
        showLoading(true)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                DataSource.invalidateCache()
                DataSource.forceLoadFromFirebase()

                var retry = 0
                while (DataSource.getAllDoctors().isEmpty() && retry < 10) {
                    delay(500)
                    retry++
                }
            }

            isDataLoaded = true
            showLoading(false)

            setupSpecializationSpinner()
            setupDatePicker()
            setupBookingButton()
            clearDoctorSpinner()
            clearTimeSpinner()
        }
    }

    // ======================================================
    // SPINNER SPESIALIS ✔ CUSTOM UI
    // ======================================================
    private fun setupSpecializationSpinner() {

        val specializations = DataSource.getSpecializations()
        val names = mutableListOf("Pilih Layanan Klinik")

        names.addAll(specializations.map { "${it.emoji} ${it.name}" })

        spinnerSpecialization.adapter =
            ArrayAdapter(
                requireContext(),
                R.layout.simple_spinner_item_custom,
                names
            ).also {
                it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
            }

        spinnerSpecialization.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (pos == 0) {
                        clearDoctorSpinner()
                        clearTimeSpinner()
                        return
                    }

                    selectedSpecializationId = specializations[pos - 1].id
                    loadDoctors(selectedSpecializationId)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ======================================================
    // LOAD DOKTER ✔ CUSTOM UI
    // ======================================================
    private fun loadDoctors(id: Int) {
        doctors.clear()
        doctors.addAll(DataSource.getDoctorsBySpecialization(id))
        updateDoctorSpinner()
    }

    private fun updateDoctorSpinner() {

        val names = mutableListOf("Pilih Dokter")

        names.addAll(
            doctors.map {
                "${it.name}\n📅 ${it.schedule} ${getShiftLabel(it.schedule)}"
            }
        )

        spinnerDoctor.adapter =
            ArrayAdapter(
                requireContext(),
                R.layout.simple_spinner_item_custom,
                names
            ).also {
                it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
            }

        spinnerDoctor.isEnabled = doctors.isNotEmpty()

        spinnerDoctor.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {

                    if (pos == 0) {
                        selectedDoctor = null
                        clearTimeSpinner()
                        return
                    }

                    selectedDoctor = doctors[pos - 1]
                    isNightShiftDoctor = isNightShift(selectedDoctor!!.schedule)

                    selectedDate = ""
                    tvSelectedDate.visibility = View.GONE

                    clearTimeSpinner()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ======================================================
    // DATE PICKER
    // ======================================================
    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {

            if (selectedDoctor == null) {
                toast("Pilih dokter dahulu")
                return@setOnClickListener
            }

            val cal = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->

                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().apply {
                            set(y, m, d)
                        }.time)

                    tvSelectedDate.text = selectedDate
                    tvSelectedDate.visibility = View.VISIBLE

                    updateTimeSpinner()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // ======================================================
    // TIME ✔ CUSTOM UI
    // ======================================================
    private fun updateTimeSpinner() {

        lifecycleScope.launch {

            val slots = withContext(Dispatchers.Default) {
                generateValidTimeSlots()
            }

            currentTimeSlots.clear()
            currentTimeSlots.add("Pilih Jam")
            currentTimeSlots.addAll(slots)

            spinnerTime.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.simple_spinner_item_custom,
                    currentTimeSlots
                ).also {
                    it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
                }

            spinnerTime.isEnabled = currentTimeSlots.size > 1
        }
    }

    private fun generateValidTimeSlots(): List<String> {

        val result = mutableListOf<String>()

        var m = 8 * 60
        while (m <= 20 * 60) {
            result.add(formatTime(m))
            m += 30
        }

        return result
    }

    private fun formatTime(min: Int): String =
        String.format("%02d:%02d", min / 60, min % 60)

    // ======================================================
    // BOOKING BUTTON
    // ======================================================
    private fun setupBookingButton() {
        btnConfirmBooking.setOnClickListener {
            if (validateBookingData()) {
                createBooking()
            }
        }
    }

    private fun validateBookingData(): Boolean {

        if (spinnerSpecialization.selectedItemPosition == 0) {
            toast("Pilih layanan dulu")
            return false
        }

        if (spinnerDoctor.selectedItemPosition == 0 || selectedDoctor == null) {
            toast("Pilih dokter")
            return false
        }

        if (selectedDate.isEmpty()) {
            toast("Pilih tanggal")
            return false
        }

        if (spinnerTime.selectedItemPosition == 0) {
            toast("Pilih jam")
            return false
        }

        if (etPatientName.text.length < 3) {
            etPatientName.error = "Nama minimal 3 huruf"
            return false
        }

        if (etComplaint.text.isEmpty()) {
            etComplaint.error = "Isi keluhan"
            return false
        }

        return true
    }

    // ======================================================
    // CREATE BOOKING
    // ======================================================
    private fun createBooking() {

        val queueNumber =
            DataSource.getBookingHistory().count { it.date == selectedDate } + 1

        val booking = Booking(
            id = "Q${queueNumber.toString().padStart(3, '0')}",
            queueNumber = queueNumber,
            patientName = etPatientName.text.toString(),
            doctorName = selectedDoctor!!.name,
            specialization = selectedDoctor!!.specialization,
            date = selectedDate,
            time = spinnerTime.selectedItem.toString(),
            complaint = etComplaint.text.toString(),
            diagnosis = "",
            prescription = "",
            status = BookingStatus.WAITING,
            createdAt = System.currentTimeMillis()
        )

        showLoading(true)

        BookingRepository.addBooking(booking) { success ->

            if (!isAdded) return@addBooking

            requireActivity().runOnUiThread {

                showLoading(false)

                if (!success) {
                    toast("❌ Gagal simpan booking")
                    return@runOnUiThread
                }

                DataSource.setActiveBooking(booking)
                DataSource.addToHistory(booking)

                toast("✅ Booking berhasil")

                (activity as? MainActivity)
                    ?.navigateToFragment(QueueFragment())
            }
        }
    }

    // ======================================================
    // HELPER ✔ CUSTOM UI
    // ======================================================
    private fun clearDoctorSpinner() {
        spinnerDoctor.adapter =
            ArrayAdapter(
                requireContext(),
                R.layout.simple_spinner_item_custom,
                listOf("Pilih Dokter")
            ).also {
                it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
            }
        spinnerDoctor.isEnabled = false
    }

    private fun clearTimeSpinner() {
        spinnerTime.adapter =
            ArrayAdapter(
                requireContext(),
                R.layout.simple_spinner_item_custom,
                listOf("Pilih Jam")
            ).also {
                it.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_custom)
            }
        spinnerTime.isEnabled = false
    }

    private fun isNightShift(schedule: String): Boolean =
        schedule.contains("20") || schedule.contains("21")

    private fun getShiftLabel(schedule: String) =
        if (isNightShift(schedule)) "🌙" else "☀"

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmBooking.isEnabled = !show
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
