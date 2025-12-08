package com.example.projectantrianrsrjkelompok2.utils

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

/**
 * ========================================
 * SIMPLE HASH PASSWORD UTILITY
 * ========================================
 *
 * Script sederhana untuk hash password users tertentu.
 *
 * CARA PAKAI:
 *
 * 1. Dari Activity/Fragment, panggil:
 *    SimpleHashPassword.hashExistingUsers()
 *
 * 2. Atau hash user tertentu:
 *    SimpleHashPassword.hashUser("admin001", "admin123")
 */
object SimpleHashPassword {

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    /**
     * Hash password untuk 4 user yang ada di screenshot
     */
    fun hashExistingUsers() {
        Log.d("SimpleHashPassword", "🔐 Mulai hash password existing users...")

        // Data dari screenshot Firebase
        val usersToHash = mapOf(
            "admin001" to "admin123",
            "doc001" to "dokter123",
            "doc002" to "dokter123",
            "user001" to "password123"
        )

        var successCount = 0
        var failedCount = 0
        val totalUsers = usersToHash.size

        usersToHash.forEach { (userId, plainPassword) ->

            Log.d("SimpleHashPassword", "🔐 Hashing user: $userId")
            Log.d("SimpleHashPassword", "   Plain: $plainPassword")

            // Hash password
            val hashedPassword = PasswordHasher.hashPassword(plainPassword)

            Log.d("SimpleHashPassword", "   Hash: ${hashedPassword.take(30)}...")

            // Update ke Firebase
            usersRef.child(userId)
                .child("password")
                .setValue(hashedPassword)
                .addOnSuccessListener {
                    successCount++
                    Log.d("SimpleHashPassword", "✅ Success: $userId")

                    if (successCount + failedCount == totalUsers) {
                        Log.d("SimpleHashPassword", "✅ SELESAI! Success: $successCount, Failed: $failedCount")
                    }
                }
                .addOnFailureListener { e ->
                    failedCount++
                    Log.e("SimpleHashPassword", "❌ Failed: $userId - ${e.message}")

                    if (successCount + failedCount == totalUsers) {
                        Log.d("SimpleHashPassword", "✅ SELESAI! Success: $successCount, Failed: $failedCount")
                    }
                }
        }
    }

    /**
     * Hash password untuk user tertentu
     */
    fun hashUser(userId: String, plainPassword: String) {
        Log.d("SimpleHashPassword", "🔐 Hashing user: $userId")

        val hashedPassword = PasswordHasher.hashPassword(plainPassword)

        usersRef.child(userId)
            .child("password")
            .setValue(hashedPassword)
            .addOnSuccessListener {
                Log.d("SimpleHashPassword", "✅ Berhasil hash password: $userId")
            }
            .addOnFailureListener { e ->
                Log.e("SimpleHashPassword", "❌ Gagal hash password: $userId - ${e.message}")
            }
    }

    /**
     * Cek apakah password sudah di-hash
     */
    fun checkIfHashed(userId: String, callback: (Boolean, String?) -> Unit) {
        usersRef.child(userId)
            .child("password")
            .get()
            .addOnSuccessListener { snapshot ->
                val password = snapshot.getValue(String::class.java)

                if (password == null) {
                    callback(false, null)
                    return@addOnSuccessListener
                }

                // Cek apakah sudah hash (bcrypt hash dimulai dengan $2a$, $2b$, atau $2y$)
                val isHashed = password.startsWith("$2a$") ||
                        password.startsWith("$2b$") ||
                        password.startsWith("$2y$")

                Log.d("SimpleHashPassword", "User: $userId")
                Log.d("SimpleHashPassword", "Password: ${password.take(30)}...")
                Log.d("SimpleHashPassword", "IsHashed: $isHashed")

                callback(isHashed, password)
            }
            .addOnFailureListener {
                callback(false, null)
            }
    }
}