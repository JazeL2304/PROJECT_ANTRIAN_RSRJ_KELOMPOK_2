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
        private const val KEY_PROFILE_PHOTO_PATH = "profile_photo_path"  // ✅ TAMBAHAN BARU
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

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

    fun clearAllPreferences() {
        preferences.edit().clear().apply()
    }

    fun clearSession() {
        clearAllPreferences()
    }

    // ===============================
    // ✅ TAMBAHAN: Fungsi untuk Dashboard Greeting
    // ===============================

    /**
     * ✅ Get username untuk ditampilkan di greeting dashboard
     * Menggunakan getUserFullName() yang sudah ada
     * Return default "User" jika full name tidak ada
     */
    fun getUsername(): String {
        return getUserFullName() ?: "User"
    }

    // ===============================
    // ✅ TAMBAHAN BARU: Fungsi untuk Profile Photo
    // ===============================

    /**
     * ✅ Simpan path foto profil
     */
    fun saveProfilePhotoPath(path: String) {
        preferences.edit().putString(KEY_PROFILE_PHOTO_PATH, path).apply()
    }

    /**
     * ✅ Ambil path foto profil
     * Return null jika foto belum pernah disimpan
     */
    fun getProfilePhotoPath(): String? {
        return preferences.getString(KEY_PROFILE_PHOTO_PATH, null)
    }

    /**
     * ✅ Hapus foto profil
     */
    fun clearProfilePhoto() {
        preferences.edit().remove(KEY_PROFILE_PHOTO_PATH).apply()
    }
}
