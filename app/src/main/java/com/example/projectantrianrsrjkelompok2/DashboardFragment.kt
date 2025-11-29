package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout  // ✅ TAMBAHKAN INI
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvActiveQueue: TextView

    // ✅ UBAH: Dari CardView ke LinearLayout
    private lateinit var layoutQuickBooking: LinearLayout
    private lateinit var layoutEmergency: LinearLayout

    private lateinit var ivProfileIcon: ImageView

    // Cards untuk poli klinik
    private lateinit var cardPoliUmum: CardView
    private lateinit var cardPoliGigi: CardView
    private lateinit var cardPoliMata: CardView
    private lateinit var cardPoliJantung: CardView
    private lateinit var cardPoliAnak: CardView
    private lateinit var cardPoliKandungan: CardView

    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesHelper = PreferencesHelper(requireContext())

        initViews(view)
        setupUI()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvCurrentDate = view.findViewById(R.id.tv_current_date)
        tvActiveQueue = view.findViewById(R.id.tv_active_queue)

        // ✅ UBAH: Casting ke LinearLayout
        layoutQuickBooking = view.findViewById(R.id.btn_quick_booking)
        layoutEmergency = view.findViewById(R.id.btn_emergency)

        ivProfileIcon = view.findViewById(R.id.ivProfileIcon)

        // Inisialisasi card poli
        cardPoliUmum = view.findViewById(R.id.card_poli_umum)
        cardPoliGigi = view.findViewById(R.id.card_poli_gigi)
        cardPoliMata = view.findViewById(R.id.card_poli_mata)
        cardPoliJantung = view.findViewById(R.id.card_poli_jantung)
        cardPoliAnak = view.findViewById(R.id.card_poli_anak)
        cardPoliKandungan = view.findViewById(R.id.card_poli_kandungan)
    }

    private fun setupUI() {
        // Set welcome message dengan nama user
        val username = preferencesHelper.getUsername()
        tvWelcome.text = "Selamat Datang, $username! 👋"

        // Set tanggal saat ini
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            .format(Date())
        tvCurrentDate.text = currentDate

        // Tampilkan info antrian aktif (simulasi)
        showActiveQueueInfo()
    }

    private fun setupClickListeners() {
        // Profile icon click listener
        ivProfileIcon.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ProfileFragment())
        }

        // ✅ UBAH: Gunakan layoutQuickBooking dan layoutEmergency
        layoutQuickBooking.setOnClickListener {
            navigateToBooking(0)
        }

        layoutEmergency.setOnClickListener {
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
        val hasActiveQueue = true

        if (hasActiveQueue) {
            tvActiveQueue.text = "Antrian Aktif: Layanan Klinik Umum - No. 15\nStatus: Menunggu (estimasi 30 menit)"
            tvActiveQueue.visibility = View.VISIBLE
        } else {
            tvActiveQueue.visibility = View.GONE
        }
    }

    private fun showEmergencyInfo() {
        tvActiveQueue.text = "🚨 Untuk kondisi darurat, segera hubungi: \n(021) 1234-5678 atau datang langsung ke UGD"
        tvActiveQueue.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        tvActiveQueue.visibility = View.VISIBLE
    }

    private fun navigateToBooking(specializationId: Int) {
        val bookingFragment = BookingFragment()

        if (specializationId > 0) {
            val bundle = Bundle()
            bundle.putInt("selected_specialization_id", specializationId)
            bookingFragment.arguments = bundle
        }

        (activity as MainActivity).navigateToFragment(bookingFragment)
    }
}