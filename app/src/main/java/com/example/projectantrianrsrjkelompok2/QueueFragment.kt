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

    /**
     * ✅ Hitung current queue yang realistis
     */
    private fun calculateCurrentQueue(bookings: List<Booking>): Int {
        if (bookings.isEmpty()) return 1

        val now = Calendar.getInstance()
        val currentTime = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) // dalam menit

        // Cari antrian yang seharusnya sedang berjalan
        var currentQueue = 1

        for (booking in bookings) {
            val bookingTimeParts = booking.time.split(":")
            if (bookingTimeParts.size == 2) {
                val bookingHour = bookingTimeParts[0].toIntOrNull() ?: 0
                val bookingMinute = bookingTimeParts[1].toIntOrNull() ?: 0
                val bookingTime = bookingHour * 60 + bookingMinute

                // ✅ Jika waktu booking + service time (8 menit) sudah lewat,
                // berarti antrian ini sudah selesai dilayani
                val serviceEndTime = bookingTime + AVG_SERVICE_TIME.toInt()

                if (currentTime >= serviceEndTime) {
                    // Antrian ini sudah selesai, lanjut ke antrian berikutnya
                    currentQueue = (booking.queueNumber + 1).coerceAtMost(bookings.size)
                } else if (currentTime >= bookingTime) {
                    // Antrian ini sedang dilayani
                    currentQueue = booking.queueNumber
                    break
                }
            }
        }

        Log.d(TAG, "Current time: ${now.get(Calendar.HOUR_OF_DAY)}:${now.get(Calendar.MINUTE)} → Current Queue: $currentQueue")

        return currentQueue
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

        // Current queue
        tvCurrentQueue.text = currentQueue.toString()

        // My queue number
        val myQueueNumber = activeBooking.queueNumber
        tvMyQueueNumber.text = "No. $myQueueNumber"

        // Calculate patients ahead
        val patientsAhead = (myQueueNumber - currentQueue).coerceAtLeast(0)

        // Status & card color
        when {
            myQueueNumber < currentQueue -> {
                // Terlewat
                tvMyQueueStatus.text = "❌ Terlewat"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#F44336"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                btnCompleteQueue.visibility = View.GONE
            }
            myQueueNumber == currentQueue -> {
                // Giliran sekarang
                tvMyQueueStatus.text = "🔔 DIPANGGIL"
                tvMyQueueStatus.setBackgroundColor(Color.parseColor("#4CAF50"))
                cardMyQueue.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                btnCompleteQueue.visibility = View.VISIBLE
            }
            myQueueNumber == currentQueue + 1 -> {
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
        updateQueueList(allBookings, currentQueue, myQueueNumber)
    }

    /**
     * ✅ Prediksi waktu tunggu dengan waktu yang realistis
     */
    private fun predictWaitTime(booking: Booking, patientsAhead: Int) {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

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

                // Format estimasi yang lebih ringkas
                val estimasiText = when {
                    patientsAhead == 0 -> "🎯 Giliran Anda SEKARANG!"
                    waitMinutes < 5 -> "⚡ Segera dipanggil (~$waitMinutes menit)\n🕐 Estimasi: $turnTimeStr WIB"
                    waitMinutes < 15 -> "⏱️ Estimasi: ~$waitMinutes menit ($patientsAhead pasien di depan)\n🕐 Giliran Anda: $turnTimeStr WIB"
                    waitMinutes < 60 -> {
                        val hours = waitMinutes / 60
                        val mins = waitMinutes % 60
                        if (hours > 0) {
                            "⏱️ Estimasi: ~$hours jam $mins menit ($patientsAhead pasien)\n🕐 Giliran Anda: $turnTimeStr WIB"
                        } else {
                            "⏱️ Estimasi: ~$waitMinutes menit ($patientsAhead pasien)\n🕐 Giliran Anda: $turnTimeStr WIB"
                        }
                    }
                    else -> {
                        val hours = waitMinutes / 60
                        val mins = waitMinutes % 60
                        "⏱️ Estimasi: ~$hours jam $mins menit ($patientsAhead pasien)\n🕐 Giliran Anda: $turnTimeStr WIB"
                    }
                }

                tvEstimatedTime.text = estimasiText

                Log.d(TAG, """
                ✅ Prediction:
                - Patients ahead: $patientsAhead
                - Wait time: $waitMinutes min
                - Your turn: $turnTimeStr WIB
                - Confidence: ${prediction.confidence}
            """.trimIndent())

            } else {
                // Fallback heuristic
                val waitMinutes = (patientsAhead * AVG_SERVICE_TIME).toInt()

                if (patientsAhead == 0) {
                    tvEstimatedTime.text = "🎯 Giliran Anda SEKARANG!"
                } else {
                    val turnTime = Calendar.getInstance()
                    turnTime.add(Calendar.MINUTE, waitMinutes)
                    val turnTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(turnTime.time)
                    tvEstimatedTime.text = "⏱️ Estimasi: ~$waitMinutes menit ($patientsAhead pasien di depan)\n🕐 Giliran Anda: $turnTimeStr WIB"
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error predicting: ${e.message}", e)
            val waitMinutes = (patientsAhead * AVG_SERVICE_TIME).toInt()

            if (patientsAhead == 0) {
                val now = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Calendar.getInstance().time)
                tvEstimatedTime.text = "🔔 Giliran Anda\n$now WIB"
            } else {
                val turnTime = Calendar.getInstance()
                turnTime.add(Calendar.MINUTE, waitMinutes)
                val turnTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(turnTime.time)
                tvEstimatedTime.text = "⏱️ Estimasi: ~$waitMinutes menit ($patientsAhead pasien di depan)\n🕐 Giliran Anda: $turnTimeStr WIB"
            }
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

            bookings.take(10).forEach { booking ->
                val statusIcon = when {
                    booking.queueNumber < currentQueue -> "✅"
                    booking.queueNumber == currentQueue -> "🔔"
                    booking.queueNumber == myQueue -> "👤"
                    else -> "⏳"
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

    private fun completeQueue() {
        AlertDialog.Builder(requireContext())
            .setTitle("Selesai Konsultasi")
            .setMessage("Tandai antrian ini sebagai selesai?")
            .setPositiveButton("Ya") { _, _ ->
                DataSource.completeActiveBooking()
                Toast.makeText(
                    requireContext(),
                    "✅ Antrian selesai dipindahkan ke riwayat",
                    Toast.LENGTH_SHORT
                ).show()
                (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
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
        DataSource.clearActiveBooking()
        Toast.makeText(
            requireContext(),
            "❌ Antrian dibatalkan",
            Toast.LENGTH_SHORT
        ).show()
        (activity as? MainActivity)?.navigateToFragment(EmptyQueueFragment())
    }

    private fun generateReceipt() {
        try {
            val activeBooking = DataSource.getActiveBooking() ?: return

            val bitmap = createReceiptBitmap(activeBooking)
            saveReceiptToDevice(bitmap)

            Toast.makeText(
                requireContext(),
                "✅ Struk disimpan di Downloads",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error generating receipt: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "❌ Gagal membuat struk",
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