package com.example.projectantrianrsrjkelompok2.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    companion object {
        private const val PREF_NAME = "antrian_rs_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_PROFILE_PHOTO_PATH = "profile_photo_path"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"  // ✅ ADDED
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ===============================
    // 🔐 LOGIN & SESSION
    // ===============================

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

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

    fun saveUserEmail(email: String) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return preferences.getString(KEY_USER_EMAIL, null)
    }

    fun saveUserFullName(fullName: String) {
        preferences.edit().putString(KEY_USER_FULL_NAME, fullName).apply()
    }

    fun getUserFullName(): String? {
        return preferences.getString(KEY_USER_FULL_NAME, null)
    }

    fun saveUserPhone(phone: String) {
        preferences.edit().putString(KEY_USER_PHONE, phone).apply()
    }

    fun getUserPhone(): String? {
        return preferences.getString(KEY_USER_PHONE, null)
    }

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

    fun getUserRole(): String? {
        return preferences.getString(KEY_USER_ROLE, null)
    }

    fun getUsername(): String {
        return getUserFullName() ?: "User"
    }

    // ===============================
    // 📸 PROFILE PHOTO
    // ===============================

    fun saveProfilePhotoPath(path: String) {
        preferences.edit().putString(KEY_PROFILE_PHOTO_PATH, path).apply()
    }

    fun getProfilePhotoPath(): String? {
        return preferences.getString(KEY_PROFILE_PHOTO_PATH, null)
    }

    fun clearProfilePhoto() {
        preferences.edit().remove(KEY_PROFILE_PHOTO_PATH).apply()
    }

    // ===============================
    // 🌱 FIRST LAUNCH (for Firebase seed)
    // ===============================

    /**
     * Check if this is first launch (for seeding Firebase)
     * Returns true if app never launched before
     */
    fun isFirstLaunch(): Boolean {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Mark first launch as complete (after seeding Firebase)
     */
    fun setFirstLaunchComplete() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    /**
     * Reset first launch flag (for testing/debugging)
     */
    fun resetFirstLaunch() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, true).apply()
    }
}