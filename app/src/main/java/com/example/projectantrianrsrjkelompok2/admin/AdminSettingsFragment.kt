package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.R

class AdminSettingsFragment : Fragment() {

    private lateinit var btnBackupData: Button
    private lateinit var btnClearCache: Button
    private lateinit var btnAbout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize buttons
        btnBackupData = view.findViewById(R.id.btnBackupData)
        btnClearCache = view.findViewById(R.id.btnClearCache)
        btnAbout = view.findViewById(R.id.btnAbout)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnBackupData.setOnClickListener {
            showBackupDialog()
        }

        btnClearCache.setOnClickListener {
            showClearCacheDialog()
        }

        btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showBackupDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("💾 Backup Data")
            .setMessage("Apakah Anda yakin ingin melakukan backup data sistem?")
            .setPositiveButton("Ya") { dialog, _ ->
                performBackup()
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performBackup() {
        // Simulate backup process
        Toast.makeText(requireContext(),
            "✅ Backup data berhasil!",
            Toast.LENGTH_LONG).show()
    }

    private fun showClearCacheDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🗑️ Hapus Cache")
            .setMessage("Menghapus cache akan membersihkan data sementara. Lanjutkan?")
            .setPositiveButton("Hapus") { dialog, _ ->
                clearCache()
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clearCache() {
        // Clear cache logic here
        Toast.makeText(requireContext(),
            "✅ Cache berhasil dihapus!",
            Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ℹ️ Tentang Aplikasi")
            .setMessage("""
                📱 Aplikasi Antrian RS Raja Jaya
                
                Version: 1.0.0
                Build: 2025.10.17
                
                Developed by:
                Kelompok 2
                
                © 2025 Universitas Multimedia Nusantara
            """.trimIndent())
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
