package com.example.projectantrianrsrjkelompok2.firebase

import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.google.firebase.database.*

object BookingRepository {

    private val ref = FirebaseDatabase.getInstance().getReference("bookings")

    // ✅ SIMPAN QUERY + LISTENER BIAR TIDAK DOUBLE
    private val activeListeners = mutableListOf<Pair<Query, ValueEventListener>>()

    // ==========================================================
    // ADD BOOKING - FIXED: Hanya save 1 kali dengan ID yang benar
    // ==========================================================
    fun addBooking(booking: Booking, onResult: (Boolean) -> Unit) {
        // ✅ FIXED: Gunakan booking.id (Q001, Q002, dll) sebagai Firebase key
        // BUKAN generate ID baru dengan push()

        val firebaseKey = booking.id // ← Ini yang benar! (Q001, Q002, dst)

        if (firebaseKey.isEmpty()) {
            android.util.Log.e("BookingRepository", "❌ Booking ID is empty!")
            onResult(false)
            return
        }

        // ✅ Set firebaseId sama dengan id
        val bookingToSave = booking.copy(firebaseId = firebaseKey)

        android.util.Log.d("BookingRepository", "💾 Saving booking with key: $firebaseKey")
        android.util.Log.d("BookingRepository", "   - Patient: ${booking.patientName}")
        android.util.Log.d("BookingRepository", "   - Queue: ${booking.queueNumber}")

        // ✅ Save HANYA SEKALI dengan key yang benar
        ref.child(firebaseKey)
            .setValue(bookingToSave)
            .addOnSuccessListener {
                android.util.Log.d("BookingRepository", "✅ Booking saved successfully: $firebaseKey")
                onResult(true)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("BookingRepository", "❌ Failed to save booking: ${e.message}", e)
                onResult(false)
            }
    }

    // ==========================================================
    // GENERIC LISTENER
    // ==========================================================
    private fun createListener(
        onUpdate: (List<Booking>) -> Unit
    ) = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            val list = snapshot.children.mapNotNull {
                it.getValue(Booking::class.java)
                    ?.copy(firebaseId = it.key ?: "")
            }
            onUpdate(list)
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    // ==========================================================
    // ACTIVE QUEUE (ADMIN)
    // ==========================================================
    fun listenActiveQueue(onUpdate: (List<Booking>) -> Unit) {
        val query = ref

        val listener = createListener {
            val active = it.filter {
                it.status == BookingStatus.WAITING ||
                        it.status == BookingStatus.CALLED
            }
                .distinctBy { b ->
                    "${b.patientName}|${b.queueNumber}|${b.time}|${b.date}"
                }
                .sortedWith(
                    compareBy<Booking> {
                        when (it.status) {
                            BookingStatus.CALLED -> 0
                            BookingStatus.WAITING -> 1
                            BookingStatus.COMPLETED -> 2
                            else -> 3
                        }
                    }.thenBy { it.queueNumber }
                        .thenBy { it.time }
                )

            onUpdate(active)
        }

        query.addValueEventListener(listener)
        activeListeners.add(query to listener)
    }

    // ==========================================================
// QUEUE PER DOKTER  ✅ FIX: Ambil SEMUA status termasuk COMPLETED
// ==========================================================
    fun listenQueueByDoctor(
        doctorName: String,
        date: String?,
        onUpdate: (List<Booking>) -> Unit
    ) {
        val query = ref.orderByChild("doctorName")
            .equalTo(doctorName)

        val listener = createListener {
            // ✅ FIX: Ambil SEMUA booking (termasuk COMPLETED)
            // Filter hanya berdasarkan date jika ada
            val filtered = it.filter { b ->
                date == null || b.date == date
            }

            // ❗ HILANGKAN DUPLIKAT TOTAL
            val unique = filtered.distinctBy {
                "${it.patientName}|${it.queueNumber}|${it.time}|${it.date}"
            }

            // ✅ SORT FINAL: CALLED pertama, lalu WAITING, lalu COMPLETED
            val sorted = unique.sortedWith(
                compareBy<Booking> {
                    when (it.status) {
                        BookingStatus.CALLED -> 0
                        BookingStatus.WAITING -> 1
                        BookingStatus.COMPLETED -> 2
                        else -> 3
                    }
                }
                    .thenBy { it.queueNumber }
                    .thenBy { it.time }
            )

            onUpdate(sorted)
        }

        query.addValueEventListener(listener)
        activeListeners.add(query to listener)
    }

    // ==========================================================
    // HISTORY PER DOKTER
    // ==========================================================
    fun listenHistoryByDoctor(
        doctorName: String,
        onUpdate: (List<Booking>) -> Unit
    ) {
        val query = ref.orderByChild("doctorName")
            .equalTo(doctorName)

        val listener = createListener {
            val completed = it.filter { b ->
                b.status == BookingStatus.COMPLETED
            }
                .distinctBy {
                    "${it.patientName}|${it.queueNumber}|${it.time}|${it.date}"
                }
                .sortedByDescending { b ->
                    b.createdAt
                }

            onUpdate(completed)
        }

        query.addValueEventListener(listener)
        activeListeners.add(query to listener)
    }

    // ==========================================================
    // UPDATE STATUS
    // ==========================================================
    fun updateStatus(
        firebaseId: String,
        newStatus: BookingStatus
    ) {
        ref.child(firebaseId)
            .child("status")
            .setValue(newStatus.name)
    }

    // ==========================================================
    // DELETE HISTORY (ADMIN)
    // ==========================================================
    fun deleteBooking(firebaseId: String) {
        ref.child(firebaseId).removeValue()
    }

    // ==========================================================
    // ✅ CLEAR LISTENER (ANTI DOUBLE)
    // ==========================================================
    fun clearListeners() {
        activeListeners.forEach { (query, listener) ->
            query.removeEventListener(listener)
        }
        activeListeners.clear()
    }
}