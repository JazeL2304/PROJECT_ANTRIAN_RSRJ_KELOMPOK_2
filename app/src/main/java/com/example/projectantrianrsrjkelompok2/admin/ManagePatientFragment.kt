package com.example.projectantrianrsrjkelompok2.admin

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
import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.R
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ✅ RECYCLERVIEW VERSION: Lebih reliable daripada ListView
 */
class ManagePatientFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyMessage: TextView

    private val bookingList = mutableListOf<Booking>()
    private lateinit var adapter: BookingRecyclerAdapter

    private val database = FirebaseDatabase.getInstance()
    private val bookingsRef = database.getReference("bookings")

    private val TAG = "ManagePatientFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_patient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Gunakan RecyclerView
        recyclerView = view.findViewById(R.id.recyclerPatients)
        tvEmptyMessage = view.findViewById(R.id.tvEmptyPatientMessage)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookingRecyclerAdapter(bookingList)
        recyclerView.adapter = adapter

        loadPatients()
    }

    private fun loadPatients() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔍 Loading data...")

                val snapshot = bookingsRef.get().await()

                bookingList.clear()

                for (child in snapshot.children) {
                    val booking = child.getValue(Booking::class.java)
                    if (booking != null) {
                        Log.d(TAG, "✅ Adding: ${booking.patientName}")
                        bookingList.add(booking)
                    }
                }

                bookingList.sortByDescending { it.createdAt }

                Log.d(TAG, "📊 Total items: ${bookingList.size}")

                if (bookingList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    tvEmptyMessage.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    tvEmptyMessage.visibility = View.GONE
                    adapter.notifyDataSetChanged()
                    Log.d(TAG, "✅ notifyDataSetChanged() called")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ RecyclerView Adapter (lebih reliable)
     */
    inner class BookingRecyclerAdapter(
        private val bookings: MutableList<Booking>
    ) : RecyclerView.Adapter<BookingRecyclerAdapter.BookingViewHolder>() {

        inner class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPatientName: TextView = view.findViewById(R.id.tvPatientName)
            val tvPatientInfo: TextView = view.findViewById(R.id.tvPatientInfo)
            val tvPatientStats: TextView = view.findViewById(R.id.tvPatientStats)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_patient_admin, parent, false)
            Log.d(TAG, "📦 Creating view holder")
            return BookingViewHolder(view)
        }

        override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
            val booking = bookings[position]
            Log.d(TAG, "🎨 Binding position $position: ${booking.patientName}")

            holder.tvPatientName.text = booking.patientName

            holder.tvPatientInfo.text = buildString {
                append("👨‍⚕️ Dokter: ${booking.doctorName}\n")
                append("🏥 Spesialisasi: ${booking.specialization}")
            }

            holder.tvPatientStats.text = buildString {
                append("📅 ${booking.date} ${booking.time}\n")
                append("🎫 Antrian #${booking.queueNumber} • ")
                append(when (booking.status.name) {
                    "WAITING" -> "⏳ Menunggu"
                    "CALLED" -> "✅ Dipanggil"
                    "COMPLETED" -> "✔️ Selesai"
                    "CANCELLED" -> "❌ Dibatalkan"
                    else -> booking.status.name
                })
            }
        }

        override fun getItemCount(): Int {
            Log.d(TAG, "🔢 getItemCount: ${bookings.size}")
            return bookings.size
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 onResume")
        loadPatients()
    }
}