package com.example.project_antrian_rsrj_kelompok_2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.project_antrian_rsrj_kelompok_2.R
import java.text.SimpleDateFormat
import java.util.*

class QueueFragment : Fragment() {

    private lateinit var tvCurrentQueue: TextView
    private lateinit var tvMyQueueNumber: TextView
    private lateinit var tvMyQueueStatus: TextView
    private lateinit var tvEstimatedTime: TextView
    private lateinit var tvDoctorInfo: TextView
    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardMyQueue: LinearLayout
    private lateinit var tvQueueList: TextView

    // Simulasi data antrian
    private var currentQueueNumber = 8
    private var myQueueNumber = 15
    private var myQueueStatus = "Menunggu"
    private val handler = Handler(Looper.getMainLooper())

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
        setupUI()
        setupRefreshButton()
        updateQueueDisplay()

        // Auto refresh setiap 10 detik (simulasi real-time)
        startAutoRefresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop auto refresh saat fragment di destroy
        handler.removeCallbacksAndMessages(null)
    }

    private fun initViews(view: View) {
        tvCurrentQueue = view.findViewById(R.id.tv_current_queue)
        tvMyQueueNumber = view.findViewById(R.id.tv_my_queue_number)
        tvMyQueueStatus = view.findViewById(R.id.tv_my_queue_status)
        tvEstimatedTime = view.findViewById(R.id.tv_estimated_time)
        tvDoctorInfo = view.findViewById(R.id.tv_doctor_info)
        btnRefresh = view.findViewById(R.id.btn_refresh)
        progressBar = view.findViewById(R.id.progress_bar)
        cardMyQueue = view.findViewById(R.id.card_my_queue)
        tvQueueList = view.findViewById(R.id.tv_queue_list)
    }

    private fun setupUI() {
        // Set info dokter (simulasi)
        tvDoctorInfo.text = "Dr. Ahmad Santoso - Poli Umum\n📅 ${getCurrentDate()} | 🕘 09:30"
    }

    private fun setupRefreshButton() {
        btnRefresh.setOnClickListener {
            refreshQueueData()
        }
    }

    private fun updateQueueDisplay() {
        // Update nomor antrian saat ini
        tvCurrentQueue.text = currentQueueNumber.toString()

        // Update antrian saya
        if (myQueueNumber > 0) {
            cardMyQueue.visibility = View.VISIBLE
            tvMyQueueNumber.text = "No. $myQueueNumber"
            tvMyQueueStatus.text = myQueueStatus

            // Update status berdasarkan posisi antrian
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

        // Update daftar antrian
        updateQueueList()
    }

    private fun calculateEstimatedTime() {
        val queueAhead = myQueueNumber - currentQueueNumber
        if (queueAhead <= 0) {
            tvEstimatedTime.text = "Giliran Anda!"
        } else {
            val estimatedMinutes = queueAhead * 8 // Asumsi 8 menit per pasien
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

        // Simulasi delay API call
        handler.postDelayed({
            // Simulasi kemajuan antrian (kadang-kadang maju)
            if ((1..3).random() == 1) { // 33% chance nomor antrian maju
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
                // Auto refresh tanpa loading indicator
                if ((1..5).random() == 1) { // 20% chance auto update
                    currentQueueNumber++
                    updateQueueDisplay()
                }

                // Schedule next refresh
                handler.postDelayed(this, 15000) // 15 detik
            }
        }
        handler.post(refreshRunnable)
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        return dateFormat.format(calendar.time)
    }

    // ✅ Tambahan ini
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
