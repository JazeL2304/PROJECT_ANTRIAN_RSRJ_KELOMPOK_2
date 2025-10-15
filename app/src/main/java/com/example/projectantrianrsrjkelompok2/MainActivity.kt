package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.example.projectantrianrsrjkelompok2.utils.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var btnProfileIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preferencesHelper = PreferencesHelper(this)

        NotificationHelper.createNotificationChannel(this)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        btnProfileIcon = findViewById(R.id.btnProfileIcon)

        setupBottomNavigation()
        setupProfileIcon()
        checkLoginStatus()
        handleNotificationIntent()
    }

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

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_booking -> {
                    loadFragment(BookingFragment())
                    true
                }
                R.id.nav_queue -> {
                    if (DataSource.hasActiveBooking()) {
                        loadFragment(QueueFragment())
                    } else {
                        loadFragment(EmptyQueueFragment())
                    }
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(fragment_news())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupProfileIcon() {
        btnProfileIcon.setOnClickListener {
            loadFragment(ProfileFragment())
            bottomNavigation.menu.setGroupCheckable(0, true, false)
            for (i in 0 until bottomNavigation.menu.size()) {
                bottomNavigation.menu.getItem(i).isChecked = false
            }
            bottomNavigation.menu.setGroupCheckable(0, true, true)
        }
    }

    private fun checkLoginStatus() {
        preferencesHelper.clearSession()

        loadFragment(LoginFragment())
        bottomNavigation.visibility = android.view.View.GONE
        btnProfileIcon.visibility = android.view.View.GONE
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun navigateToFragment(fragment: Fragment) {
        loadFragment(fragment)
        if (bottomNavigation.visibility != android.view.View.VISIBLE) {
            bottomNavigation.visibility = android.view.View.VISIBLE
            btnProfileIcon.visibility = android.view.View.VISIBLE
        }
    }

    fun navigateToLoginOrSignup(fragment: Fragment) {
        loadFragment(fragment)
        bottomNavigation.visibility = android.view.View.GONE
        btnProfileIcon.visibility = android.view.View.GONE
    }

    fun hideBottomNavigation() {
        bottomNavigation.visibility = android.view.View.GONE
        btnProfileIcon.visibility = android.view.View.GONE
    }

    // ← FIXED: Ganti preferencesHelper.logout() jadi clearSession()
    fun logout() {
        preferencesHelper.clearSession()
        DataSource.clearActiveBooking()
        bottomNavigation.visibility = android.view.View.GONE
        btnProfileIcon.visibility = android.view.View.GONE
        loadFragment(LoginFragment())
    }

    fun showBottomNavigation() {
        bottomNavigation.visibility = android.view.View.VISIBLE
        btnProfileIcon.visibility = android.view.View.VISIBLE
        loadFragment(DashboardFragment())
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }
}
