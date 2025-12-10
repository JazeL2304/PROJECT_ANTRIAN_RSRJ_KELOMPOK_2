package com.example.projectantrianrsrjkelompok2.doctor

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.firebase.database.FirebaseDatabase
import com.example.projectantrianrsrjkelompok2.model.DiagnosisData
import com.example.projectantrianrsrjkelompok2.model.MedicineData

class DoctorQueueFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyLayout: LinearLayout
    private lateinit var pref: PreferencesHelper
    private lateinit var adapter: QueueAdapter
    private val bookingList = mutableListOf<Booking>()

    private var doctorName = "Unknown Doctor"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_doctor_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listDoctorQueue)
        emptyLayout = view.findViewById(R.id.tvEmptyQueue)
        pref = PreferencesHelper(requireContext())

        doctorName = pref.getDoctorName() ?: pref.getUserFullName() ?: "Dr. Ahmad Santoso"

        // ✅ Inisialisasi Custom Adapter
        adapter = QueueAdapter(requireContext(), bookingList)
        listView.adapter = adapter

        // ✅ Handle item click
        listView.setOnItemClickListener { _, _, position, _ ->
            val booking = bookingList[position]
            handleStatus(booking)
        }

        // 🔥 Ambil semua antrian dokter (tanpa filter tanggal)
        BookingRepository.listenQueueByDoctor(
            doctorName,
            null
        ) { list ->
            updateUI(list)
        }
    }

    private fun updateUI(list: List<Booking>) {

        if (list.isEmpty()) {
            listView.visibility = View.GONE
            emptyLayout.visibility = View.VISIBLE
            return
        }

        emptyLayout.visibility = View.GONE
        listView.visibility = View.VISIBLE

        // =====================================================
        // 🔥 SORT & REMOVE DUPLICATE DATA (Logic Anda tetap)
        // =====================================================

        val sorted = list
            .distinctBy { "${it.patientName}|${it.time}" } // anti duplikat
            .sortedWith(
                compareBy<Booking> {
                    // Urutkan status: Called → Waiting → Completed
                    when (it.status) {
                        BookingStatus.CALLED -> 0
                        BookingStatus.WAITING -> 1
                        BookingStatus.COMPLETED -> 2
                        else -> 3
                    }
                }.thenBy { it.time }  // urut waktu
                    .thenBy { it.patientName.lowercase() } // fallback
            )

        // =====================================================
        // 🔥 AUTO NUMBERING & Update Adapter
        // =====================================================
        bookingList.clear()
        sorted.forEachIndexed { index, booking ->
            bookingList.add(booking.copy(queueNumber = index + 1))
        }
        adapter.notifyDataSetChanged()
    }

    private fun handleStatus(b: Booking) {

        when (b.status) {

            BookingStatus.WAITING -> {
                // Panggil pasien
                BookingRepository.updateStatus(
                    b.firebaseId,
                    BookingStatus.CALLED
                )

                Toast.makeText(
                    requireContext(),
                    "📢 ${b.patientName} dipanggil!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            BookingStatus.CALLED -> {
                // ✅ TAMPILKAN DIALOG FORM untuk input Diagnosis & Resep
                showCompletionDialog(b)
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    "Status tidak dapat diubah",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =====================================================
// ✅ DIALOG FORM - Pilih Diagnosis & Resep dari Dropdown
// =====================================================
    private fun showCompletionDialog(booking: Booking) {

        // Create custom layout programmatically
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        // Title
        val tvTitle = TextView(requireContext()).apply {
            text = "📋 Selesaikan Pemeriksaan"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#212121"))
            setPadding(0, 0, 0, 32)
        }
        container.addView(tvTitle)

        // Patient Info
        val tvPatient = TextView(requireContext()).apply {
            text = "👤 Pasien: ${booking.patientName}\n⏰ Waktu: ${booking.time}\n💬 Keluhan: ${booking.complaint.ifEmpty { "-" }}"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#616161"))
            setPadding(0, 0, 0, 24)
            setLineSpacing(8f, 1f)
        }
        container.addView(tvPatient)

        // ==================== DIAGNOSIS SECTION ====================
        val tvDiagnosisLabel = TextView(requireContext()).apply {
            text = "🩺 Pilih Diagnosis:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(0, 0, 0, 8)
        }
        container.addView(tvDiagnosisLabel)

        // Diagnosis Spinner
        val spinnerDiagnosis = Spinner(requireContext()).apply {
            setPadding(24, 24, 24, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        val diagnosisAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            DiagnosisData.getDiagnosisNames()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerDiagnosis.adapter = diagnosisAdapter

        // Set selected diagnosis jika ada
        if (booking.diagnosis.isNotEmpty()) {
            val position = DiagnosisData.getDiagnosisNames().indexOf(booking.diagnosis)
            if (position >= 0) {
                spinnerDiagnosis.setSelection(position)
            }
        }

        container.addView(spinnerDiagnosis)

        // Spacing
        val spacer1 = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24
            )
        }
        container.addView(spacer1)

        // ==================== MEDICINE SECTION ====================
        val tvMedicineLabel = TextView(requireContext()).apply {
            text = "💊 Pilih Resep Obat (bisa pilih lebih dari 1):"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(0, 0, 0, 8)
        }
        container.addView(tvMedicineLabel)

        // Selected medicines container
        val selectedMedicinesLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }
        container.addView(selectedMedicinesLayout)

        val selectedMedicines = mutableListOf<String>()

        // Load existing prescription
        if (booking.prescription.isNotEmpty()) {
            booking.prescription.split("\n").forEach { med ->
                if (med.isNotBlank()) {
                    selectedMedicines.add(med.trim())
                }
            }
        }

        // Function to update selected medicines display
        fun updateSelectedMedicinesView() {
            selectedMedicinesLayout.removeAllViews()

            if (selectedMedicines.isEmpty()) {
                val tvEmpty = TextView(requireContext()).apply {
                    text = "Belum ada obat dipilih"
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                    setPadding(0, 16, 0, 16)
                }
                selectedMedicinesLayout.addView(tvEmpty)
            } else {
                selectedMedicines.forEachIndexed { index, medicine ->
                    val itemLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 8, 0, 8)
                        gravity = Gravity.CENTER_VERTICAL
                    }

                    val tvMedicine = TextView(requireContext()).apply {
                        text = "• $medicine"
                        textSize = 13f
                        setTextColor(android.graphics.Color.parseColor("#424242"))
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }
                    itemLayout.addView(tvMedicine)

                    val btnRemove = Button(requireContext()).apply {
                        text = "❌"
                        textSize = 12f
                        setPadding(16, 8, 16, 8)
                        setOnClickListener {
                            selectedMedicines.removeAt(index)
                            updateSelectedMedicinesView()
                        }
                    }
                    itemLayout.addView(btnRemove)

                    selectedMedicinesLayout.addView(itemLayout)
                }
            }
        }

        // Initial display
        updateSelectedMedicinesView()

        // Spacing
        val spacer2 = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16
            )
        }
        container.addView(spacer2)

        // Medicine Spinner
        val spinnerMedicine = Spinner(requireContext()).apply {
            setPadding(24, 24, 24, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"))
        }

        val medicineAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            MedicineData.getMedicineDescriptions()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerMedicine.adapter = medicineAdapter
        container.addView(spinnerMedicine)

        // Spacing
        val spacer3 = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8
            )
        }
        container.addView(spacer3)

        // Add Medicine Button
        val btnAddMedicine = Button(requireContext()).apply {
            text = "➕ Tambah Obat"
            setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val selectedMedicine = spinnerMedicine.selectedItem.toString()
                if (!selectedMedicines.contains(selectedMedicine)) {
                    selectedMedicines.add(selectedMedicine)
                    updateSelectedMedicinesView()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ Obat sudah ditambahkan!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        container.addView(btnAddMedicine)

        // Create Dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(container)
            .setPositiveButton("✅ Selesaikan", null)
            .setNegativeButton("❌ Batal", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val diagnosis = spinnerDiagnosis.selectedItem.toString()

                // Validasi
                if (selectedMedicines.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ Pilih minimal 1 obat!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Format prescription
                val prescription = selectedMedicines.joinToString("\n") { "- $it" }

                // Save ke Firebase
                saveCompletionData(booking, diagnosis, prescription)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // =====================================================
    // 💾 SAVE Data ke Firebase
    // =====================================================
    private fun saveCompletionData(
        booking: Booking,
        diagnosis: String,
        prescription: String
    ) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("bookings")
            .child(booking.firebaseId)

        val updates = hashMapOf<String, Any>(
            "diagnosis" to diagnosis,
            "prescription" to prescription,
            "status" to BookingStatus.COMPLETED.name
        )

        ref.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "✅ ${booking.patientName} selesai diperiksa!\n📋 Data tersimpan.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "❌ Gagal menyimpan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ========================================
    // ✅ CUSTOM ADAPTER - Menggunakan Card View
    // ========================================
    inner class QueueAdapter(
        private val context: android.content.Context,
        private val data: List<Booking>
    ) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_queue_card, parent, false)

            val booking = data[position]

            // Find views
            val tvQueueNumber = view.findViewById<TextView>(R.id.tvQueueNumber)
            val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
            val tvPatientName = view.findViewById<TextView>(R.id.tvPatientName)
            val tvDateTime = view.findViewById<TextView>(R.id.tvDateTime)
            val tvComplaint = view.findViewById<TextView>(R.id.tvComplaint)

            // Set data
            tvQueueNumber.text = "No. ${booking.queueNumber ?: position + 1}"
            tvPatientName.text = "👤 ${booking.patientName}"
            tvDateTime.text = "📅 ${booking.date} • ${booking.time}"
            tvComplaint.text = "💬 ${booking.complaint.ifEmpty { "-" }}"

            // Set status badge dengan warna
            when (booking.status) {
                BookingStatus.WAITING -> {
                    tvStatus.text = "🕒 Menunggu"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_waiting)
                }
                BookingStatus.CALLED -> {
                    tvStatus.text = "📢 Dipanggil"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_called)
                }
                BookingStatus.COMPLETED -> {
                    tvStatus.text = "✅ Selesai"
                    tvStatus.setBackgroundResource(R.drawable.bg_status_completed)
                }
                else -> {
                    tvStatus.text = booking.status.name
                    tvStatus.setBackgroundResource(R.drawable.bg_status_waiting)
                }
            }

            return view
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}