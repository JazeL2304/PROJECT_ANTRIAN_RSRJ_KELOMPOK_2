package com.example.projectantrianrsrjkelompok2


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

// ========== FRAGMENT PASIEN ==========
import com.example.projectantrianrsrjkelompok2.BookingFragment
import com.example.projectantrianrsrjkelompok2.DashboardFragment
import com.example.projectantrianrsrjkelompok2.EmptyQueueFragment
import com.example.projectantrianrsrjkelompok2.HistoryFragment
import com.example.projectantrianrsrjkelompok2.LoginFragment
import com.example.projectantrianrsrjkelompok2.ProfileFragment
import com.example.projectantrianrsrjkelompok2.QueueFragment
import com.example.projectantrianrsrjkelompok2.fragment_news

// ========== FRAGMENT ADMIN ==========
import com.example.projectantrianrsrjkelompok2.admin.AdminDashboardFragment
import com.example.projectantrianrsrjkelompok2.admin.AdminSettingsFragment
import com.example.projectantrianrsrjkelompok2.admin.ManageDoctorFragment
import com.example.projectantrianrsrjkelompok2.admin.ManagePatientFragment
import com.example.projectantrianrsrjkelompok2.admin.ManageScheduleFragment
import com.example.projectantrianrsrjkelompok2.admin.ViewReportFragment

// ========== FRAGMENT DOKTER ==========
import com.example.projectantrianrsrjkelompok2.doctor.DoctorDashboardFragment
import com.example.projectantrianrsrjkelompok2.doctor.DoctorQueueFragment
import com.example.projectantrianrsrjkelompok2.doctor.DoctorPatientHistoryFragment

// ========== UTILS ==========
import com.example.projectantrianrsrjkelompok2.utils.NotificationHelper
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper

// ========== MATERIAL COMPONENTS ==========
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var btnProfileIcon: ImageView
    private var tvToolbarTitle: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔧 Inisialisasi helper & komponen UI
        preferencesHelper = PreferencesHelper(this)
        NotificationHelper.createNotificationChannel(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        btnProfileIcon = findViewById(R.id.btnProfileIcon)
        tvToolbarTitle = findViewById(R.id.toolbarTitle)

        setupProfileIcon()

        // ✅ FIXED: Clear session SAJA (jangan clear data booking)
        preferencesHelper.clearSession()
        // ❌ DIHAPUS: DataSource.clearActiveBooking()

        // ✅ Langsung tampilkan halaman login
        setToolbarTitle("Login Akun")
        loadFragment(LoginFragment())
        hideBottomNavigation()

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

    // ⚙️ Setup Bottom Navigation untuk PASIEN
    private fun setupPatientNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    showBottomNavigation()
                    setToolbarTitle("Antrian Rumah Sakit")
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_booking -> {
                    showBottomNavigation()
                    setToolbarTitle("Booking Dokter")
                    loadFragment(BookingFragment())
                    true
                }
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
                R.id.nav_history -> {
                    showBottomNavigation()
                    setToolbarTitle("Riwayat Kunjungan")
                    loadFragment(HistoryFragment())
                    true
                }
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

    // ⚙️ ← BARU! Setup Bottom Navigation untuk ADMIN
    private fun setupAdminNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu_admin)
        bottomNavigation.visibility = View.VISIBLE

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard_admin -> {
                    showBottomNavigation()
                    setToolbarTitle("Dashboard Admin")
                    loadFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_reports -> {
                    showBottomNavigation()
                    setToolbarTitle("Laporan Rumah Sakit")
                    loadFragment(ViewReportFragment())
                    true
                }
                R.id.nav_settings -> {
                    showBottomNavigation()
                    setToolbarTitle("Pengaturan")
                    loadFragment(AdminSettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // ⚙️ ← BARU! Setup Bottom Navigation untuk DOKTER
    private fun setupDoctorNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu_doctor)
        bottomNavigation.visibility = View.VISIBLE

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard_doctor -> {
                    showBottomNavigation()
                    setToolbarTitle("Dashboard Dokter")
                    loadFragment(DoctorDashboardFragment())
                    true
                }
                R.id.nav_doctor_queue -> {
                    showBottomNavigation()
                    setToolbarTitle("Antrian Pasien")
                    loadFragment(DoctorQueueFragment())
                    true
                }
                R.id.nav_patient_history -> {
                    showBottomNavigation()
                    setToolbarTitle("Riwayat Pasien")
                    loadFragment(DoctorPatientHistoryFragment())
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

    // ✅ Cek status login & arahkan sesuai role (TETAP ADA, dipanggil saat login berhasil)
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
        bottomNavigation.visibility = View.GONE
        btnProfileIcon.visibility = View.GONE
    }

    // 🔹 Tampilkan bottom navigation
    fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
        btnProfileIcon.visibility = View.VISIBLE
    }

    // 🚪 Logout user
    // ✅ FIXED: Jangan hapus bookingHistory saat logout!
    fun logout() {
        preferencesHelper.clearSession()
        // ❌ DIHAPUS: DataSource.clearActiveBooking()
        hideBottomNavigation()
        setToolbarTitle("Login Akun")
        loadFragment(LoginFragment())
    }

    // 👤 ← UPDATE! Pasien → dashboard pasien + setup nav pasien
    fun showPatientDashboard() {
        setupPatientNavigation()  // ← Setup menu pasien
        showBottomNavigation()
        setToolbarTitle("Antrian Rumah Sakit")
        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    // 🩺 ← UPDATE! Dokter → dashboard dokter + setup nav dokter
    fun showDoctorDashboard() {
        setupDoctorNavigation()  // ← Setup menu dokter
        showBottomNavigation()
        setToolbarTitle("Dashboard Dokter")
        loadFragment(DoctorDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_doctor
    }

    // 🧾 ← UPDATE! Admin → dashboard admin + setup nav admin
    fun showAdminDashboard() {
        setupAdminNavigation()  // ← Setup menu admin (TANPA booking, queue, history)
        showBottomNavigation()
        setToolbarTitle("Dashboard Admin")
        loadFragment(AdminDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_admin
    }

    // 🆕 Ubah judul toolbar utama
    private fun setToolbarTitle(title: String) {
        tvToolbarTitle?.text = title
    }
}
