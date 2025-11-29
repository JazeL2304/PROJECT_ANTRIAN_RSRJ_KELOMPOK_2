package com.example.projectantrianrsrjkelompok2.utils

import android.util.Log
import com.example.projectantrianrsrjkelompok2.data.FirebaseMigrator
import kotlinx.coroutines.runBlocking

/**
 * ✅ Utility untuk mengisi Firebase dengan data dummy
 * Panggil sekali saat pertama setup
 */
object FirebaseSeedData {

    private const val TAG = "FirebaseSeedData"

    /**
     * 🌱 Seed semua data dummy ke Firebase
     * Call this once in MainActivity onCreate() atau saat first launch
     */
    fun seedAllData() {
        runBlocking {
            try {
                Log.d(TAG, "🌱 Starting Firebase seed...")

                // Check if data already exists
                val status = FirebaseMigrator.checkMigrationStatus()

                if (status.isComplete) {
                    Log.d(TAG, "ℹ️ Data already exists in Firebase, skipping seed")
                    Log.d(TAG, status.toString())
                    return@runBlocking
                }

                // Migrate all data
                FirebaseMigrator.migrateAllData()

                // Check status after migration
                val finalStatus = FirebaseMigrator.checkMigrationStatus()
                Log.d(TAG, "✅ Seed completed!")
                Log.d(TAG, finalStatus.toString())

            } catch (e: Exception) {
                Log.e(TAG, "❌ Seed failed: ${e.message}", e)
            }
        }
    }

    /**
     * 🔍 Check if Firebase already has data
     */
    fun hasData(): Boolean {
        return runBlocking {
            try {
                val status = FirebaseMigrator.checkMigrationStatus()
                status.isComplete
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking data: ${e.message}")
                false
            }
        }
    }

    /**
     * 🗑️ Clear all data (use with caution!)
     */
    fun clearAllData() {
        runBlocking {
            try {
                Log.d(TAG, "⚠️ Clearing all Firebase data...")
                FirebaseMigrator.clearFirebaseData()
                Log.d(TAG, "✅ All data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Clear failed: ${e.message}", e)
            }
        }
    }

    /**
     * 📊 Get migration status
     */
    fun getMigrationStatus(): String {
        return runBlocking {
            try {
                val status = FirebaseMigrator.checkMigrationStatus()
                status.toString()
            } catch (e: Exception) {
                "❌ Error: ${e.message}"
            }
        }
    }
}