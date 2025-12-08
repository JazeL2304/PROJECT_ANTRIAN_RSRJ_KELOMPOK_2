package com.example.projectantrianrsrjkelompok2.utils

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

/**
 * ========================================
 * SCRIPT UPDATE DATA USER
 * ========================================
 *
 * Script untuk update data user dengan aman:
 * 1. Admin001 fullName: "Siti Nurhaliza" → "angelica"
 * 2. Doc002 password: "dokter123" → "siti123" (dengan hash)
 *
 * CARA PAKAI:
 * UpdateUserData.updateAdminAndDoc002()
 */
object UpdateUserData {

    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    /**
     * Update admin001 dan doc002 sekaligus
     */
    fun updateAdminAndDoc002(onComplete: (success: Boolean) -> Unit = {}) {
        Log.d("UpdateUserData", "🔄 Memulai update data...")

        var admin001Success = false
        var doc002Success = false
        var completedCount = 0

        // Update admin001 fullName
        updateAdminName { success ->
            admin001Success = success
            completedCount++

            if (completedCount == 2) {
                onComplete(admin001Success && doc002Success)
            }
        }

        // Update doc002 password
        updateDoc002Password { success ->
            doc002Success = success
            completedCount++

            if (completedCount == 2) {
                onComplete(admin001Success && doc002Success)
            }
        }
    }

    /**
     * Update admin001 fullName menjadi "angelica"
     */
    fun updateAdminName(onComplete: (Boolean) -> Unit = {}) {
        Log.d("UpdateUserData", "🔄 Updating admin001 fullName...")
        Log.d("UpdateUserData", "   Old: Siti Nurhaliza")
        Log.d("UpdateUserData", "   New: angelica")

        usersRef.child("admin001")
            .child("fullName")
            .setValue("angelica")
            .addOnSuccessListener {
                Log.d("UpdateUserData", "✅ Admin001 fullName berhasil diubah menjadi 'angelica'")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("UpdateUserData", "❌ Gagal update admin001 fullName: ${e.message}")
                onComplete(false)
            }
    }

    /**
     * Update doc002 password menjadi "siti123" (dengan hash)
     */
    fun updateDoc002Password(onComplete: (Boolean) -> Unit = {}) {
        Log.d("UpdateUserData", "🔄 Updating doc002 password...")
        Log.d("UpdateUserData", "   Old (plain): dokter123")
        Log.d("UpdateUserData", "   New (plain): siti123")

        // Hash password baru
        val newPassword = "siti123"
        val hashedPassword = PasswordHasher.hashPassword(newPassword)

        Log.d("UpdateUserData", "   New (hash): ${hashedPassword.take(30)}...")

        usersRef.child("doc002")
            .child("password")
            .setValue(hashedPassword)
            .addOnSuccessListener {
                Log.d("UpdateUserData", "✅ Doc002 password berhasil diubah")
                Log.d("UpdateUserData", "   Login dengan: siti123")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("UpdateUserData", "❌ Gagal update doc002 password: ${e.message}")
                onComplete(false)
            }
    }

    /**
     * Verify hasil update
     */
    fun verifyUpdate(onResult: (admin: String?, doc002Pass: String?) -> Unit) {
        Log.d("UpdateUserData", "🔍 Verifying update...")

        var adminName: String? = null
        var doc002Pass: String? = null
        var completedCount = 0

        // Get admin001 fullName
        usersRef.child("admin001")
            .child("fullName")
            .get()
            .addOnSuccessListener { snapshot ->
                adminName = snapshot.getValue(String::class.java)
                completedCount++

                Log.d("UpdateUserData", "Admin001 fullName: $adminName")

                if (completedCount == 2) {
                    onResult(adminName, doc002Pass)
                }
            }

        // Get doc002 password
        usersRef.child("doc002")
            .child("password")
            .get()
            .addOnSuccessListener { snapshot ->
                doc002Pass = snapshot.getValue(String::class.java)
                completedCount++

                Log.d("UpdateUserData", "Doc002 password: ${doc002Pass?.take(30)}...")

                if (completedCount == 2) {
                    onResult(adminName, doc002Pass)
                }
            }
    }

    /**
     * Rollback ke data original (jika diperlukan)
     */
    fun rollbackToOriginal(onComplete: (Boolean) -> Unit = {}) {
        Log.d("UpdateUserData", "🔄 Rolling back to original data...")

        var admin001Success = false
        var doc002Success = false
        var completedCount = 0

        // Rollback admin001
        usersRef.child("admin001")
            .child("fullName")
            .setValue("Siti Nurhaliza")
            .addOnSuccessListener {
                admin001Success = true
                completedCount++
                Log.d("UpdateUserData", "✅ Admin001 rolled back")

                if (completedCount == 2) {
                    onComplete(admin001Success && doc002Success)
                }
            }

        // Rollback doc002 (hash password lama)
        val oldPasswordHash = PasswordHasher.hashPassword("dokter123")
        usersRef.child("doc002")
            .child("password")
            .setValue(oldPasswordHash)
            .addOnSuccessListener {
                doc002Success = true
                completedCount++
                Log.d("UpdateUserData", "✅ Doc002 rolled back")

                if (completedCount == 2) {
                    onComplete(admin001Success && doc002Success)
                }
            }
    }
}