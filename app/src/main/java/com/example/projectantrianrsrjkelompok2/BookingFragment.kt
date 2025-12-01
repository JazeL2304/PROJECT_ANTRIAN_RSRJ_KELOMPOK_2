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

    private var selectedDate: String = ""
    private var selectedSpecializationId: Int = 0
    private var selectedDoctor: Doctor? = null
    private val doctors = mutableListOf<Doctor>()
    private var isDataLoaded = false

    companion object {
        private const val TAG = "BookingFragment"
    }

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

    // ✅ Load data dengan retry mechanism
    private fun loadDataAndSetupUI() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "🔄 Loading data from Firebase...")

                    DataSource.invalidateCache()
                    DataSource.forceLoadFromFirebase()

                    var retryCount = 0
                    while (DataSource.getAllDoctors().isEmpty() && retryCount < 10) {
                        Log.d(TAG, "⏳ Waiting for data... retry $retryCount")
                        delay(500)
                        retryCount++
                    }
                }

                val allDoctors = DataSource.getAllDoctors()
                val specializations = DataSource.getSpecializations()

                Log.d(TAG, "=== DATA LOADED ===")
                Log.d(TAG, "Doctors: ${allDoctors.size}")
                allDoctors.forEach {
                    Log.d(TAG, "  - ${it.name} (${it.specialization})")
                }
                Log.d(TAG, "Specializations: ${specializations.size}")
                specializations.forEach {
                    Log.d(TAG, "  - ${it.name}")
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (allDoctors.isNotEmpty() && specializations.isNotEmpty()) {
                        isDataLoaded = true

                        setupSpecializationSpinner()
                        delay(100)
                        setupDatePicker()
                        delay(100)
                        setupBookingButton()

                        clearDoctorSpinner()
                        clearTimeSpinner()

                        handlePreSelectedSpecialization()

                        Log.d(TAG, "✅ UI Setup Complete")

                        Toast.makeText(
                            requireContext(),
                            "✅ Data berhasil dimuat",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e(TAG, "❌ Data masih kosong setelah load!")

                        val errorMsg = "❌ Gagal memuat data.\n" +
                                "Doctors: ${allDoctors.size}\n" +
                                "Specs: ${specializations.size}\n\n" +
                                "Coba restart aplikasi atau hubungi admin."

                        Toast.makeText(
                            requireContext(),
                            errorMsg,
                            Toast.LENGTH_LONG
                        ).show()

                        showRetryDialog()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    showRetryDialog()
                }
            }
        }
    }

    private fun showRetryDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Gagal Memuat Data")
            .setMessage("Data tidak dapat dimuat dari server. Coba lagi?")
            .setPositiveButton("Coba Lagi") { _, _ ->
                loadDataAndSetupUI()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // ✅ Setup spinner spesialisasi
    private fun setupSpecializationSpinner() {
        try {
            val specializations = DataSource.getSpecializations()

            Log.d(TAG, "=== SETUP SPECIALIZATION SPINNER ===")
            Log.d(TAG, "Specializations count: ${specializations.size}")

            if (specializations.isEmpty()) {
                Log.e(TAG, "❌ No specializations available!")
                Toast.makeText(
                    requireContext(),
                    "❌ Data spesialisasi kosong!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val specNames = mutableListOf<String>()
            specNames.add("Pilih Layanan Klinik")
            specializations.forEach { spec ->
                specNames.add("${spec.emoji} ${spec.name}")
                Log.d(TAG, "Added: ${spec.emoji} ${spec.name}")
            }

            val specAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                specNames
            )
            specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerSpecialization.adapter = specAdapter
            spinnerSpecialization.isEnabled = true
            spinnerSpecialization.isClickable = true
            spinnerSpecialization.isFocusable = true

            spinnerSpecialization.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    Log.d(TAG, "Specialization selected: position=$position")

                    if (position > 0) {
                        val selectedSpec = specializations[position - 1]
                        selectedSpecializationId = selectedSpec.id

                        Log.d(TAG, "Selected: ${selectedSpec.name} (ID: ${selectedSpec.id})")

                        loadDoctors(selectedSpecializationId)
                    } else {
                        clearDoctorSpinner()
                        clearTimeSpinner()
                        selectedDoctor = null
                        selectedDate = ""
                        tvSelectedDate.text = ""
                        tvSelectedDate.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    clearDoctorSpinner()
                    clearTimeSpinner()
                    selectedDoctor = null
                }
            }

            Log.d(TAG, "✅ Specialization spinner setup complete")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up specialization spinner: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "❌ Error setup spinner: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ✅ Load doctors berdasarkan spesialisasi
    private fun loadDoctors(specializationId: Int) {
        doctors.clear()
        selectedDoctor = null
        selectedDate = ""
        tvSelectedDate.text = ""
        tvSelectedDate.visibility = View.GONE

        Log.d(TAG, "=== LOADING DOCTORS ===")
        Log.d(TAG, "Specialization ID: $specializationId")

        val filteredDoctors = DataSource.getDoctorsBySpecialization(specializationId)

        Log.d(TAG, "Filtered doctors: ${filteredDoctors.size}")

        if (filteredDoctors.isEmpty()) {
            Log.w(TAG, "⚠️ No doctors found")

            Toast.makeText(
                requireContext(),
                "Belum ada dokter untuk layanan ini",
                Toast.LENGTH_SHORT
            ).show()

            clearDoctorSpinner()
            clearTimeSpinner()
        } else {
            doctors.addAll(filteredDoctors)

            Log.d(TAG, "✅ Doctors loaded:")
            doctors.forEach { doctor ->
                Log.d(TAG, "  - ${doctor.name} (${doctor.specialization}) - ${doctor.schedule}")
            }

            updateDoctorSpinner()
        }
    }

    // ✅ Update doctor spinner
    private fun updateDoctorSpinner() {
        try {
            val doctorNames = mutableListOf<String>()
            doctorNames.add("Pilih Dokter")

            doctors.forEach { doctor ->
                doctorNames.add("${doctor.name}\n📅 ${doctor.schedule}")
            }

            val doctorAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                doctorNames
            )
            doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerDoctor.adapter = doctorAdapter
            spinnerDoctor.isEnabled = true
            spinnerDoctor.isClickable = true
            spinnerDoctor.isFocusable = true

            spinnerDoctor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        selectedDoctor = doctors[position - 1]
                        Log.d(TAG, "✅ Doctor selected: ${selectedDoctor?.name}")

                        // Reset tanggal dan jam saat dokter berubah
                        selectedDate = ""
                        tvSelectedDate.text = ""
                        tvSelectedDate.visibility = View.GONE
                        clearTimeSpinner()

                        Toast.makeText(
                            requireContext(),
                            "Silakan pilih tanggal booking",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        selectedDoctor = null
                        clearTimeSpinner()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedDoctor = null
                    clearTimeSpinner()
                }
            }

            Log.d(TAG, "✅ Doctor spinner updated: ${doctorNames.size} items")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating doctor spinner: ${e.message}", e)
        }
    }

    // ✅ Update time spinner berdasarkan jadwal dokter
    private fun updateTimeSpinner() {
        try {
            if (selectedDoctor == null) {
                clearTimeSpinner()
                return
            }

            val timeSlots = mutableListOf<String>()
            timeSlots.add("Pilih Jam")

            val doctorTimeSlots = DataSource.getTimeSlotsForDoctor(selectedDoctor!!)
            timeSlots.addAll(doctorTimeSlots)

            val timeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                timeSlots
            )
            timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerTime.adapter = timeAdapter
            spinnerTime.isEnabled = true
            spinnerTime.isClickable = true
            spinnerTime.isFocusable = true

            Log.d(TAG, "✅ Time spinner updated: ${timeSlots.size} slots for ${selectedDoctor?.name}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating time spinner: ${e.message}", e)
        }
    }

    private fun clearDoctorSpinner() {
        doctors.clear()
        selectedDoctor = null

        val emptyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Pilih Dokter")
        )
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerDoctor.adapter = emptyAdapter
        spinnerDoctor.isEnabled = false
    }

    private fun clearTimeSpinner() {
        val emptyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Pilih Jam")
        )
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerTime.adapter = emptyAdapter
        spinnerTime.isEnabled = false
    }

    // ✅ Setup date picker dengan validasi jadwal dokter
    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {
            // Validasi: Harus pilih dokter dulu
            if (selectedDoctor == null) {
                Toast.makeText(
                    requireContext(),
                    "⚠️ Pilih dokter terlebih dahulu!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)

                    // Validasi: Cek apakah tanggal ini sesuai jadwal dokter
                    if (isDoctorAvailableOnDate(calendar, selectedDoctor!!)) {
                        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        val displayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(calendar.time)
                        tvSelectedDate.text = displayDate
                        tvSelectedDate.visibility = View.VISIBLE

                        // Update time spinner setelah tanggal valid dipilih
                        updateTimeSpinner()

                        Toast.makeText(
                            requireContext(),
                            "Silakan pilih jam booking",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val dayName = SimpleDateFormat("EEEE", Locale("id", "ID")).format(calendar.time)
                        Toast.makeText(
                            requireContext(),
                            "❌ ${selectedDoctor!!.name} tidak praktik pada hari $dayName!\n\nJadwal: ${selectedDoctor!!.schedule}",
                            Toast.LENGTH_LONG
                        ).show()

                        // Reset jika tanggal tidak valid
                        selectedDate = ""
                        tvSelectedDate.text = ""
                        tvSelectedDate.visibility = View.GONE
                        clearTimeSpinner()
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.DAY_OF_MONTH, 30)
            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

            datePickerDialog.show()
        }
    }

    // ✅ Validasi hari praktik dokter
    private fun isDoctorAvailableOnDate(calendar: Calendar, doctor: Doctor): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val schedule = doctor.schedule.lowercase()

        Log.d(TAG, "=== VALIDASI HARI PRAKTIK ===")
        Log.d(TAG, "Dokter: ${doctor.name}")
        Log.d(TAG, "Jadwal: ${doctor.schedule}")
        Log.d(TAG, "Hari dipilih: ${getDayName(dayOfWeek)}")

        // CASE 1: Dokter praktik "Setiap Hari" atau "24 Jam"
        if (schedule.contains("setiap hari") || schedule.contains("24 jam")) {
            Log.d(TAG, "✅ Dokter praktik setiap hari")
            return true
        }

        // CASE 2: Parse hari praktik dari schedule string
        val availableDays = parseDoctorScheduleDays(schedule)

        Log.d(TAG, "Hari praktik: $availableDays")

        val isDayAvailable = when (dayOfWeek) {
            Calendar.SUNDAY -> availableDays.contains("minggu")
            Calendar.MONDAY -> availableDays.contains("senin")
            Calendar.TUESDAY -> availableDays.contains("selasa")
            Calendar.WEDNESDAY -> availableDays.contains("rabu")
            Calendar.THURSDAY -> availableDays.contains("kamis")
            Calendar.FRIDAY -> availableDays.contains("jumat")
            Calendar.SATURDAY -> availableDays.contains("sabtu")
            else -> false
        }

        Log.d(TAG, if (isDayAvailable) "✅ Hari tersedia" else "❌ Hari tidak tersedia")
        return isDayAvailable
    }

    // ✅ Parse hari praktik dari schedule string
    private fun parseDoctorScheduleDays(schedule: String): List<String> {
        val scheduleLower = schedule.lowercase()
        val days = mutableListOf<String>()

        // Handle format "Senin-Jumat", "Senin–Sabtu", dll
        when {
            // Senin sampai Jumat
            scheduleLower.contains("senin") && scheduleLower.contains("jumat") &&
                    (scheduleLower.contains("-") || scheduleLower.contains("–")) -> {
                days.addAll(listOf("senin", "selasa", "rabu", "kamis", "jumat"))
            }
            // Senin sampai Sabtu
            scheduleLower.contains("senin") && scheduleLower.contains("sabtu") &&
                    (scheduleLower.contains("-") || scheduleLower.contains("–")) -> {
                days.addAll(listOf("senin", "selasa", "rabu", "kamis", "jumat", "sabtu"))
            }
            // Senin sampai Kamis
            scheduleLower.contains("senin") && scheduleLower.contains("kamis") &&
                    (scheduleLower.contains("-") || scheduleLower.contains("–")) -> {
                days.addAll(listOf("senin", "selasa", "rabu", "kamis"))
            }
            // Selasa sampai Sabtu
            scheduleLower.contains("selasa") && scheduleLower.contains("sabtu") &&
                    (scheduleLower.contains("-") || scheduleLower.contains("–")) -> {
                days.addAll(listOf("selasa", "rabu", "kamis", "jumat", "sabtu"))
            }
            // Rabu sampai Minggu
            scheduleLower.contains("rabu") && scheduleLower.contains("minggu") &&
                    (scheduleLower.contains("-") || scheduleLower.contains("–")) -> {
                days.addAll(listOf("rabu", "kamis", "jumat", "sabtu", "minggu"))
            }
            // Jumat sampai Minggu
            scheduleLower.contains("jumat") && scheduleLower.contains("minggu") &&
                    (scheduleLower.contains("-") || scheduleLower.contains("–")) -> {
                days.addAll(listOf("jumat", "sabtu", "minggu"))
            }
            // Parse individual days jika tidak ada range
            else -> {
                if (scheduleLower.contains("senin")) days.add("senin")
                if (scheduleLower.contains("selasa")) days.add("selasa")
                if (scheduleLower.contains("rabu")) days.add("rabu")
                if (scheduleLower.contains("kamis")) days.add("kamis")
                if (scheduleLower.contains("jumat")) days.add("jumat")
                if (scheduleLower.contains("sabtu")) days.add("sabtu")
                if (scheduleLower.contains("minggu")) days.add("minggu")
            }
        }

        return days
    }

    // Helper: Get day name in Indonesian
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Unknown"
        }
    }

    private fun setupBookingButton() {
        btnConfirmBooking.setOnClickListener {
            if (validateBookingData()) {
                createBooking()
            }
        }
    }

    private fun handlePreSelectedSpecialization() {
        arguments?.getInt("selected_specialization_id")?.let { specId ->
            if (specId > 0) {
                Log.d(TAG, "Pre-selected specialization: $specId")
                selectedSpecializationId = specId

                view?.postDelayed({
                    spinnerSpecialization.setSelection(specId)
                }, 300)
            }
        }
    }

    private fun validateBookingData(): Boolean {
        if (!isDataLoaded) {
            Toast.makeText(requireContext(), "⏳ Data sedang dimuat...", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spinnerSpecialization.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "❌ Pilih layanan klinik", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spinnerDoctor.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "❌ Pilih dokter", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDoctor == null) {
            Toast.makeText(requireContext(), "❌ Dokter tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), "❌ Pilih tanggal", Toast.LENGTH_SHORT).show()
            return false
        }

        if (spinnerTime.selectedItemPosition == 0) {
            Toast.makeText(requireContext(), "❌ Pilih jam", Toast.LENGTH_SHORT).show()
            return false
        }

        val patientName = etPatientName.text.toString().trim()
        if (patientName.isEmpty()) {
            etPatientName.error = "Nama harus diisi"
            etPatientName.requestFocus()
            return false
        }

        if (patientName.length < 3) {
            etPatientName.error = "Nama minimal 3 karakter"
            etPatientName.requestFocus()
            return false
        }

        val complaint = etComplaint.text.toString().trim()
        if (complaint.isEmpty()) {
            etComplaint.error = "Keluhan harus diisi"
            etComplaint.requestFocus()
            return false
        }

        return true
    }

    private fun createBooking() {
        try {
            showLoading(true)
            btnConfirmBooking.isEnabled = false

            if (selectedDoctor == null) {
                Toast.makeText(requireContext(), "❌ Dokter tidak valid", Toast.LENGTH_SHORT).show()
                showLoading(false)
                btnConfirmBooking.isEnabled = true
                return
            }

            val specialization = DataSource.getSpecializations().find { it.id == selectedSpecializationId }

            val selectedDateBookings = DataSource.getBookingHistory().filter { it.date == selectedDate }
            val baseQueueNumber = (5..50).random()
            val queueNumber = baseQueueNumber + selectedDateBookings.size

            // Get selected time from spinner
            val timeSlots = DataSource.getTimeSlotsForDoctor(selectedDoctor!!)
            val selectedTimeIndex = spinnerTime.selectedItemPosition - 1
            val selectedTime = if (selectedTimeIndex >= 0 && selectedTimeIndex < timeSlots.size) {
                timeSlots[selectedTimeIndex]
            } else {
                "08:00"
            }

            val booking = Booking(
                id = "Q${queueNumber.toString().padStart(3, '0')}",
                queueNumber = queueNumber,
                patientName = etPatientName.text.toString().trim(),
                doctorName = selectedDoctor!!.name,
                specialization = specialization?.name ?: "",
                date = selectedDate,
                time = selectedTime,
                complaint = etComplaint.text.toString().trim(),
                diagnosis = "",
                prescription = "",
                status = BookingStatus.WAITING,
                createdAt = System.currentTimeMillis()
            )

            DataSource.setActiveBooking(booking)
            DataSource.addToHistory(booking)

            showLoading(false)
            btnConfirmBooking.isEnabled = true

            Toast.makeText(
                requireContext(),
                "✅ Booking berhasil!\nNo. Antrian: $queueNumber\nJam: $selectedTime",
                Toast.LENGTH_LONG
            ).show()

            (activity as? MainActivity)?.navigateToFragment(QueueFragment())

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            showLoading(false)
            btnConfirmBooking.isEnabled = true

            Toast.makeText(
                requireContext(),
                "❌ Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnConfirmBooking.alpha = 0.5f
            btnConfirmBooking.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnConfirmBooking.alpha = 1.0f
            btnConfirmBooking.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded && selectedSpecializationId > 0) {
            loadDoctors(selectedSpecializationId)
        }
    }
}