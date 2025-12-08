package com.example.projectantrianrsrjkelompok2

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.ml.EnhancedQueuePredictionModel
import com.example.projectantrianrsrjkelompok2.utils.RealTimeQueueManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.projectantrianrsrjkelompok2.utils.QRCodeGenerator
import com.example.projectantrianrsrjkelompok2.utils.ReceiptGenerator
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper


class QueueFragment : Fragment() {

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

    private var predictionModel: EnhancedQueuePredictionModel? = null
    private var isMonitoring = false

    // ✅ NEW: Store current user's booking
    private var currentUserBooking: Booking? = null
    private var currentUserId: String? = null

    companion object {
        private const val TAG = "QueueFragment"
        private const val AVG_SERVICE_TIME = 8f // 8 menit per pasien
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initMLModel()
        setupButtons()

        // ✅ FIXED: Load data untuk USER yang sedang login
        loadInitialData()
    }

    /**
     * ✅ FIXED: Load data berdasarkan USER yang sedang login
     * Booking di-load dari FIREBASE, bukan dari memory
     */
    private fun loadInitialData() {
        // ✅ STEP 1: Get current user ID
        currentUserId = PreferencesHelper.getUserId(requireContext())

        if (currentUserId.isNullOrEmpty()) {
            Log.e(TAG, "❌ User not logged in!")
            showLoading(false)
            Toast.makeText(requireContext(), "❌ Silakan login", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateToFragment(LoginFragment())
            return
        }

        Log.d(TAG, "👤 Current userId: $currentUserId")

        cardMyQueue.visibility = View.INVISIBLE
        showLoading(true)
        clearAllTexts()

        lifecycleScope.launch {
            try {
                // ✅ STEP 2: Force load dari Firebase
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "📥 Force loading data from Firebase...")
                    DataSource.forceLoadFromFirebase()
                    delay(500)
                }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                // ✅ STEP 3: Cari booking AKTIF untuk USER INI dari Firebase/Cache
                // BUKAN dari DataSource.getActiveBooking() yang bisa ke-overwrite
                val myActiveBooking = findActiveBookingForCurrentUser(currentUserId!!, today)

                if (myActiveBooking == null) {
                    Log.w(TAG, "⚠️ No active booking for user $currentUserId")
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "❌ Tidak ada antrian aktif", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.navigateToFragment(EmptyQueueFragment())
                    }
                    return@launch
                }

                // ✅ Store booking untuk user ini
                currentUserBooking = myActiveBooking
                Log.d(TAG, "✅ Found booking for user $currentUserId: ${myActiveBooking.patientName}, Queue #${myActiveBooking.queueNumber}")

                // ✅ Check if already completed
                if (myActiveBooking.status == BookingStatus.COMPLETED) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Booking sudah selesai", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                    }
                    return@launch
                }

                // ✅ STEP 4: Get ALL bookings untuk DOKTER yang sama (untuk hitung current queue)
                val allBookingsForDoctor = DataSource.getBookingHistory()
                    .filter {
                        it.date == today &&
                                it.doctorName == myActiveBooking.doctorName &&
                                (it.status == BookingStatus.WAITING ||
                                        it.status == BookingStatus.CALLED ||
                                        it.status == BookingStatus.COMPLETED)
                    }
                    .sortedBy { it.queueNumber }

                Log.d(TAG, "📊 All bookings for ${myActiveBooking.doctorName} today: ${allBookingsForDoctor.size}")
                allBookingsForDoctor.forEach { b ->
                    Log.d(TAG, "  - Queue #${b.queueNumber}: ${b.patientName} (${b.status}) [userId: ${b.userId}]")
                }

                // ✅ STEP 5: Calculate current queue based on GLOBAL status
                val currentQueue = calculateCurrentQueueGlobal(allBookingsForDoctor)

