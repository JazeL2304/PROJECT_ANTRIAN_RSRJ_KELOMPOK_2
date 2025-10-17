package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.Patient
import com.example.projectantrianrsrjkelompok2.R

class ManagePatientFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etGender: EditText
    private lateinit var etAge: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnAddPatient: Button
    private lateinit var listViewPatients: ListView

    private var patientList = mutableListOf<Patient>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_patient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.etPatientName)
        etGender = view.findViewById(R.id.etPatientGender)
        etAge = view.findViewById(R.id.etPatientAge)
        etAddress = view.findViewById(R.id.etPatientAddress)
        btnAddPatient = view.findViewById(R.id.btnAddPatient)
        listViewPatients = view.findViewById(R.id.listPatients)

        btnAddPatient.setOnClickListener { addPatient() }
        loadPatients()
    }

    private fun loadPatients() {
        patientList = DataSource.getAllPatients().toMutableList()

        if (patientList.isEmpty()) {
            val emptyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("Belum ada pasien terdaftar."))
            listViewPatients.adapter = emptyAdapter
            return
        }

        val adapter = object : ArrayAdapter<Patient>(requireContext(), R.layout.item_patient, patientList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_patient, parent, false)
                val patient = getItem(position)!!

                val tvInfo = view.findViewById<TextView>(R.id.tvPatientInfo)
                val btnDelete = view.findViewById<ImageButton>(R.id.btnDeletePatient)

                tvInfo.text = "${patient.name} (${patient.gender}, ${patient.age} th)\nAlamat: ${patient.address}"

                btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Hapus Pasien")
                        .setMessage("Apakah Anda yakin ingin menghapus ${patient.name}?")
                        .setPositiveButton("Hapus") { _, _ ->
                            DataSource.removePatient(patient)
                            Toast.makeText(requireContext(), "🗑️ ${patient.name} dihapus", Toast.LENGTH_SHORT).show()
                            loadPatients()
                        }
                        .setNegativeButton("Batal", null)
                        .show()
                }

                return view
            }
        }

        listViewPatients.adapter = adapter
    }

    private fun addPatient() {
        val name = etName.text.toString().trim()
        val gender = etGender.text.toString().trim()
        val ageText = etAge.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (name.isEmpty() || gender.isEmpty() || ageText.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(requireContext(), "⚠️ Usia tidak valid!", Toast.LENGTH_SHORT).show()
            return
        }

        val newPatient = Patient(
            id = (patientList.maxOfOrNull { it.id } ?: 0) + 1,
            name = name,
            gender = gender,
            age = age,
            address = address
        )

        val success = DataSource.addPatient(newPatient)
        if (success) {
            Toast.makeText(requireContext(), "✅ Pasien berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            clearInputs()
            loadPatients()
        } else {
            Toast.makeText(requireContext(), "⚠️ Pasien sudah terdaftar di alamat tersebut!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        etName.text.clear()
        etGender.text.clear()
        etAge.text.clear()
        etAddress.text.clear()
    }
}
