package com.example.projectantrianrsrjkelompok2

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.admin.AdminDashboardFragment
import com.example.projectantrianrsrjkelompok2.admin.AdminSettingsFragment
import com.example.projectantrianrsrjkelompok2.admin.ViewReportFragment
import com.example.projectantrianrsrjkelompok2.doctor.DoctorDashboardFragment
import com.example.projectantrianrsrjkelompok2.doctor.DoctorQueueFragment
import com.example.projectantrianrsrjkelompok2.doctor.DoctorPatientHistoryFragment
import com.example.projectantrianrsrjkelompok2.utils.NotificationHelper
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

// ========== FIREBASE MIGRATION IMPORTS ==========
import com.example.projectantrianrsrjkelompok2.data.FirebaseMigrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesHelper = PreferencesHelper(this)
        NotificationHelper.createNotificationChannel(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        preferencesHelper.clearSession()

        loadFragment(LoginFragment())
        hideBottomNavigation()

        handleNotificationIntent()

        // ✅ TAMBAHAN BARU: Check dan jalankan migration jika belum pernah
        checkAndRunMigration()
    }

    // ========================================
    // 🚀 FIREBASE MIGRATION FUNCTIONS
    // ========================================

    /**
     * Check if migration needed and run it
     * HANYA JALAN SEKALI saat pertama kali app dibuka
     */
    private fun checkAndRunMigration() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isMigrated = prefs.getBoolean("is_data_migrated", false)

        if (!isMigrated) {
            // Show migration dialog
            showMigrationDialog()
        } else {
            Log.d("MainActivity", "✅ Data already migrated")
        }
    }

    /**
     * Show dialog untuk migration
     */
    private fun showMigrationDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("🔄 Setup Database")
        builder.setMessage("Aplikasi perlu melakukan setup database pertama kali.\n\nProses ini hanya dilakukan sekali dan membutuhkan koneksi internet.\n\nLanjutkan?")
        builder.setPositiveButton("Ya, Lanjutkan") { dialog, _ ->
            dialog.dismiss()
            runMigration()
        }
        builder.setNegativeButton("Nanti") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(this, "⚠️ App membutuhkan setup database untuk berfungsi", Toast.LENGTH_LONG).show()
        }
        builder.setCancelable(false)
        builder.show()
    }

    /**
     * Run migration process
     */
    private fun runMigration() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("🔄 Migrasi data ke Firebase...\n\nMohon tunggu...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val migrator = FirebaseMigrator(this@MainActivity)

                // Check if data already exists
                val needsMigration = migrator.isMigrationNeeded()

                if (!needsMigration) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Data sudah ada di Firebase",
                            Toast.LENGTH_SHORT
                        ).show()
                        markMigrationComplete()
                    }
                    return@launch
                }

                // Run migration
                val success = migrator.migrateAllData()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    if (success) {
                        val successBuilder = android.app.AlertDialog.Builder(this@MainActivity)
                        successBuilder.setTitle("✅ Setup Berhasil!")
                        successBuilder.setMessage(
                            "Database berhasil disetup!\n\n" +
                                    "Data yang telah dimigrasikan:\n" +
                                    "• 4 Dokter\n" +
                                    "• 3 Pasien\n" +
                                    "• 6 Booking (contoh)\n\n" +
                                    "Aplikasi siap digunakan!"
                        )
                        successBuilder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            markMigrationComplete()
                        }
                        successBuilder.setCancelable(false)
                        successBuilder.show()
                    } else {
                        val errorBuilder = android.app.AlertDialog.Builder(this@MainActivity)
                        errorBuilder.setTitle("❌ Setup Gagal")
                        errorBuilder.setMessage("Terjadi kesalahan saat setup database.\n\nPastikan koneksi internet aktif dan coba lagi.")
                        errorBuilder.setPositiveButton("Coba Lagi") { dialog, _ ->
                            dialog.dismiss()
                            runMigration()
                        }
                        errorBuilder.setNegativeButton("Batal") { dialog, _ ->
                            dialog.dismiss()
                        }
                        errorBuilder.show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("MainActivity", "Migration error: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Mark migration as complete
     */
    private fun markMigrationComplete() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_data_migrated", true).apply()
        Log.d("MainActivity", "✅ Migration marked as complete")
    }

    // ========================================
    // 🔔 NOTIFICATION HANDLER
    // ========================================

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

    // ========================================
    // ⚙️ SETUP NAVIGATION
    // ========================================

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

    // ========================================
    // 📦 FRAGMENT NAVIGATION
    // ========================================

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun navigateToFragment(fragment: Fragment) {
        loadFragment(fragment)
        showBottomNavigation()
    }

    fun navigateToLoginOrSignup(fragment: Fragment) {
        loadFragment(fragment)
        hideBottomNavigation()
    }

    // ========================================
    // 🔹 BOTTOM NAVIGATION VISIBILITY
    // ========================================

    fun hideBottomNavigation() {
        bottomNavigation.visibility = View.GONE
    }

    fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
    }

    // ========================================
    // 🚪 LOGOUT & DASHBOARD FUNCTIONS
    // ========================================

    fun logout() {
        preferencesHelper.clearSession()
        hideBottomNavigation()
        loadFragment(LoginFragment())
    }

    fun showPatientDashboard() {
        setupPatientNavigation()
        showBottomNavigation()
        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    fun showDoctorDashboard() {
        setupDoctorNavigation()
        showBottomNavigation()
        loadFragment(DoctorDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_doctor
    }

    fun showAdminDashboard() {
        setupAdminNavigation()
        showBottomNavigation()
        loadFragment(AdminDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_admin
    }
}