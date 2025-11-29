package com.example.projectantrianrsrjkelompok2.data

import com.example.projectantrianrsrjkelompok2.*
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ✅ Firebase Repository - Handle all Firebase operations
 */
class FirebaseRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val doctorsRef: DatabaseReference = database.getReference("doctors")
    private val patientsRef: DatabaseReference = database.getReference("patients")
    private val bookingsRef: DatabaseReference = database.getReference("bookings")
    private val specializationsRef: DatabaseReference = database.getReference("specializations")

    // ===============================
    // 👨‍⚕️ DOCTORS
    // ===============================

    suspend fun getAllDoctors(): List<Doctor> {
        return try {
            val snapshot = doctorsRef.get().await()
            snapshot.children.mapNotNull { it.getValue(Doctor::class.java) }
        } catch (e: Exception) {
            println("❌ Error getting doctors: ${e.message}")
            emptyList()
        }
    }

    suspend fun getDoctorsBySpecialization(specialization: String): List<Doctor> {
        return try {
            val snapshot = doctorsRef
                .orderByChild("specialization")
                .equalTo(specialization)
                .get()
                .await()
            snapshot.children.mapNotNull { it.getValue(Doctor::class.java) }
        } catch (e: Exception) {
            println("❌ Error getting doctors by spec: ${e.message}")
            emptyList()
        }
    }

    suspend fun addDoctor(doctor: Doctor): Boolean {
        return try {
            val id = doctor.id.toString()
            doctorsRef.child(id).setValue(doctor).await()
            println("✅ Doctor added: ${doctor.name}")
            true
        } catch (e: Exception) {
            println("❌ Error adding doctor: ${e.message}")
            false
        }
    }

    suspend fun updateDoctor(doctor: Doctor): Boolean {
        return try {
            val id = doctor.id.toString()
            doctorsRef.child(id).setValue(doctor).await()
            true
        } catch (e: Exception) {
            println("❌ Error updating doctor: ${e.message}")
            false
        }
    }

    suspend fun deleteDoctor(doctorId: Int): Boolean {
        return try {
            doctorsRef.child(doctorId.toString()).removeValue().await()
            true
        } catch (e: Exception) {
            println("❌ Error deleting doctor: ${e.message}")
            false
        }
    }

    // ===============================
    // 🧍‍♀️ PATIENTS
    // ===============================

    suspend fun getAllPatients(): List<Patient> {
        return try {
            val snapshot = patientsRef.get().await()
            snapshot.children.mapNotNull { it.getValue(Patient::class.java) }
        } catch (e: Exception) {
            println("❌ Error getting patients: ${e.message}")
            emptyList()
        }
    }

    suspend fun addPatient(patient: Patient): Boolean {
        return try {
            val id = patient.id.toString()
            patientsRef.child(id).setValue(patient).await()
            println("✅ Patient added: ${patient.name}")
            true
        } catch (e: Exception) {
            println("❌ Error adding patient: ${e.message}")
            false
        }
    }

    suspend fun updatePatient(patient: Patient): Boolean {
        return try {
            val id = patient.id.toString()
            patientsRef.child(id).setValue(patient).await()
            true
        } catch (e: Exception) {
            println("❌ Error updating patient: ${e.message}")
            false
        }
    }

    suspend fun deletePatient(patientId: Int): Boolean {
        return try {
            patientsRef.child(patientId.toString()).removeValue().await()
            true
        } catch (e: Exception) {
            println("❌ Error deleting patient: ${e.message}")
            false
        }
    }

    // ===============================
    // 🏥 SPECIALIZATIONS
    // ===============================

    suspend fun getSpecializations(): List<Specialization> {
        return try {
            val snapshot = specializationsRef.get().await()
            snapshot.children.mapNotNull { it.getValue(Specialization::class.java) }
        } catch (e: Exception) {
            println("❌ Error getting specializations: ${e.message}")
            // Return default specializations if Firebase fails
            listOf(
                Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
                Specialization(2, "Layanan Gigi", "Perawatan gigi dan mulut", "🦷"),
                Specialization(3, "Layanan Mata", "Kesehatan mata dan penglihatan", "👁️"),
                Specialization(4, "Layanan Anak", "Kesehatan bayi dan anak-anak", "👶"),
                Specialization(5, "Layanan Jantung", "Kesehatan jantung dan pembuluh darah", "❤️"),
                Specialization(6, "Layanan Kandungan", "Kesehatan ibu dan anak", "🤰")
            )
        }
    }

    suspend fun addSpecialization(specialization: Specialization): Boolean {
        return try {
            val id = specialization.id.toString()
            specializationsRef.child(id).setValue(specialization).await()
            true
        } catch (e: Exception) {
            println("❌ Error adding specialization: ${e.message}")
            false
        }
    }

    // ===============================
    // 📋 BOOKINGS
    // ===============================

    suspend fun getBookingHistory(): List<Booking> {
        return try {
            val snapshot = bookingsRef
                .orderByChild("createdAt")
                .get()
                .await()
            snapshot.children.mapNotNull { it.getValue(Booking::class.java) }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            println("❌ Error getting booking history: ${e.message}")
            emptyList()
        }
    }

    suspend fun createBooking(booking: Booking): Boolean {
        return try {
            bookingsRef.child(booking.id).setValue(booking).await()
            println("✅ Booking created: ${booking.id}")
            true
        } catch (e: Exception) {
            println("❌ Error creating booking: ${e.message}")
            false
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Boolean {
        return try {
            bookingsRef.child(bookingId).child("status").setValue(status.name).await()
            true
        } catch (e: Exception) {
            println("❌ Error updating booking status: ${e.message}")
            false
        }
    }

    suspend fun updateBookingDiagnosis(
        bookingId: String,
        diagnosis: String,
        prescription: String
    ): Boolean {
        return try {
            val updates = mapOf(
                "diagnosis" to diagnosis,
                "prescription" to prescription,
                "status" to BookingStatus.COMPLETED.name
            )
            bookingsRef.child(bookingId).updateChildren(updates).await()
            true
        } catch (e: Exception) {
            println("❌ Error updating diagnosis: ${e.message}")
            false
        }
    }

    suspend fun getTodayBookings(): List<Booking> {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        return try {
            val snapshot = bookingsRef
                .orderByChild("date")
                .equalTo(today)
                .get()
                .await()
            snapshot.children.mapNotNull { it.getValue(Booking::class.java) }
        } catch (e: Exception) {
            println("❌ Error getting today bookings: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActiveQueues(): List<Booking> {
        return try {
            val allBookings = getBookingHistory()
            allBookings.filter {
                it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
            }
        } catch (e: Exception) {
            println("❌ Error getting active queues: ${e.message}")
            emptyList()
        }
    }

    suspend fun getNextQueueNumber(): Int {
        return try {
            val snapshot = bookingsRef
                .orderByChild("queueNumber")
                .limitToLast(1)
                .get()
                .await()
            val maxQueue = snapshot.children.mapNotNull {
                it.getValue(Booking::class.java)?.queueNumber
            }.maxOrNull() ?: 0
            maxQueue + 1
        } catch (e: Exception) {
            println("❌ Error getting next queue number: ${e.message}")
            1
        }
    }

    // ===============================
    // 🗑️ UTILITY - Clear all data
    // ===============================

    suspend fun clearAllData() {
        try {
            doctorsRef.removeValue().await()
            patientsRef.removeValue().await()
            bookingsRef.removeValue().await()
            specializationsRef.removeValue().await()
            println("✅ All Firebase data cleared")
        } catch (e: Exception) {
            println("❌ Error clearing data: ${e.message}")
        }
    }
}