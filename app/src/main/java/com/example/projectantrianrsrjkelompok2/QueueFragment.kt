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

    private fun startRealTimeMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        RealTimeQueueManager.startMonitoring { update ->
            activity?.runOnUiThread {
                updateQueueDisplay(update)
            }
        }

        refreshQueueStatus()
    }

    private fun refreshQueueStatus() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                delay(500)

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

                // Get today's bookings
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().time)

                val todayBookings = DataSource.getBookingHistory()
                    .filter { it.date == today }
                    .filter {
                        it.status == BookingStatus.WAITING ||
                                it.status == BookingStatus.CALLED
                    }
                    .sortedBy { it.queueNumber }

                // Calculate current queue (simulasi progres realistis)
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



    // app/src/main/java/com/example/projectantrianrsrjkelompok2/QueueFragment.kt

    /**
     * ✅ FIXED: Hitung current queue berdasarkan waktu REAL yang lebih akurat
     * Antrian hanya maju jika benar-benar sudah melewati waktu booking + service time
     */
    private fun calculateCurrentQueue(bookings: List<Booking>): Int {
        if (bookings.isEmpty()) return 0

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentTime = currentHour * 60 + currentMinute // dalam menit

        Log.d(TAG, "=== CALCULATE CURRENT QUEUE ===")
        Log.d(TAG, "Current time: $currentHour:${String.format("%02d", currentMinute)} ($currentTime minutes)")

        // ✅ FIXED: Filter hanya booking yang WAITING atau CALLED (exclude COMPLETED)
        val activeBookings = bookings.filter {
            it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
        }.sortedBy { it.queueNumber }

        if (activeBookings.isEmpty()) {
            // Semua booking sudah selesai
            val maxQueue = bookings.maxOfOrNull { it.queueNumber } ?: 0
            Log.d(TAG, "All bookings completed. Max queue was: $maxQueue")
            return maxQueue
        }

        // Ambil nomor antrian terkecil dari yang masih aktif
        val firstQueueNumber = activeBookings.firstOrNull()?.queueNumber ?: 1

        // Cek apakah klinik sudah buka
        val clinicOpenTime = 8 * 60 // 08:00 = 480 menit

        if (currentTime < clinicOpenTime) {
            // Klinik belum buka, current queue masih di nomor pertama
            Log.d(TAG, "Clinic not open yet. Current queue: $firstQueueNumber")
            return firstQueueNumber
        }

        // ✅ VALIDASI: Cek apakah ada booking yang statusnya CALLED
        val calledBooking = activeBookings.find { it.status == BookingStatus.CALLED }

        if (calledBooking != null) {
            Log.d(TAG, "Found CALLED booking: ${calledBooking.queueNumber}")
            return calledBooking.queueNumber
        }

        // Hitung berapa banyak antrian yang SEHARUSNYA sudah selesai dilayani
        val elapsedMinutes = currentTime - clinicOpenTime
        val completedQueues = (elapsedMinutes / AVG_SERVICE_TIME).toInt()

        Log.d(TAG, "Elapsed time since clinic open: $elapsedMinutes minutes")
        Log.d(TAG, "Theoretical completed queues: $completedQueues")

        // Current queue = nomor pertama + jumlah yang sudah selesai
        val theoreticalCurrentQueue = firstQueueNumber + completedQueues

        // Batasi agar tidak melebihi total booking aktif
        val maxQueueNumber = activeBookings.lastOrNull()?.queueNumber ?: firstQueueNumber
        val finalCurrentQueue = theoreticalCurrentQueue.coerceAtMost(maxQueueNumber)

        Log.d(TAG, """
        Queue Calculation Result:
        - First active queue: $firstQueueNumber
        - Max active queue: $maxQueueNumber
        - Theoretical current: $theoreticalCurrentQueue
        - FINAL current: $finalCurrentQueue
    """.trimIndent())

        return finalCurrentQueue
    }

    private fun updateQueueInfo(
        activeBooking: Booking,
        currentQueue: Int,
        allBookings: List<Booking>
    ) {
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
        // ✅ Update status ke CANCELLED dulu
        DataSource.updateBookingStatus(activeBooking.id, BookingStatus.CANCELLED)

        // ✅ Tunggu update selesai
        delay(300)

        // ✅ Clear active booking setelah status di-update
        DataSource.clearActiveBookingOnly()

        // ✅ Navigate ke HistoryFragment (bukan EmptyQueue)
        navigateToFragment(HistoryFragment())
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