package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

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
import com.example.projectantrianrsrjkelompok2.utils.FirebaseSeedData
import com.example.projectantrianrsrjkelompok2.utils.FirebaseDataMigration

// ========== MATERIAL COMPONENTS ==========
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * ✅ UPDATED MainActivity - With Auto Migration
 *
 * FITUR BARU:
 * 1. Auto-update admin001 → "angelica"
 * 2. Auto-update doc002 password → "siti123"
 * 3. Auto-hash semua password plain text
 * 4. Jalan otomatis saat app start
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔧 Inisialisasi helper & komponen UI
        preferencesHelper = PreferencesHelper(this)
        NotificationHelper.createNotificationChannel(this)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        // ✅ AUTO-MIGRATE existing data (SEBELUM seed)
        autoMigrateExistingData()

        // ✅ Seed data Firebase HANYA pada first launch
        seedFirebaseDataIfNeeded()

        // ✅ Clear session untuk logout otomatis
        preferencesHelper.clearSession()

        // ✅ Tampilkan halaman login
        loadFragment(LoginFragment())
        hideBottomNavigation()

        // Handle notifikasi
        handleNotificationIntent()
    }

    /**
     * 🔄 AUTO-MIGRATE EXISTING DATA
     *
     * Fungsi ini akan:
     * 1. Cek apakah data perlu di-update
     * 2. Update admin001 fullName → "angelica"
     * 3. Update doc002 password → "siti123" (hashed)
     * 4. Hash SEMUA password yang masih plain text
     *
     * Jalan OTOMATIS setiap kali app start, tapi hanya update yang perlu.
     */
    private fun autoMigrateExistingData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "🔍 Checking if data migration is needed...")

                // Check apakah perlu migration
                val needsMigration = FirebaseDataMigration.needsMigration()

                if (!needsMigration) {
                    Log.d("MainActivity", "✅ Data already up-to-date, no migration needed")
                    return@launch
                }

                Log.d("MainActivity", "🔄 Starting data migration...")

                // Run migration
                val success = FirebaseDataMigration.migrateExistingData()

                withContext(Dispatchers.Main) {
                    if (success) {
                        Log.d("MainActivity", "✅ Data migration completed!")
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Data berhasil diupdate",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.e("MainActivity", "❌ Data migration failed")
                    }
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Migration error: ${e.message}", e)
            }
        }
    }

    /**
     * 🌱 Seed Firebase with data on FIRST LAUNCH only
     */
    private fun seedFirebaseDataIfNeeded() {
        val isFirstLaunch = preferencesHelper.isFirstLaunch()

        Log.d("MainActivity", "🔍 Checking first launch: $isFirstLaunch")

        if (isFirstLaunch) {
            // ✅ FIRST LAUNCH - Seed data baru
            Log.d("MainActivity", "🌱 First launch detected, starting seed...")

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Wait for migration to complete first
                    delay(1000)

                    Log.d("MainActivity", "🌱 Seeding Firebase data...")

                    // Invalidate cache
                    DataSource.invalidateCache()

                    // Seed semua data (users sudah include password hashed)
                    FirebaseSeedData.seedAllData()
                    delay(2000)

                    // Force load from Firebase
                    DataSource.forceLoadFromFirebase()

                    withContext(Dispatchers.Main) {
                        // Mark first launch as complete
                        preferencesHelper.setFirstLaunchComplete()
                        Log.d("MainActivity", "✅ Firebase seed completed!")

                        Toast.makeText(
                            this@MainActivity,
                            "✅ Data berhasil dimuat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Firebase seed failed: ${e.message}", e)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "⚠️ Gagal memuat data awal",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            // ✅ NOT FIRST LAUNCH - Preload existing data
            Log.d("MainActivity", "📥 Not first launch, preloading existing data...")
            preloadDataFromFirebase()
        }
    }

    /**
     * 🔔 Handle notification intent
     */
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

    /**
     * ⚙️ Setup Bottom Navigation untuk PASIEN
     */
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

    /**
     * ⚙️ Setup Bottom Navigation untuk ADMIN
     */
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

    /**
     * ⚙️ Setup Bottom Navigation untuk DOKTER
     */
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

    /**
     * 📦 Ganti fragment dengan animasi
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Navigasi manual antar fragment
     */
    fun navigateToFragment(fragment: Fragment) {
        loadFragment(fragment)
        showBottomNavigation()
    }

    /**
     * Navigasi ke login/signup
     */
    fun navigateToLoginOrSignup(fragment: Fragment) {
        loadFragment(fragment)
        hideBottomNavigation()
    }

    /**
     * 🔹 Sembunyikan bottom navigation
     */
    fun hideBottomNavigation() {
        bottomNavigation.visibility = View.GONE
    }

    /**
     * 🔹 Tampilkan bottom navigation
     */
    fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
    }

    /**
     * 🚪 Logout user
     */
    fun logout() {
        preferencesHelper.clearSession()
        hideBottomNavigation()
        loadFragment(LoginFragment())
    }

    /**
     * 👤 Pasien → dashboard pasien
     */
    fun showPatientDashboard() {
        setupPatientNavigation()
        showBottomNavigation()
        preloadDataFromFirebase()
        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    /**
     * 🩺 Dokter → dashboard dokter
     */
    fun showDoctorDashboard() {
        setupDoctorNavigation()
        showBottomNavigation()
        loadFragment(DoctorDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_doctor
        preloadDataFromFirebase()
    }

    /**
     * 🧾 Admin → dashboard admin
     */
    fun showAdminDashboard() {
        setupAdminNavigation()
        showBottomNavigation()
        loadFragment(AdminDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_admin
        preloadDataFromFirebase()
    }

    /**
     * 📥 Preload data from Firebase
     */
    private fun preloadDataFromFirebase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "📥 Preloading data from Firebase...")
                DataSource.forceLoadFromFirebase()

                withContext(Dispatchers.Main) {
                    val doctors = DataSource.getAllDoctors()
                    Log.d("MainActivity", "✅ Data preloaded: ${doctors.size} doctors")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "❌ Preload failed: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Handle back button
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        when (currentFragment) {
            is LoginFragment,
            is DashboardFragment,
            is AdminDashboardFragment,
            is DoctorDashboardFragment -> {
                finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}