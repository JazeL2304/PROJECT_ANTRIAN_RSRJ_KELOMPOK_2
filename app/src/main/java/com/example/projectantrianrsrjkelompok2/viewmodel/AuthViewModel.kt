package com.example.projectantrianrsrjkelompok2.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import com.example.projectantrianrsrjkelompok2.model.User
import com.example.projectantrianrsrjkelompok2.model.UserAccount
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

    // ✅ Firebase Repository untuk auth
    private val firebaseRepo = FirebaseRepository()

    fun login(email: String, password: String) {
        _authState.value = AuthState(isLoading = true)

        viewModelScope.launch {
            try {
                // ✅ Login dari Firebase
                val userAccount = firebaseRepo.loginUser(email, password)

                if (userAccount != null) {
                    _authState.value = AuthState(
                        isLoading = false,
                        isSuccess = true,
                        user = userAccount.toUser()
                    )
                    _isLoggedIn.value = true
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
                val newUser = UserAccount(
                    id = "user_${System.currentTimeMillis()}",
                    email = email,
                    password = password,
                    fullName = fullName,
                    phoneNumber = "",
                    userType = "PATIENT"
                )

                // ✅ Register ke Firebase
                val success = firebaseRepo.registerUser(newUser)

                if (success) {
                    _authState.value = AuthState(
                        isLoading = false,
                        isSuccess = true,
                        user = newUser.toUser()
                    )
                } else {
                    _authState.value = AuthState(
                        isLoading = false,
                        isSuccess = false,
                        error = "Email sudah terdaftar, gunakan email lain"
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
}