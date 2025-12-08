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
    // ✅ DIALOG FORM - Input Diagnosis & Resep
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

        // Diagnosis Label
        val tvDiagnosisLabel = TextView(requireContext()).apply {
            text = "🩺 Diagnosis:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(0, 0, 0, 8)
        }
        container.addView(tvDiagnosisLabel)

        // Diagnosis Input
        val etDiagnosis = EditText(requireContext()).apply {
            hint = "Contoh: Demam Tifoid"
            setText(booking.diagnosis)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            setSingleLine(true)
        }
        container.addView(etDiagnosis)

        // Spacing
        val spacer1 = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24
            )
        }
        container.addView(spacer1)

        // Prescription Label
        val tvPrescriptionLabel = TextView(requireContext()).apply {
            text = "💊 Resep Obat:"
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#424242"))
            setPadding(0, 0, 0, 8)
        }
        container.addView(tvPrescriptionLabel)

        // Prescription Input (Multiline)
        val etPrescription = EditText(requireContext()).apply {
            hint = "Contoh:\n- Paracetamol 500mg (3x1)\n- Amoxicillin 500mg (3x1)\n- Vitamin C (1x1)"
            setText(booking.prescription)
            setPadding(24, 24, 24, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            minLines = 4
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        container.addView(etPrescription)

        // Create Dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(container)
            .setPositiveButton("✅ Selesaikan", null) // Set null dulu
            .setNegativeButton("❌ Batal", null)
            .create()

        dialog.setOnShowListener {
            // Custom button behavior
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val diagnosis = etDiagnosis.text.toString().trim()
                val prescription = etPrescription.text.toString().trim()

                // Validasi
                if (diagnosis.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ Diagnosis tidak boleh kosong!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (prescription.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ Resep tidak boleh kosong!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

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