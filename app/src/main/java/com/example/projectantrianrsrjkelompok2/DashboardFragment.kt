package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvActiveQueue: TextView
    private lateinit var btnQuickBooking: Button
    private lateinit var btnEmergency: Button

    // Cards untuk poli klinik
    private lateinit var cardPoliUmum: CardView
    private lateinit var cardPoliGigi: CardView
    private lateinit var cardPoliMata: CardView
    private lateinit var cardPoliJantung: CardView
    private lateinit var cardPoliAnak: CardView
    private lateinit var cardPoliKandungan: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupUI()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvCurrentDate = view.findViewById(R.id.tv_current_date)
        tvActiveQueue = view.findViewById(R.id.tv_active_queue)
        btnQuickBooking = view.findViewById(R.id.btn_quick_booking)
        btnEmergency = view.findViewById(R.id.btn_emergency)

        // Inisialisasi card poli
        cardPoliUmum = view.findViewById(R.id.card_poli_umum)
        cardPoliGigi = view.findViewById(R.id.card_poli_gigi)
        cardPoliMata = view.findViewById(R.id.card_poli_mata)
        cardPoliJantung = view.findViewById(R.id.card_poli_jantung)
        cardPoliAnak = view.findViewById(R.id.card_poli_anak)
        cardPoliKandungan = view.findViewById(R.id.card_poli_kandungan)
    }

    private fun setupUI() {
        // Set welcome message
        tvWelcome.text = "Selamat Datang di RS Sehat"

        // Set tanggal saat ini
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            .format(Date())
        tvCurrentDate.text = currentDate

        // Tampilkan info antrian aktif (simulasi)
        showActiveQueueInfo()
    }

    private fun setupClickListeners() {
        // Quick booking button
        btnQuickBooking.setOnClickListener {
            navigateToBooking(0) // 0 = tanpa spesialisasi terpilih
        }

        // Emergency button
        btnEmergency.setOnClickListener {
            // Untuk sekarang hanya tampilkan pesan
            showEmergencyInfo()
        }

        // Click listeners untuk setiap card poli
        cardPoliUmum.setOnClickListener { navigateToBooking(1) }
        cardPoliGigi.setOnClickListener { navigateToBooking(2) }
        cardPoliMata.setOnClickListener { navigateToBooking(3) }
        cardPoliJantung.setOnClickListener { navigateToBooking(4) }
        cardPoliAnak.setOnClickListener { navigateToBooking(5) }
        cardPoliKandungan.setOnClickListener { navigateToBooking(6) }
    }

    private fun showActiveQueueInfo() {
        // Simulasi antrian aktif
        val hasActiveQueue = true // Bisa diubah untuk testing

        if (hasActiveQueue) {
            tvActiveQueue.text = "Antrian Aktif: Layanan Klinik Umum - No. 15\nStatus: Menunggu (estimasi 30 menit)"
            tvActiveQueue.visibility = View.VISIBLE
        } else {
            tvActiveQueue.visibility = View.GONE
        }
    }

    private fun showEmergencyInfo() {
        // Simulasi info emergency - bisa diganti dengan dialog atau navigasi khusus
        tvActiveQueue.text = "🚨 Untuk kondisi darurat, segera hubungi: \n(021) 1234-5678 atau datang langsung ke UGD"
        tvActiveQueue.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        tvActiveQueue.visibility = View.VISIBLE
    }

    private fun navigateToBooking(specializationId: Int) {
        val bookingFragment = BookingFragment()

        // Jika ada spesialisasi yang dipilih, kirim via Bundle
        if (specializationId > 0) {
            val bundle = Bundle()
            bundle.putInt("selected_specialization_id", specializationId)
            bookingFragment.arguments = bundle
        }

        // Navigate menggunakan MainActivity
        (activity as MainActivity).navigateToFragment(bookingFragment)
    }
}