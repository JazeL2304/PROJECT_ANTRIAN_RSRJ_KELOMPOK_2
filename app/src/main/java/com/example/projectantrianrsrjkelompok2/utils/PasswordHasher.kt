package com.example.projectantrianrsrjkelompok2.utils

import android.util.Log
import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Utility class untuk hashing dan verifikasi password
 * Menggunakan BCrypt algorithm (industry standard)
 */
object PasswordHasher {

    private const val TAG = "PasswordHasher"
    private const val BCRYPT_COST = 12 // Tingkat keamanan (4-31, recommended: 10-12)

    /**
     * Hash password menggunakan BCrypt
     * @param password Password plain text yang akan di-hash
     * @return Password yang sudah di-hash
     */
    fun hashPassword(password: String): String {
        return try {
            val hashedPassword = BCrypt.withDefaults()
                .hashToString(BCRYPT_COST, password.toCharArray())
            Log.d(TAG, "✅ Password successfully hashed")
            hashedPassword
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error hashing password: ${e.message}", e)
            throw Exception("Failed to hash password: ${e.message}")
        }
    }

    /**
     * Verifikasi password dengan hash yang tersimpan
     * @param password Password plain text dari user
     * @param hashedPassword Password hash dari database
     * @return true jika password cocok, false jika tidak
     */
    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return try {
            val result = BCrypt.verifyer()
                .verify(password.toCharArray(), hashedPassword)

            if (result.verified) {
                Log.d(TAG, "✅ Password verification successful")
            } else {
                Log.w(TAG, "❌ Password verification failed")
            }

            result.verified
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verifying password: ${e.message}", e)
            false
        }
    }

    /**
     * Check apakah string adalah BCrypt hash
     * Berguna untuk migrasi data lama
     */
    fun isBCryptHash(input: String): Boolean {
        // BCrypt hash selalu dimulai dengan "$2a$", "$2b$", atau "$2y$"
        // dan memiliki panjang 60 karakter
        return input.length == 60 &&
                (input.startsWith("\$2a$") ||
                        input.startsWith("\$2b$") ||
                        input.startsWith("\$2y$"))
    }
}