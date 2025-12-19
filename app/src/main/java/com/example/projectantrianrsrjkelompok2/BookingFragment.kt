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

    private lateinit var layoutBookingBlocked: LinearLayout
    private lateinit var tvBlockedMessage: TextView
    private lateinit var btnViewQueue: Button
    private lateinit var layoutBookingForm: LinearLayout

    private var selectedDate = ""
    private var selectedSpecializationId = 0
    private var selectedDoctor: Doctor? = null

    private val doctors = mutableListOf<Doctor>()
    private val currentTimeSlots = mutableListOf<String>()

    private var isNightShiftDoctor = false
    private var isProcessing = false
    private var isDataLoaded = false

    private var activeBooking: Booking? = null
    private var hasActiveQueue = false

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
        checkActiveQueueAndSetup()
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

        try {
            layoutBookingBlocked = view.findViewById(R.id.layout_booking_blocked)
            tvBlockedMessage = view.findViewById(R.id.tv_blocked_message)
            btnViewQueue = view.findViewById(R.id.btn_view_queue)
            layoutBookingForm = view.findViewById(R.id.layout_booking_form)
        } catch (e: Exception) {
            Log.e(TAG, "Blocking views not found in layout. Feature disabled.")
        }
    }

    private fun checkActiveQueueAndSetup() {
        val currentUserId = PreferencesHelper.getUserId(requireContext())

        if (currentUserId.isNullOrEmpty()) {
            toast("Silakan login terlebih dahulu")
            (activity as? MainActivity)?.navigateToFragment(LoginFragment())
            return
        }

        showLoading(true)
        Log.d(TAG, "Checking active queue for user: $currentUserId")

        val ref = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("bookings")

        ref.orderByChild("userId")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {

                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (!isAdded) return

                    val userBookings = snapshot.children.mapNotNull {
                        it.getValue(Booking::class.java)?.copy(firebaseId = it.key ?: "")
                    }

                    val active = userBookings.firstOrNull {
                        it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
                    }

                    if (active != null) {
                        activeBooking = active
                        hasActiveQueue = true
                        showBlockedState(active)
                    } else {
                        hasActiveQueue = false
                        showBookingForm()
                    }

                    showLoading(false)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    if (!isAdded) return
                    showLoading(false)
                    hasActiveQueue = false
                    showBookingForm()
                }
            })
    }

    private fun showBlockedState(booking: Booking) {
        try {
            layoutBookingForm.visibility = View.GONE
            layoutBookingBlocked.visibility = View.VISIBLE

            val message = """
            Silakan selesaikan antrian Anda terlebih dahulu sebelum membuat booking baru.
            
            Nomor Antrian: #${booking.queueNumber}
            Status: ${booking.status.toDisplayString()}
        """.trimIndent()

            tvBlockedMessage.text = message

            btnViewQueue.setOnClickListener {
                (activity as? MainActivity)?.navigateToFragment(QueueFragment())
            }

        } catch (e: Exception) {
            showActiveQueueDialog(booking)
        }
    }

    private fun showActiveQueueDialog(booking: Booking) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_active_queue, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvQueueNumber = dialogView.findViewById<TextView>(R.id.tv_queue_number)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tv_status)
        val tvDoctorName = dialogView.findViewById<TextView>(R.id.tv_doctor_name)
        val tvSpecialization = dialogView.findViewById<TextView>(R.id.tv_specialization)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_date)
        val tvTime = dialogView.findViewById<TextView>(R.id.tv_time)
        val tvComplaint = dialogView.findViewById<TextView>(R.id.tv_complaint)
        val btnViewQueue = dialogView.findViewById<Button>(R.id.btn_view_queue)

        tvQueueNumber.text = "#${booking.queueNumber}"
        tvStatus.text = booking.status.toDisplayString()
        tvDoctorName.text = booking.doctorName
        tvSpecialization.text = booking.specialization
        tvDate.text = booking.date
        tvTime.text = booking.time
        tvComplaint.text = booking.complaint

        val statusColor = when (booking.status) {
            BookingStatus.WAITING -> R.color.warning_orange
            BookingStatus.CALLED -> R.color.info_blue
            else -> R.color.text_secondary
        }
        tvStatus.setTextColor(resources.getColor(statusColor, null))

        btnViewQueue.setOnClickListener {
            dialog.dismiss()
            (activity as? MainActivity)?.navigateToFragment(QueueFragment())
        }

        dialog.show()
    }

    private fun showBookingForm() {
        try {
            layoutBookingBlocked.visibility = View.GONE
            layoutBookingForm.visibility = View.VISIBLE
        } catch (e: Exception) {
        }
        loadUserNameAndSetup()
        loadDataAndSetupUI()
    }

    private fun createBooking() {
        if (hasActiveQueue) {
            toast("Anda sudah memiliki antrian aktif!")
            checkActiveQueueAndSetup()
            return
        }

        val currentUserId = PreferencesHelper.getUserId(requireContext())

        if (currentUserId.isNullOrEmpty()) {
            toast("Silakan login terlebih dahulu")
            (activity as? MainActivity)?.navigateToFragment(LoginFragment())
            return
        }

        val queueNumber = DataSource.getBookingHistory().count { booking ->
            booking.date == selectedDate &&
                    booking.doctorName == selectedDoctor!!.name &&
                    booking.status != BookingStatus.CANCELLED
        } + 1

        val doctorInitial = selectedDoctor!!.name.take(2).uppercase()
        val dateShort = selectedDate.replace("-", "")
        val bookingId = "Q${doctorInitial}_${dateShort}_${queueNumber.toString().padStart(3, '0')}"

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
            status = BookingStatus.WAITING,
            createdAt = System.currentTimeMillis(),
            firebaseId = bookingId,
            userId = currentUserId
        )

        showLoading(true)

        BookingRepository.addBooking(booking) { success ->
            if (!isAdded) return@addBooking

            requireActivity().runOnUiThread {
                showLoading(false)

                if (!success) {
                    toast("Gagal simpan booking")
                    return@runOnUiThread
                }

                DataSource.setActiveBooking(booking)
                toast("Booking berhasil! Antrian Anda: #$queueNumber")

                (activity as? MainActivity)?.navigateToFragment(QueueFragment())
            }
        }
    }

    private fun loadUserNameAndSetup() {
        val userName = PreferencesHelper.getUserFullName(requireContext())

        if (!userName.isNullOrEmpty()) {
            etPatientName.setText(userName)
            etPatientName.isEnabled = false
            etPatientName.isFocusable = false
            etPatientName.isFocusableInTouchMode = false
            etPatientName.alpha = 0.6f
        } else {
            toast("Silakan login terlebih dahulu")
            (activity as? MainActivity)?.navigateToFragment(LoginFragment())
        }
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

    private fun setupSpecializationSpinner() {
        val specializations = DataSource.getSpecializations()
        val names = mutableListOf("Pilih Layanan Klinik")

        // Perbaikan: Hapus ${it.emoji} agar hanya menampilkan nama
        names.addAll(specializations.map { it.name })

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

    private fun loadDoctors(id: Int) {
        doctors.clear()
        doctors.addAll(DataSource.getDoctorsBySpecialization(id))
        updateDoctorSpinner()
    }

    private fun updateDoctorSpinner() {
        val names = mutableListOf("Pilih Dokter")

        // Perbaikan: Hapus emoji kalender dan icon shift
        names.addAll(
            doctors.map {
                "${it.name}\nJadwal: ${it.schedule}"
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

    private fun setupDatePicker() {
        btnSelectDate.setOnClickListener {

            if (selectedDoctor == null) {
                toast("Pilih dokter dahulu")
                return@setOnClickListener
            }

            val cal = Calendar.getInstance()
            val minDate = cal.timeInMillis

            val picker = DatePickerDialog(
                requireContext(),
                { _, y, m, d ->

                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Calendar.getInstance().apply {
                            set(y, m, d)
                        }.time)

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

            picker.datePicker.minDate = minDate
            picker.show()
        }
    }

    private fun showDoctorOffDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Dokter Tidak Tersedia")
            .setMessage("""
                Maaf, ${selectedDoctor?.name} tidak praktik di hari yang Anda pilih.
                
                Jadwal Praktik:
                ${selectedDoctor?.getWorkingDaysString()}
                
                Jam Praktik:
                ${selectedDoctor?.getWorkingHours()}
                
                Silakan pilih tanggal lain sesuai jadwal praktik dokter.
            """.trimIndent())
            .setPositiveButton("Pilih Lagi") { dialog, _ ->
                dialog.dismiss()
                btnSelectDate.performClick()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

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

            if (currentTimeSlots.size == 1) {
                toast("Tidak ada jam praktik tersedia untuk tanggal ini")
            }
        }
    }

    private fun generateValidTimeSlots(): List<String> {
        if (selectedDoctor == null || selectedDate.isEmpty()) {
            return emptyList()
        }

        val doctorSlots = selectedDoctor!!.getAvailableTimeSlots(selectedDate)

        if (doctorSlots.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        val isToday = isSelectedDateToday()

        for (slot in doctorSlots) {
            if (isToday) {
                if (isTimeFuture(slot)) {
                    result.add(slot)
                }
            } else {
                result.add(slot)
            }
        }

        return result
    }

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
            true
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

        if (!selectedDoctor!!.isWorkingOn(selectedDate)) {
            showDoctorOffDialog()
            return false
        }

        if (spinnerTime.selectedItemPosition == 0) {
            toast("Pilih jam")
            return false
        }

        val selectedTime = spinnerTime.selectedItem.toString()

        if (isSelectedDateToday() && !isTimeFuture(selectedTime)) {
            AlertDialog.Builder(requireContext())
                .setTitle("Jam Sudah Lewat")
                .setMessage("Jam yang Anda pilih sudah lewat. Silakan pilih jam yang lain.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            return false
        }

        if (!selectedDoctor!!.isTimeValid(selectedTime, selectedDate)) {
            AlertDialog.Builder(requireContext())
                .setTitle("Jam Tidak Sesuai")
                .setMessage("""
                    Jam yang Anda pilih tidak sesuai dengan jadwal praktik dokter.
                    
                    Jam Praktik ${selectedDoctor?.name}:
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

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmBooking.isEnabled = !show
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}