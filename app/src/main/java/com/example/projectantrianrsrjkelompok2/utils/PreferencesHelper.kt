package com.example.projectantrianrsrjkelompok2.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * ✅ PreferencesHelper - FIXED VERSION
 * Support untuk semua user data termasuk fullName untuk filter booking dokter
 */
class PreferencesHelper(private val context: Context) {

    companion object {
        private const val TAG = "PreferencesHelper"
        private const val PREF_NAME = "antrian_rs_prefs"

        // Keys
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_PROFILE_PHOTO_PATH = "profile_photo_path"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_DOCTOR_NAME = "doctor_name"

        // ===============================
        // ✅ STATIC HELPER METHODS
        // Untuk dipanggil dari Fragment tanpa instance
        // ===============================

        fun getUserId(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_ID, null)
        }

        fun getUserFullName(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_FULL_NAME, null)
        }

        fun getUserEmail(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_EMAIL, null)
        }

        fun getUserRole(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_ROLE, null)
        }

        fun getDoctorName(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_DOCTOR_NAME, null)
        }

        fun isLoggedIn(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        }

        fun isDoctor(context: Context): Boolean {
            return getUserRole(context) == "DOCTOR"
        }

        fun isAdmin(context: Context): Boolean {
            return getUserRole(context) == "ADMIN"
        }

        fun isPatient(context: Context): Boolean {
            return getUserRole(context) == "PATIENT"
        }

        /**
         * ✅ Debug: Print semua preferences
         */
        fun debugPrintAll(context: Context) {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            Log.d(TAG, "=== PREFERENCES DEBUG ===")
            Log.d(TAG, "  IS_LOGGED_IN: ${prefs.getBoolean(KEY_IS_LOGGED_IN, false)}")
            Log.d(TAG, "  USER_ID: ${prefs.getString(KEY_USER_ID, "null")}")
            Log.d(TAG, "  USER_EMAIL: ${prefs.getString(KEY_USER_EMAIL, "null")}")
            Log.d(TAG, "  USER_FULL_NAME: ${prefs.getString(KEY_USER_FULL_NAME, "null")}")
            Log.d(TAG, "  USER_ROLE: ${prefs.getString(KEY_USER_ROLE, "null")}")
            Log.d(TAG, "  DOCTOR_NAME: ${prefs.getString(KEY_DOCTOR_NAME, "null")}")
            Log.d(TAG, "=========================")
        }
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ===============================
    // 🔐 LOGIN & SESSION
    // ===============================

    fun isLoggedIn(): Boolean =
        preferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun setLoggedIn(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun clearSession() {
        clearAllPreferences()
    }

    fun clearAllPreferences() {
        preferences.edit().clear().apply()
        Log.d(TAG, "✅ All preferences cleared")
    }

    // ===============================
    // 👤 USER DATA - Individual Setters
    // ===============================

    fun saveUserId(userId: String) {
        preferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return preferences.getString(KEY_USER_ID, null)
    }

    fun saveUserEmail(email: String) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? =
        preferences.getString(KEY_USER_EMAIL, null)

    fun saveUserFullName(fullName: String) {
        preferences.edit().putString(KEY_USER_FULL_NAME, fullName).apply()
        Log.d(TAG, "✅ Saved fullName: $fullName")
    }

    fun getUserFullName(): String? =
        preferences.getString(KEY_USER_FULL_NAME, null)

    fun saveUserPhone(phone: String) {
        preferences.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    fun getUserPhone(): String? =
        preferences.getString(KEY_USER_PHONE, null)

    fun saveUserRole(role: String) {
        preferences.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getUserRole(): String? =
        preferences.getString(KEY_USER_ROLE, null)

    fun getUsername(): String =
        getUserFullName() ?: "User"

    // ===============================
    // 👨‍⚕️ DOCTOR DATA
    // ===============================

    fun saveDoctorName(name: String) {
        preferences.edit().putString(KEY_DOCTOR_NAME, name).apply()
        Log.d(TAG, "✅ Saved doctorName: $name")
    }

    fun getDoctorName(): String? =
        preferences.getString(KEY_DOCTOR_NAME, null)

    // ===============================
    // 📸 PROFILE PHOTO
    // ===============================

    fun saveProfilePhotoPath(path: String) {
        preferences.edit().putString(KEY_PROFILE_PHOTO_PATH, path).apply()
    }

    fun getProfilePhotoPath(): String? =
        preferences.getString(KEY_PROFILE_PHOTO_PATH, null)

    fun clearProfilePhoto() {
        preferences.edit().remove(KEY_PROFILE_PHOTO_PATH).apply()
    }

    // ===============================
    // 🌱 FIRST LAUNCH
    // ===============================

    fun isFirstLaunch(): Boolean =
        preferences.getBoolean(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchComplete() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun resetFirstLaunch() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }

    // ===============================
    // 💾 SAVE COMPLETE LOGIN DATA
    // ===============================

    /**
     * ✅ Save all user data at once during login
     * This is the RECOMMENDED method to use after successful login
     */
    fun saveCompleteLoginData(
        userId: String,
        email: String,
        fullName: String,
        phone: String = "",
        role: String = "PATIENT"
    ) {
        preferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_FULL_NAME, fullName)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_ROLE, role)

            // ✅ TAMBAHAN: Jika role DOCTOR, simpan juga ke doctor_name
            if (role == "DOCTOR") {
                putString(KEY_DOCTOR_NAME, fullName)
            }

            apply()
        }

        Log.d(TAG, "✅ Complete login data saved:")
        Log.d(TAG, "  - userId: $userId")
        Log.d(TAG, "  - email: $email")
        Log.d(TAG, "  - fullName: $fullName")
        Log.d(TAG, "  - role: $role")
        if (role == "DOCTOR") {
            Log.d(TAG, "  - doctorName: $fullName (auto-saved for doctor)")
        }
    }

    /**
     * ✅ Legacy method - Save basic user data
     */
    fun saveUserData(email: String, fullName: String) {
        preferences.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_FULL_NAME, fullName)
            apply()
        }
    }

    // ===============================
    // 🔍 ROLE CHECKS (Instance methods)
    // ===============================

    fun isDoctor(): Boolean = getUserRole() == "DOCTOR"

    fun isAdmin(): Boolean = getUserRole() == "ADMIN"

    fun isPatient(): Boolean = getUserRole() == "PATIENT"

    // ===============================
    // 🐛 DEBUG
    // ===============================

    fun debugPrint() {
        Log.d(TAG, "=== PREFERENCES DEBUG (Instance) ===")
        Log.d(TAG, "  IS_LOGGED_IN: ${isLoggedIn()}")
        Log.d(TAG, "  USER_ID: ${getUserId()}")
        Log.d(TAG, "  USER_EMAIL: ${getUserEmail()}")
        Log.d(TAG, "  USER_FULL_NAME: ${getUserFullName()}")
        Log.d(TAG, "  USER_ROLE: ${getUserRole()}")
        Log.d(TAG, "  DOCTOR_NAME: ${getDoctorName()}")
        Log.d(TAG, "====================================")
    }
}