package com.example.projectantrianrsrjkelompok2.doctor

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.firebase.BookingRepository
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper

class DoctorPatientHistoryFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyState: ViewGroup
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<Booking>()

    private lateinit var pref: PreferencesHelper
    private var doctorName = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(R.layout.fragment_doctor_patient_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.listPatientHistory)
        emptyState = view.findViewById(R.id.tvEmptyHistory)

        pref = PreferencesHelper(requireContext())
        doctorName = pref.getDoctorName() ?: pref.getUserFullName() ?: "Dr. Ahmad Santoso"

        // ✅ Inisialisasi Custom Adapter
        adapter = HistoryAdapter(requireContext(), historyList)
        listView.adapter = adapter

        // ✅ SINGLE CLICK - Tampilkan detail lengkap
        listView.setOnItemClickListener { _, _, pos, _ ->
            val data = historyList[pos]
            showDetailDialog(data)
        }

        // ✅ LONG CLICK DELETE
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val data = historyList[pos]
            confirmDelete(data)
            true
        }

        startRealtimeHistory()
    }

    private fun startRealtimeHistory() {

        BookingRepository.listenHistoryByDoctor(doctorName) { list ->

            if (list.isEmpty()) {
                emptyState.visibility = View.VISIBLE
                listView.visibility = View.GONE
                return@listenHistoryByDoctor
            }

            emptyState.visibility = View.GONE
            listView.visibility = View.VISIBLE

            // ✅ Update data ke adapter
            historyList.clear()
            list.forEachIndexed { index, booking ->
                historyList.add(booking.copy(queueNumber = index + 1))
            }
            adapter.notifyDataSetChanged()
        }
    }

    // ✅ DIALOG DETAIL - Tampilan lebih rapi untuk baca detail
    private fun showDetailDialog(booking: Booking) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 40)
        }

        // Title
        val tvTitle = TextView(requireContext()).apply {
            text = "📋 Detail Pemeriksaan"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#212121"))
            setPadding(0, 0, 0, 24)
        }
        container.addView(tvTitle)

        // Content with better formatting
        val tvContent = TextView(requireContext()).apply {
            text = """
👤 Nama Pasien:
   ${booking.patientName}

📅 Tanggal Pemeriksaan:
   ${booking.date} • ${booking.time}

💬 Keluhan Pasien:
   ${booking.complaint.ifEmpty { "-" }}

🩺 Diagnosis:
   ${booking.diagnosis.ifEmpty { "(Belum diisi)" }}

💊 Resep Obat:
   ${booking.prescription.ifEmpty { "(Belum diisi)" }}

✅ Status: ${booking.status.toDisplayString()}
            """.trimIndent()
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setLineSpacing(8f, 1f)
        }
        container.addView(tvContent)

        android.app.AlertDialog.Builder(requireContext())
            .setView(container)
            .setPositiveButton("Tutup", null)
            .setNeutralButton("🗑️ Hapus") { _, _ ->
                confirmDelete(booking)
            }
            .show()
    }

    private fun confirmDelete(b: Booking) {

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Hapus Riwayat")
            .setMessage("Yakin ingin menghapus data pemeriksaan pasien ${b.patientName}?\n\nData yang dihapus tidak dapat dikembalikan.")
            .setPositiveButton("HAPUS") { _, _ ->

                BookingRepository.deleteBooking(b.firebaseId)

                Toast.makeText(
                    requireContext(),
                    "✅ Riwayat ${b.patientName} berhasil dihapus",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("BATAL", null)
            .show()
    }

    // ========================================
    // ✅ CUSTOM ADAPTER - Menggunakan Card View
    // ========================================
    inner class HistoryAdapter(
        private val context: android.content.Context,
        private val data: List<Booking>
    ) : BaseAdapter() {

        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_history_card, parent, false)

            val booking = data[position]

            // Find views
            val tvQueueNumber = view.findViewById<TextView>(R.id.tvQueueNumber)
            val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
            val tvPatientName = view.findViewById<TextView>(R.id.tvPatientName)
            val tvDateTime = view.findViewById<TextView>(R.id.tvDateTime)
            val tvComplaint = view.findViewById<TextView>(R.id.tvComplaint)
            val tvDiagnosis = view.findViewById<TextView>(R.id.tvDiagnosis)
            val tvPrescription = view.findViewById<TextView>(R.id.tvPrescription)

            // Set data
            tvQueueNumber.text = "No. ${booking.queueNumber ?: position + 1}"
            tvStatus.text = "✅ Selesai"
            tvPatientName.text = "👤 ${booking.patientName}"
            tvDateTime.text = "📅 ${booking.date} • ${booking.time}"

            // Format diagnosis dan resep dengan line breaks yang rapi
            val diagnosisText = if (booking.diagnosis.isNotEmpty()) {
                booking.diagnosis
            } else {
                "(Belum diisi)"
            }

            val prescriptionText = if (booking.prescription.isNotEmpty()) {
                booking.prescription
            } else {
                "(Belum diisi)"
            }

            tvComplaint.text = booking.complaint.ifEmpty { "-" }
            tvDiagnosis.text = diagnosisText
            tvPrescription.text = prescriptionText

            // Set status badge color
            tvStatus.setBackgroundResource(R.drawable.bg_status_completed)

            return view
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        BookingRepository.clearListeners()
    }
}