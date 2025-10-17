package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.admin.AdminDashboardFragment
import com.example.projectantrianrsrjkelompok2.doctor.DoctorDashboardFragment
import com.example.projectantrianrsrjkelompok2.utils.NotificationHelper
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var btnProfileIcon: ImageView
    private var tvToolbarTitle: TextView? = null // untuk ubah judul toolbar atas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔧 Inisialisasi helper & komponen UI
        preferencesHelper = PreferencesHelper(this)
        NotificationHelper.createNotificationChannel(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        btnProfileIcon = findViewById(R.id.btnProfileIcon)
        tvToolbarTitle = findViewById(R.id.toolbarTitle)

        setupBottomNavigation()
        setupProfileIcon()

        // ✅ Jalankan checkLoginStatus setelah sedikit delay agar role tersimpan
        window.decorView.postDelayed({
            checkLoginStatus()
        }, 250)

        handleNotificationIntent()
    }

    // 🔔 Jika notifikasi membuka QueueFragment
    private fun handleNotificationIntent() {
        if (intent.getBooleanExtra("open_queue_fragment", false)) {
            if (DataSource.hasActiveBooking()) {
                loadFragment(QueueFragment())
            } else {
                loadFragment(EmptyQueueFragment())
            }
            bottomNavigation.selectedItemId = R.id.nav_queue
        }
    }

    // ⚙️ Navigasi bottom — semua role bisa akses menu umum
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val userRole = preferencesHelper.getUserRole()

            when (item.itemId) {
                // 🏠 Dashboard menyesuaikan role
                R.id.nav_dashboard -> {
                    when (userRole) {
                        "ADMIN" -> {
                            showBottomNavigation()
                            setToolbarTitle("Dashboard Admin")
                            loadFragment(AdminDashboardFragment())
                        }

                        "DOCTOR" -> {
                            showBottomNavigation() // ✅ tampilkan bottom nav untuk dokter
                            setToolbarTitle("Dashboard Dokter")
                            loadFragment(DoctorDashboardFragment())
                        }

                        else -> {
                            showBottomNavigation()
                            setToolbarTitle("Antrian Rumah Sakit")
                            loadFragment(DashboardFragment())
                        }
                    }
                    true
                }

                // 📅 Semua role boleh buka Booking
                R.id.nav_booking -> {
                    showBottomNavigation()
                    setToolbarTitle("Booking Dokter")
                    loadFragment(BookingFragment())
                    true
                }

                // 🧾 Semua role boleh buka Queue
                R.id.nav_queue -> {
                    showBottomNavigation()
                    setToolbarTitle("Antrian Anda")
                    if (DataSource.hasActiveBooking()) {
                        loadFragment(QueueFragment())
                    } else {
                        loadFragment(EmptyQueueFragment())
                    }
                    true
                }

                // 🕒 Semua role boleh buka Riwayat
                R.id.nav_history -> {
                    showBottomNavigation()
                    setToolbarTitle("Riwayat Kunjungan")
                    loadFragment(HistoryFragment())
                    true
                }

                // 📰 Semua role boleh buka News
                R.id.nav_profile -> {
                    showBottomNavigation()
                    setToolbarTitle("Berita Kesehatan")
                    loadFragment(fragment_news())
                    true
                }

                else -> false
            }
        }
    }

    // 🧍 Tombol profil di pojok atas
    private fun setupProfileIcon() {
        btnProfileIcon.setOnClickListener {
            showBottomNavigation()
            setToolbarTitle("Profil Pengguna")
            loadFragment(ProfileFragment())

            // Hilangkan highlight menu di bottom navigation
            bottomNavigation.menu.setGroupCheckable(0, true, false)
            for (i in 0 until bottomNavigation.menu.size()) {
                bottomNavigation.menu.getItem(i).isChecked = false
            }
            bottomNavigation.menu.setGroupCheckable(0, true, true)
        }
    }

    // ✅ Cek status login & arahkan sesuai role
    private fun checkLoginStatus() {
        val isLoggedIn = preferencesHelper.isLoggedIn()
        val userRole = preferencesHelper.getUserRole()

        Log.d("ROLE_DEBUG", "LoggedIn=$isLoggedIn, Role=$userRole")

        if (!isLoggedIn) {
            setToolbarTitle("Login Akun")
            loadFragment(LoginFragment())
            hideBottomNavigation()
            return
        }

        when (userRole) {
            "PATIENT" -> showPatientDashboard()
            "DOCTOR" -> showDoctorDashboard()
            "ADMIN" -> showAdminDashboard()
            else -> showPatientDashboard()
        }
    }

    // 📦 Ganti fragment dengan animasi lembut
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Navigasi manual antar fragment
    fun navigateToFragment(fragment: Fragment) {
        loadFragment(fragment)
        showBottomNavigation()
    }

    // Navigasi ke login/signup
    fun navigateToLoginOrSignup(fragment: Fragment) {
        loadFragment(fragment)
        hideBottomNavigation()
    }

    // 🔹 Sembunyikan bottom navigation
    fun hideBottomNavigation() {
        bottomNavigation.visibility = android.view.View.GONE
        btnProfileIcon.visibility = android.view.View.GONE
    }

    // 🔹 Tampilkan bottom navigation
    fun showBottomNavigation() {
        bottomNavigation.visibility = android.view.View.VISIBLE
        btnProfileIcon.visibility = android.view.View.VISIBLE
    }

    // 🚪 Logout user
    fun logout() {
        preferencesHelper.clearSession()
        DataSource.clearActiveBooking()
        hideBottomNavigation()
        setToolbarTitle("Login Akun")
        loadFragment(LoginFragment())
    }

    // 👤 Pasien → dashboard pasien
    fun showPatientDashboard() {
        showBottomNavigation()
        setToolbarTitle("Antrian Rumah Sakit")
        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    // 🩺 Dokter → dashboard dokter (❗update baru)
    fun showDoctorDashboard() {
        showBottomNavigation()
        setToolbarTitle("Dashboard Dokter")
        loadFragment(DoctorDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    // 🧾 Admin → dashboard admin
    fun showAdminDashboard() {
        showBottomNavigation()
        setToolbarTitle("Dashboard Admin")
        loadFragment(AdminDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    // 🆕 Ubah judul toolbar utama
    private fun setToolbarTitle(title: String) {
        tvToolbarTitle?.text = title
    }
}
