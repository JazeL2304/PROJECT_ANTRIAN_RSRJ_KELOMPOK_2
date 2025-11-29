package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // ❌ REMOVED: btnProfileIcon dan tvToolbarTitle (toolbar sudah dihapus)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔧 Inisialisasi helper & komponen UI
        preferencesHelper = PreferencesHelper(this)
        NotificationHelper.createNotificationChannel(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        // ✅ SEED DATA FIREBASE (async, tidak blocking UI)
        seedFirebaseDataIfNeeded()

        // ✅ Clear session SAJA
        preferencesHelper.clearSession()

        // ✅ Langsung tampilkan halaman login
        loadFragment(LoginFragment())
        hideBottomNavigation()

        handleNotificationIntent()
    }

    // 🌱 Seed Firebase with dummy data on first launch (ASYNC)
    private fun seedFirebaseDataIfNeeded() {
        val isFirstLaunch = preferencesHelper.isFirstLaunch()

        if (isFirstLaunch) {
            // ✅ FIXED: Use lifecycleScope with Dispatchers.IO
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("MainActivity", "🌱 Starting Firebase seed...")
                    com.example.projectantrianrsrjkelompok2.utils.FirebaseSeedData.seedAllData()

                    // Mark as complete
                    withContext(Dispatchers.Main) {
                        preferencesHelper.setFirstLaunchComplete()
                        Log.d("MainActivity", "✅ Firebase seed completed!")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Firebase seed failed: ${e.message}", e)
                }
            }
        } else {
            Log.d("MainActivity", "ℹ️ Not first launch, skipping seed")
        }
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
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_booking -> {
                    showBottomNavigation()
                    loadFragment(BookingFragment())
                    true
                }
                R.id.nav_queue -> {
                    showBottomNavigation()
                    if (DataSource.hasActiveBooking()) {
                        loadFragment(QueueFragment())
                    } else {
                        loadFragment(EmptyQueueFragment())
                    }
                    true
                }
                R.id.nav_history -> {
                    showBottomNavigation()
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_profile -> {
                    showBottomNavigation()
                    loadFragment(fragment_news())
                    true
                }
                else -> false
            }
        }
    }

    // ⚙️ Setup Bottom Navigation untuk ADMIN
    private fun setupAdminNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu_admin)
        bottomNavigation.visibility = View.VISIBLE

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard_admin -> {
                    showBottomNavigation()
                    loadFragment(AdminDashboardFragment())
                    true
                }
                R.id.nav_reports -> {
                    showBottomNavigation()
                    loadFragment(ViewReportFragment())
                    true
                }
                R.id.nav_settings -> {
                    showBottomNavigation()
                    loadFragment(AdminSettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // ⚙️ Setup Bottom Navigation untuk DOKTER
    private fun setupDoctorNavigation() {
        bottomNavigation.menu.clear()
        bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu_doctor)
        bottomNavigation.visibility = View.VISIBLE

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard_doctor -> {
                    showBottomNavigation()
                    loadFragment(DoctorDashboardFragment())
                    true
                }
                R.id.nav_doctor_queue -> {
                    showBottomNavigation()
                    loadFragment(DoctorQueueFragment())
                    true
                }
                R.id.nav_patient_history -> {
                    showBottomNavigation()
                    loadFragment(DoctorPatientHistoryFragment())
                    true
                }
                else -> false
            }
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
        // ❌ REMOVED: btnProfileIcon visibility (sudah tidak ada)
    }

    // 🔹 Tampilkan bottom navigation
    fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
        // ❌ REMOVED: btnProfileIcon visibility (sudah tidak ada)
    }

    // 🚪 Logout user
    fun logout() {
        preferencesHelper.clearSession()
        hideBottomNavigation()
        loadFragment(LoginFragment())
    }

    // 👤 Pasien → dashboard pasien + setup nav pasien
    fun showPatientDashboard() {
        setupPatientNavigation()
        showBottomNavigation()
        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard

        // ✅ Preload data from Firebase
        preloadDataFromFirebase()
    }

    // 🩺 Dokter → dashboard dokter + setup nav dokter
    fun showDoctorDashboard() {
        setupDoctorNavigation()
        showBottomNavigation()
        loadFragment(DoctorDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_doctor

        // ✅ Preload data from Firebase
        preloadDataFromFirebase()
    }

    // 🧾 Admin → dashboard admin + setup nav admin
    fun showAdminDashboard() {
        setupAdminNavigation()
        showBottomNavigation()
        loadFragment(AdminDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_admin

        // ✅ Preload data from Firebase
        preloadDataFromFirebase()
    }

    // 📥 Preload data from Firebase after login
    private fun preloadDataFromFirebase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "📥 Preloading data from Firebase...")
                DataSource.forceLoadFromFirebase()
                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "✅ Data preloaded successfully!")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Preload failed: ${e.message}", e)
            }
        }
    }

    // ❌ REMOVED: setToolbarTitle() - tidak ada lagi toolbar
}