package com.example.projectantrianrsrjkelompok2.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    companion object {
        private const val PREF_NAME = "antrian_rs_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_PROFILE_PHOTO_PATH = "profile_photo_path"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"

        // ✅ ===== TAMBAHAN UNTUK DOKTER =====
        private const val KEY_DOCTOR_NAME = "doctor_name"
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
    }

    // ===============================
    // 👤 USER DATA
    // ===============================

    fun saveUserId(userId: String) {
        preferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        val savedId = preferences.getString(KEY_USER_ID, null)
        if (!savedId.isNullOrEmpty()) return savedId

        val email = getUserEmail()
        return email?.substringBefore("@")?.replace(".", "_")
    }

    fun saveUserEmail(email: String) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? =
        preferences.getString(KEY_USER_EMAIL, null)

    fun saveUserFullName(fullName: String) {
        preferences.edit().putString(KEY_USER_FULL_NAME, fullName).apply()
    }

    fun getUserFullName(): String? =
        preferences.getString(KEY_USER_FULL_NAME, null)

    fun saveUserPhone(phone: String) {
        preferences.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    fun getUserPhone(): String? =
        preferences.getString(KEY_USER_PHONE, null)

    fun saveUserData(email: String, fullName: String) {
        preferences.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_FULL_NAME, fullName)
            apply()
        }
    }

    fun saveUserRole(role: String) {
        preferences.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getUserRole(): String? =
        preferences.getString(KEY_USER_ROLE, null)

    fun getUsername(): String =
        getUserFullName() ?: "User"

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
    // 👨‍⚕️ DOCTOR DATA ✅ FIX
    // ===============================

    fun saveDoctorName(name: String) {
        preferences.edit().putString(KEY_DOCTOR_NAME, name).apply()
    }

    fun getDoctorName(): String? =
        preferences.getString(KEY_DOCTOR_NAME, null)

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
}
