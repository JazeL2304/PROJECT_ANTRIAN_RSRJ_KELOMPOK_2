package com.example.projectantrianrsrjkelompok2.firebase

import com.example.projectantrianrsrjkelompok2.Booking
import com.example.projectantrianrsrjkelompok2.BookingStatus
import com.google.firebase.database.*

object BookingRepository {

    private val ref =
        FirebaseDatabase.getInstance().getReference("bookings")

    // ✅ SIMPAN QUERY + LISTENER BIAR TIDAK DOUBLE
    private val activeListeners =
        mutableListOf<Pair<Query, ValueEventListener>>()

    // ==========================================================
    // ADD BOOKING
    // ==========================================================
    fun addBooking(booking: Booking, onResult: (Boolean) -> Unit) {

        val firebaseId = ref.push().key ?: return

        val bookingWithId = booking.copy(firebaseId = firebaseId)

        ref.child(firebaseId)
            .setValue(bookingWithId)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    // ==========================================================
    // GENERIC LISTENER
    // ==========================================================
    private fun createListener(
        onUpdate: (List<Booking>) -> Unit
    ) = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {

            val list =
                snapshot.children.mapNotNull {
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

            val active =
                it.filter {
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
    // QUEUE PER DOKTER  ✅ FIX DOUBEL + URUT
    // ==========================================================
    fun listenQueueByDoctor(
        doctorName: String,
        date: String?,
        onUpdate: (List<Booking>) -> Unit
    ) {

        val query =
            ref.orderByChild("doctorName")
                .equalTo(doctorName)

        val listener = createListener {

            val filtered =
                it.filter { b ->
                    (date == null || b.date == date) &&
                            (b.status == BookingStatus.WAITING ||
                                    b.status == BookingStatus.CALLED)
                }

            // ❗ HILANGKAN DUPLIKAT TOTAL
            val unique =
                filtered.distinctBy {
                    "${it.patientName}|${it.queueNumber}|${it.time}|${it.date}"
                }

            // ✅ SORT FINAL
            val sorted =
                unique.sortedWith(
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

        val query =
            ref.orderByChild("doctorName")
                .equalTo(doctorName)

        val listener = createListener {

            val completed =
                it.filter { b ->
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
