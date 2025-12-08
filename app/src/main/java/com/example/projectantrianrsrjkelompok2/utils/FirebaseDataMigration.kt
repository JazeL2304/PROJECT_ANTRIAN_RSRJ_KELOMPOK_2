package com.example.projectantrianrsrjkelompok2.utils

import android.util.Log
import com.example.projectantrianrsrjkelompok2.model.UserAccount
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * ════════════════════════════════════════════════════════════════════
 *  AUTO-UPDATE EXISTING FIREBASE DATA
 * ════════════════════════════════════════════════════════════════════
 *
 * Script ini akan:
 * 1. Update admin001 fullName → "angelica"
 * 2. Update doc002 password → "siti123" (hashed)
 * 3. Hash SEMUA password yang masih plain text
 *
 * Jalan otomatis saat app start, hanya sekali.
 */
object FirebaseDataMigration {

    private const val TAG = "FirebaseMigration"
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    /**
     * Main migration function
     * Panggil dari MainActivity onCreate
     */
    suspend fun migrateExistingData(): Boolean {
        return try {
            Log.d(TAG, "🔄 Starting data migration...")

            // Step 1: Update admin001 fullName
            updateAdminName()

            // Step 2: Update doc002 password
            updateDoc002Password()

            // Step 3: Hash all plain passwords
            hashAllPlainPasswords()

            Log.d(TAG, "✅ Migration completed successfully!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed: ${e.message}", e)
            false
        }
    }

    /**
     * Update admin001 fullName to "angelica"
     */
    private suspend fun updateAdminName() {
        try {
            Log.d(TAG, "📝 Updating admin001 fullName...")

            val snapshot = usersRef.child("admin001").get().await()

            if (!snapshot.exists()) {
                Log.w(TAG, "⚠️ admin001 not found, skipping")
                return
            }

            val currentName = snapshot.child("fullName").getValue(String::class.java)

            if (currentName == "angelica") {
                Log.d(TAG, "✅ admin001 fullName already correct: angelica")
                return
            }

            // Update to "angelica"
            usersRef.child("admin001")
                .child("fullName")
                .setValue("angelica")
                .await()

            Log.d(TAG, "✅ admin001 fullName updated: $currentName → angelica")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update admin001: ${e.message}")
        }
    }

    /**
     * Update doc002 password to "siti123" (hashed)
     */
    private suspend fun updateDoc002Password() {
        try {
            Log.d(TAG, "🔐 Updating doc002 password...")

            val snapshot = usersRef.child("doc002").get().await()

            if (!snapshot.exists()) {
                Log.w(TAG, "⚠️ doc002 not found, skipping")
                return
            }

            val currentPassword = snapshot.child("password").getValue(String::class.java) ?: ""

            // Check if password is already "siti123" (hashed)
            if (currentPassword.startsWith("$2a$") || currentPassword.startsWith("$2b$")) {
                // Already hashed, verify if it's "siti123"
                val isCorrectPassword = PasswordHasher.verifyPassword("siti123", currentPassword)

                if (isCorrectPassword) {
                    Log.d(TAG, "✅ doc002 password already correct (hashed siti123)")
                    return
                }
            }

            // Hash new password "siti123"
            val hashedPassword = PasswordHasher.hashPassword("siti123")

            usersRef.child("doc002")
                .child("password")
                .setValue(hashedPassword)
                .await()

            Log.d(TAG, "✅ doc002 password updated to siti123 (hashed)")
            Log.d(TAG, "   Hash: ${hashedPassword.take(30)}...")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update doc002 password: ${e.message}")
        }
    }

    /**
     * Hash all passwords that are still plain text
     */
    private suspend fun hashAllPlainPasswords() {
        try {
            Log.d(TAG, "🔐 Checking all user passwords...")

            val snapshot = usersRef.get().await()

            if (!snapshot.exists()) {
                Log.w(TAG, "⚠️ No users found")
                return
            }

            var hashedCount = 0
            var alreadyHashedCount = 0

            for (userSnapshot in snapshot.children) {
                val userId = userSnapshot.key ?: continue
                val password = userSnapshot.child("password").getValue(String::class.java) ?: continue

                // Check if already hashed
                val isAlreadyHashed = password.startsWith("$2a$") ||
                        password.startsWith("$2b$") ||
                        password.startsWith("$2y$")

                if (isAlreadyHashed) {
                    alreadyHashedCount++
                    Log.d(TAG, "  ✅ $userId: Already hashed")
                    continue
                }

                // Password is plain text, hash it
                Log.d(TAG, "  🔐 $userId: Hashing plain password...")
                Log.d(TAG, "     Plain: $password")

                val hashedPassword = PasswordHasher.hashPassword(password)
                Log.d(TAG, "     Hash: ${hashedPassword.take(30)}...")

                usersRef.child(userId)
                    .child("password")
                    .setValue(hashedPassword)
                    .await()

                hashedCount++
                Log.d(TAG, "  ✅ $userId: Password hashed!")
            }

            Log.d(TAG, "✅ Password hash complete:")
            Log.d(TAG, "   - Already hashed: $alreadyHashedCount")
            Log.d(TAG, "   - Newly hashed: $hashedCount")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to hash passwords: ${e.message}")
        }
    }

    /**
     * Check if migration is needed
     */
    suspend fun needsMigration(): Boolean {
        return try {
            val snapshot = usersRef.child("admin001").get().await()

            if (!snapshot.exists()) {
                Log.d(TAG, "ℹ️ No users found, migration not needed")
                return false
            }

            val fullName = snapshot.child("fullName").getValue(String::class.java)
            val password = snapshot.child("password").getValue(String::class.java) ?: ""

            // Check if admin001 name is old or password is plain
            val needsUpdate = fullName != "angelica" || !password.startsWith("$2a$")

            Log.d(TAG, "🔍 Migration needed: $needsUpdate")
            Log.d(TAG, "   - Admin name: $fullName (should be angelica)")
            Log.d(TAG, "   - Password hashed: ${password.startsWith("$2a$")}")

            needsUpdate

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check migration status: ${e.message}")
            false
        }
    }
}