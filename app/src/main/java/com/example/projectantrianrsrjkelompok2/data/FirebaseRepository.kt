package com.example.projectantrianrsrjkelompok2.data

import android.util.Log
import com.example.projectantrianrsrjkelompok2.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository untuk handle semua operasi Firebase Firestore
 * Menggantikan DataSource yang static dengan database real
 */
class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    // Collection names
    companion object {
        private const val COLLECTION_DOCTORS = "doctors"
        private const val COLLECTION_PATIENTS = "patients"
        private const val COLLECTION_BOOKINGS = "bookings"
        private const val COLLECTION_SPECIALIZATIONS = "specializations"
        private const val TAG = "FirebaseRepo"
    }

    // ===============================
    // 👨‍⚕️ DOCTORS
    // ===============================

    /**
     * Get all doctors
     */
    suspend fun getAllDoctors(): List<Doctor> {
        return try {
            val snapshot = db.collection(COLLECTION_DOCTORS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Doctor(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    schedule = doc.getString("schedule") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting doctors: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get doctors by specialization
     */
    suspend fun getDoctorsBySpecialization(specializationName: String): List<Doctor> {
        return try {
            val snapshot = db.collection(COLLECTION_DOCTORS)
                .whereEqualTo("specialization", specializationName)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Doctor(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    schedule = doc.getString("schedule") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting doctors by spec: ${e.message}")
            emptyList()
        }
    }

    /**
     * Add doctor
     */
    suspend fun addDoctor(doctor: Doctor): Boolean {
        return try {
            db.collection(COLLECTION_DOCTORS)
                .document(doctor.id.toString())
                .set(doctor)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding doctor: ${e.message}")
            false
        }
    }

    // ===============================
    // 🧍‍♀️ PATIENTS
    // ===============================

    /**
     * Get all patients
     */
    suspend fun getAllPatients(): List<Patient> {
        return try {
            val snapshot = db.collection(COLLECTION_PATIENTS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Patient(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    name = doc.getString("name") ?: "",
                    gender = doc.getString("gender") ?: "",
                    age = doc.getLong("age")?.toInt() ?: 0,
                    address = doc.getString("address") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting patients: ${e.message}")
            emptyList()
        }
    }

    /**
     * Add patient
     */
    suspend fun addPatient(patient: Patient): Boolean {
        return try {
            db.collection(COLLECTION_PATIENTS)
                .document(patient.id.toString())
                .set(patient)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding patient: ${e.message}")
            false
        }
    }

    // ===============================
    // 📋 BOOKINGS
    // ===============================

    /**
     * Create new booking
     */
    suspend fun createBooking(booking: Booking): Boolean {
        return try {
            val bookingData = hashMapOf(
                "id" to booking.id,
                "queueNumber" to booking.queueNumber,
                "patientName" to booking.patientName,
                "doctorName" to booking.doctorName,
                "specialization" to booking.specialization,
                "date" to booking.date,
                "time" to booking.time,
                "complaint" to booking.complaint,
                "diagnosis" to booking.diagnosis,
                "prescription" to booking.prescription,
                "status" to booking.status.name,
                "createdAt" to booking.createdAt,
                "calledAt" to 0L,
                "completedAt" to 0L
            )

            db.collection(COLLECTION_BOOKINGS)
                .document(booking.id)
                .set(bookingData)
                .await()

            Log.d(TAG, "✅ Booking created: ${booking.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating booking: ${e.message}")
            false
        }
    }

    /**
     * Get booking history (all bookings)
     */
    suspend fun getBookingHistory(): List<Booking> {
        return try {
            val snapshot = db.collection(COLLECTION_BOOKINGS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Booking(
                    id = doc.getString("id") ?: "",
                    queueNumber = doc.getLong("queueNumber")?.toInt() ?: 0,
                    patientName = doc.getString("patientName") ?: "",
                    doctorName = doc.getString("doctorName") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    date = doc.getString("date") ?: "",
                    time = doc.getString("time") ?: "",
                    complaint = doc.getString("complaint") ?: "",
                    diagnosis = doc.getString("diagnosis") ?: "",
                    prescription = doc.getString("prescription") ?: "",
                    status = BookingStatus.valueOf(doc.getString("status") ?: "WAITING"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bookings: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get bookings by date
     */
    suspend fun getBookingsByDate(date: String): List<Booking> {
        return try {
            val snapshot = db.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("date", date)
                .orderBy("queueNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Booking(
                    id = doc.getString("id") ?: "",
                    queueNumber = doc.getLong("queueNumber")?.toInt() ?: 0,
                    patientName = doc.getString("patientName") ?: "",
                    doctorName = doc.getString("doctorName") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    date = doc.getString("date") ?: "",
                    time = doc.getString("time") ?: "",
                    complaint = doc.getString("complaint") ?: "",
                    diagnosis = doc.getString("diagnosis") ?: "",
                    prescription = doc.getString("prescription") ?: "",
                    status = BookingStatus.valueOf(doc.getString("status") ?: "WAITING"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bookings by date: ${e.message}")
            emptyList()
        }
    }

    /**
     * Update booking status
     */
    suspend fun updateBookingStatus(bookingId: String, newStatus: BookingStatus): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "status" to newStatus.name
            )

            // Add timestamp
            when (newStatus) {
                BookingStatus.CALLED -> updates["calledAt"] = System.currentTimeMillis()
                BookingStatus.COMPLETED -> updates["completedAt"] = System.currentTimeMillis()
                else -> {}
            }

            db.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Booking status updated: $bookingId -> $newStatus")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating booking status: ${e.message}")
            false
        }
    }

    /**
     * Update booking diagnosis & prescription (saat selesai pemeriksaan)
     */
    suspend fun updateBookingDiagnosis(
        bookingId: String,
        diagnosis: String,
        prescription: String
    ): Boolean {
        return try {
            val updates = hashMapOf<String, Any>(
                "diagnosis" to diagnosis,
                "prescription" to prescription,
                "status" to BookingStatus.COMPLETED.name,
                "completedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_BOOKINGS)
                .document(bookingId)
                .update(updates)
                .await()

            Log.d(TAG, "✅ Booking diagnosis updated: $bookingId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating diagnosis: ${e.message}")
            false
        }
    }

    /**
     * Get today's bookings
     */
    suspend fun getTodayBookings(): List<Booking> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return getBookingsByDate(today)
    }

    /**
     * Get active queues (WAITING or CALLED)
     */
    suspend fun getActiveQueues(): List<Booking> {
        return try {
            val snapshot = db.collection(COLLECTION_BOOKINGS)
                .whereIn("status", listOf(BookingStatus.WAITING.name, BookingStatus.CALLED.name))
                .orderBy("queueNumber", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Booking(
                    id = doc.getString("id") ?: "",
                    queueNumber = doc.getLong("queueNumber")?.toInt() ?: 0,
                    patientName = doc.getString("patientName") ?: "",
                    doctorName = doc.getString("doctorName") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    date = doc.getString("date") ?: "",
                    time = doc.getString("time") ?: "",
                    complaint = doc.getString("complaint") ?: "",
                    diagnosis = doc.getString("diagnosis") ?: "",
                    prescription = doc.getString("prescription") ?: "",
                    status = BookingStatus.valueOf(doc.getString("status") ?: "WAITING"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active queues: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get next queue number for today
     */
    suspend fun getNextQueueNumber(): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayBookings = getBookingsByDate(today)
        val maxQueue = todayBookings.maxOfOrNull { it.queueNumber } ?: 0
        return maxQueue + 1
    }

    // ===============================
    // 🏥 SPECIALIZATIONS
    // ===============================

    /**
     * Get all specializations
     */
    suspend fun getSpecializations(): List<Specialization> {
        return try {
            val snapshot = db.collection(COLLECTION_SPECIALIZATIONS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                Specialization(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    emoji = doc.getString("emoji") ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting specializations: ${e.message}")
            emptyList()
        }
    }
}