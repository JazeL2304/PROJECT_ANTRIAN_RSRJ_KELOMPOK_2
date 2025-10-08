package com.example.project_antrian_rsrj_kelompok_2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.project_antrian_rsrj_kelompok_2.DashboardFragment
import com.example.project_antrian_rsrj_kelompok_2.BookingFragment
import com.example.project_antrian_rsrj_kelompok_2.QueueFragment
import com.example.project_antrian_rsrj_kelompok_2.HistoryFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBottomNavigation()

        // Load fragment pertama saat aplikasi dibuka
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation)

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
                    loadFragment(QueueFragment())
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Method untuk navigasi dari fragment lain
    fun navigateToFragment(fragment: Fragment) {
        loadFragment(fragment)
    }
}