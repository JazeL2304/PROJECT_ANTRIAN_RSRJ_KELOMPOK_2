package com.example.projectantrianrsrjkelompok2.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectantrianrsrjkelompok2.R
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ✅ FINAL BENAR: Kelola Dokter
 * - Ambil data dari Firebase "doctors" (bukan users!)
 * - Tampilkan jadwal dokter (bisa diubah)
 * - Update jadwal ke Firebase doctors
 */
class ManageDoctorFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyMessage: TextView

    private val doctorList = mutableListOf<DoctorData>()
    private lateinit var adapter: DoctorRecyclerAdapter

    private val database = FirebaseDatabase.getInstance()
    private val doctorsRef = database.getReference("doctors")

    private val TAG = "ManageDoctorFragment"

    // Data class untuk dokter
    data class DoctorData(
        val id: Int = 0,
        val name: String = "",
        val specialization: String = "",
        val schedule: String = ""
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_doctor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerDoctors)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyDoctorMessage)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DoctorRecyclerAdapter(doctorList)
        recyclerView.adapter = adapter

        loadDoctorData()
    }

    /**
     * ✅ Ambil dokter dari "doctors" (bukan users!)
     */
    private fun loadDoctorData() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔍 Loading doctors from 'doctors' table...")

                val snapshot = doctorsRef.get().await()

                doctorList.clear()

                for (child in snapshot.children) {
                    try {
                        val doctorMap = child.value as? Map<String, Any> ?: continue

                        // Extract fields
                        val id = (doctorMap["id"] as? Long)?.toInt() ?: 0
                        val name = doctorMap["name"] as? String ?: ""
                        val specialization = doctorMap["specialization"] as? String ?: ""
                        val schedule = doctorMap["schedule"] as? String ?: "Belum ditentukan"

                        val doctor = DoctorData(
                            id = id,
                            name = name,
                            specialization = specialization,
                            schedule = schedule
                        )

                        Log.d(TAG, "✅ Doctor found: $name - $schedule")
                        doctorList.add(doctor)

                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Error parsing doctor: ${e.message}")
                    }
                }

                // Sort by id
                doctorList.sortBy { it.id }

                Log.d(TAG, "📊 Total doctors: ${doctorList.size}")

                // Update UI
                if (doctorList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    tvEmptyMessage.visibility = View.VISIBLE
                    tvEmptyMessage.text = "Belum ada dokter terdaftar"
                } else {
                    recyclerView.visibility = View.VISIBLE
                    tvEmptyMessage.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error memuat data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * ✅ RecyclerView Adapter
     */
    inner class DoctorRecyclerAdapter(
        private val doctors: MutableList<DoctorData>
    ) : RecyclerView.Adapter<DoctorRecyclerAdapter.DoctorViewHolder>() {

        inner class DoctorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
            val tvDoctorEmail: TextView = view.findViewById(R.id.tvDoctorEmail)
            val tvDoctorSchedule: TextView = view.findViewById(R.id.tvDoctorSchedule)
            val btnEditSchedule: Button = view.findViewById(R.id.btnEditSchedule)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_doctor_admin, parent, false)
            return DoctorViewHolder(view)
        }

        override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
            val doctor = doctors[position]

            holder.tvDoctorName.text = doctor.name
            holder.tvDoctorEmail.text = "🏥 ${doctor.specialization}"
            holder.tvDoctorSchedule.text = "🕒 ${doctor.schedule}"

            // Edit jadwal
            holder.btnEditSchedule.setOnClickListener {
                showEditScheduleDialog(doctor, position)
            }
        }

        override fun getItemCount(): Int = doctors.size
    }

    /**
     * ✅ Dialog edit jadwal
     */
    private fun showEditScheduleDialog(doctor: DoctorData, position: Int) {
        val context = requireContext()

        val input = EditText(context).apply {
            hint = "Contoh: Senin-Jumat 08:00-16:00"
            setText(doctor.schedule)
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Jadwal Dokter")
            .setMessage("Ubah jadwal untuk ${doctor.name}")
            .setView(input)
            .setPositiveButton("Simpan") { dialog, _ ->
                val newSchedule = input.text.toString().trim()
                if (newSchedule.isNotEmpty()) {
                    updateDoctorSchedule(doctor, newSchedule, position)
                } else {
                    Toast.makeText(context, "Jadwal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * ✅ Update jadwal ke Firebase doctors
     */
    private fun updateDoctorSchedule(doctor: DoctorData, newSchedule: String, position: Int) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "💾 Updating schedule for ${doctor.name}")

                // Update di Firebase doctors/${id}/schedule
                val updates = mapOf<String, Any>(
                    "schedule" to newSchedule
                )

                doctorsRef.child(doctor.id.toString()).updateChildren(updates).await()

                // Update local list
                doctorList[position] = doctor.copy(schedule = newSchedule)
                adapter.notifyItemChanged(position)

                Log.d(TAG, "✅ Schedule updated in Firebase")
                Toast.makeText(
                    requireContext(),
                    "✅ Jadwal ${doctor.name} diperbarui",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "❌ Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 onResume")
        loadDoctorData()
    }
}