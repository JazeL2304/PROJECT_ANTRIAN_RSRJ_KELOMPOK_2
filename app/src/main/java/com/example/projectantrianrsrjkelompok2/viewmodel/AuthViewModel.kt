package com.example.projectantrianrsrjkelompok2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectantrianrsrjkelompok2.model.User
import com.example.projectantrianrsrjkelompok2.model.UserType
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: User? = null
)

class AuthViewModel : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    // Database pengguna simulasi dengan 3 role
    private val registeredUsers = mutableMapOf(
        // 1. User/Pasien
        "user@example.com" to UserCredentials(
            password = "password123",
            user = User(
                id = "user001",
                email = "user@example.com",
                fullName = "John Doe",
                phoneNumber = "081234567890",
                userType = UserType.PATIENT
            )
        ),

        // 2. Dokter
        "dokter@rumahsakit.com" to UserCredentials(
            password = "dokter123",
            user = User(
                id = "doc001",
                email = "dokter@rumahsakit.com",
                fullName = "Dr. Ahmad Susanto",
                phoneNumber = "081234567891",
                userType = UserType.DOCTOR
            )
        ),

        // 3. Admin (Asisten Dokter)
        "admin@rumahsakit.com" to UserCredentials(
            password = "admin123",
            user = User(
                id = "admin001",
                email = "admin@rumahsakit.com",
                fullName = "Siti Nurhaliza",
                phoneNumber = "081234567892",
                userType = UserType.ADMIN
            )
        )
    )

    fun login(email: String, password: String) {
        _authState.value = AuthState(isLoading = true)

        viewModelScope.launch {
            try {
                // Simulasi API call delay
                kotlinx.coroutines.delay(1000)

                if (validateLogin(email, password)) {
                    val userCredentials = registeredUsers[email]
                    if (userCredentials != null) {
                        _authState.value = AuthState(
                            isLoading = false,
                            isSuccess = true,
                            user = userCredentials.user
                        )
                        _isLoggedIn.value = true
                    }
                } else {
                    _authState.value = AuthState(
                        isLoading = false,
                        isSuccess = false,
                        error = "Email atau password salah"
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState(
                    isLoading = false,
                    isSuccess = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun signUp(fullName: String, email: String, password: String) {
        _authState.value = AuthState(isLoading = true)

        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(1000)

                // Validasi email sudah terdaftar
                if (registeredUsers.containsKey(email)) {
                    _authState.value = AuthState(
                        isLoading = false,
                        isSuccess = false,
                        error = "Email sudah terdaftar, gunakan email lain"
                    )
                    return@launch
                }

                // Tambahkan user baru (default: PATIENT)
                val newUser = User(
                    id = "user_${System.currentTimeMillis()}",
                    email = email,
                    fullName = fullName,
                    userType = UserType.PATIENT
                )

                registeredUsers[email] = UserCredentials(password, newUser)

                _authState.value = AuthState(
                    isLoading = false,
                    isSuccess = true,
                    user = newUser,
                    error = null
                )
            } catch (e: Exception) {
                _authState.value = AuthState(
                    isLoading = false,
                    isSuccess = false,
                    error = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _authState.value = AuthState()
    }

    fun clearError() {
        val currentState = _authState.value
        if (currentState != null) {
            _authState.value = currentState.copy(error = null)
        }
    }

    private fun validateLogin(email: String, password: String): Boolean {
        val userCredentials = registeredUsers[email]
        return userCredentials?.password == password
    }

    // Helper data class
    private data class UserCredentials(
        val password: String,
        val user: User
    )
}
