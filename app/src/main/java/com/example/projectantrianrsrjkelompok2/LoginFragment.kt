package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.projectantrianrsrjkelompok2.viewmodel.AuthViewModel
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.example.projectantrianrsrjkelompok2.model.UserType
import com.google.android.material.textfield.TextInputEditText

/**
 * ✅ Login Fragment - UPDATED VERSION
 * Menyimpan semua user data termasuk fullName untuk filter booking dokter
 */
class LoginFragment : Fragment() {

    private val TAG = "LoginFragment"
    private val authViewModel: AuthViewModel by viewModels()

    // Initialize PreferencesHelper
    private lateinit var prefsHelper: PreferencesHelper

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize PreferencesHelper
        prefsHelper = PreferencesHelper(requireContext())

        // Sembunyikan bottom navigation saat login
        (activity as? MainActivity)?.hideBottomNavigation()

        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        tvSignUp = view.findViewById(R.id.tvSignUp)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }

        tvSignUp.setOnClickListener {
            (activity as? MainActivity)?.navigateToLoginOrSignup(SignUpFragment())
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(context, "Fitur forgot password akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when {
                state.isLoading -> {
                    btnLogin.isEnabled = false
                    btnLogin.text = "Memproses..."
                }
                state.isSuccess && state.user != null -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Masuk"

                    // Handle login success
                    handleLoginSuccess(state.user)
                }
                state.error != null -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Masuk"
                    Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "❌ Login error: ${state.error}")
                }
            }
        }
    }

    /**
     * ✅ Handle Login Success - Simpan data lengkap user
     */
    private fun handleLoginSuccess(user: com.example.projectantrianrsrjkelompok2.model.User) {

        // ✅ METHOD 1: Gunakan saveCompleteLoginData (RECOMMENDED)
        prefsHelper.saveCompleteLoginData(
            userId = user.id,
            email = user.email,
            fullName = user.fullName,  // ✅ INI YANG AKAN MUNCUL DI BOOKING!
            phone = user.phoneNumber ?: "",
            role = user.userType.name  // "PATIENT", "DOCTOR", atau "ADMIN"
        )

        Log.d(TAG, "✅ User logged in successfully:")
        Log.d(TAG, "  - User ID: ${user.id}")
        Log.d(TAG, "  - Email: ${user.email}")
        Log.d(TAG, "  - FullName: ${user.fullName}")
        Log.d(TAG, "  - Role: ${user.userType.name}")
        Log.d(TAG, "  - Phone: ${user.phoneNumber ?: "N/A"}")

        // ✅ TAMBAHAN: Jika user adalah DOCTOR, simpan juga ke doctorName
        // Ini untuk backward compatibility dengan code lama
        if (user.userType == UserType.DOCTOR) {
            prefsHelper.saveDoctorName(user.fullName)
            Log.d(TAG, "👨‍⚕️ Doctor name saved: ${user.fullName}")
        }

        // ✅ Debug print untuk memastikan data tersimpan
        Log.d(TAG, "📋 Verification - Saved preferences:")
        Log.d(TAG, "  - getUserId(): ${prefsHelper.getUserId()}")
        Log.d(TAG, "  - getUserFullName(): ${prefsHelper.getUserFullName()}")
        Log.d(TAG, "  - getUserRole(): ${prefsHelper.getUserRole()}")
        Log.d(TAG, "  - getDoctorName(): ${prefsHelper.getDoctorName()}")

        // Optional: Debug print semua data
        prefsHelper.debugPrint()

        Toast.makeText(requireContext(), "✅ Login berhasil!", Toast.LENGTH_SHORT).show()

        // Navigate berdasarkan role
        when (user.userType) {
            UserType.PATIENT -> {
                Log.d(TAG, "👤 Navigating to Patient Dashboard")
                (activity as? MainActivity)?.showPatientDashboard()
            }
            UserType.DOCTOR -> {
                Log.d(TAG, "👨‍⚕️ Navigating to Doctor Dashboard")
                (activity as? MainActivity)?.showDoctorDashboard()
            }
            UserType.ADMIN -> {
                Log.d(TAG, "👔 Navigating to Admin Dashboard")
                (activity as? MainActivity)?.showAdminDashboard()
            }
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateInput(email, password)) return

        Log.d(TAG, "🔐 Attempting login with email: $email")
        authViewModel.login(email, password)
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            etEmail.error = "Email tidak boleh kosong"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            isValid = false
        } else {
            etEmail.error = null
        }

        if (password.isEmpty()) {
            etPassword.error = "Password tidak boleh kosong"
            isValid = false
        } else if (password.length < 6) {
            etPassword.error = "Password minimal 6 karakter"
            isValid = false
        } else {
            etPassword.error = null
        }

        return isValid
    }
}