                withContext(Dispatchers.Main) {
                    updateQueueInfo(myActiveBooking, currentQueue, allBookingsForDoctor)
                    showLoading(false)
                    startRealTimeMonitoring()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * ✅ NEW: Find active booking untuk user tertentu dari cache/Firebase
     * Ini TIDAK bergantung pada DataSource.activeBooking yang bisa ke-overwrite
     */
    private fun findActiveBookingForCurrentUser(userId: String, date: String): Booking? {
        val allBookings = DataSource.getBookingHistory()

        Log.d(TAG, "🔍 Searching active booking for userId: $userId, date: $date")
        Log.d(TAG, "   Total bookings in cache: ${allBookings.size}")

        val activeBooking = allBookings.find { booking ->
            booking.userId == userId &&
                    booking.date == date &&
                    (booking.status == BookingStatus.WAITING || booking.status == BookingStatus.CALLED)
        }

        if (activeBooking != null) {
            Log.d(TAG, "✅ Found: ${activeBooking.patientName}, Queue #${activeBooking.queueNumber}, Status: ${activeBooking.status}")
        } else {
            Log.d(TAG, "❌ No active booking found")

            // Debug: Show all bookings for this user
            val userBookings = allBookings.filter { it.userId == userId }
            Log.d(TAG, "   All bookings for user $userId: ${userBookings.size}")
            userBookings.forEach { b ->
                Log.d(TAG, "   - ${b.date}: Queue #${b.queueNumber}, Status: ${b.status}")
            }
        }

        return activeBooking
    }

    private fun clearAllTexts() {
        tvCurrentQueue.text = ""
        tvMyQueueNumber.text = ""
        tvMyQueueStatus.text = ""
        tvEstimatedTime.text = ""
        tvDoctorInfo.text = ""
        tvQueueList.text = ""
    }

    private fun initViews(view: View) {
        tvCurrentQueue = view.findViewById(R.id.tv_current_queue)
        tvMyQueueNumber = view.findViewById(R.id.tv_my_queue_number)
        tvMyQueueStatus = view.findViewById(R.id.tv_my_queue_status)
        tvEstimatedTime = view.findViewById(R.id.tv_estimated_time)
        tvDoctorInfo = view.findViewById(R.id.tv_doctor_info)
        tvQueueList = view.findViewById(R.id.tv_queue_list)
        cardMyQueue = view.findViewById(R.id.card_my_queue)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnCancelQueue = view.findViewById(R.id.btn_cancel_queue)
        btnDownloadReceipt = view.findViewById(R.id.btn_download_receipt)
        btnCompleteQueue = view.findViewById(R.id.btn_complete_queue)
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
            refreshQueueStatus()
        }

        btnCancelQueue.setOnClickListener {
            showCancelConfirmation()
        }

        btnDownloadReceipt.setOnClickListener {
            generateReceipt()
        }

        btnCompleteQueue.setOnClickListener {
            completeQueue()
        }
    }

    /**
     * ✅ DISABLED: Auto-refresh dimatikan untuk menghindari refresh terus-menerus
     * User bisa refresh manual dengan tombol REFRESH
     */
    private fun startRealTimeMonitoring() {
        // ✅ DISABLED - Tidak perlu auto-refresh
        // User bisa klik tombol REFRESH untuk update manual
        Log.d(TAG, "ℹ️ Real-time monitoring disabled - use manual refresh")

        // Jika ingin mengaktifkan kembali, uncomment code di bawah:
        /*
        if (isMonitoring) return
        isMonitoring = true

        Log.d(TAG, "▶️ Starting real-time monitoring...")

        RealTimeQueueManager.startMonitoring { update ->
            activity?.runOnUiThread {
                if (currentUserBooking?.doctorName == update.booking.doctorName) {
                    refreshQueueStatus()
                }
            }
        }
        */
    }

    private fun refreshQueueStatus() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                if (currentUserId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "❌ Not logged in", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    DataSource.forceLoadFromFirebase()
                    delay(300)
                }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                // ✅ FIXED: Cari booking untuk USER INI dari Firebase
                val myActiveBooking = findActiveBookingForCurrentUser(currentUserId!!, today)

