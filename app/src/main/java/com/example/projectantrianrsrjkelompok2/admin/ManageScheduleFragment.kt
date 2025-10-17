package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.DataSource
import com.example.projectantrianrsrjkelompok2.Doctor
import com.example.projectantrianrsrjkelompok2.R

class ManageScheduleFragment : Fragment() {

    private lateinit var listViewDoctors: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val doctorDisplayList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewDoctors = view.findViewById(R.id.listDoctors)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, doctorDisplayList)
        listViewDoctors.adapter = adapter

        refreshDoctorList()

        // 🕐 Klik dokter untuk ubah jadwal
        listViewDoctors.setOnItemClickListener { _, _, position, _ ->
            val doctors = DataSource.getAllDoctors()
            if (position >= doctors.size) return@setOnItemClickListener

            val doctor = doctors[position]

            // Dialog input jadwal baru
            val input = EditText(requireContext()).apply {
                hint = "Masukkan jadwal baru"
                setText(doctor.schedule)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Ubah Jadwal Dokter")
                .setMessage("Ubah jadwal untuk ${doctor.name}")
                .setView(input)
                .setPositiveButton("Simpan") { _, _ ->
                    val newSchedule = input.text.toString().trim()
                    if (newSchedule.isNotEmpty()) {
                        DataSource.updateDoctorSchedule(doctor.id, newSchedule)
                        Toast.makeText(requireContext(), "Jadwal diperbarui!", Toast.LENGTH_SHORT).show()
                        refreshDoctorList()
                    } else {
                        Toast.makeText(requireContext(), "Jadwal tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun refreshDoctorList() {
        doctorDisplayList.clear()
        val doctors = DataSource.getAllDoctors()
        doctors.forEach {
            doctorDisplayList.add("${it.name}\nSpesialis: ${it.specialization}\n🕒 ${it.schedule}")
        }
        adapter.notifyDataSetChanged()
    }
}
