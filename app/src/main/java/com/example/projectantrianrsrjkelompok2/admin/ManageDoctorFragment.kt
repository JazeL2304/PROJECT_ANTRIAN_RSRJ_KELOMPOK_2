package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.R
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.Doctor

class ManageDoctorFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var btnAddDoctor: Button
    private lateinit var etDoctorName: EditText
    private lateinit var etDoctorSpecialization: EditText
    private val doctorList = mutableListOf<Doctor>()
    private lateinit var adapter: DoctorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_doctor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listDoctors)
        btnAddDoctor = view.findViewById(R.id.btnAddDoctor)
        etDoctorName = view.findViewById(R.id.etDoctorName)
        etDoctorSpecialization = view.findViewById(R.id.etDoctorSpecialization)

        loadDoctorData()

        // ➕ Tambah Dokter Baru
        btnAddDoctor.setOnClickListener {
            val name = etDoctorName.text.toString().trim()
            val specialization = etDoctorSpecialization.text.toString().trim()

            if (name.isEmpty() || specialization.isEmpty()) {
                Toast.makeText(requireContext(), "Nama dan spesialisasi harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newDoctor = Doctor(
                id = 0, // ID auto dari DataSource
                name = name,
                specialization = specialization,
                schedule = "Tidak ditentukan"
            )

            DataSource.addDoctor(newDoctor)
            etDoctorName.text.clear()
            etDoctorSpecialization.text.clear()

            Toast.makeText(requireContext(), "Dokter berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            loadDoctorData()
        }
    }

    private fun loadDoctorData() {
        doctorList.clear()
        doctorList.addAll(DataSource.getAllDoctors())

        adapter = DoctorAdapter(requireContext(), doctorList)
        listView.adapter = adapter
    }

    // Adapter dengan fitur edit + hapus
    inner class DoctorAdapter(
        private val context: android.content.Context,
        private val doctors: MutableList<Doctor>
    ) : ArrayAdapter<Doctor>(context, 0, doctors) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_doctor, parent, false)

            val doctor = doctors[position]
            val tvDoctorInfo = view.findViewById<TextView>(R.id.tvDoctorInfo)
            val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteDoctor)

            tvDoctorInfo.text = "${doctor.name} - ${doctor.specialization} (${doctor.schedule})"

            // ✏️ Klik teks dokter → Edit Jadwal
            tvDoctorInfo.setOnClickListener {
                val input = EditText(context).apply {
                    hint = "Masukkan jadwal baru"
                    setText(doctor.schedule)
                }

                AlertDialog.Builder(context)
                    .setTitle("Edit Jadwal Dokter")
                    .setMessage("Ubah jadwal untuk ${doctor.name}")
                    .setView(input)
                    .setPositiveButton("Simpan") { _, _ ->
                        val newSchedule = input.text.toString().trim()
                        if (newSchedule.isNotEmpty()) {
                            DataSource.updateDoctorSchedule(doctor.id, newSchedule)
                            doctors[position] = doctor.copy(schedule = newSchedule)
                            notifyDataSetChanged()
                            Toast.makeText(context, "Jadwal diperbarui", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Jadwal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            // 🗑️ Tombol hapus
            btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Hapus Dokter")
                    .setMessage("Yakin ingin menghapus ${doctor.name}?")
                    .setPositiveButton("Hapus") { _, _ ->
                        DataSource.removeDoctor(doctor)
                        doctors.removeAt(position)
                        notifyDataSetChanged()
                        Toast.makeText(context, "Dokter dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            return view
        }
    }
}
