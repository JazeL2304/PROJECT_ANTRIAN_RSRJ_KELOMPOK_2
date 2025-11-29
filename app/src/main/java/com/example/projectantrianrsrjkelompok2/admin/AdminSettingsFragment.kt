package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.R

class AdminSettingsFragment : Fragment() {

    // ✅ FIXED: CardView bukan Button!
    private lateinit var cardBackupData: CardView
    private lateinit var cardClearCache: CardView
    private lateinit var cardAbout: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ FIXED: Initialize CardView dengan tipe yang benar
        cardBackupData = view.findViewById(R.id.btnBackupData)
        cardClearCache = view.findViewById(R.id.btnClearCache)
        cardAbout = view.findViewById(R.id.btnAbout)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // ✅ Click listener untuk CardView
        cardBackupData.setOnClickListener {
            showBackupDialog()
        }

        cardClearCache.setOnClickListener {
            showClearCacheDialog()
        }

        cardAbout.setOnClickListener {
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
                Build: 2025.11.29
                
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