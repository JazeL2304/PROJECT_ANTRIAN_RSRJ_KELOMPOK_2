package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
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

class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

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

                    // Simpan data login & role user
                    val prefsHelper = PreferencesHelper(requireContext())
                    prefsHelper.setLoggedIn(true)
                    prefsHelper.saveUserData(state.user.email, state.user.fullName)
                    prefsHelper.saveUserRole(state.user.userType.name)

                    Toast.makeText(context, "Login berhasil!", Toast.LENGTH_SHORT).show()

                    // Arahkan sesuai role
                    when (state.user.userType) {
                        UserType.PATIENT -> (activity as? MainActivity)?.showPatientDashboard()
                        UserType.DOCTOR -> (activity as? MainActivity)?.showDoctorDashboard()
                        UserType.ADMIN -> (activity as? MainActivity)?.showAdminDashboard()
                    }
                }
                state.error != null -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Masuk"
                    Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (!validateInput(email, password)) return

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
