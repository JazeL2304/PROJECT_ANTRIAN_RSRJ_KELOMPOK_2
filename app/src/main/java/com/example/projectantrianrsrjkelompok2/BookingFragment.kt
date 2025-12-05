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

    // ✅ TAMBAHAN: Flag untuk prevent multiple clicks
    private var isProcessing = false

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
                Log.d(TAG, "Specializations: ${specializations.size}")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (allDoctors.isNotEmpty() && specializations.isNotEmpty()) {
                        isDataLoaded = true

                        setupSpecializationSpinner()
                        setupDatePicker()
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

                        Toast.makeText(
                            requireContext(),
                            "❌ Gagal memuat data. Coba restart aplikasi.",
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
            }

            val specAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                specNames
            )
            specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerSpecialization.adapter = specAdapter
            spinnerSpecialization.isEnabled = true

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
        }
    }

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

    private fun updateDoctorSpinner() {
        try {
            val doctorNames = mutableListOf<String>()
            doctorNames.add("Pilih Dokter")

            doctors.forEach { doctor ->
                val shift = getShiftLabel(doctor.schedule)
                doctorNames.add("${doctor.name}\n📅 ${doctor.schedule} $shift")
            }

            val doctorAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                doctorNames
            )
            doctorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerDoctor.adapter = doctorAdapter
            spinnerDoctor.isEnabled = true

            spinnerDoctor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        selectedDoctor = doctors[position - 1]
                        Log.d(TAG, "✅ Doctor selected: ${selectedDoctor?.name}")

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

    private fun getShiftLabel(schedule: String): String {
        val timePattern = "(\\d{2}:\\d{2})".toRegex()
        val times = timePattern.findAll(schedule).map { it.value }.toList()

        if (times.size < 2) return ""

        val startHour = times[0].split(":")[0].toIntOrNull() ?: 8
        val endHour = times[1].split(":")[0].toIntOrNull() ?: 20

        return when {
            startHour >= 20 || endHour <= 8 -> "🌙"
            else -> "☀️"
        }
    }

    /**
     * ✅ FIXED: Generate time slots berdasarkan jadwal dokter yang sebenarnya
     */
    private fun updateTimeSpinner() {
        try {
            if (selectedDoctor == null) {
                clearTimeSpinner()
                return
            }

            val timeSlots = mutableListOf<String>()
            timeSlots.add("Pilih Jam")

            // ✅ Parse jadwal dokter untuk mendapatkan jam mulai dan selesai
            val schedule = selectedDoctor!!.schedule
            Log.d(TAG, "Parsing schedule: $schedule")

            // ✅ Extract jam dari format: "Senin-Jumat, 08:00-15:00"
            val timePattern = "(\\d{1,2})[:\\.](\\d{2})".toRegex()
            val matches = timePattern.findAll(schedule).toList()

            Log.d(TAG, "Found ${matches.size} time patterns")

            var generatedSlots = false

            if (matches.size >= 2) {
                // Ambil jam pertama (start time) dan jam kedua (end time)
                val startHour = matches[0].groupValues[1].toInt()
                val startMinute = matches[0].groupValues[2].toInt()
                val endHour = matches[1].groupValues[1].toInt()
                val endMinute = matches[1].groupValues[2].toInt()

                val startMinutes = startHour * 60 + startMinute
                var endMinutes = endHour * 60 + endMinute

                // ✅ Handle shift malam (misal 20:00-02:00)
                if (endHour < startHour) {
                    endMinutes += 24 * 60 // tambah 24 jam
                }

                Log.d(TAG, "Time range: ${formatTime(startMinutes)} - ${formatTime(endMinutes)}")
                Log.d(TAG, "Start: $startHour:$startMinute, End: $endHour:$endMinute")

                // ✅ Generate slot waktu setiap 30 menit
                var currentMinutes = startMinutes
                while (currentMinutes < endMinutes) {
                    timeSlots.add(formatTime(currentMinutes))
                    currentMinutes += 30
                }

                generatedSlots = true
                Log.d(TAG, "✅ Generated ${timeSlots.size - 1} time slots from schedule")
            }

            // ✅ Fallback: Jika gagal parse atau slot kosong
            if (!generatedSlots || timeSlots.size <= 1) {
                Log.w(TAG, "⚠️ Could not parse schedule, using fallback slots")

                // Cek keyword untuk shift malam atau siang
                val scheduleLower = schedule.lowercase()

                when {
                    // Shift malam: 20:00 - 02:00
                    scheduleLower.contains("20") ||
                            scheduleLower.contains("malam") ||
                            scheduleLower.contains("21") ||
                            scheduleLower.contains("22") -> {
                        timeSlots.addAll(listOf(
                            "20:00", "20:30", "21:00", "21:30", "22:00",
                            "22:30", "23:00", "23:30", "00:00", "00:30",
                            "01:00", "01:30", "02:00"
                        ))
                        Log.d(TAG, "Using night shift slots")
                    }
                    // Shift sore: 14:00 - 20:00
                    scheduleLower.contains("14") ||
                            scheduleLower.contains("sore") ||
                            scheduleLower.contains("16") -> {
                        timeSlots.addAll(listOf(
                            "14:00", "14:30", "15:00", "15:30", "16:00",
                            "16:30", "17:00", "17:30", "18:00", "18:30",
                            "19:00", "19:30", "20:00"
                        ))
                        Log.d(TAG, "Using afternoon shift slots")
                    }
                    // Default shift pagi: 08:00 - 15:00
                    else -> {
                        timeSlots.addAll(listOf(
                            "08:00", "08:30", "09:00", "09:30", "10:00",
                            "10:30", "11:00", "11:30", "12:00", "12:30",
                            "13:00", "13:30", "14:00", "14:30", "15:00"
                        ))
                        Log.d(TAG, "Using default morning shift slots")
                    }
                }
            }

            // ✅ Update spinner dengan data yang sudah di-generate
            val timeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                timeSlots
            )
            timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerTime.adapter = timeAdapter
            spinnerTime.isEnabled = true

            Log.d(TAG, "✅ Time spinner updated with ${timeSlots.size - 1} slots")
            Log.d(TAG, "Time slots: ${timeSlots.drop(1).joinToString(", ")}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating time spinner: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            clearTimeSpinner()
        }
    }


    /**
     * ✅ Helper: Format minutes to HH:mm
     */
    private fun formatTime(minutes: Int): String {
        val hour = (minutes / 60) % 24
        val min = minutes % 60
        return String.format("%02d:%02d", hour, min)
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

    /**
     * ✅ FIXED: Setup date picker - LIGHTWEIGHT, NO HEAVY PROCESSING
     */
    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {
            // ✅ Prevent multiple clicks
            if (isProcessing) {
                Toast.makeText(
                    requireContext(),
                    "Mohon tunggu...",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (selectedDoctor == null) {
                Toast.makeText(
                    requireContext(),
                    "⚠️ Pilih dokter terlebih dahulu!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()

            // ✅ Create DatePickerDialog - LIGHTWEIGHT
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // ✅ CRITICAL FIX: Process di background thread
                    isProcessing = true

                    lifecycleScope.launch {
                        try {
                            // Show loading
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.VISIBLE
                                btnSelectDate.isEnabled = false
                            }

                            // ✅ Validasi di background dengan timeout
                            val isAvailable = withContext(Dispatchers.Default) {
                                calendar.set(year, month, dayOfMonth)

                                // Simple validation - no heavy processing
                                isDoctorAvailableOnDateSimple(calendar, selectedDoctor!!)
                            }

                            // Update UI di Main thread
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                btnSelectDate.isEnabled = true
                                isProcessing = false

                                if (isAvailable) {
                                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .format(calendar.time)
                                    val displayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                                        .format(calendar.time)

                                    tvSelectedDate.text = displayDate
                                    tvSelectedDate.visibility = View.VISIBLE

                                    updateTimeSpinner()

                                    Toast.makeText(
                                        requireContext(),
                                        "Silakan pilih jam booking",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val dayName = SimpleDateFormat("EEEE", Locale("id", "ID"))
                                        .format(calendar.time)

                                    Toast.makeText(
                                        requireContext(),
                                        "❌ ${selectedDoctor!!.name} tidak praktik pada hari $dayName!",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    selectedDate = ""
                                    tvSelectedDate.text = ""
                                    tvSelectedDate.visibility = View.GONE
                                    clearTimeSpinner()
                                }
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error validating date: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                btnSelectDate.isEnabled = true
                                isProcessing = false

                                Toast.makeText(
                                    requireContext(),
                                    "❌ Error: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Set date range
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.DAY_OF_MONTH, 30)
            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

            // ✅ Show dialog - ini ringan
            datePickerDialog.show()
        }
    }

    /**
     * ✅ SIMPLIFIED: Validasi hari praktik TANPA operasi berat
     */
    private fun isDoctorAvailableOnDateSimple(calendar: Calendar, doctor: Doctor): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val schedule = doctor.schedule.lowercase()

        Log.d(TAG, "Validating: ${getDayName(dayOfWeek)} for ${doctor.name}")

        // ✅ Simple, fast validation
        return when {
            // Senin-Minggu (7 hari)
            schedule.contains("senin") && schedule.contains("minggu") -> true

            // Senin-Sabtu (kecuali Minggu)
            schedule.contains("senin") && schedule.contains("sabtu") ->
                dayOfWeek != Calendar.SUNDAY

            // Senin-Jumat (hari kerja)
            schedule.contains("senin") && schedule.contains("jumat") ->
                dayOfWeek !in listOf(Calendar.SATURDAY, Calendar.SUNDAY)

            // Senin-Kamis
            schedule.contains("senin") && schedule.contains("kamis") ->
                dayOfWeek in listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)

            // Default: allow all days
            else -> true
        }
    }

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

    private fun generateRealisticBookingTime(queueNumber: Int, selectedDate: String): String {
        val clinicOpenHour = 8
        val clinicOpenMinute = 0
        val minutesPerPatient = 8

        val totalMinutes = (queueNumber - 1) * minutesPerPatient
        val bookingHour = clinicOpenHour + (totalMinutes / 60)
        val bookingMinute = clinicOpenMinute + (totalMinutes % 60)

        return String.format("%02d:%02d", bookingHour, bookingMinute)
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

            val selectedDateBookings = DataSource.getBookingHistory()
                .filter { it.date == selectedDate }

            val queueNumber = selectedDateBookings.size + 1

            // ✅ CRITICAL FIX: Gunakan JAM YANG USER PILIH, bukan generate otomatis!
            val selectedTimeSlot = spinnerTime.selectedItem?.toString() ?: ""

            // ✅ Validasi: pastikan user sudah memilih jam
            if (selectedTimeSlot.isEmpty() || selectedTimeSlot == "Pilih Jam") {
                Toast.makeText(requireContext(), "❌ Pilih jam terlebih dahulu!", Toast.LENGTH_SHORT).show()
                showLoading(false)
                btnConfirmBooking.isEnabled = true
                return
            }

            Log.d(TAG, """
            ✅ Booking Info:
            - Queue: $queueNumber
            - Patient: ${etPatientName.text}
            - Doctor: ${selectedDoctor!!.name}
            - Date: $selectedDate
            - Time: $selectedTimeSlot (USER SELECTED)
        """.trimIndent())

            val booking = Booking(
                id = "Q${queueNumber.toString().padStart(3, '0')}",
                queueNumber = queueNumber,
                patientName = etPatientName.text.toString().trim(),
                doctorName = selectedDoctor!!.name,
                specialization = specialization?.name ?: "",
                date = selectedDate,
                time = selectedTimeSlot, // ✅ GUNAKAN JAM YANG USER PILIH!
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

            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            val displayDate = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)
                dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                selectedDate
            }

            Toast.makeText(
                requireContext(),
                "✅ Booking berhasil!\n" +
                        "No. Antrian: $queueNumber\n" +
                        "Tanggal: $displayDate\n" +
                        "Jam: $selectedTimeSlot WIB", // ✅ Tampilkan jam yang benar
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
        try {
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                btnConfirmBooking.alpha = 0.5f
                btnConfirmBooking.isEnabled = false
                btnSelectDate.isEnabled = false
            } else {
                progressBar.visibility = View.GONE
                btnConfirmBooking.alpha = 1.0f
                btnConfirmBooking.isEnabled = true
                btnSelectDate.isEnabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in showLoading: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded && selectedSpecializationId > 0) {
            loadDoctors(selectedSpecializationId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Reset flag
        isProcessing = false
    }
}