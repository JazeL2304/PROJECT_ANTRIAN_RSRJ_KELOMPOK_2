package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvCurrentDate: TextView
    private lateinit var tvActiveQueue: TextView

    private lateinit var layoutQuickBooking: LinearLayout
    private lateinit var layoutEmergency: LinearLayout
    private lateinit var ivProfileIcon: ImageView

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
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesHelper = PreferencesHelper(requireContext())

        initViews(view)
        setupUI()
        setupClickListeners()
        loadDashboardData() // ✅ REAL DATABASE DATA
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tv_welcome)
        tvCurrentDate = view.findViewById(R.id.tv_current_date)
        tvActiveQueue = view.findViewById(R.id.tv_active_queue)

        layoutQuickBooking = view.findViewById(R.id.btn_quick_booking)
        layoutEmergency = view.findViewById(R.id.btn_emergency)
        ivProfileIcon = view.findViewById(R.id.ivProfileIcon)

        cardPoliUmum = view.findViewById(R.id.card_poli_umum)
        cardPoliGigi = view.findViewById(R.id.card_poli_gigi)
        cardPoliMata = view.findViewById(R.id.card_poli_mata)
        cardPoliJantung = view.findViewById(R.id.card_poli_jantung)
        cardPoliAnak = view.findViewById(R.id.card_poli_anak)
        cardPoliKandungan = view.findViewById(R.id.card_poli_kandungan)
    }

    private fun setupUI() {
        val username = preferencesHelper.getUsername()
        tvWelcome.text = "Selamat Datang, $username! 👋"

        val currentDate =
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                .format(Date())

        tvCurrentDate.text = currentDate
    }

    // ================================
    // ✅ REALTIME DASHBOARD LOAD
    // ================================
    private fun loadDashboardData() {

        lifecycleScope.launch {

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val activeBooking = withContext(Dispatchers.IO) {
                DataSource.getBookingHistory()
                    .firstOrNull { it.date == today }
            }

            if (activeBooking != null) {

                tvActiveQueue.text =
                    "Antrian Aktif:\n" +
                            "${activeBooking.specialization}\n" +
                            "Dokter: ${activeBooking.doctorName}\n" +
                            "No: ${activeBooking.queueNumber}\n" +
                            "Jam: ${activeBooking.time}\n" +
                            "Status: ${activeBooking.status}"

                tvActiveQueue.visibility = View.VISIBLE

            } else {
                tvActiveQueue.text = "Tidak ada antrian aktif hari ini"
                tvActiveQueue.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {

        ivProfileIcon.setOnClickListener {
            (activity as? MainActivity)
                ?.navigateToFragment(ProfileFragment())
        }

        layoutQuickBooking.setOnClickListener {
            navigateToBooking(0)
        }

        layoutEmergency.setOnClickListener {
            showEmergencyInfo()
        }

        cardPoliUmum.setOnClickListener { navigateToBooking(1) }
        cardPoliGigi.setOnClickListener { navigateToBooking(2) }
        cardPoliMata.setOnClickListener { navigateToBooking(3) }
        cardPoliJantung.setOnClickListener { navigateToBooking(4) }
        cardPoliAnak.setOnClickListener { navigateToBooking(5) }
        cardPoliKandungan.setOnClickListener { navigateToBooking(6) }
    }

    private fun showEmergencyInfo() {
        tvActiveQueue.text =
            "🚨 Untuk kondisi darurat hubungi:\n(021) 1234-5678 atau datang langsung ke UGD"
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

        (activity as? MainActivity)
            ?.navigateToFragment(bookingFragment)
    }
}
