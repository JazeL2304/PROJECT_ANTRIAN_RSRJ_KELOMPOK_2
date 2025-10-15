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

    fun clearAllPreferences() {
        preferences.edit().clear().apply()
    }

    // ← TAMBAHAN BARU: Alias untuk clearAllPreferences
    fun clearSession() {
        clearAllPreferences()
    }
}
