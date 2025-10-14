package com.example.projectantrianrsrjkelompok2

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper

class ProfileFragment : Fragment() {

    private lateinit var preferencesHelper: PreferencesHelper

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditPassword: Button
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesHelper = PreferencesHelper(requireContext())

        // Initialize views
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnEditPassword = view.findViewById(R.id.btnEditPassword)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Load user data
        loadUserData()

        // Setup click listeners
        setupClickListeners()
    }

    private fun loadUserData() {
        val userName = preferencesHelper.getUserFullName() ?: "User"
        val userEmail = preferencesHelper.getUserEmail() ?: "email@example.com"

        tvUserName.text = userName
        tvUserEmail.text = userEmail
    }

    private fun setupClickListeners() {
        // Edit Password
        btnEditPassword.setOnClickListener {
            showEditPasswordDialog()
        }

        // Logout
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showEditPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Password")
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val oldPassword = etOldPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (validatePasswordChange(oldPassword, newPassword, confirmPassword)) {
                    // TODO: Implement actual password change logic
                    Toast.makeText(context, "Password berhasil diubah!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun validatePasswordChange(oldPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (oldPassword.isEmpty()) {
            Toast.makeText(context, "Password lama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(context, "Password baru tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.length < 6) {
            Toast.makeText(context, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(context, "Password baru tidak cocok", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Logout via MainActivity
        (activity as? MainActivity)?.logout()

        Toast.makeText(context, "Berhasil logout", Toast.LENGTH_SHORT).show()
    }
}
