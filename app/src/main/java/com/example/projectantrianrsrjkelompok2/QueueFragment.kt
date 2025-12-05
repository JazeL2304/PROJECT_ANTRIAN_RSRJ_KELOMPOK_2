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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.projectantrianrsrjkelompok2.utils.QRCodeGenerator
import com.example.projectantrianrsrjkelompok2.utils.ReceiptGenerator


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

    companion object {
        private const val TAG = "QueueFragment"
        private const val AVG_SERVICE_TIME = 8f // 8 menit per pasien (lebih realistis)
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
        startRealTimeMonitoring()

        // ✅ FIXED: Load data SEGERA tanpa delay, dan show loading
        loadInitialData()
    }

    /**
     * ✅ NEW: Load data initial dengan proper loading state
     */
    private fun loadInitialData() {
        // ✅ Hide card dulu saat loading
        cardMyQueue.visibility = View.INVISIBLE
        showLoading(true)

        // ✅ TAMBAHKAN INI - Clear semua text default
        tvCurrentQueue.text = ""
        tvMyQueueNumber.text = ""
        tvMyQueueStatus.text = ""
        tvEstimatedTime.text = ""
        tvDoctorInfo.text = ""
        tvQueueList.text = ""

        lifecycleScope.launch {
            try {
                // ✅ CRITICAL: Force reload dari Firebase DULU sebelum tampilkan apapun
                withContext(Dispatchers.IO) {
                    Log.d(TAG, "📥 Force loading fresh data from Firebase...")
                    DataSource.forceLoadFromFirebase()
                    delay(500) // Beri waktu untuk data ter-load
                }

                // ✅ Sekarang ambil data yang fresh
                val activeBooking = DataSource.getActiveBooking()

                if (activeBooking == null) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            "❌ Tidak ada antrian aktif",
                            Toast.LENGTH_SHORT
                        ).show()
                        (activity as? MainActivity)?.navigateToFragment(EmptyQueueFragment())
                    }
                    return@launch
                }

                // ✅ Cek apakah booking sudah selesai
                if (activeBooking.status == BookingStatus.COMPLETED) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            "Booking sudah selesai",
                            Toast.LENGTH_SHORT
                        ).show()
                        (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                    }
                    return@launch
                }

                // ✅ Get today's bookings
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                val todayBookings = DataSource.getBookingHistory()
                    .filter { it.date == today }
                    .sortedBy { it.queueNumber }

                // ✅ Calculate current queue dengan data yang FRESH
                val currentQueue = calculateCurrentQueue(todayBookings)

                // ✅ Update UI dengan data yang benar
                withContext(Dispatchers.Main) {
                    updateQueueInfo(activeBooking, currentQueue, todayBookings)
                    showLoading(false)

                    // ✅ BARU start monitoring setelah data initial sudah benar
                    startRealTimeMonitoring()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading initial data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
        progressBar = view.findViewById(R.id.progress_bar)

        // ✅ TAMBAHKAN INI - Clear semua text default
        tvCurrentQueue.text = ""
        tvMyQueueNumber.text = ""
        tvMyQueueStatus.text = ""
        tvEstimatedTime.text = ""
        tvDoctorInfo.text = ""
        tvQueueList.text = ""
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
     * ✅ MODIFIED: startRealTimeMonitoring dipanggil SETELAH data initial ready
     */
    private fun startRealTimeMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        Log.d(TAG, "▶️ Starting real-time monitoring...")

        RealTimeQueueManager.startMonitoring { update ->
            activity?.runOnUiThread {
                updateQueueDisplay(update)
            }
        }

        // ✅ Tidak perlu refreshQueueStatus() di sini karena sudah di-handle di loadInitialData()
    }

    private fun refreshQueueStatus() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // ✅ Force reload data dari Firebase
                withContext(Dispatchers.IO) {
                    DataSource.forceLoadFromFirebase()
                    delay(300)
                }

                val activeBooking = DataSource.getActiveBooking()

                if (activeBooking == null) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "❌ Tidak ada antrian aktif",
                        Toast.LENGTH_SHORT
                    ).show()
                    (activity as? MainActivity)?.navigateToFragment(EmptyQueueFragment())
                    return@launch
                }

                // ✅ Cek status booking
                if (activeBooking.status == BookingStatus.COMPLETED) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Booking sudah selesai",
                        Toast.LENGTH_SHORT
                    ).show()
                    (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                    return@launch
                }

                // Get today's bookings
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                val todayBookings = DataSource.getBookingHistory()
                    .filter { it.date == today }
                    .sortedBy { it.queueNumber }

                // Calculate current queue
                val currentQueue = calculateCurrentQueue(todayBookings)

                // Update display
                updateQueueInfo(activeBooking, currentQueue, todayBookings)

                showLoading(false)

                Toast.makeText(
                    requireContext(),
                    "✅ Status diperbarui",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing: ${e.message}", e)
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * ✅ FIXED: Calculate current queue - HANYA berdasarkan status AKTUAL
     */
    private fun calculateCurrentQueue(bookings: List<Booking>): Int {
        if (bookings.isEmpty()) return 0

        Log.d(TAG, "=== CALCULATE CURRENT QUEUE ===")

        // ✅ Filter HANYA booking yang WAITING atau CALLED
        val activeBookings = bookings.filter {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        }.sortedBy { it.queueNumber }

        Log.d(TAG, "Total bookings today: ${bookings.size}")
        Log.d(TAG, "Active bookings (WAITING/CALLED): ${activeBookings.size}")

        // ✅ Debug log
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
            // Semua sudah selesai
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

        // ✅ PRIORITY 2: Ambil WAITING terkecil
        val firstWaiting = activeBookings.firstOrNull { it.status == BookingStatus.WAITING }
        if (firstWaiting != null) {
            Log.d(TAG, "⏳ First WAITING: Queue #${firstWaiting.queueNumber}")
            return firstWaiting.queueNumber
        }

        // ✅ Fallback
        val first = activeBookings.firstOrNull()?.queueNumber ?: 1
        Log.d(TAG, "⚠️ Fallback to: $first")
        return first
    }

    private fun updateQueueInfo(
        activeBooking: Booking,
        currentQueue: Int,
        allBookings: List<Booking>
    ) {
        // ✅ Show card setelah data siap
        cardMyQueue.visibility = View.VISIBLE
        // ✅ TAMBAHKAN INI - Clear semua text default
        tvCurrentQueue.text = ""
        tvMyQueueNumber.text = ""
        tvMyQueueStatus.text = ""
        tvEstimatedTime.text = ""
        tvDoctorInfo.text = ""
        tvQueueList.text = ""

        // Update doctor info dengan WIB
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val timeStr = timeFormat.format(Date())

        tvDoctorInfo.text = "${activeBooking.doctorName} - ${activeBooking.specialization}\n" +
                "📅 $dateStr | 🕘 $timeStr WIB"

        // ✅ FIXED: Current queue tidak boleh 0
        val displayCurrentQueue = if (currentQueue == 0) {
            allBookings.minOfOrNull { it.queueNumber } ?: 1
        } else {
            currentQueue
        }

        tvCurrentQueue.text = displayCurrentQueue.toString()

        // My queue number
        val myQueueNumber = activeBooking.queueNumber
        tvMyQueueNumber.text = "No. $myQueueNumber"

        // ✅ FIXED: Calculate patients ahead dengan benar
        val patientsAhead = when {
            currentQueue == 0 -> myQueueNumber - 1 // Belum ada yang dilayani
            myQueueNumber > currentQueue -> myQueueNumber - currentQueue
            else -> 0
        }.coerceAtLeast(0)

        // Status & card color
        when {
            myQueueNumber < displayCurrentQueue -> {
                // Terlewat
                tvMyQueueStatus.text = "❌ Terlewat"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#F44336"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                btnCompleteQueue.visibility = View.GONE
            }
            myQueueNumber == displayCurrentQueue -> {
                // Giliran sekarang
                tvMyQueueStatus.text = "🔔 DIPANGGIL"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                btnCompleteQueue.visibility = View.VISIBLE
            }
            myQueueNumber == displayCurrentQueue + 1 -> {
                // Antrian berikutnya
                tvMyQueueStatus.text = "⚠️ Siap-siap"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#FF9800"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
                btnCompleteQueue.visibility = View.GONE
            }
            else -> {
                // Menunggu
                tvMyQueueStatus.text = "⏳ Menunggu"
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

    /**
     * ✅ FIXED: Prediksi waktu tunggu dengan tampilan yang lebih jelas
     */
    private fun predictWaitTime(booking: Booking, patientsAhead: Int) {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // ✅ CASE 1: Jika tidak ada pasien di depan
            if (patientsAhead <= 0) {
                tvEstimatedTime.text = "🎯 Giliran Anda SEKARANG!\n" +
                        "Silakan menuju ruang pemeriksaan"
                return
            }

            // ✅ CASE 2: Prediksi dengan ML
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

                // Hitung jam giliran
                val turnTime = Calendar.getInstance()
                turnTime.add(Calendar.MINUTE, waitMinutes)
                val turnTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(turnTime.time)

                // ✅ Format tampilan berdasarkan jumlah pasien di depan
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
                // Fallback heuristic
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

        if (bookings.isEmpty()) {
            listText.append("Belum ada antrian hari ini")
        } else {
            listText.append("Total: ${bookings.size} pasien\n\n")

            // ✅ FIXED: Gunakan display current queue yang benar
            val displayCurrentQueue = if (currentQueue == 0) {
                bookings.minOfOrNull { it.queueNumber } ?: 1
            } else {
                currentQueue
            }

            bookings.sortedBy { it.queueNumber }.take(10).forEach { booking ->
                val statusIcon = when {
                    booking.queueNumber < displayCurrentQueue -> "✅" // Sudah selesai
                    booking.queueNumber == displayCurrentQueue -> "🔔" // Sedang dilayani
                    booking.queueNumber == myQueue -> "👤" // Antrian Anda
                    else -> "⏳" // Menunggu
                }

                val highlight = if (booking.queueNumber == myQueue) " ← ANDA" else ""
                listText.append("$statusIcon ${booking.queueNumber}. ${booking.patientName}$highlight\n")
            }

            if (bookings.size > 10) {
                listText.append("\n... dan ${bookings.size - 10} pasien lainnya")
            }
        }

        tvQueueList.text = listText.toString()
    }

    private fun updateQueueDisplay(update: RealTimeQueueManager.QueueUpdate) {
        tvCurrentQueue.text = update.currentQueueNumber.toString()

        // Update with realistic timing
        val calendar = Calendar.getInstance()
        updateQueueInfo(update.booking, update.currentQueueNumber, listOf(update.booking))
    }

// app/src/main/java/com/example/projectantrianrsrjkelompok2/QueueFragment.kt

    private fun completeQueue() {
        AlertDialog.Builder(requireContext())
            .setTitle("Selesai Konsultasi")
            .setMessage("Tandai antrian ini sebagai selesai?")
            .setPositiveButton("Ya") { dialog, _ ->
                dialog.dismiss()

                val activeBooking = DataSource.getActiveBooking()

                if (activeBooking != null) {
                    // ✅ Show loading
                    progressBar.visibility = View.VISIBLE
                    btnCompleteQueue.isEnabled = false

                    lifecycleScope.launch {
                        try {
                            // ✅ Update status di DataSource (akan update cache lokal + Firebase)
                            withContext(Dispatchers.IO) {
                                DataSource.completeActiveBooking()

                                // ✅ Tunggu sebentar untuk memastikan update selesai
                                delay(500)

                                // ✅ Force reload untuk memastikan data fresh
                                DataSource.forceLoadFromFirebase()
                            }

                            // ✅ Update UI di Main thread
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                btnCompleteQueue.isEnabled = true

                                Toast.makeText(
                                    requireContext(),
                                    "✅ Antrian selesai! Pindah ke riwayat...",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // ✅ Delay kecil sebelum navigate untuk memastikan data sudah ready
                                delay(300)

                                // Navigate ke HistoryFragment
                                (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error completing booking: ${e.message}", e)

                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                btnCompleteQueue.isEnabled = true

                                Toast.makeText(
                                    requireContext(),
                                    "⚠️ Antrian ditandai selesai",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Tetap navigate meskipun ada error
                                (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Tidak ada booking aktif",
                        Toast.LENGTH_SHORT
                    ).show()
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
        // ✅ FIXED: Ambil activeBooking dari DataSource
        val activeBooking = DataSource.getActiveBooking()

        if (activeBooking == null) {
            Toast.makeText(
                requireContext(),
                "❌ Tidak ada booking aktif",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // ✅ Show loading
        progressBar.visibility = View.VISIBLE
        btnCancelQueue.isEnabled = false

        // ✅ FIXED: Gunakan lifecycleScope untuk coroutine
        lifecycleScope.launch {
            try {
                // ✅ Update status ke CANCELLED di background thread
                withContext(Dispatchers.IO) {
                    DataSource.updateBookingStatus(activeBooking.id, BookingStatus.CANCELLED)

                    // ✅ FIXED: delay harus dalam coroutine
                    delay(300)

                    // ✅ Clear active booking setelah status di-update
                    DataSource.clearActiveBookingOnly()

                    // ✅ Force reload data untuk memastikan cache ter-update
                    DataSource.forceLoadFromFirebase()
                }

                // ✅ Update UI di Main thread
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCancelQueue.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "✅ Antrian berhasil dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    // ✅ FIXED: Gunakan cara yang benar untuk navigate
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

                    // ✅ Tetap navigate meskipun ada error
                    (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
                }
            }
        }
    }

    private fun generateReceipt() {
        try {
            val activeBooking = DataSource.getActiveBooking()

            if (activeBooking == null) {
                Toast.makeText(
                    requireContext(),
                    "❌ Tidak ada booking aktif",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // ✅ Tampilkan dialog dengan QR Code
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

    /**
     * ✅ Tampilkan dialog struk dengan QR Code menggunakan dialog_receipt_options.xml
     */
// app/src/main/java/com/example/projectantrianrsrjkelompok2/QueueFragment.kt

    /**
     * ✅ Tampilkan dialog struk dengan QR Code menggunakan dialog_receipt_options.xml
     */
    private fun showReceiptDialog(booking: Booking) {
        try {
            // Inflate dialog layout
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_receipt_options, null)

            // Setup views
            val tvQueueInfo = dialogView.findViewById<TextView>(R.id.tv_queue_info)
            val ivQrCode = dialogView.findViewById<ImageView>(R.id.iv_qr_code)
            val btnDownloadPdf = dialogView.findViewById<Button>(R.id.btn_download_pdf)
            val btnClose = dialogView.findViewById<Button>(R.id.btn_close)

            // ✅ Format tanggal dengan benar
            val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
            val displayDate = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                dateFormat.format(date ?: Date())
            } catch (e: Exception) {
                booking.date
            }

            // ✅ CRITICAL FIX: Pastikan menggunakan booking.time yang ASLI
            val displayTime = booking.time // Langsung dari booking, JANGAN dihitung ulang!

            // ✅ Set booking info dengan JAM YANG BENAR
            tvQueueInfo.text = """
Nomor Antrian: ${booking.queueNumber}

Nama: ${booking.patientName}
Dokter: ${booking.doctorName}
Layanan: ${booking.specialization}
Tanggal: $displayDate
Jam: $displayTime WIB
        """.trimIndent()

            Log.d(TAG, """
            ✅ Receipt Info:
            - Queue: ${booking.queueNumber}
            - Patient: ${booking.patientName}
            - Date: $displayDate
            - Time: $displayTime WIB (ORIGINAL: ${booking.time})
        """.trimIndent())

            // ✅ Generate QR Code menggunakan QRCodeGenerator yang sudah ada
            val qrContent = QRCodeGenerator.generateBookingQRContent(booking)
            val qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 512, 512)

            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap)
                Log.d(TAG, "✅ QR Code generated successfully")
            } else {
                ivQrCode.setImageResource(android.R.drawable.ic_dialog_alert)
                Toast.makeText(requireContext(), "⚠️ Gagal generate QR Code", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "❌ Failed to generate QR Code")
            }

            // ✅ Create dialog
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // ✅ Download PDF button
            btnDownloadPdf.setOnClickListener {
                downloadReceiptAsPDF(booking)
                Toast.makeText(
                    requireContext(),
                    "💾 Menyimpan struk...",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // ✅ Close button
            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            // Show dialog
            dialog.show()

            Log.d(TAG, "✅ Receipt dialog shown for booking: ${booking.id}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing receipt dialog: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "❌ Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * ✅ Download struk sebagai PDF
     */
    private fun downloadReceiptAsPDF(booking: Booking) {
        try {
            val success = ReceiptGenerator.generateAndSaveReceipt(requireContext(), booking)

            if (success) {
                Toast.makeText(
                    requireContext(),
                    "✅ Struk berhasil disimpan di folder Downloads",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "✅ Receipt PDF saved successfully")
            } else {
                Toast.makeText(
                    requireContext(),
                    "❌ Gagal menyimpan struk",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "❌ Failed to save receipt PDF")
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
    private fun createReceiptBitmap(booking: Booking): Bitmap {
        val width = 800
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var y = 80f

        // Header
        paint.textSize = 48f
        paint.color = Color.parseColor("#00897B")
        paint.isFakeBoldText = true
        canvas.drawText("🏥 RS RJ", width / 2f, y, paint)
        y += 60

        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        canvas.drawText("Struk Antrian", width / 2f, y, paint)
        y += 80

        // Booking details
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 28f

        val details = listOf(
            "No. Antrian: ${booking.queueNumber}",
            "Nama: ${booking.patientName}",
            "Dokter: ${booking.doctorName}",
            "Layanan: ${booking.specialization}",
            "Tanggal: ${booking.date}",
            "Jam: ${booking.time} WIB",
            "Status: ${booking.status}"
        )

        details.forEach { text ->
            canvas.drawText(text, 80f, y, paint)
            y += 50
        }

        y += 50

        // Footer
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 20f
        paint.color = Color.GRAY
        canvas.drawText("Simpan struk ini sebagai bukti", width / 2f, y, paint)
        y += 40
        canvas.drawText("Datang 15 menit sebelum jadwal", width / 2f, y, paint)

        return bitmap
    }

    private fun saveReceiptToDevice(bitmap: Bitmap) {
        val fileName = "Struk_Antrian_${System.currentTimeMillis()}.png"
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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