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
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper


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
    // DATE PICKER ✔ ENHANCED WITH VALIDATION
    // ======================================================
    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {

            if (selectedDoctor == null) {
                toast("Pilih dokter dahulu")
                return@setOnClickListener
            }

            val cal = Calendar.getInstance()
            val minDate = cal.timeInMillis // Hari ini

            val picker = DatePickerDialog(
                requireContext(),
                { _, y, m, d ->

                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().apply {
                            set(y, m, d)
                        }.time)

                    // ✅ VALIDASI: Check apakah dokter bekerja di hari ini
                    if (!selectedDoctor!!.isWorkingOn(selectedDate)) {
                        showDoctorOffDialog()
                        selectedDate = ""
                        tvSelectedDate.visibility = View.GONE
                        clearTimeSpinner()
                        return@DatePickerDialog
                    }

                    tvSelectedDate.text = selectedDate
                    tvSelectedDate.visibility = View.VISIBLE

                    updateTimeSpinner()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            // Set minimum date ke hari ini (tidak bisa pilih tanggal lampau)
            picker.datePicker.minDate = minDate

            picker.show()
        }
    }

    // ======================================================
    // DIALOG DOKTER TIDAK TERSEDIA
    // ======================================================
    private fun showDoctorOffDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("❌ Dokter Tidak Tersedia")
            .setMessage("""
                Maaf, ${selectedDoctor?.name} tidak praktik di hari yang Anda pilih.
                
                📅 Jadwal Praktik:
                ${selectedDoctor?.getWorkingDaysString()}
                
                ⏰ Jam Praktik:
                ${selectedDoctor?.getWorkingHours()}
                
                Silakan pilih tanggal lain sesuai jadwal praktik dokter.
            """.trimIndent())
            .setPositiveButton("Pilih Lagi") { dialog, _ ->
                dialog.dismiss()
                // Trigger date picker lagi
                btnSelectDate.performClick()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ======================================================
    // TIME SPINNER ✔ ENHANCED WITH VALIDATION
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

            // ✅ Show info jika tidak ada slot tersedia
            if (currentTimeSlots.size == 1) {
                toast("⚠️ Tidak ada jam praktik tersedia untuk tanggal ini")
            }
        }
    }

    private fun generateValidTimeSlots(): List<String> {

        if (selectedDoctor == null || selectedDate.isEmpty()) {
            return emptyList()
        }

        // ✅ Get time slots dari jadwal dokter
        val doctorSlots = selectedDoctor!!.getAvailableTimeSlots(selectedDate)

        if (doctorSlots.isEmpty()) {
            return emptyList()
        }

        // ✅ Filter berdasarkan waktu sekarang (jika pilih hari ini)
        val result = mutableListOf<String>()
        val isToday = isSelectedDateToday()

        for (slot in doctorSlots) {
            if (isToday) {
                // Hanya tampilkan jam yang belum lewat
                if (isTimeFuture(slot)) {
                    result.add(slot)
                }
            } else {
                // Untuk hari lain, tampilkan semua jam
                result.add(slot)
            }
        }

        return result
    }

    // ======================================================
    // TIME VALIDATION HELPERS
    // ======================================================
    private fun isSelectedDateToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return selectedDate == today
    }

    private fun isTimeFuture(time: String): Boolean {
        return try {
            val currentTime = Calendar.getInstance()
            val currentMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 +
                    currentTime.get(Calendar.MINUTE)

            val timeParts = time.split(":")
            val timeMinutes = timeParts[0].toInt() * 60 + timeParts[1].toInt()

            timeMinutes > currentMinutes
        } catch (e: Exception) {
            true // Default: allow jika ada error
        }
    }

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

        // ✅ VALIDASI: Double check dokter bekerja di hari ini
        if (!selectedDoctor!!.isWorkingOn(selectedDate)) {
            showDoctorOffDialog()
            return false
        }

        if (spinnerTime.selectedItemPosition == 0) {
            toast("Pilih jam")
            return false
        }

        val selectedTime = spinnerTime.selectedItem.toString()

        // ✅ VALIDASI: Check jam masih valid (belum lewat)
        if (isSelectedDateToday() && !isTimeFuture(selectedTime)) {
            AlertDialog.Builder(requireContext())
                .setTitle("⏰ Jam Sudah Lewat")
                .setMessage("Jam yang Anda pilih sudah lewat. Silakan pilih jam yang lain.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            return false
        }

        // ✅ VALIDASI: Check jam sesuai dengan jadwal dokter
        if (!selectedDoctor!!.isTimeValid(selectedTime, selectedDate)) {
            AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Jam Tidak Sesuai")
                .setMessage("""
                    Jam yang Anda pilih tidak sesuai dengan jadwal praktik dokter.
                    
                    ⏰ Jam Praktik ${selectedDoctor?.name}:
                    ${selectedDoctor?.getWorkingHours()}
                """.trimIndent())
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
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
    // CREATE BOOKING - ✅ FIXED: Queue number per dokter & tanggal
    // ======================================================
    private fun createBooking() {

        // ✅ STEP 1: Get current logged in user ID
        val currentUserId = PreferencesHelper.getUserId(requireContext())

        if (currentUserId.isNullOrEmpty()) {
            toast("❌ Silakan login terlebih dahulu")
            Log.e("BookingFragment", "❌ User not logged in!")
            // Navigate to login
            (activity as? MainActivity)?.navigateToFragment(LoginFragment())
            return
        }

        Log.d("BookingFragment", "👤 Creating booking for userId: $currentUserId")

        // ✅ FIX: Queue number berdasarkan DOKTER + TANGGAL yang sama (bukan semua booking)
        val queueNumber = DataSource.getBookingHistory().count { booking ->
            booking.date == selectedDate &&
                    booking.doctorName == selectedDoctor!!.name &&  // ✅ SAME DOCTOR
                    booking.status != BookingStatus.CANCELLED  // ✅ Exclude cancelled bookings
        } + 1

        Log.d("BookingFragment", "📊 Queue calculation:")
        Log.d("BookingFragment", "  - Date: $selectedDate")
        Log.d("BookingFragment", "  - Doctor: ${selectedDoctor!!.name}")
        Log.d("BookingFragment", "  - Assigned Queue #: $queueNumber")

        // ✅ Generate unique booking ID dengan format: Q{doctorInitial}_{date}_{queueNumber}
        val doctorInitial = selectedDoctor!!.name.take(2).uppercase()
        val dateShort = selectedDate.replace("-", "")
        val bookingId = "Q${doctorInitial}_${dateShort}_${queueNumber.toString().padStart(3, '0')}"

        // ✅ STEP 2: Create booking WITH userId
        val booking = Booking(
            id = bookingId,
            queueNumber = queueNumber,
            patientName = etPatientName.text.toString(),
            doctorName = selectedDoctor!!.name,
            specialization = selectedDoctor!!.specialization,
            date = selectedDate,
            time = spinnerTime.selectedItem.toString(),
            complaint = etComplaint.text.toString(),
            diagnosis = "",
            prescription = "",
            status = BookingStatus.WAITING,  // ✅ Always start as WAITING
            createdAt = System.currentTimeMillis(),
            firebaseId = bookingId,
            userId = currentUserId  // ✅ CRITICAL: Set userId!
        )

        Log.d("BookingFragment", "📋 New booking:")
        Log.d("BookingFragment", "  - Booking ID: ${booking.id}")
        Log.d("BookingFragment", "  - Queue #: ${booking.queueNumber}")
        Log.d("BookingFragment", "  - Patient: ${booking.patientName}")
        Log.d("BookingFragment", "  - Doctor: ${booking.doctorName}")
        Log.d("BookingFragment", "  - Date: ${booking.date}")
        Log.d("BookingFragment", "  - Status: ${booking.status}")
        Log.d("BookingFragment", "  - User ID: $currentUserId")

        showLoading(true)

        BookingRepository.addBooking(booking) { success ->

            if (!isAdded) return@addBooking

            requireActivity().runOnUiThread {

                showLoading(false)

                if (!success) {
                    toast("❌ Gagal simpan booking")
                    Log.e("BookingFragment", "❌ Failed to save booking")
                    return@runOnUiThread
                }

                Log.d("BookingFragment", "✅ Booking saved successfully!")

                DataSource.setActiveBooking(booking)
                // ✅ Note: setActiveBooking sudah memanggil addToHistory, jangan duplikat
                // DataSource.addToHistory(booking)  // ← HAPUS INI untuk mencegah duplikat

                toast("✅ Booking berhasil! Antrian Anda: #$queueNumber")

                (activity as? MainActivity)
                    ?.navigateToFragment(QueueFragment())
            }
        }
    }

    // ======================================================
    // HELPER METHODS
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