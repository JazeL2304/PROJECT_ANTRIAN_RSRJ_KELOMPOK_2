package com.example.projectantrianrsrjkelompok2.data

import android.content.Context
import android.util.Log
import com.example.projectantrianrsrjkelompok2.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper untuk migrasi data dummy dari DataSource.kt ke Firebase
 * HANYA DIJALANKAN SEKALI saat pertama kali setup!
 */
object FirebaseMigrator {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FirebaseMigrator"

    /**
     * Main migration function - call this once!
     */
    suspend fun migrateAllData(): Result<String> {
        return try {
            Log.d(TAG, "🔄 Starting migration...")

            // 1. Migrate Specializations
            migrateSpecializations()

            // 2. Migrate Doctors
            migrateDoctors()

            // 3. Migrate Patients
            migratePatients()

            // 4. Migrate Bookings
            migrateBookings()

            Log.d(TAG, "✅ Migration completed successfully!")
            Result.success("✅ All data migrated successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Migrate Specializations
     */
    private suspend fun migrateSpecializations() {
        Log.d(TAG, "📋 Migrating specializations...")

        val specializations = listOf(
            hashMapOf(
                "id" to 1,
                "name" to "Layanan Umum",
                "description" to "Pelayanan kesehatan umum",
                "emoji" to "🏥"
            ),
            hashMapOf(
                "id" to 2,
                "name" to "Layanan Gigi",
                "description" to "Perawatan gigi dan mulut",
                "emoji" to "🦷"
            ),
            hashMapOf(
                "id" to 3,
                "name" to "Layanan Mata",
                "description" to "Kesehatan mata dan penglihatan",
                "emoji" to "👁️"
            ),
            hashMapOf(
                "id" to 4,
                "name" to "Layanan Anak",
                "description" to "Kesehatan bayi dan anak-anak",
                "emoji" to "👶"
            )
        )

        specializations.forEach { spec ->
            db.collection("specializations")
                .document(spec["id"].toString())
                .set(spec)
                .await()
        }

        Log.d(TAG, "✅ ${specializations.size} specializations migrated")
    }

    /**
     * Migrate Doctors
     */
    private suspend fun migrateDoctors() {
        Log.d(TAG, "👨‍⚕️ Migrating doctors...")

        val doctors = listOf(
            hashMapOf(
                "id" to 1,
                "name" to "Dr. Ahmad Santoso",
                "specialization" to "Dokter Umum",
                "schedule" to "Senin–Jumat 08:00–15:00"
            ),
            hashMapOf(
                "id" to 2,
                "name" to "Dr. Budi Dental",
                "specialization" to "Dokter Gigi",
                "schedule" to "Senin–Kamis 09:00–16:00"
            ),
            hashMapOf(
                "id" to 3,
                "name" to "Dr. Indra Mata",
                "specialization" to "Dokter Mata",
                "schedule" to "Senin–Jumat 08:00–14:00"
            ),
            hashMapOf(
                "id" to 4,
                "name" to "Dr. Ani Pediatri",
                "specialization" to "Dokter Anak",
                "schedule" to "Setiap Hari 24 Jam"
            )
        )

        doctors.forEach { doctor ->
            db.collection("doctors")
                .document(doctor["id"].toString())
                .set(doctor)
                .await()
        }

        Log.d(TAG, "✅ ${doctors.size} doctors migrated")
    }

    /**
     * Migrate Patients
     */
    private suspend fun migratePatients() {
        Log.d(TAG, "🧍‍♀️ Migrating patients...")

        val patients = listOf(
            hashMapOf(
                "id" to 1,
                "name" to "Atila Falah",
                "gender" to "Laki-laki",
                "age" to 21,
                "address" to "Jl. Anggrek No. 12"
            ),
            hashMapOf(
                "id" to 2,
                "name" to "Rizky Amalia",
                "gender" to "Perempuan",
                "age" to 23,
                "address" to "Jl. Melati No. 8"
            ),
            hashMapOf(
                "id" to 3,
                "name" to "Dewi Lestari",
                "gender" to "Perempuan",
                "age" to 20,
                "address" to "Jl. Mawar No. 5"
            )
        )

        patients.forEach { patient ->
            db.collection("patients")
                .document(patient["id"].toString())
                .set(patient)
                .await()
        }

        Log.d(TAG, "✅ ${patients.size} patients migrated")
    }

    /**
     * Migrate Bookings (historical data)
     */
    private suspend fun migrateBookings() {
        Log.d(TAG, "📅 Migrating bookings...")

        val bookings = listOf(
            // Booking 1 - COMPLETED
            hashMapOf(
                "id" to "B001",
                "queueNumber" to 7,
                "patientName" to "Ahmad Santoso",
                "doctorName" to "Dr. Budi Dental",
                "specialization" to "Layanan Gigi",
                "date" to "2025-01-10",
                "time" to "08:30",
                "complaint" to "Pemeriksaan rutin gigi dan pembersihan karang gigi",
                "diagnosis" to "Karang gigi ringan",
                "prescription" to "Scaling gigi, sikat gigi lebih teratur",
                "status" to "COMPLETED",
                "createdAt" to (System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L)),
                "calledAt" to (System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L) + (10 * 60 * 1000L)),
                "completedAt" to (System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L) + (25 * 60 * 1000L))
            ),

            // Booking 2 - COMPLETED
            hashMapOf(
                "id" to "B002",
                "queueNumber" to 12,
                "patientName" to "Ahmad Santoso",
                "doctorName" to "Dr. Indra Mata",
                "specialization" to "Layanan Mata",
                "date" to "2025-01-15",
                "time" to "09:00",
                "complaint" to "Mata perih dan penglihatan kabur",
                "diagnosis" to "Mata kering dan lelah akibat terlalu lama menatap layar",
                "prescription" to "Tetes mata artifisial, istirahat mata setiap 20 menit",
                "status" to "COMPLETED",
                "createdAt" to (System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000L)),
                "calledAt" to (System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000L) + (15 * 60 * 1000L)),
                "completedAt" to (System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000L) + (30 * 60 * 1000L))
            ),

            // Booking 3 - COMPLETED
            hashMapOf(
                "id" to "B003",
                "queueNumber" to 5,
                "patientName" to "Ahmad Santoso",
                "doctorName" to "Dr. Ani Pediatri",
                "specialization" to "Layanan Anak",
                "date" to "2025-01-20",
                "time" to "10:30",
                "complaint" to "Imunisasi anak umur 2 tahun",
                "diagnosis" to "Anak sehat, imunisasi lengkap",
                "prescription" to "Vaksin DPT, Vitamin A",
                "status" to "COMPLETED",
                "createdAt" to (System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)),
                "calledAt" to (System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L) + (8 * 60 * 1000L)),
                "completedAt" to (System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L) + (20 * 60 * 1000L))
            ),

            // Booking 4 - WAITING (hari ini)
            hashMapOf(
                "id" to "B004",
                "queueNumber" to 1,
                "patientName" to "Rizky Amalia",
                "doctorName" to "Dr. Ahmad Santoso",
                "specialization" to "Layanan Umum",
                "date" to getCurrentDate(),
                "time" to "14:00",
                "complaint" to "Demam dan batuk",
                "diagnosis" to "",
                "prescription" to "",
                "status" to "WAITING",
                "createdAt" to System.currentTimeMillis(),
                "calledAt" to 0L,
                "completedAt" to 0L
            ),

            // Booking 5 - WAITING (hari ini)
            hashMapOf(
                "id" to "B005",
                "queueNumber" to 2,
                "patientName" to "Dewi Lestari",
                "doctorName" to "Dr. Ahmad Santoso",
                "specialization" to "Layanan Umum",
                "date" to getCurrentDate(),
                "time" to "14:30",
                "complaint" to "Sakit kepala",
                "diagnosis" to "",
                "prescription" to "",
                "status" to "WAITING",
                "createdAt" to System.currentTimeMillis(),
                "calledAt" to 0L,
                "completedAt" to 0L
            ),

            // Booking 6 - CALLED (hari ini)
            hashMapOf(
                "id" to "B006",
                "queueNumber" to 3,
                "patientName" to "Atila Falah",
                "doctorName" to "Dr. Ahmad Santoso",
                "specialization" to "Layanan Umum",
                "date" to getCurrentDate(),
                "time" to "15:00",
                "complaint" to "Kontrol tekanan darah",
                "diagnosis" to "",
                "prescription" to "",
                "status" to "CALLED",
                "createdAt" to System.currentTimeMillis(),
                "calledAt" to System.currentTimeMillis() + (2 * 60 * 1000L),
                "completedAt" to 0L
            )
        )

        bookings.forEach { booking ->
            db.collection("bookings")
                .document(booking["id"].toString())
                .set(booking)
                .await()
        }

        Log.d(TAG, "✅ ${bookings.size} bookings migrated")
    }

    /**
     * Get current date in yyyy-MM-dd format
     */
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    /**
     * Check if data already exists (to prevent duplicate migration)
     */
    suspend fun checkIfDataExists(): Boolean {
        return try {
            val doctors = db.collection("doctors").limit(1).get().await()
            !doctors.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}