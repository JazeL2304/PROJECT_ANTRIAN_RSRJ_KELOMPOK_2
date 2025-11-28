package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper  // ← TAMBAHAN
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvActiveQueue: TextView

    // ✅ UBAH: Ganti Button jadi LinearLayout atau View
    private lateinit var btnQuickBooking: View  // atau LinearLayout
    private lateinit var btnEmergency: View     // atau LinearLayout

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

        // ✅ FIXED: Ganti Button jadi View/LinearLayout
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
        // Set welcome message dengan nama user
        val username = preferencesHelper.getUsername()
        tvWelcome.text = "Selamat Datang, $username! 👋"

        // Set tanggal saat ini
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            .format(Date())
        tvCurrentDate.text = currentDate

        // Tampilkan info antrian aktif (opsional)
        showActiveQueueInfo()
    }

    private fun setupClickListeners() {
        // ✅ FIXED: setOnClickListener tetap bisa digunakan untuk View/LinearLayout
        btnQuickBooking.setOnClickListener {
            navigateToBooking(0) // 0 = tanpa spesialisasi terpilih
        }

        btnEmergency.setOnClickListener {
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
        // Cek apakah ada booking aktif
        val activeBooking = DataSource.getActiveBooking()

        if (activeBooking != null) {
            tvActiveQueue.text = "Antrian Aktif: ${activeBooking.specialization} - No. ${activeBooking.queueNumber}\n" +
                    "Status: ${activeBooking.status.toDisplayString()} (estimasi 30 menit)"

            // ✅ Tampilkan card active queue jika ada
            val cardActiveQueue = view?.findViewById<CardView>(R.id.card_active_queue)
            cardActiveQueue?.visibility = View.VISIBLE
        } else {
            // Sembunyikan card jika tidak ada antrian aktif
            val cardActiveQueue = view?.findViewById<CardView>(R.id.card_active_queue)
            cardActiveQueue?.visibility = View.GONE
        }
    }

    private fun showEmergencyInfo() {
        // Show emergency dialog atau info
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("🚨 Layanan Darurat")
            .setMessage("Untuk kondisi darurat, segera hubungi:\n\n📞 (021) 1234-5678\n\natau datang langsung ke UGD")
            .setPositiveButton("OK", null)
            .show()
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