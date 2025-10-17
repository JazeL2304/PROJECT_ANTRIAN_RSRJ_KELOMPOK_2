package com.example.projectantrianrsrjkelompok2

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.example.projectantrianrsrjkelompok2.utils.QRCodeGenerator
import com.example.projectantrianrsrjkelompok2.utils.ReceiptGenerator
import com.example.projectantrianrsrjkelompok2.utils.NotificationHelper
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
    private lateinit var progressBar: ProgressBar
    private lateinit var cardMyQueue: LinearLayout
    private lateinit var tvQueueList: TextView

    private var currentQueueNumber = 8
    private var myQueueNumber = 15
    private var myQueueStatus = "Menunggu"
    private val handler = Handler(Looper.getMainLooper())
    private var hasShownNotification3 = false
    private var hasShownNotification1 = false
    private var hasShownNotificationReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_queue, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        loadBookingData()
        setupRefreshButton()
        setupDownloadButton()
        setupCancelButton()
        updateQueueDisplay()
        startAutoRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
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
        progressBar = view.findViewById(R.id.progress_bar)
        cardMyQueue = view.findViewById(R.id.card_my_queue)
        tvQueueList = view.findViewById(R.id.tv_queue_list)
    }

    // ← UPDATED: Tambah cardMyQueue.visibility = View.VISIBLE
    private fun loadBookingData() {
        val activeBooking = DataSource.getActiveBooking()

        if (activeBooking != null) {
            myQueueNumber = activeBooking.queueNumber
            myQueueStatus = when(activeBooking.status) {
                BookingStatus.WAITING -> "Menunggu"
                BookingStatus.CALLED -> "Dipanggil"
                BookingStatus.COMPLETED -> "Selesai"
                BookingStatus.CANCELLED -> "Dibatalkan"
                BookingStatus.MISSED -> "Terlewat"
            }

            tvDoctorInfo.text = "${activeBooking.doctorName} - ${activeBooking.specialization}\n" +
                    "📅 ${formatDateIndonesia(activeBooking.date)} | 🕘 ${activeBooking.time}"

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (activeBooking.date == today) {
                // ✅ FIXED: Current queue minimal 2, maksimal myQueueNumber - 3
                currentQueueNumber = maxOf(2, (myQueueNumber - (7..12).random()))
            } else {
                currentQueueNumber = 1
                Toast.makeText(
                    requireContext(),
                    "📅 Booking Anda untuk tanggal ${formatDateIndonesia(activeBooking.date)}",
                    Toast.LENGTH_LONG
                ).show()
            }

            cardMyQueue.visibility = View.VISIBLE
        } else {
            myQueueNumber = 0
            cardMyQueue.visibility = View.GONE
        }
    }


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

    private fun setupRefreshButton() {
        btnRefresh.setOnClickListener {
            refreshQueueData()
        }
    }

    private fun setupDownloadButton() {
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
    }

    private fun setupCancelButton() {
        btnCancelQueue.setOnClickListener {
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
    }

    private fun cancelQueue() {
        DataSource.getActiveBooking()?.let { booking ->
            val cancelledBooking = booking.copy(
                status = BookingStatus.CANCELLED
            )
            DataSource.addToHistory(cancelledBooking)
        }

        DataSource.clearActiveBooking()
        cardMyQueue.visibility = View.GONE

        Toast.makeText(
            requireContext(),
            "✅ Antrian berhasil dibatalkan dan dipindahkan ke riwayat",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showReceiptDialog() {
        val prefsHelper = PreferencesHelper(requireContext())
        val activeBooking = DataSource.getActiveBooking()

        val booking = if (activeBooking != null) {
            activeBooking
        } else {
            Booking(
                id = "Q${myQueueNumber.toString().padStart(3, '0')}",
                queueNumber = myQueueNumber,
                patientName = prefsHelper.getUserFullName() ?: "Pasien",
                doctorName = "Dr. Ahmad Santoso",
                specialization = "Layanan Umum",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                time = "09:30",
                complaint = "",
                status = when (myQueueStatus) {
                    "Menunggu" -> BookingStatus.WAITING
                    "Dipanggil" -> BookingStatus.CALLED
                    "Siap-siap" -> BookingStatus.WAITING
                    "Terlewat" -> BookingStatus.MISSED
                    else -> BookingStatus.WAITING
                },
                createdAt = System.currentTimeMillis()
            )
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_receipt_options, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val qrContent = QRCodeGenerator.generateBookingQRContent(booking)
        val qrBitmap = QRCodeGenerator.generateQRCode(qrContent, 512, 512)

        val ivQrCode = dialogView.findViewById<ImageView>(R.id.iv_qr_code)
        val tvQueueInfo = dialogView.findViewById<TextView>(R.id.tv_queue_info)
        val btnDownloadPdf = dialogView.findViewById<Button>(R.id.btn_download_pdf)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_close)

        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap)
        } else {
            Toast.makeText(requireContext(), "Gagal generate QR Code", Toast.LENGTH_SHORT).show()
        }

        tvQueueInfo.text = "Nomor Antrian: ${booking.queueNumber}"

        btnDownloadPdf.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            ) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        100
                    )
                    dialog.dismiss()
                    return@setOnClickListener
                }
            }

            Toast.makeText(requireContext(), "Membuat PDF...", Toast.LENGTH_SHORT).show()
            val success = ReceiptGenerator.generateAndSaveReceipt(requireContext(), booking)

            if (success) {
                Toast.makeText(
                    requireContext(),
                    "✅ Struk berhasil diunduh!",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showReceiptDialog()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Izin penyimpanan diperlukan untuk download struk",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            200 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        requireContext(),
                        "Izin notifikasi ditolak. Anda tidak akan menerima notifikasi antrian.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateQueueDisplay() {
        val today = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
        val activeBooking = DataSource.getActiveBooking()

        if (activeBooking != null) {
            tvDoctorInfo.text = "${activeBooking.doctorName} - ${activeBooking.specialization}\n" +
                    "📅 ${formatDateIndonesia(activeBooking.date)} | 🕘 ${activeBooking.time}\n\n" +
                    "📆 Antrian Hari Ini: $today"
        }

        tvCurrentQueue.text = currentQueueNumber.toString()

        if (myQueueNumber > 0) {
            cardMyQueue.visibility = View.VISIBLE
            tvMyQueueNumber.text = "No. $myQueueNumber"

            if (myQueueNumber < currentQueueNumber - 5) {
                myQueueNumber = currentQueueNumber + 10
                myQueueStatus = "Terlambat - Dipindahkan"

                Toast.makeText(
                    requireContext(),
                    "⚠️ Anda terlambat! Antrian dipindahkan ke nomor $myQueueNumber",
                    Toast.LENGTH_LONG
                ).show()

                DataSource.getActiveBooking()?.let {
                    val updated = it.copy(queueNumber = myQueueNumber)
                    DataSource.setActiveBooking(updated)
                }

                hasShownNotification3 = false
                hasShownNotification1 = false
                hasShownNotificationReady = false
            }

            when {
                myQueueNumber < currentQueueNumber -> {
                    myQueueStatus = "Terlewat"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    tvEstimatedTime.text = "Silakan hubungi petugas"
                }
                myQueueNumber == currentQueueNumber -> {
                    myQueueStatus = "Dipanggil"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
                    tvEstimatedTime.text = "Silakan menuju ruang dokter"
                }
                myQueueNumber == currentQueueNumber + 1 -> {
                    myQueueStatus = "Siap-siap"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark))
                    tvEstimatedTime.text = "Bersiap, giliran Anda selanjutnya!"
                }
                else -> {
                    myQueueStatus = "Menunggu"
                    tvMyQueueStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark))
                    calculateEstimatedTime()
                }
            }

            tvMyQueueStatus.text = myQueueStatus
        } else {
            cardMyQueue.visibility = View.GONE
        }

        updateQueueList()
    }

    private fun calculateEstimatedTime() {
        val queueAhead = myQueueNumber - currentQueueNumber
        if (queueAhead <= 0) {
            tvEstimatedTime.text = "Giliran Anda!"
        } else {
            val estimatedMinutes = queueAhead * 5
            tvEstimatedTime.text = "Estimasi: $estimatedMinutes menit lagi\n($queueAhead pasien di depan Anda)"
        }
    }

    private fun updateQueueList() {
        val queueListText = StringBuilder()
        queueListText.append("📋 Daftar Antrian Hari Ini:\n\n")

        for (i in 1..25) {
            when {
                i < currentQueueNumber -> queueListText.append("$i. ✅ Selesai\n")
                i == currentQueueNumber -> queueListText.append("$i. 🔵 Sedang Dilayani\n")
                i == myQueueNumber -> queueListText.append("$i. 🟡 Saya (${myQueueStatus})\n")
                i <= currentQueueNumber + 5 -> queueListText.append("$i. ⏳ Menunggu\n")
                else -> break
            }
        }

        tvQueueList.text = queueListText.toString()
    }

    private fun refreshQueueData() {
        showLoading(true)

        handler.postDelayed({
            if ((1..3).random() == 1) {
                currentQueueNumber++
            }

            updateQueueDisplay()
            showLoading(false)

            Toast.makeText(requireContext(), "Data antrian diperbarui", Toast.LENGTH_SHORT).show()
        }, 1500)
    }

    private fun startAutoRefresh() {
        val refreshRunnable = object : Runnable {
            override fun run() {
                if ((1..2).random() == 1) {
                    currentQueueNumber++
                    updateQueueDisplay()

                    val queueAhead = myQueueNumber - currentQueueNumber

                    when {
                        myQueueNumber == currentQueueNumber && !hasShownNotificationReady -> {
                            NotificationHelper.showQueueReadyNotification(
                                requireContext(),
                                myQueueNumber
                            )
                            hasShownNotificationReady = true
                        }
                        queueAhead == 3 && !hasShownNotification3 -> {
                            NotificationHelper.showQueueAlmostReadyNotification(
                                requireContext(),
                                myQueueNumber,
                                queueAhead
                            )
                            hasShownNotification3 = true
                        }
                        queueAhead == 1 && !hasShownNotification1 -> {
                            NotificationHelper.showQueueAlmostReadyNotification(
                                requireContext(),
                                myQueueNumber,
                                queueAhead
                            )
                            hasShownNotification1 = true
                        }
                    }
                }

                handler.postDelayed(this, 5000)
            }
        }
        handler.post(refreshRunnable)
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        return dateFormat.format(calendar.time)
    }

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
