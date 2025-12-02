package com.example.projectantrianrsrjkelompok2

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.ml.EnhancedQueuePredictionModel
import com.example.projectantrianrsrjkelompok2.utils.*
import java.text.SimpleDateFormat
import java.util.*

class QueueFragment : Fragment() {

    private lateinit var tvCurrentQueue: TextView
    private lateinit var tvMyQueueNumber: TextView
    private lateinit var tvMyQueueStatus: TextView
    private lateinit var tvEstimatedTime: TextView
    private lateinit var tvDoctorInfo: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnDownloadReceipt: Button
    private lateinit var btnCancelQueue: Button
    private lateinit var btnCompleteQueue: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardMyQueue: View
    private lateinit var tvQueueList: TextView

    // ML Model - Enhanced version
    private var mlModel: EnhancedQueuePredictionModel? = null

    // State
    private var currentQueueNumber = 0
    private var myQueueNumber = 0
    private var myQueueStatus = "Menunggu"

    companion object {
        private const val TAG = "QueueFragment"
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

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    200
                )
            }
        }

        initViews(view)
        initMLModel()
        loadBookingData()
        setupButtons()
        startRealTimeMonitoring()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopRealTimeMonitoring()
        mlModel?.close()
        NotificationHelper.cancelAllNotifications(requireContext())
    }

    private fun initViews(view: View) {
        tvCurrentQueue = view.findViewById(R.id.tv_current_queue)
        tvMyQueueNumber = view.findViewById(R.id.tv_my_queue_number)
        tvMyQueueStatus = view.findViewById(R.id.tv_my_queue_status)
        tvEstimatedTime = view.findViewById(R.id.tv_estimated_time)
        tvDoctorInfo = view.findViewById(R.id.tv_doctor_info)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        btnDownloadReceipt = view.findViewById(R.id.btn_download_receipt)
        btnCancelQueue = view.findViewById(R.id.btn_cancel_queue)
        btnCompleteQueue = view.findViewById(R.id.btn_complete_queue)
        progressBar = view.findViewById(R.id.progress_bar)
        cardMyQueue = view.findViewById(R.id.card_my_queue)
        tvQueueList = view.findViewById(R.id.tv_queue_list)
    }

    /**
     * ✅ Initialize ML Model
     */
    private fun initMLModel() {
        try {
            mlModel = EnhancedQueuePredictionModel(requireContext())
            val modelInfo = mlModel?.getModelInfo()

            Toast.makeText(
                requireContext(),
                "✅ AI Model Active (${modelInfo?.inputSize} features)",
                Toast.LENGTH_SHORT
            ).show()

            Log.d(TAG, "Model Info: $modelInfo")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
            Toast.makeText(
                requireContext(),
                "⚠️ Using standard estimation",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * ✅ Load booking data
     */
    private fun loadBookingData() {
        val activeBooking = DataSource.getActiveBooking()

        if (activeBooking != null) {
            myQueueNumber = activeBooking.queueNumber
            myQueueStatus = activeBooking.status.toDisplayString()

            // Display doctor info dengan waktu real
            val calendar = Calendar.getInstance()
            val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                .format(calendar.time)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(calendar.time)

            tvDoctorInfo.text = """
                ${activeBooking.doctorName} - ${activeBooking.specialization}
                📅 ${formatDateIndonesia(activeBooking.date)} | 🕘 ${activeBooking.time}
                
                📆 Hari Ini: $currentDate
                🕐 Waktu Sekarang: $currentTime WITA
            """.trimIndent()

            // Get today's bookings for current queue calculation
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (activeBooking.date == today) {
                val todayBookings = DataSource.getBookingHistory()
                    .filter { it.date == today }
                    .sortedBy { it.queueNumber }

                // Find current queue (booking yang CALLED)
                currentQueueNumber = todayBookings
                    .filter { it.status == BookingStatus.CALLED }
                    .minByOrNull { it.queueNumber }
                    ?.queueNumber ?: 0

                // If no one called yet, use first waiting
                if (currentQueueNumber == 0) {
                    currentQueueNumber = todayBookings
                        .filter { it.status == BookingStatus.WAITING }
                        .minByOrNull { it.queueNumber }
                        ?.queueNumber ?: 1
                }
            } else {
                currentQueueNumber = 1
                Toast.makeText(
                    requireContext(),
                    "📅 Booking Anda untuk tanggal ${formatDateIndonesia(activeBooking.date)}",
                    Toast.LENGTH_LONG
                ).show()
            }

            cardMyQueue.visibility = View.VISIBLE
            updateQueueDisplay()
        } else {
            myQueueNumber = 0
            cardMyQueue.visibility = View.GONE
        }
    }

    /**
     * ✅ Start real-time monitoring
     */
    private fun startRealTimeMonitoring() {
        RealTimeQueueManager.startMonitoring { update ->
            if (!isAdded) return@startMonitoring

            currentQueueNumber = update.currentQueueNumber
            myQueueNumber = update.myQueueNumber

            when (update.status) {
                RealTimeQueueManager.QueueStatus.CALLED -> {
                    myQueueStatus = "Dipanggil"
                    NotificationHelper.showQueueReadyNotification(
                        requireContext(),
                        myQueueNumber
                    )
                }
                RealTimeQueueManager.QueueStatus.READY -> {
                    myQueueStatus = "Siap-siap"
                    NotificationHelper.showQueueAlmostReadyNotification(
                        requireContext(),
                        myQueueNumber,
                        1
                    )
                }
                RealTimeQueueManager.QueueStatus.WAITING -> {
                    myQueueStatus = "Menunggu"
                }
                RealTimeQueueManager.QueueStatus.MISSED -> {
                    myQueueStatus = "Terlewat"
                }
            }

            updateQueueDisplay()
        }
    }

    /**
     * ✅ Stop real-time monitoring
     */
    private fun stopRealTimeMonitoring() {
        RealTimeQueueManager.stopMonitoring()
    }

    /**
     * ✅ Setup buttons
     */
    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            refreshQueueData()
        }

        btnDownloadReceipt.setOnClickListener {
            if (myQueueNumber <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Tidak ada antrian aktif",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            showReceiptDialog()
        }

        btnCancelQueue.setOnClickListener {
            showCancelDialog()
        }

        btnCompleteQueue.setOnClickListener {
            showCompleteDialog()
        }
    }

    /**
     * ✅ Update queue display dengan ML prediction
     */
    private fun updateQueueDisplay() {
        tvCurrentQueue.text = currentQueueNumber.toString()

        if (myQueueNumber > 0) {
            cardMyQueue.visibility = View.VISIBLE
            tvMyQueueNumber.text = "No. $myQueueNumber"

            val activeBooking = DataSource.getActiveBooking()

            when {
                myQueueNumber < currentQueueNumber -> {
                    myQueueStatus = "Terlewat"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    tvEstimatedTime.text = "Silakan hubungi petugas"
                    btnCompleteQueue.visibility = View.GONE
                }
                myQueueNumber == currentQueueNumber -> {
                    myQueueStatus = "Dipanggil"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                    tvEstimatedTime.text = "🔔 GILIRAN ANDA SEKARANG!\nSilakan menuju ruang dokter"
                    btnCompleteQueue.visibility = View.VISIBLE
                }
                myQueueNumber == currentQueueNumber + 1 -> {
                    myQueueStatus = "Siap-siap"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                    tvEstimatedTime.text = "⚡ Bersiap! Giliran Anda selanjutnya!"
                    btnCompleteQueue.visibility = View.GONE
                }
                else -> {
                    myQueueStatus = "Menunggu"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    calculateMLEstimatedTime(activeBooking)
                    btnCompleteQueue.visibility = View.GONE
                }
            }

            tvMyQueueStatus.text = myQueueStatus

            // Show time until appointment
            activeBooking?.let {
                val timeDiff = RealTimeQueueManager.getTimeUntilAppointment(it.time)
                if (timeDiff != null) {
                    val timeText = if (timeDiff.isPast) {
                        "⏰ Jadwal: ${it.time} (Sudah lewat)"
                    } else {
                        "⏰ Jadwal: ${it.time} (${timeDiff.getFormattedTime()})"
                    }

                    val calendar = Calendar.getInstance()
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(calendar.time)

                    tvDoctorInfo.text = """
                        ${it.doctorName} - ${it.specialization}
                        📅 ${formatDateIndonesia(it.date)}
                        $timeText
                        🕐 Sekarang: $currentTime WITA
                    """.trimIndent()
                }
            }
        } else {
            cardMyQueue.visibility = View.GONE
        }

        updateQueueList()
    }

    /**
     * ✅ Calculate estimated time using ML
     */
    private fun calculateMLEstimatedTime(booking: Booking?) {
        if (booking == null) {
            tvEstimatedTime.text = "Estimasi tidak tersedia"
            return
        }

        val patientsAhead = myQueueNumber - currentQueueNumber
        if (patientsAhead <= 0) {
            tvEstimatedTime.text = "Giliran Anda!"
            return
        }

        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Use Enhanced ML model for prediction
            val prediction = mlModel?.predictWithConfidence(
                patientsAhead = patientsAhead,
                currentHour = currentHour,
                dayOfWeek = dayOfWeek,
                specialization = booking.specialization,
                avgServiceTime = 10f,
                queueNumber = myQueueNumber
            )

            if (prediction != null) {
                val predictionSource = if (prediction.isMLPrediction) "🤖 Prediksi AI" else "📊 Estimasi"

                val estimatedText = """
                    $predictionSource:
                    ⏱️ Perkiraan: ${prediction.getFormattedPrediction()}
                    📊 Range: ${prediction.getFormattedRange()}
                    ✅ Tingkat akurasi: ${prediction.getConfidenceLevel()}
                    👥 Pasien di depan: $patientsAhead orang
                    
                    💡 ${mlModel?.getRecommendation(prediction.predictedMinutes) ?: ""}
                """.trimIndent()

                tvEstimatedTime.text = estimatedText

                Log.d(TAG, "Prediction successful: ${prediction.predictedMinutes} minutes")
            } else {
                // Fallback to simple calculation
                val simpleEstimate = patientsAhead * 10
                tvEstimatedTime.text = """
                    ⏱️ Estimasi: $simpleEstimate menit
                    👥 $patientsAhead pasien di depan Anda
                """.trimIndent()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error calculating ML estimate: ${e.message}", e)
            // Fallback
            val simpleEstimate = patientsAhead * 10
            tvEstimatedTime.text = """
                ⏱️ Estimasi: $simpleEstimate menit
                👥 $patientsAhead pasien di depan Anda
            """.trimIndent()
        }
    }

    /**
     * ✅ Update queue list
     */
    private fun updateQueueList() {
        val queueListText = StringBuilder()
        queueListText.append("📋 Daftar Antrian Hari Ini:\n\n")

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayBookings = DataSource.getBookingHistory()
            .filter { it.date == today }
            .sortedBy { it.queueNumber }

        if (todayBookings.isEmpty()) {
            queueListText.append("Tidak ada antrian hari ini")
        } else {
            todayBookings.take(15).forEach { booking ->
                val statusIcon = when (booking.status) {
                    BookingStatus.COMPLETED -> "✅"
                    BookingStatus.CALLED -> "🔵"
                    BookingStatus.WAITING -> "⏳"
                    BookingStatus.CANCELLED -> "❌"
                    BookingStatus.MISSED -> "⚠️"
                }

                val highlight = if (booking.queueNumber == myQueueNumber) " 👈 SAYA" else ""

                queueListText.append("${booking.queueNumber}. $statusIcon ${booking.status.toDisplayString()}$highlight\n")
            }

            if (todayBookings.size > 15) {
                queueListText.append("\n... dan ${todayBookings.size - 15} antrian lainnya")
            }
        }

        tvQueueList.text = queueListText.toString()
    }

    /**
     * Format date
     */
    private fun formatDateIndonesia(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Refresh queue data
     */
    private fun refreshQueueData() {
        showLoading(true)

        // Reload data
        loadBookingData()
        updateQueueDisplay()

        showLoading(false)
        Toast.makeText(requireContext(), "✅ Data diperbarui", Toast.LENGTH_SHORT).show()
    }

    /**
     * Show cancel dialog
     */
    private fun showCancelDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Batalkan Antrian")
            .setMessage("Apakah Anda yakin ingin membatalkan antrian?")
            .setPositiveButton("Ya") { dialog, _ ->
                cancelQueue()
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Cancel queue
     */
    private fun cancelQueue() {
        DataSource.getActiveBooking()?.let { booking ->
            val cancelled = booking.copy(status = BookingStatus.CANCELLED)
            DataSource.addToHistory(cancelled)
        }

        DataSource.clearActiveBooking()
        cardMyQueue.visibility = View.GONE

        Toast.makeText(
            requireContext(),
            "✅ Antrian dibatalkan",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Show complete dialog
     */
    private fun showCompleteDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Selesaikan Antrian")
            .setMessage("Apakah pemeriksaan Anda sudah selesai?")
            .setPositiveButton("Ya") { dialog, _ ->
                completeQueue()
                dialog.dismiss()
            }
            .setNegativeButton("Belum") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Complete queue
     */
    private fun completeQueue() {
        DataSource.getActiveBooking()?.let { booking ->
            val completed = booking.copy(status = BookingStatus.COMPLETED)
            DataSource.addToHistory(completed)
        }

        DataSource.clearActiveBooking()

        Toast.makeText(
            requireContext(),
            "✅ Terima kasih! Semoga lekas sembuh.",
            Toast.LENGTH_LONG
        ).show()

        (activity as? MainActivity)?.navigateToFragment(HistoryFragment())
    }

    /**
     * Show receipt dialog
     */
    private fun showReceiptDialog() {
        val activeBooking = DataSource.getActiveBooking() ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt_options, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val qrContent = QRCodeGenerator.generateBookingQRContent(activeBooking)
        val qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 512, 512)

        val ivQrCode = dialogView.findViewById<ImageView>(R.id.iv_qr_code)
        val tvQueueInfo = dialogView.findViewById<TextView>(R.id.tv_queue_info)
        val btnDownloadPdf = dialogView.findViewById<Button>(R.id.btn_download_pdf)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)

        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap)
        }

        tvQueueInfo.text = "Nomor Antrian: ${activeBooking.queueNumber}"

        btnDownloadPdf.setOnClickListener {
            val success = ReceiptGenerator.generateAndSaveReceipt(requireContext(), activeBooking)
            if (success) {
                Toast.makeText(requireContext(), "✅ Struk berhasil diunduh!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Show/hide loading
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnRefresh.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            btnRefresh.isEnabled = true
        }
    }
}