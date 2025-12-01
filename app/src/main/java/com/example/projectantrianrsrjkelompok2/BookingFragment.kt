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

        // Load data and setup UI
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

    // ✅ FIXED: Load data dengan retry mechanism
    private fun loadDataAndSetupUI() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Force load from Firebase dengan retry mechanism
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "🔄 Loading data from Firebase...")

                    // Clear cache dulu
                    DataSource.invalidateCache()

                    // Force reload
                    DataSource.forceLoadFromFirebase()

                    // Tunggu sampai data benar-benar ter-load
                    var retryCount = 0
                    while (DataSource.getAllDoctors().isEmpty() && retryCount < 10) {
                        Log.d(TAG, "⏳ Waiting for data... retry $retryCount")
                        delay(500)
                        retryCount++
                    }
                }

                // Check data dengan logging detail
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

                        // Setup UI components
                        setupSpecializationSpinner()
                        delay(100)
                        setupTimeSpinner()
                        delay(100)
                        setupDatePicker()
                        delay(100)
                        setupBookingButton()

                        // Initialize doctor spinner
                        clearDoctorSpinner()

                        // Handle pre-selected specialization
                        handlePreSelectedSpecialization()

                        Log.d(TAG, "✅ UI Setup Complete")

                        Toast.makeText(
                            requireContext(),
                            "✅ Data berhasil dimuat",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e(TAG, "❌ Data masih kosong setelah load!")

                        // Show detailed error
                        val errorMsg = "❌ Gagal memuat data.\n" +
                                "Doctors: ${allDoctors.size}\n" +
                                "Specs: ${specializations.size}\n\n" +
                                "Coba restart aplikasi atau hubungi admin."

                        Toast.makeText(
                            requireContext(),
                            errorMsg,
                            Toast.LENGTH_LONG
                        ).show()

                        // Offer retry
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

    // ✅ FIXED: Setup spinner dengan proper adapter dan clickable
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

            // ✅ CRITICAL: Gunakan layout yang benar untuk dropdown
            val specAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                specNames
            )
            specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerSpecialization.adapter = specAdapter

            // ✅ CRITICAL: Enable spinner
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
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    clearDoctorSpinner()
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

    // ✅ FIXED: Setup time spinner dengan adapter yang benar
    private fun setupTimeSpinner() {
        try {
            val timeSlots = mutableListOf<String>()
            timeSlots.add("Pilih Jam")
            timeSlots.addAll(DataSource.getTimeSlots())

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

            Log.d(TAG, "✅ Time spinner setup complete: ${timeSlots.size} slots")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting up time spinner: ${e.message}", e)
        }
    }

    private fun loadDoctors(specializationId: Int) {
        doctors.clear()

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
        } else {
            doctors.addAll(filteredDoctors)

            Log.d(TAG, "✅ Doctors loaded:")
            doctors.forEach { doctor ->
                Log.d(TAG, "  - ${doctor.name} (${doctor.specialization})")
            }

            updateDoctorSpinner()
        }
    }

    // ✅ FIXED: Update doctor spinner dengan adapter yang benar
    private fun updateDoctorSpinner() {
        try {
            val doctorNames = mutableListOf<String>()
            doctorNames.add("Pilih Dokter")

            doctors.forEach { doctor ->
                doctorNames.add("${doctor.name} - ${doctor.schedule}")
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

            Log.d(TAG, "✅ Doctor spinner updated: ${doctorNames.size} items")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating doctor spinner: ${e.message}", e)
        }
    }

    private fun clearDoctorSpinner() {
        doctors.clear()

        val emptyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            listOf("Pilih Dokter")
        )
        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerDoctor.adapter = emptyAdapter
        spinnerDoctor.isEnabled = false
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

            datePickerDialog.datePicker.minDate = System.currentTimeMillis()

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

    private fun handlePreSelectedSpecialization() {
        arguments?.getInt("selected_specialization_id")?.let { specId ->
            if (specId > 0) {
                Log.d(TAG, "Pre-selected specialization: $specId")
                selectedSpecializationId = specId

                // Set selection after a small delay
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

        if (doctors.isEmpty()) {
            Toast.makeText(requireContext(), "❌ Tidak ada dokter tersedia", Toast.LENGTH_SHORT).show()
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

            val selectedDoctorIndex = spinnerDoctor.selectedItemPosition - 1
            if (selectedDoctorIndex < 0 || selectedDoctorIndex >= doctors.size) {
                Toast.makeText(requireContext(), "❌ Dokter tidak valid", Toast.LENGTH_SHORT).show()
                showLoading(false)
                btnConfirmBooking.isEnabled = true
                return
            }

            val selectedDoctor = doctors[selectedDoctorIndex]
            val specialization = DataSource.getSpecializations().find { it.id == selectedSpecializationId }

            val selectedDateBookings = DataSource.getBookingHistory().filter { it.date == selectedDate }
            val baseQueueNumber = (5..50).random()
            val queueNumber = baseQueueNumber + selectedDateBookings.size

            val booking = Booking(
                id = "Q${queueNumber.toString().padStart(3, '0')}",
                queueNumber = queueNumber,
                patientName = etPatientName.text.toString().trim(),
                doctorName = selectedDoctor.name,
                specialization = specialization?.name ?: "",
                date = selectedDate,
                time = DataSource.getTimeSlots()[spinnerTime.selectedItemPosition - 1],
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
                "✅ Booking berhasil!\nNo. ${queueNumber}",
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