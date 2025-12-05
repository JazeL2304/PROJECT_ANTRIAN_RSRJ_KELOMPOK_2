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

    // Flag untuk prevent multiple clicks
    private var isProcessing = false

    // Simpan list time slots untuk validasi
    private var currentTimeSlots = mutableListOf<String>()

    // Flag untuk menandai apakah dokter shift malam
    private var isNightShiftDoctor = false

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
        if (!isAdded) return

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

                        // Detect apakah shift malam
                        isNightShiftDoctor = isNightShift(selectedDoctor!!.schedule)
                        Log.d(TAG, "  Is night shift: $isNightShiftDoctor")

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
                        isNightShiftDoctor = false
                        clearTimeSpinner()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedDoctor = null
                    isNightShiftDoctor = false
                    clearTimeSpinner()
                }
            }

            Log.d(TAG, "✅ Doctor spinner updated: ${doctorNames.size} items")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating doctor spinner: ${e.message}", e)
        }
    }

    /**
     * Detect apakah jadwal dokter adalah shift malam
     */
    private fun isNightShift(schedule: String): Boolean {
        val timePattern = "(\\d{1,2})[:\\.](\\d{2})".toRegex()
        val matches = timePattern.findAll(schedule).toList()

        if (matches.size >= 2) {
            val startHour = matches[0].groupValues[1].toInt()
            val endHour = matches[1].groupValues[1].toInt()

            // Night shift jika end hour lebih kecil dari start hour
            // Contoh: 20:00 - 08:00
            return endHour < startHour
        }

        return false
    }

    private fun getShiftLabel(schedule: String): String {
        val timePattern = "(\\d{1,2})[:\\.](\\d{2})".toRegex()
        val matches = timePattern.findAll(schedule).toList()

        if (matches.size >= 2) {
            val startHour = matches[0].groupValues[1].toInt()
            val endHour = matches[1].groupValues[1].toInt()

            // Night shift
            if (endHour < startHour || startHour >= 20) {
                return "🌙"
            }
        }

        return "☀️"
    }

    /**
     * ✅ FIXED: Generate time slots dengan coroutine untuk hindari ANR
     */
    private fun updateTimeSpinner() {
        if (!isAdded) return

        try {
            if (selectedDoctor == null) {
                clearTimeSpinner()
                return
            }

            // Show loading state
            spinnerTime.isEnabled = false

            // Proses di background thread untuk hindari ANR
            lifecycleScope.launch {
                try {
                    val validSlots = withContext(Dispatchers.Default) {
                        generateValidTimeSlots()
                    }

                    // Update UI di Main thread
                    if (!isAdded) return@launch

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext

                        currentTimeSlots.clear()
                        currentTimeSlots.add("Pilih Jam")
                        currentTimeSlots.addAll(validSlots)

                        Log.d(TAG, "📋 Final time slots: ${currentTimeSlots.size - 1} available")

                        // Update spinner
                        val timeAdapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            currentTimeSlots.toList()
                        )
                        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                        spinnerTime.adapter = timeAdapter
                        spinnerTime.isEnabled = currentTimeSlots.size > 1

                        // Listener untuk validasi real-time
                        spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                if (position > 0 && position < currentTimeSlots.size) {
                                    val selectedTime = currentTimeSlots[position]
                                    Log.d(TAG, "Time selected: $selectedTime")
                                }
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }

                        if (currentTimeSlots.size <= 1) {
                            Toast.makeText(
                                requireContext(),
                                "⚠️ Tidak ada jam tersedia. Pilih tanggal lain.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        if (isAdded) clearTimeSpinner()
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating time spinner: ${e.message}", e)
            clearTimeSpinner()
        }
    }

    /**
     * ✅ FIXED: Generate valid time slots dengan handling shift malam yang benar
     */
    private fun generateValidTimeSlots(): List<String> {
        val validSlots = mutableListOf<String>()

        try {
            val doctor = selectedDoctor ?: return validSlots
            val schedule = doctor.schedule

            Log.d(TAG, "🕐 Parsing schedule: $schedule")

            // Parse jam dari schedule
            val timePattern = "(\\d{1,2})[:\\.](\\d{2})".toRegex()
            val matches = timePattern.findAll(schedule).toList()

            if (matches.size >= 2) {
                val startHour = matches[0].groupValues[1].toInt()
                val startMinute = matches[0].groupValues[2].toInt()
                val endHour = matches[1].groupValues[1].toInt()
                val endMinute = matches[1].groupValues[2].toInt()

                Log.d(TAG, "  Parsed: $startHour:$startMinute - $endHour:$endMinute")

                // Detect shift malam (end < start, misal 20:00-08:00)
                val isNightShift = endHour < startHour

                if (isNightShift) {
                    Log.d(TAG, "🌙 Night shift detected: $startHour:00 - $endHour:00")

                    // ========== BAGIAN 1: Jam malam hari ini (20:00 - 23:30) ==========
                    var currentMinutes = startHour * 60 + startMinute
                    while (currentMinutes < 24 * 60) {
                        val slot = formatTime(currentMinutes)

                        // Filter berdasarkan jam HP untuk bagian malam ini
                        if (isTimeSlotValidForNightShift(slot, isAfterMidnight = false)) {
                            validSlots.add(slot)
                            Log.d(TAG, "  ✓ Added (evening): $slot")
                        } else {
                            Log.d(TAG, "  ✗ Filtered (evening): $slot - sudah lewat")
                        }

                        currentMinutes += 30
                    }

                    // ========== BAGIAN 2: Jam dini hari (00:00 - 08:00) ==========
                    // Jam ini untuk hari BERIKUTNYA, jadi tidak perlu filter berdasarkan jam sekarang
                    currentMinutes = 0
                    val endMinutes = endHour * 60 + endMinute
                    while (currentMinutes < endMinutes) {
                        val slot = formatTime(currentMinutes)

                        // Untuk jam setelah tengah malam, validasi berbeda
                        if (isTimeSlotValidForNightShift(slot, isAfterMidnight = true)) {
                            validSlots.add(slot)
                            Log.d(TAG, "  ✓ Added (midnight): $slot")
                        }

                        currentMinutes += 30
                    }

                    Log.d(TAG, "✅ Night shift total slots: ${validSlots.size}")

                } else {
                    // ========== SHIFT SIANG NORMAL ==========
                    Log.d(TAG, "☀️ Day shift detected: $startHour:00 - $endHour:00")

                    val startMinutes = startHour * 60 + startMinute
                    val endMinutes = endHour * 60 + endMinute

                    var currentMinutes = startMinutes
                    while (currentMinutes < endMinutes) {
                        val slot = formatTime(currentMinutes)

                        if (isTimeSlotValidSync(slot)) {
                            validSlots.add(slot)
                            Log.d(TAG, "  ✓ Added: $slot")
                        } else {
                            Log.d(TAG, "  ✗ Filtered: $slot - sudah lewat")
                        }

                        currentMinutes += 30
                    }

                    Log.d(TAG, "✅ Day shift total slots: ${validSlots.size}")
                }

            } else {
                // Default slots jika parsing gagal
                Log.w(TAG, "⚠️ Cannot parse schedule, using default slots (08:00-20:00)")

                val defaultSlots = listOf(
                    "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                    "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
                    "14:00", "14:30", "15:00", "15:30", "16:00", "16:30",
                    "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00"
                )

                defaultSlots.forEach { slot ->
                    if (isTimeSlotValidSync(slot)) {
                        validSlots.add(slot)
                    }
                }
            }

            Log.d(TAG, "📋 Total valid slots: ${validSlots.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating slots: ${e.message}", e)
        }

        return validSlots
    }

    /**
     * ✅ Validasi time slot untuk shift malam
     * @param timeSlot - jam yang akan divalidasi (format HH:mm)
     * @param isAfterMidnight - true jika jam ini setelah tengah malam (00:00-08:00)
     */
    private fun isTimeSlotValidForNightShift(timeSlot: String, isAfterMidnight: Boolean): Boolean {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().time)

            // Jika booking BUKAN hari ini, semua jam valid
            if (selectedDate.isNotEmpty() && selectedDate != today) {
                return true
            }

            // Jika jam setelah tengah malam (00:00-08:00), ini untuk hari berikutnya
            // Jadi SELALU valid karena belum terjadi
            if (isAfterMidnight) {
                Log.d(TAG, "    $timeSlot is after midnight - always valid")
                return true
            }

            // Untuk jam sebelum tengah malam (20:00-23:59), cek dengan jam HP
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)

            val slotParts = timeSlot.split(":")
            if (slotParts.size < 2) return true

            val slotHour = slotParts[0].toIntOrNull() ?: return true
            val slotMinute = slotParts[1].toIntOrNull() ?: return true

            val currentTotal = currentHour * 60 + currentMinute
            val slotTotal = slotHour * 60 + slotMinute

            // Jam harus minimal 30 menit dari sekarang
            val isValid = slotTotal > (currentTotal + 30)

            Log.d(TAG, "    $timeSlot vs now ${String.format("%02d:%02d", currentHour, currentMinute)} -> valid: $isValid")

            return isValid

        } catch (e: Exception) {
            Log.e(TAG, "Error validating night shift time: ${e.message}")
            return true
        }
    }

    /**
     * ✅ Validasi time slot untuk shift siang
     */
    private fun isTimeSlotValidSync(timeSlot: String): Boolean {
        try {
            if (timeSlot == "Pilih Jam") return true

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().time)

            // Jika booking BUKAN hari ini, semua jam valid
            if (selectedDate.isNotEmpty() && selectedDate != today) {
                return true
            }

            // Cek jam HP sekarang
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)

            val slotParts = timeSlot.split(":")
            if (slotParts.size < 2) return true

            val slotHour = slotParts[0].toIntOrNull() ?: return true
            val slotMinute = slotParts[1].toIntOrNull() ?: return true

            val currentTotal = currentHour * 60 + currentMinute
            val slotTotal = slotHour * 60 + slotMinute

            // Jam harus minimal 30 menit dari sekarang
            return slotTotal > (currentTotal + 30)

        } catch (e: Exception) {
            Log.e(TAG, "Error validating time: ${e.message}")
            return true
        }
    }

    /**
     * ✅ Wrapper untuk backward compatibility
     */
    private fun isTimeSlotValid(timeSlot: String): Boolean {
        return if (isNightShiftDoctor) {
            // Untuk shift malam, cek apakah jam setelah midnight
            val slotHour = timeSlot.split(":").firstOrNull()?.toIntOrNull() ?: 0
            val isAfterMidnight = slotHour < 12 // 00:00 - 11:59 dianggap after midnight
            isTimeSlotValidForNightShift(timeSlot, isAfterMidnight)
        } else {
            isTimeSlotValidSync(timeSlot)
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
        isNightShiftDoctor = false

        if (!isAdded) return

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
        currentTimeSlots.clear()

        if (!isAdded) return

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
     * ✅ FIXED: Setup date picker
     */
    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {
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

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    isProcessing = true

                    lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.VISIBLE
                                btnSelectDate.isEnabled = false
                            }

                            val isAvailable = withContext(Dispatchers.Default) {
                                calendar.set(year, month, dayOfMonth)
                                isDoctorAvailableOnDateSimple(calendar, selectedDoctor!!)
                            }

                            if (!isAdded) return@launch

                            withContext(Dispatchers.Main) {
                                if (!isAdded) return@withContext

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

                                    // Update time spinner
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
                                if (!isAdded) return@withContext

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

            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.DAY_OF_MONTH, 30)
            datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

            datePickerDialog.show()
        }
    }

    /**
     * ✅ SIMPLIFIED: Validasi hari praktik
     */
    private fun isDoctorAvailableOnDateSimple(calendar: Calendar, doctor: Doctor): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val schedule = doctor.schedule.lowercase()

        Log.d(TAG, "Validating: ${getDayName(dayOfWeek)} for ${doctor.name}")

        return when {
            schedule.contains("senin") && schedule.contains("minggu") -> true
            schedule.contains("senin") && schedule.contains("sabtu") ->
                dayOfWeek != Calendar.SUNDAY
            schedule.contains("senin") && schedule.contains("jumat") ->
                dayOfWeek !in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            schedule.contains("senin") && schedule.contains("kamis") ->
                dayOfWeek in listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY)
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

        // Validasi final: cek lagi apakah jam masih valid
        val selectedTime = spinnerTime.selectedItem.toString()
        if (!isTimeSlotValid(selectedTime)) {
            Toast.makeText(
                requireContext(),
                "❌ Jam $selectedTime sudah terlewat! Silakan pilih jam lain.",
                Toast.LENGTH_LONG
            ).show()
            updateTimeSpinner()
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

            val selectedDateBookings = DataSource.getBookingHistory()
                .filter { it.date == selectedDate }

            val queueNumber = selectedDateBookings.size + 1

            val selectedTimeSlot = spinnerTime.selectedItem?.toString() ?: ""

            if (selectedTimeSlot.isEmpty() || selectedTimeSlot == "Pilih Jam") {
                Toast.makeText(requireContext(), "❌ Pilih jam terlebih dahulu!", Toast.LENGTH_SHORT).show()
                showLoading(false)
                btnConfirmBooking.isEnabled = true
                return
            }

            // Final check sebelum create
            if (!isTimeSlotValid(selectedTimeSlot)) {
                Toast.makeText(
                    requireContext(),
                    "❌ Jam $selectedTimeSlot sudah terlewat!",
                    Toast.LENGTH_LONG
                ).show()
                showLoading(false)
                btnConfirmBooking.isEnabled = true
                updateTimeSpinner()
                return
            }

            Log.d(TAG, """
                ✅ Booking Info:
                - Queue: $queueNumber
                - Patient: ${etPatientName.text}
                - Doctor: ${selectedDoctor!!.name}
                - Date: $selectedDate
                - Time: $selectedTimeSlot
                - Night Shift: $isNightShiftDoctor
            """.trimIndent())

            val booking = Booking(
                id = "Q${queueNumber.toString().padStart(3, '0')}",
                queueNumber = queueNumber,
                patientName = etPatientName.text.toString().trim(),
                doctorName = selectedDoctor!!.name,
                specialization = specialization?.name ?: "",
                date = selectedDate,
                time = selectedTimeSlot,
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
                        "Jam: $selectedTimeSlot WIB",
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
        if (!isAdded) return

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

        // Refresh time slots saat kembali ke fragment
        if (isDataLoaded && selectedDate.isNotEmpty() && selectedDoctor != null) {
            Log.d(TAG, "🔄 Refreshing time slots on resume...")
            updateTimeSpinner()
        }

        if (isDataLoaded && selectedSpecializationId > 0) {
            loadDoctors(selectedSpecializationId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isProcessing = false
        currentTimeSlots.clear()
        isNightShiftDoctor = false
    }
}