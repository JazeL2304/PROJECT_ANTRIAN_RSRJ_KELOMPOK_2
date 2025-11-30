package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ✅ Fragment untuk testing Firebase connection
 * Tambahkan button ini di DashboardFragment untuk testing
 */
class TestFirebaseFragment : Fragment() {

    private lateinit var tvResult: TextView
    private lateinit var btnTest: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = View.inflate(requireContext(), android.R.layout.activity_list_item, null)

        tvResult = TextView(requireContext()).apply {
            textSize = 14f
            setPadding(16, 16, 16, 16)
        }

        btnTest = Button(requireContext()).apply {
            text = "Test Firebase Connection"
            setOnClickListener { testFirebase() }
        }

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(btnTest)
            addView(tvResult)
        }

        return layout
    }

    private fun testFirebase() {
        lifecycleScope.launch {
            tvResult.text = "Testing...\n"

            try {
                withContext(Dispatchers.IO) {
                    // Force load
                    DataSource.forceLoadFromFirebase()
                }

                // Check doctors
                val doctors = DataSource.getAllDoctors()
                tvResult.append("\n✅ Doctors: ${doctors.size}\n")
                doctors.forEach {
                    tvResult.append("  - ${it.name} (${it.specialization})\n")
                }

                // Check patients
                val patients = DataSource.getAllPatients()
                tvResult.append("\n✅ Patients: ${patients.size}\n")

                // Check specializations
                val specs = DataSource.getSpecializations()
                tvResult.append("\n✅ Specializations: ${specs.size}\n")

            } catch (e: Exception) {
                tvResult.append("\n❌ Error: ${e.message}")
            }
        }
    }
}