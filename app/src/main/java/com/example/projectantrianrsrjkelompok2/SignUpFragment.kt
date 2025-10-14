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
import com.google.android.material.textfield.TextInputEditText

class SignUpFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    // Declare views
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: Button
    private lateinit var tvLogin: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TAMBAHAN BARU: Sembunyikan bottom navigation saat di SignUp
        (activity as? MainActivity)?.hideBottomNavigation()

        // Initialize views
        etFullName = view.findViewById(R.id.etFullName)
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnSignUp = view.findViewById(R.id.btnSignUp)
        tvLogin = view.findViewById(R.id.tvLogin)

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        btnSignUp.setOnClickListener {
            performSignUp()
        }

        // DIUBAH: Dari navigateToFragment() ke navigateToLoginOrSignup()
        tvLogin.setOnClickListener {
            (activity as? MainActivity)?.navigateToLoginOrSignup(LoginFragment())
        }
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when {
                state.isLoading -> {
                    btnSignUp.isEnabled = false
                    btnSignUp.text = "Memproses..."
                }
                state.isSuccess -> {
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "Daftar"
                    Toast.makeText(context, "Registrasi berhasil! Silakan login", Toast.LENGTH_SHORT).show()

                    // DIUBAH: Dari navigateToFragment() ke navigateToLoginOrSignup()
                    (activity as? MainActivity)?.navigateToLoginOrSignup(LoginFragment())
                }
                state.error != null -> {
                    btnSignUp.isEnabled = true
                    btnSignUp.text = "Daftar"
                    Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun performSignUp() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (!validateSignUpInput(fullName, email, password, confirmPassword)) {
            return
        }

        authViewModel.signUp(fullName, email, password)
    }

    private fun validateSignUpInput(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var isValid = true

        if (fullName.isEmpty()) {
            etFullName.error = "Nama lengkap tidak boleh kosong"
            isValid = false
        } else if (fullName.length < 3) {
            etFullName.error = "Nama minimal 3 karakter"
            isValid = false
        } else {
            etFullName.error = null
        }

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

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Konfirmasi password tidak boleh kosong"
            isValid = false
        } else if (password != confirmPassword) {
            etConfirmPassword.error = "Password tidak sesuai"
            isValid = false
        } else {
            etConfirmPassword.error = null
        }

        return isValid
    }
}
