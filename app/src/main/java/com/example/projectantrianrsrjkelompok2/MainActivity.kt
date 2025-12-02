package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import com.example.projectantrianrsrjkelompok2.TestMLFragment  // ✅ NEW

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

// ========== MATERIAL COMPONENTS ==========
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var preferencesHelper: PreferencesHelper
    private var fabTestML: FloatingActionButton? = null  // ✅ NEW: FAB untuk test ML

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔧 Inisialisasi helper & komponen UI
        preferencesHelper = PreferencesHelper(this)
        NotificationHelper.createNotificationChannel(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        // ✅ NEW: Setup FAB untuk test ML
        setupMLTestButton()

        // ✅ SEED DATA FIREBASE (async, tidak blocking UI)
        seedFirebaseDataIfNeeded()

        // ✅ Clear session SAJA
        preferencesHelper.clearSession()

        // ✅ Langsung tampilkan halaman login
        loadFragment(LoginFragment())
        hideBottomNavigation()

        handleNotificationIntent()
    }

    // ✅ NEW: Setup FAB untuk Test ML
    private fun setupMLTestButton() {
        fabTestML = findViewById(R.id.fab_test_ml)

        fabTestML?.setOnClickListener {
            Log.d("MainActivity", "🧪 Opening ML Test Fragment")
            loadFragment(TestMLFragment())
            hideBottomNavigation()
            fabTestML?.hide()  // Hide FAB saat di test fragment

            Toast.makeText(
                this,
                "🧪 ML Test Console",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Initially hide FAB (show only after login)
        fabTestML?.hide()
    }

    // 🌱 Seed Firebase with dummy data on first launch (ASYNC)
    private fun seedFirebaseDataIfNeeded() {
        val isFirstLaunch = preferencesHelper.isFirstLaunch()

        // ⚠️ UNCOMMENT UNTUK FORCE RESET DATABASE (TESTING)
        // preferencesHelper.resetFirstLaunch()
        // lifecycleScope.launch(Dispatchers.IO) {
        //     FirebaseSeedData.clearAllData()
        // }

        Log.d("MainActivity", "🔍 First Launch: $isFirstLaunch")

        if (isFirstLaunch) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    Log.d("MainActivity", "🌱 Starting Firebase seed (FIRST LAUNCH)...")

                    // Clear cache dulu
                    DataSource.invalidateCache()

                    // Seed data
                    FirebaseSeedData.seedAllData()

                    // Wait for data to be written
                    delay(2000)

                    // Force load data
                    DataSource.forceLoadFromFirebase()

                    withContext(Dispatchers.Main) {
                        preferencesHelper.setFirstLaunchComplete()
                        Log.d("MainActivity", "✅ Firebase seed completed!")

                        // Verify data loaded
                        val doctors = DataSource.getAllDoctors()
                        val specs = DataSource.getSpecializations()
                        Log.d("MainActivity", "📊 Verification - Doctors: ${doctors.size}, Specs: ${specs.size}")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "❌ Firebase seed failed: ${e.message}", e)
                }
            }
        } else {
            // Jika bukan first launch, tetap preload data
            preloadDataFromFirebase()
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
                    fabTestML?.show()  // ✅ Show FAB di dashboard
                    true
                }
                R.id.nav_booking -> {
                    showBottomNavigation()
                    loadFragment(BookingFragment())
                    fabTestML?.hide()
                    true
                }
                R.id.nav_queue -> {
                    showBottomNavigation()
                    if (DataSource.hasActiveBooking()) {
                        loadFragment(QueueFragment())
                    } else {
                        loadFragment(EmptyQueueFragment())
                    }
                    fabTestML?.hide()
                    true
                }
                R.id.nav_history -> {
                    showBottomNavigation()
                    loadFragment(HistoryFragment())
                    fabTestML?.hide()
                    true
                }
                R.id.nav_profile -> {
                    showBottomNavigation()
                    loadFragment(fragment_news())
                    fabTestML?.hide()
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
                    fabTestML?.show()  // ✅ Show FAB di admin dashboard
                    true
                }
                R.id.nav_reports -> {
                    showBottomNavigation()
                    loadFragment(ViewReportFragment())
                    fabTestML?.hide()
                    true
                }
                R.id.nav_settings -> {
                    showBottomNavigation()
                    loadFragment(AdminSettingsFragment())
                    fabTestML?.hide()
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
                    fabTestML?.show()  // ✅ Show FAB di doctor dashboard
                    true
                }
                R.id.nav_doctor_queue -> {
                    showBottomNavigation()
                    loadFragment(DoctorQueueFragment())
                    fabTestML?.hide()
                    true
                }
                R.id.nav_patient_history -> {
                    showBottomNavigation()
                    loadFragment(DoctorPatientHistoryFragment())
                    fabTestML?.hide()
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

        // ✅ Show/hide FAB based on fragment
        when (fragment) {
            is DashboardFragment,
            is AdminDashboardFragment,
            is DoctorDashboardFragment -> fabTestML?.show()
            else -> fabTestML?.hide()
        }
    }

    // Navigasi ke login/signup
    fun navigateToLoginOrSignup(fragment: Fragment) {
        loadFragment(fragment)
        hideBottomNavigation()
        fabTestML?.hide()  // ✅ Hide FAB di login/signup
    }

    // 🔹 Sembunyikan bottom navigation
    fun hideBottomNavigation() {
        bottomNavigation.visibility = View.GONE
    }

    // 🔹 Tampilkan bottom navigation
    fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
    }

    // 🚪 Logout user
    fun logout() {
        preferencesHelper.clearSession()
        hideBottomNavigation()
        fabTestML?.hide()  // ✅ Hide FAB saat logout
        loadFragment(LoginFragment())
    }

    // 👤 Pasien → dashboard pasien + setup nav pasien
    fun showPatientDashboard() {
        setupPatientNavigation()
        showBottomNavigation()

        // ✅ CRITICAL: Preload data SEBELUM navigate
        preloadDataFromFirebase()

        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard

        // ✅ Show FAB after login
        fabTestML?.show()
    }

    // 🩺 Dokter → dashboard dokter + setup nav dokter
    fun showDoctorDashboard() {
        setupDoctorNavigation()
        showBottomNavigation()
        loadFragment(DoctorDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_doctor

        // ✅ Preload data from Firebase after login
        preloadDataFromFirebase()

        // ✅ Show FAB after login
        fabTestML?.show()
    }

    // 🧾 Admin → dashboard admin + setup nav admin
    fun showAdminDashboard() {
        setupAdminNavigation()
        showBottomNavigation()
        loadFragment(AdminDashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard_admin

        // ✅ Preload data from Firebase after login
        preloadDataFromFirebase()

        // ✅ Show FAB after login
        fabTestML?.show()
    }

    // 📥 Preload data from Firebase after login
    private fun preloadDataFromFirebase() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainActivity", "📥 Preloading data from Firebase...")
                DataSource.forceLoadFromFirebase()

                withContext(Dispatchers.Main) {
                    val doctors = DataSource.getAllDoctors()
                    Log.d("MainActivity", "✅ Data preloaded: ${doctors.size} doctors")

                    Toast.makeText(
                        this@MainActivity,
                        "✅ Data dimuat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainActivity", "❌ Preload failed: ${e.message}", e)
                }
            }
        }
    }

    // ✅ NEW: Handle back press untuk test fragment
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        when (currentFragment) {
            is TestMLFragment -> {
                // Kembali ke dashboard sesuai role
                val userRole = preferencesHelper.getUserRole()
                when (userRole) {
                    "PATIENT" -> showPatientDashboard()
                    "DOCTOR" -> showDoctorDashboard()
                    "ADMIN" -> showAdminDashboard()
                    else -> showPatientDashboard()
                }
            }
            is LoginFragment, is DashboardFragment,
            is AdminDashboardFragment, is DoctorDashboardFragment -> {
                // Exit app
                finish()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}