                if (myActiveBooking == null) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "❌ Tidak ada antrian aktif", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.navigateToFragment(EmptyQueueFragment())
                    }
                    return@launch
                }

                currentUserBooking = myActiveBooking

                if (myActiveBooking.status == BookingStatus.COMPLETED) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(requireContext(), "Booking sudah selesai", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                    }
                    return@launch
                }

                // ✅ Get ALL bookings untuk dokter yang sama
                val allBookingsForDoctor = DataSource.getBookingHistory()
                    .filter {
                        it.date == today &&
                                it.doctorName == myActiveBooking.doctorName &&
                                (it.status == BookingStatus.WAITING ||
                                        it.status == BookingStatus.CALLED ||
                                        it.status == BookingStatus.COMPLETED)
                    }
                    .sortedBy { it.queueNumber }

                val currentQueue = calculateCurrentQueueGlobal(allBookingsForDoctor)

                withContext(Dispatchers.Main) {
                    updateQueueInfo(myActiveBooking, currentQueue, allBookingsForDoctor)
                    showLoading(false)
                    Toast.makeText(requireContext(), "✅ Status diperbarui", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * ✅ Calculate current queue GLOBALLY - berdasarkan status SEMUA pasien untuk dokter ini
     */
    private fun calculateCurrentQueueGlobal(bookings: List<Booking>): Int {
        if (bookings.isEmpty()) return 0

        Log.d(TAG, "=== CALCULATE CURRENT QUEUE (GLOBAL) ===")

        // ✅ Filter HANYA booking yang WAITING atau CALLED
        val activeBookings = bookings.filter {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        }.sortedBy { it.queueNumber }

        Log.d(TAG, "Total bookings for doctor today: ${bookings.size}")
        Log.d(TAG, "Active bookings (WAITING/CALLED): ${activeBookings.size}")

        // Debug log
        bookings.sortedBy { it.queueNumber }.forEach { booking ->
            val icon = when (booking.status) {
                BookingStatus.COMPLETED -> "✅"
                BookingStatus.CALLED -> "📢"
                BookingStatus.WAITING -> "⏳"
                BookingStatus.CANCELLED -> "❌"
                BookingStatus.MISSED -> "⚠️"
            }
            Log.d(TAG, "  $icon Queue #${booking.queueNumber}: ${booking.patientName} - ${booking.status}")
        }

        if (activeBookings.isEmpty()) {
            val maxQueue = bookings.maxOfOrNull { it.queueNumber } ?: 0
            Log.d(TAG, "✅ All completed. Max queue was: $maxQueue")
            return maxQueue
        }

        // ✅ PRIORITY 1: Cari yang sedang CALLED
        val calledBooking = activeBookings.find { it.status == BookingStatus.CALLED }
        if (calledBooking != null) {
            Log.d(TAG, "📢 CALLED booking found: Queue #${calledBooking.queueNumber}")
            return calledBooking.queueNumber
        }

        // ✅ PRIORITY 2: Ambil WAITING dengan queue number terkecil
        val firstWaiting = activeBookings.firstOrNull { it.status == BookingStatus.WAITING }
        if (firstWaiting != null) {
            Log.d(TAG, "⏳ First WAITING: Queue #${firstWaiting.queueNumber}")
            return firstWaiting.queueNumber
        }

        val first = activeBookings.firstOrNull()?.queueNumber ?: 1
        Log.d(TAG, "⚠️ Fallback to: $first")
        return first
    }

    private fun updateQueueInfo(
        activeBooking: Booking,
        currentQueue: Int,
        allBookings: List<Booking>
    ) {
        cardMyQueue.visibility = View.VISIBLE
        clearAllTexts()

        // Update doctor info
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val timeStr = timeFormat.format(Date())

        tvDoctorInfo.text = "${activeBooking.doctorName} - ${activeBooking.specialization}\n" +
                "📅 $dateStr | 🕘 $timeStr WIB"

        // ✅ Current queue tidak boleh 0
        val displayCurrentQueue = if (currentQueue == 0) {
            allBookings.filter {
                it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
            }.minOfOrNull { it.queueNumber } ?: 1
        } else {
            currentQueue
        }

        tvCurrentQueue.text = displayCurrentQueue.toString()

        // My queue number
        val myQueueNumber = activeBooking.queueNumber
        tvMyQueueNumber.text = "No. $myQueueNumber"

        // ✅ Calculate patients ahead berdasarkan GLOBAL queue
        val patientsAhead = allBookings.count { booking ->
            booking.queueNumber < myQueueNumber &&
                    (booking.status == BookingStatus.WAITING || booking.status == BookingStatus.CALLED)
        }

        Log.d(TAG, "📊 My queue: $myQueueNumber, Current: $displayCurrentQueue, Patients ahead: $patientsAhead")

        // ✅ Status berdasarkan perbandingan dengan GLOBAL current queue
        when {
            activeBooking.status == BookingStatus.CALLED -> {
                // Dokter sudah memanggil
                tvMyQueueStatus.text = "🔔 DIPANGGIL"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                btnCompleteQueue.visibility = View.VISIBLE  // ✅ Show button
            }
            myQueueNumber < displayCurrentQueue -> {
                // Terlewat
                tvMyQueueStatus.text = "❌ Terlewat"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#F44336"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                btnCompleteQueue.visibility = View.GONE
            }
            patientsAhead == 0 -> {
                // ✅ FIXED: Jika tidak ada pasien di depan = GILIRAN ANDA = show button!
                tvMyQueueStatus.text = "🔔 Giliran Anda"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                btnCompleteQueue.visibility = View.VISIBLE  // ✅ Show button when it's your turn!
            }
            patientsAhead == 1 -> {
                // Siap-siap
                tvMyQueueStatus.text = "⚠️ Siap-siap (1 pasien lagi)"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#FF9800"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                btnCompleteQueue.visibility = View.GONE
            }
            else -> {
                // Menunggu
                tvMyQueueStatus.text = "⏳ Menunggu ($patientsAhead pasien di depan)"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#2196F3"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                btnCompleteQueue.visibility = View.GONE
            }
        }

        // Prediction dengan ML
        predictWaitTime(activeBooking, patientsAhead)

        // Update queue list
        updateQueueList(allBookings, displayCurrentQueue, myQueueNumber)
    }

    private fun predictWaitTime(booking: Booking, patientsAhead: Int) {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            if (patientsAhead <= 0) {
                tvEstimatedTime.text = "🎯 Giliran Anda SEKARANG!\n" +
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
                        "⚡ Antrian berikutnya (~$waitMinutes menit)\n" +
                                "🕐 Estimasi giliran: $turnTimeStr WIB"
                    }
                    patientsAhead <= 3 -> {
                        "⏱️ Sebentar lagi (~$waitMinutes menit)\n" +
                                "👥 $patientsAhead pasien di depan\n" +
                                "🕐 Estimasi giliran: $turnTimeStr WIB"
                    }
                    waitMinutes < 30 -> {
                        "⏱️ Estimasi: ~$waitMinutes menit\n" +
                                "👥 $patientsAhead pasien di depan\n" +
                                "🕐 Giliran Anda: $turnTimeStr WIB"
                    }
                    else -> {
                        val hours = waitMinutes / 60
                        val mins = waitMinutes % 60
                        val timeStr = if (hours > 0) {
                            "$hours jam $mins menit"
                        } else {
                            "$waitMinutes menit"
                        }
                        "⏱️ Estimasi: ~$timeStr\n" +
                                "👥 $patientsAhead pasien di depan\n" +
                                "🕐 Giliran Anda: $turnTimeStr WIB"
                    }
                }

                tvEstimatedTime.text = estimasiText

                Log.d(TAG, """
                ✅ Prediction:
                - Current time: $currentHour:${String.format("%02d", currentMinute)}
                - Patients ahead: $patientsAhead
                - Wait time: $waitMinutes min
                - Your turn: $turnTimeStr WIB
                - Confidence: ${prediction.confidence}
            """.trimIndent())

            } else {
                val waitMinutes = (patientsAhead * AVG_SERVICE_TIME).toInt()
                val turnTime = Calendar.getInstance()
                turnTime.add(Calendar.MINUTE, waitMinutes)
                val turnTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(turnTime.time)

                tvEstimatedTime.text = "⏱️ Estimasi: ~$waitMinutes menit\n" +
                        "👥 $patientsAhead pasien di depan\n" +
                        "🕐 Giliran Anda: $turnTimeStr WIB"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error predicting: ${e.message}", e)
            val waitMinutes = (patientsAhead * AVG_SERVICE_TIME).toInt()
            tvEstimatedTime.text = "⏱️ Estimasi: ~$waitMinutes menit ($patientsAhead pasien)"
        }
    }

    private fun updateQueueList(
        bookings: List<Booking>,
        currentQueue: Int,
        myQueue: Int
    ) {
        val listText = StringBuilder()

        // ✅ FIXED: Show ALL bookings including COMPLETED, sorted by queue number
        val allBookings = bookings.sortedBy { it.queueNumber }

        // Count only active (not completed/cancelled)
        val activeCount = allBookings.count {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        }

        if (allBookings.isEmpty()) {
            listText.append("Tidak ada antrian hari ini")
        } else {
            listText.append("Total: ${allBookings.size} pasien ($activeCount aktif)\n\n")

            allBookings.forEach { booking ->
                val statusIcon = when (booking.status) {
                    BookingStatus.COMPLETED -> "✅"  // Sudah selesai
                    BookingStatus.CALLED -> "🔔"     // Sedang dipanggil
                    BookingStatus.CANCELLED -> "❌"  // Dibatalkan
                    BookingStatus.MISSED -> "⚠️"     // Terlewat
                    else -> {
                        // WAITING
                        if (booking.queueNumber == myQueue) "👤" else "⏳"
                    }
                }

                val highlight = if (booking.queueNumber == myQueue) " ← ANDA" else ""
                val statusText = if (booking.status == BookingStatus.COMPLETED) " (selesai)" else ""
                listText.append("$statusIcon ${booking.queueNumber}. ${booking.patientName}$highlight$statusText\n")
            }
        }

        tvQueueList.text = listText.toString()
    }

    private fun completeQueue() {
        val activeBooking = currentUserBooking

        if (activeBooking == null) {
            Toast.makeText(
                requireContext(),
                "❌ Tidak ada booking aktif",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (activeBooking.status == BookingStatus.COMPLETED) {
            Log.w(TAG, "⚠️ Booking ${activeBooking.id} already COMPLETED")
            Toast.makeText(
                requireContext(),
                "⚠️ Booking sudah selesai",
                Toast.LENGTH_SHORT
            ).show()
            (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Selesai Konsultasi")
            .setMessage("Tandai antrian ini sebagai selesai?")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()

                progressBar.visibility = View.VISIBLE
                btnCompleteQueue.isEnabled = false

                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // ✅ Update status di Firebase
                            DataSource.updateBookingStatus(activeBooking.id, BookingStatus.COMPLETED)
                            delay(500)
                            DataSource.forceLoadFromFirebase()
                        }

                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            btnCompleteQueue.isEnabled = true
                            currentUserBooking = null

                            Toast.makeText(
                                requireContext(),
                                "✅ Antrian selesai! Pindah ke riwayat...",
                                Toast.LENGTH_SHORT
                            ).show()

                            delay(300)
                            (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error completing booking: ${e.message}", e)

                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            btnCompleteQueue.isEnabled = true

                            Toast.makeText(
                                requireContext(),
                                "⚠️ Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showCancelConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Batalkan Antrian")
            .setMessage("Yakin ingin membatalkan antrian ini?")
            .setPositiveButton("Ya") { _, _ ->
                cancelQueue()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun cancelQueue() {
        val activeBooking = currentUserBooking

        if (activeBooking == null) {
            Toast.makeText(
                requireContext(),
                "❌ Tidak ada booking aktif",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnCancelQueue.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DataSource.updateBookingStatus(activeBooking.id, BookingStatus.CANCELLED)
                    delay(300)
                    DataSource.forceLoadFromFirebase()
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCancelQueue.isEnabled = true
                    currentUserBooking = null

                    Toast.makeText(
                        requireContext(),
                        "✅ Antrian berhasil dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cancelling booking: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCancelQueue.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "⚠️ Antrian dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                }
            }
        }
    }

    private fun generateReceipt() {
        try {
            val activeBooking = currentUserBooking

            if (activeBooking == null) {
                Toast.makeText(
                    requireContext(),
                    "❌ Tidak ada booking aktif",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            showReceiptDialog(activeBooking)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating receipt: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "❌ Gagal membuat struk",
                Toast.LENGTH_SHORT
            ).show()
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

            val displayTime = booking.time

            tvQueueInfo.text = """
Nomor Antrian: ${booking.queueNumber}

Nama: ${booking.patientName}
Dokter: ${booking.doctorName}
Layanan: ${booking.specialization}
Tanggal: $displayDate
Jam: $displayTime WIB
        """.trimIndent()

            val qrContent = QRCodeGenerator.generateBookingQRContent(booking)
            val qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 512, 512)

            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap)
            } else {
                ivQrCode.setImageResource(android.R.drawable.ic_dialog_alert)
                Toast.makeText(requireContext(), "⚠️ Gagal generate QR Code", Toast.LENGTH_SHORT).show()
            }

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()

            btnDownloadPdf.setOnClickListener {
                downloadReceiptAsPDF(booking)
                Toast.makeText(
                    requireContext(),
                    "💾 Menyimpan struk...",
                    Toast.LENGTH_SHORT
                ).show()
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing receipt dialog: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "❌ Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun downloadReceiptAsPDF(booking: Booking) {
        try {
            val success = ReceiptGenerator.generateAndSaveReceipt(requireContext(), booking)

            if (success) {
                Toast.makeText(
                    requireContext(),
                    "✅ Struk berhasil disimpan di folder Downloads",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "❌ Gagal menyimpan struk",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error downloading PDF: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "❌ Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRefresh.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        RealTimeQueueManager.stopMonitoring()
        isMonitoring = false
        predictionModel?.close()
    }
}