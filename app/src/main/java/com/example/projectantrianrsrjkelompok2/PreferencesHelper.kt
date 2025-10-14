package com.example.projectantrianrsrjkelompok2.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_FULL_NAME = "user_full_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TIME = "login_time"
    }

    // Method asli Anda (TETAP)
    fun saveUser(userId: String, email: String, fullName: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_FULL_NAME, fullName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }

    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)

    fun getUserEmail(): String? = sharedPreferences.getString(KEY_USER_EMAIL, null)

    fun getUserFullName(): String? = sharedPreferences.getString(KEY_USER_FULL_NAME, null)

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getLoginTime(): Long = sharedPreferences.getLong(KEY_LOGIN_TIME, 0L)

    fun logout() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_FULL_NAME)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_LOGIN_TIME)
            apply()
        }
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    // ========== TAMBAHAN BARU (untuk LoginFragment) ==========

    // Method untuk set login status saja (tanpa save user data)
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            if (isLoggedIn) {
                putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            }
            apply()
        }
    }

    // Method untuk save user data (alternatif dari saveUser)
    fun saveUserData(email: String, fullName: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_FULL_NAME, fullName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
    }
}
