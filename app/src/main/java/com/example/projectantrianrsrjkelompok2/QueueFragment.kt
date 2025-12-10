package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.ml.EnhancedQueuePredictionModel
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.example.projectantrianrsrjkelompok2.utils.QRCodeGenerator
import com.example.projectantrianrsrjkelompok2.utils.ReceiptGenerator
import com.example.projectantrianrsrjkelompok2.utils.RealTimeQueueManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class QueueFragment : Fragment() {

    // ===== Views =====
    private lateinit var tvCurrentQueue: TextView
    private lateinit var tvMyQueueNumber: TextView
    private lateinit var tvMyQueueStatus: TextView
    private lateinit var tvEstimatedTime: TextView
    private lateinit var tvDoctorInfo: TextView
    private lateinit var tvQueueList: TextView
    private lateinit var cardMyQueue: CardView
    private lateinit var btnRefresh: Button
    private lateinit var btnCancelQueue: Button
    private lateinit var btnDownloadReceipt: Button
    private lateinit var btnCompleteQueue: Button
    private lateinit var progressBar: ProgressBar

    // ===== State Variables =====
    private var predictionModel: EnhancedQueuePredictionModel? = null
    private var currentUserBooking: Booking? = null
    private var currentUserId: String? = null
    private var userRole: String = "patient" // Default: patient
    private var firebaseListener: ValueEventListener? = null
    private var isMonitoring = false

    companion object {
        private const val TAG = "QueueFragment"
        private const val AVG_SERVICE_TIME = 8f // 8 menit per pasien
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initMLModel()

        // Get user role first
        userRole = PreferencesHelper.getUserRole(requireContext()) ?: "patient"
        Log.d(TAG, "👤 User role: $userRole")

        setupButtons()
        loadActiveBooking()
    }

    private fun initViews(view: View) {
        // Main info views
        tvCurrentQueue = view.findViewById(R.id.tv_current_queue)
        tvMyQueueNumber = view.findViewById(R.id.tv_my_queue_number)
        tvMyQueueStatus = view.findViewById(R.id.tv_my_queue_status)
        tvEstimatedTime = view.findViewById(R.id.tv_estimated_time)
        tvDoctorInfo = view.findViewById(R.id.tv_doctor_info)
        tvQueueList = view.findViewById(R.id.tv_queue_list)

        // Card container
        cardMyQueue = view.findViewById(R.id.card_my_queue)

        // Buttons
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnCancelQueue = view.findViewById(R.id.btn_cancel_queue)
        btnDownloadReceipt = view.findViewById(R.id.btn_download_receipt)
        btnCompleteQueue = view.findViewById(R.id.btn_complete_queue)

        // Progress
        progressBar = view.findViewById(R.id.progress_bar)

        clearAllTexts()
    }

    private fun initMLModel() {
        try {
            predictionModel = EnhancedQueuePredictionModel(requireContext())
            Log.d(TAG, "✅ ML Model initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to init ML model: ${e.message}", e)
        }
    }

    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            loadActiveBooking()
        }

        btnDownloadReceipt.setOnClickListener {
            currentUserBooking?.let { booking ->
                generateReceipt(booking)
            }
        }

        btnCompleteQueue.setOnClickListener {
            currentUserBooking?.let { booking ->
                confirmCompleteBooking(booking)
            }
        }

        btnCancelQueue.setOnClickListener {
            currentUserBooking?.let { booking ->
                confirmCancelBooking(booking)
            }
        }
    }

    // ============================================================
    // LOAD ACTIVE BOOKING (with Firebase real-time listener)
    // ============================================================

    private fun loadActiveBooking() {
        currentUserId = PreferencesHelper.getUserId(requireContext())

        if (currentUserId.isNullOrEmpty()) {
            toast("❌ Silakan login terlebih dahulu")
            (activity as? MainActivity)?.navigateToFragment(LoginFragment())
            return
        }

        showLoading(true)
        Log.d(TAG, "🔍 Loading active booking for user: $currentUserId")

        // Remove old listener if exists
        firebaseListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("bookings")
                .removeEventListener(it)
        }

        val ref = FirebaseDatabase.getInstance()
            .getReference("bookings")

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val userBookings = snapshot.children.mapNotNull {
                    it.getValue(Booking::class.java)?.copy(firebaseId = it.key ?: "")
                }

                Log.d(TAG, "📋 Found ${userBookings.size} total bookings")

                // Find active booking for this user
                val activeBooking = userBookings.firstOrNull {
                    it.userId == currentUserId &&
                            (it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED)
                }

                if (activeBooking != null) {
                    Log.d(TAG, "✅ Active booking found: ${activeBooking.id}")
                    currentUserBooking = activeBooking

                    // Load today's queue data for ML prediction
                    loadTodayQueueData(activeBooking)
                } else {
                    Log.d(TAG, "📭 No active booking found")
                    displayEmptyQueue()
                }

                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Log.e(TAG, "❌ Firebase error: ${error.message}")
                showLoading(false)
                displayEmptyQueue()
            }
        }

        ref.orderByChild("userId")
            .equalTo(currentUserId)
            .addValueEventListener(firebaseListener!!)
    }

    private fun loadTodayQueueData(activeBooking: Booking) {
        lifecycleScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                withContext(Dispatchers.IO) {
                    DataSource.forceLoadFromFirebase()
                    delay(300)
                }

                // Get all bookings for same doctor today
                val allBookingsForDoctor = DataSource.getBookingHistory()
                    .filter {
                        it.date == today &&
                                it.doctorName == activeBooking.doctorName &&
                                (it.status == BookingStatus.WAITING ||
                                        it.status == BookingStatus.CALLED ||
                                        it.status == BookingStatus.COMPLETED)
                    }
                    .sortedBy { it.queueNumber }

                Log.d(TAG, "📊 All bookings for ${activeBooking.doctorName} today: ${allBookingsForDoctor.size}")

                val currentQueue = calculateCurrentQueueGlobal(allBookingsForDoctor)

                withContext(Dispatchers.Main) {
                    displayActiveQueue(activeBooking, currentQueue, allBookingsForDoctor)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading queue data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    toast("⚠️ Error loading data")
                }
            }
        }
    }

    // ============================================================
    // DISPLAY QUEUE INFO
    // ============================================================

    private fun displayActiveQueue(
        booking: Booking,
        currentQueue: Int,
        allBookings: List<Booking>
    ) {
        cardMyQueue.visibility = View.VISIBLE

        // Update doctor info
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val timeStr = timeFormat.format(Date())

        tvDoctorInfo.text = "${booking.doctorName} - ${booking.specialization}\n" +
                "📅 $dateStr | 🕘 $timeStr WIB"

        // Display current queue (global)
        val displayCurrentQueue = if (currentQueue == 0) {
            allBookings.filter {
                it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
            }.minOfOrNull { it.queueNumber } ?: 1
        } else {
            currentQueue
        }

        tvCurrentQueue.text = displayCurrentQueue.toString()

        // My queue number
        val myQueueNumber = booking.queueNumber
        tvMyQueueNumber.text = "No. $myQueueNumber"

        // Calculate patients ahead
        val patientsAhead = allBookings.count { b ->
            b.queueNumber < myQueueNumber &&
                    (b.status == BookingStatus.WAITING || b.status == BookingStatus.CALLED)
        }

        Log.d(TAG, "📊 My queue: $myQueueNumber, Current: $displayCurrentQueue, Patients ahead: $patientsAhead")

        // Update status based on position
        updateQueueStatus(booking, displayCurrentQueue, myQueueNumber, patientsAhead)

        // Prediction with ML
        predictWaitTime(booking, patientsAhead)

        // Update queue list
        updateQueueList(allBookings, displayCurrentQueue, myQueueNumber)

        // Update button visibility based on role
        updateButtonsVisibility(booking.status)
    }

    private fun updateQueueStatus(
        booking: Booking,
        currentQueue: Int,
        myQueue: Int,
        patientsAhead: Int
    ) {
        when {
            booking.status == BookingStatus.CALLED -> {
                tvMyQueueStatus.text = "DIPANGGIL"
                tvMyQueueStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                cardMyQueue.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
            }
            myQueue < currentQueue -> {
                tvMyQueueStatus.text = "Terlewat"
                tvMyQueueStatus.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
                cardMyQueue.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
            }
            patientsAhead == 0 -> {
                tvMyQueueStatus.text = "Giliran Anda"
                tvMyQueueStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                cardMyQueue.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
            }
            patientsAhead == 1 -> {
                tvMyQueueStatus.text = "Siap-siap (1 pasien lagi)"
                tvMyQueueStatus.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"))
                cardMyQueue.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"))
            }
            else -> {
                tvMyQueueStatus.text = "Menunggu ($patientsAhead pasien di depan)"
                tvMyQueueStatus.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                cardMyQueue.setCardBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
            }
        }
    }

    private fun displayEmptyQueue() {
        cardMyQueue.visibility = View.GONE

        toast("Tidak ada antrian aktif")

        // Navigate to booking or history
        // (activity as? MainActivity)?.navigateToFragment(BookingFragment())
    }

    // ============================================================
    // ROLE-BASED BUTTON VISIBILITY
    // ============================================================

    private fun updateButtonsVisibility(status: BookingStatus) {
        when (userRole) {
            "doctor", "admin" -> {
                // Doctor & Admin: Can complete and cancel
                btnCompleteQueue.visibility = View.VISIBLE
                btnCancelQueue.visibility = View.VISIBLE
                btnDownloadReceipt.visibility = View.VISIBLE

                Log.d(TAG, "🩺 Doctor/Admin view - All buttons enabled")
            }
            "patient" -> {
                // Patient: Can only cancel and download receipt (not complete)
                btnCompleteQueue.visibility = View.GONE  // ✅ HIDE COMPLETE BUTTON
                btnCancelQueue.visibility = View.VISIBLE
                btnDownloadReceipt.visibility = View.VISIBLE

                Log.d(TAG, "👤 Patient view - Complete button hidden")
            }
            else -> {
                // Unknown role: Limited access
                btnCompleteQueue.visibility = View.GONE
                btnCancelQueue.visibility = View.VISIBLE
                btnDownloadReceipt.visibility = View.VISIBLE

                Log.w(TAG, "⚠️ Unknown role: $userRole - Limited access")
            }
        }
    }

    // ============================================================
    // ML PREDICTION
    // ============================================================

    private fun predictWaitTime(booking: Booking, patientsAhead: Int) {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (patientsAhead <= 0) {
                tvEstimatedTime.text = "Giliran Anda SEKARANG!\n" +
                        "Silakan menuju ruang pemeriksaan"
                return
            }

            val prediction = predictionModel?.predictWithConfidence(
                patientsAhead = patientsAhead,
                currentHour = currentHour,
                dayOfWeek = dayOfWeek,
                specialization = booking.specialization,
                avgServiceTime = AVG_SERVICE_TIME,
                queueNumber = booking.queueNumber
            )

            if (prediction != null) {
                val waitMinutes = prediction.predictedMinutes.toInt()

                val turnTime = Calendar.getInstance()
                turnTime.add(Calendar.MINUTE, waitMinutes)
                val turnTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(turnTime.time)

                val estimasiText = when {
                    patientsAhead == 1 -> {
                        "Antrian berikutnya (~$waitMinutes menit)\n" +
                                "Estimasi giliran: $turnTimeStr WIB"
                    }
                    patientsAhead <= 3 -> {
                        "Sebentar lagi (~$waitMinutes menit)\n" +
                                "$patientsAhead pasien di depan\n" +
                                "Estimasi giliran: $turnTimeStr WIB"
                    }
                    waitMinutes < 30 -> {
                        "Estimasi: ~$waitMinutes menit\n" +
                                "$patientsAhead pasien di depan\n" +
                                "Giliran Anda: $turnTimeStr WIB"
                    }
                    else -> {
                        val hours = waitMinutes / 60
                        val mins = waitMinutes % 60
                        val timeStr = if (hours > 0) {
                            "$hours jam $mins menit"
                        } else {
                            "$waitMinutes menit"
                        }
                        "Estimasi: ~$timeStr\n" +
                                "$patientsAhead pasien di depan\n" +
                                "Giliran Anda: $turnTimeStr WIB"
                    }
                }

                tvEstimatedTime.text = estimasiText

            } else {
                // Fallback calculation
                val waitMinutes = (patientsAhead * AVG_SERVICE_TIME).toInt()
                val turnTime = Calendar.getInstance()
                turnTime.add(Calendar.MINUTE, waitMinutes)
                val turnTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(turnTime.time)

                tvEstimatedTime.text = "Estimasi: ~$waitMinutes menit\n" +
                        "$patientsAhead pasien di depan\n" +
                        "Giliran Anda: $turnTimeStr WIB"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error predicting: ${e.message}", e)
            val waitMinutes = (patientsAhead * AVG_SERVICE_TIME).toInt()
            tvEstimatedTime.text = "Estimasi: ~$waitMinutes menit ($patientsAhead pasien)"
        }
    }

    // ============================================================
    // QUEUE CALCULATION
    // ============================================================

    private fun calculateCurrentQueueGlobal(bookings: List<Booking>): Int {
        if (bookings.isEmpty()) return 0

        Log.d(TAG, "=== CALCULATE CURRENT QUEUE (GLOBAL) ===")

        val activeBookings = bookings.filter {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        }.sortedBy { it.queueNumber }

        Log.d(TAG, "Total bookings for doctor today: ${bookings.size}")
        Log.d(TAG, "Active bookings (WAITING/CALLED): ${activeBookings.size}")

        if (activeBookings.isEmpty()) {
            val maxQueue = bookings.maxOfOrNull { it.queueNumber } ?: 0
            Log.d(TAG, "✅ All completed. Max queue was: $maxQueue")
            return maxQueue
        }

        // Priority 1: Currently CALLED
        val calledBooking = activeBookings.find { it.status == BookingStatus.CALLED }
        if (calledBooking != null) {
            Log.d(TAG, "📢 CALLED booking found: Queue #${calledBooking.queueNumber}")
            return calledBooking.queueNumber
        }

        // Priority 2: First WAITING
        val firstWaiting = activeBookings.firstOrNull { it.status == BookingStatus.WAITING }
        if (firstWaiting != null) {
            Log.d(TAG, "⏳ First WAITING: Queue #${firstWaiting.queueNumber}")
            return firstWaiting.queueNumber
        }

        val first = activeBookings.firstOrNull()?.queueNumber ?: 1
        Log.d(TAG, "⚠️ Fallback to: $first")
        return first
    }

    private fun updateQueueList(
        bookings: List<Booking>,
        currentQueue: Int,
        myQueue: Int
    ) {
        val listText = StringBuilder()

        val allBookings = bookings.sortedBy { it.queueNumber }
        val activeCount = allBookings.count {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        }

        if (allBookings.isEmpty()) {
            listText.append("Tidak ada antrian hari ini")
        } else {
            listText.append("Total: ${allBookings.size} pasien ($activeCount aktif)\n\n")

            allBookings.forEach { booking ->
                val statusIcon = when (booking.status) {
                    BookingStatus.COMPLETED -> "✅"
                    BookingStatus.CALLED -> "🔔"
                    BookingStatus.CANCELLED -> "❌"
                    BookingStatus.MISSED -> "⚠️"
                    else -> if (booking.queueNumber == myQueue) "👤" else "⏳"
                }

                val highlight = if (booking.queueNumber == myQueue) " ← ANDA" else ""
                val statusText = if (booking.status == BookingStatus.COMPLETED) " (selesai)" else ""
                listText.append("$statusIcon ${booking.queueNumber}. ${booking.patientName}$highlight$statusText\n")
            }
        }

        tvQueueList.text = listText.toString()
    }

    // ============================================================
    // COMPLETE QUEUE (Doctor/Admin only)
    // ============================================================

    private fun confirmCompleteBooking(booking: Booking) {
        // ✅ DOUBLE CHECK: Only doctor/admin can complete
        if (userRole != "doctor" && userRole != "admin") {
            toast("❌ Hanya dokter dan admin yang dapat menyelesaikan antrian")
            Log.w(TAG, "⚠️ Unauthorized complete attempt by role: $userRole")
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selesaikan Antrian?")
            .setMessage("""
                Pindahkan antrian #${booking.queueNumber} ke riwayat?
                
                Pasien: ${booking.patientName}
                Dokter: ${booking.doctorName}
                
                Tindakan ini tidak dapat dibatalkan.
            """.trimIndent())
            .setPositiveButton("Ya, Selesai") { _, _ ->
                completeBooking(booking)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun completeBooking(booking: Booking) {
        showLoading(true)
        Log.d(TAG, "🏁 Completing booking: ${booking.id}")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataSource.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
                    delay(500)
                    DataSource.forceLoadFromFirebase()
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    toast("✅ Antrian selesai dan dipindahkan ke riwayat")
                    Log.d(TAG, "✅ Booking completed: ${booking.id}")

                    // Refresh to show next queue
                    loadActiveBooking()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error completing: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    toast("❌ Gagal menyelesaikan antrian")
                }
            }
        }
    }

    // ============================================================
    // CANCEL QUEUE
    // ============================================================

    private fun confirmCancelBooking(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("Batalkan Antrian?")
            .setMessage("""
                Anda yakin ingin membatalkan antrian #${booking.queueNumber}?
                
                Pasien: ${booking.patientName}
                Dokter: ${booking.doctorName}
                Tanggal: ${booking.date}
                Jam: ${booking.time}
                
                Tindakan ini tidak dapat dibatalkan.
            """.trimIndent())
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun cancelBooking(booking: Booking) {
        showLoading(true)
        Log.d(TAG, "🚫 Cancelling booking: ${booking.id}")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataSource.updateBookingStatus(booking.id, BookingStatus.CANCELLED)
                    delay(500)
                    DataSource.forceLoadFromFirebase()
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    toast("✅ Antrian berhasil dibatalkan")
                    Log.d(TAG, "✅ Booking cancelled: ${booking.id}")

                    // Refresh to show empty state
                    loadActiveBooking()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cancelling: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    toast("⚠️ Antrian dibatalkan")
                }
            }
        }
    }

    // ============================================================
    // RECEIPT GENERATION
    // ============================================================

    private fun generateReceipt(booking: Booking) {
        try {
            showReceiptDialog(booking)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating receipt: ${e.message}", e)
            toast("❌ Gagal membuat struk")
        }
    }

    private fun showReceiptDialog(booking: Booking) {
        try {
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_receipt_options, null)

            val tvQueueInfo = dialogView.findViewById<TextView>(R.id.tv_queue_info)
            val ivQrCode = dialogView.findViewById<ImageView>(R.id.iv_qr_code)
            val btnDownloadPdf = dialogView.findViewById<Button>(R.id.btn_download_pdf)
            val btnClose = dialogView.findViewById<Button>(R.id.btn_close)

            val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
            val displayDate = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                booking.date
            }

            tvQueueInfo.text = """
Nomor Antrian: ${booking.queueNumber}

Nama: ${booking.patientName}
Dokter: ${booking.doctorName}
Layanan: ${booking.specialization}
Tanggal: $displayDate
Jam: ${booking.time} WIB
            """.trimIndent()

            // Generate QR Code
            val qrContent = QRCodeGenerator.generateBookingQRContent(booking)
            val qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 512, 512)

            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap)
            } else {
                ivQrCode.setImageResource(android.R.drawable.ic_dialog_alert)
                toast("⚠️ Gagal generate QR Code")
            }

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()

            btnDownloadPdf.setOnClickListener {
                downloadReceiptAsPDF(booking)
                toast("💾 Menyimpan struk...")
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing receipt dialog: ${e.message}", e)
            toast("❌ Error: ${e.message}")
        }
    }

    private fun downloadReceiptAsPDF(booking: Booking) {
        try {
            val success = ReceiptGenerator.generateAndSaveReceipt(requireContext(), booking)

            if (success) {
                toast("✅ Struk berhasil disimpan di folder Downloads")
            } else {
                toast("❌ Gagal menyimpan struk")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading PDF: ${e.message}", e)
            toast("❌ Error: ${e.message}")
        }
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    private fun clearAllTexts() {
        tvCurrentQueue.text = ""
        tvMyQueueNumber.text = ""
        tvMyQueueStatus.text = ""
        tvEstimatedTime.text = ""
        tvDoctorInfo.text = ""
        tvQueueList.text = ""
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRefresh.isEnabled = !show
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up Firebase listener
        firebaseListener?.let {
            FirebaseDatabase.getInstance()
                .getReference("bookings")
                .removeEventListener(it)
        }

        // Clean up ML model
        predictionModel?.close()

        // Stop monitoring
        RealTimeQueueManager.stopMonitoring()
        isMonitoring = false

        Log.d(TAG, "🧹 QueueFragment destroyed, resources cleaned")
    }
}