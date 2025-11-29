package com.example.projectantrianrsrjkelompok2.data

import android.util.Log
import com.example.projectantrianrsrjkelompok2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ✅ FirebaseMigrator - Utility untuk migrasi data lokal ke Firebase
 */
object FirebaseMigrator {

    private val TAG = "FirebaseMigrator"
    private val firebaseRepo = FirebaseRepository()

    /**
     * 🔄 Migrate semua data dummy ke Firebase
     */
    suspend fun migrateAllData() = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Starting migration to Firebase...")

        try {
            migrateDoctors()
            migratePatients()
            migrateSpecializations()

            Log.d(TAG, "✅ Migration completed successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration failed: ${e.message}", e)
        }
    }

    /**
     * 👨‍⚕️ Migrate Doctors
     */
    private suspend fun migrateDoctors() {
        val doctors = listOf(
            Doctor(1, "Dr. Ahmad Santoso", "Dokter Umum", "Senin–Jumat 08:00–15:00"),
            Doctor(2, "Dr. Budi Dental", "Dokter Gigi", "Senin–Kamis 09:00–16:00"),
            Doctor(3, "Dr. Indra Mata", "Dokter Mata", "Senin–Jumat 08:00–14:00"),
            Doctor(4, "Dr. Ani Pediatri", "Dokter Anak", "Setiap Hari 24 Jam"),
            Doctor(5, "Dr. Siti Jantung", "Dokter Jantung", "Senin–Jumat 10:00–16:00"),
            Doctor(6, "Dr. Rina Kandungan", "Dokter Kandungan", "Senin–Sabtu 09:00–15:00")
        )

        Log.d(TAG, "📤 Migrating ${doctors.size} doctors...")
        var successCount = 0

        doctors.forEach { doctor ->
            val success = firebaseRepo.addDoctor(doctor)
            if (success) {
                successCount++
                Log.d(TAG, "✅ Migrated: ${doctor.name}")
            } else {
                Log.e(TAG, "❌ Failed to migrate: ${doctor.name}")
            }
        }

        Log.d(TAG, "✅ Doctors migration: $successCount/${doctors.size} successful")
    }

    /**
     * 🧍‍♀️ Migrate Patients
     */
    private suspend fun migratePatients() {
        val patients = listOf(
            Patient(1, "Atila Falah", "Laki-laki", 21, "Jl. Anggrek No. 12"),
            Patient(2, "Rizky Amalia", "Perempuan", 23, "Jl. Melati No. 8"),
            Patient(3, "Dewi Lestari", "Perempuan", 20, "Jl. Mawar No. 5")
        )

        Log.d(TAG, "📤 Migrating ${patients.size} patients...")
        var successCount = 0

        patients.forEach { patient ->
            val success = firebaseRepo.addPatient(patient)
            if (success) {
                successCount++
                Log.d(TAG, "✅ Migrated: ${patient.name}")
            } else {
                Log.e(TAG, "❌ Failed to migrate: ${patient.name}")
            }
        }

        Log.d(TAG, "✅ Patients migration: $successCount/${patients.size} successful")
    }

    /**
     * 🏥 Migrate Specializations
     */
    private suspend fun migrateSpecializations() {
        val specializations = listOf(
            Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
            Specialization(2, "Layanan Gigi", "Perawatan gigi dan mulut", "🦷"),
            Specialization(3, "Layanan Mata", "Kesehatan mata dan penglihatan", "👁️"),
            Specialization(4, "Layanan Anak", "Kesehatan bayi dan anak-anak", "👶"),
            Specialization(5, "Layanan Jantung", "Kesehatan jantung dan pembuluh darah", "❤️"),
            Specialization(6, "Layanan Kandungan", "Kesehatan ibu dan anak", "🤰")
        )

        Log.d(TAG, "📤 Migrating ${specializations.size} specializations...")
        var successCount = 0

        specializations.forEach { spec ->
            val success = firebaseRepo.addSpecialization(spec)
            if (success) {
                successCount++
                Log.d(TAG, "✅ Migrated: ${spec.name}")
            } else {
                Log.e(TAG, "❌ Failed to migrate: ${spec.name}")
            }
        }

        Log.d(TAG, "✅ Specializations migration: $successCount/${specializations.size} successful")
    }

    /**
     * 📋 Migrate sample bookings (optional)
     */
    private suspend fun migrateBookings() {
        val bookings = listOf(
            Booking(
                id = "B001",
                queueNumber = 1,
                patientName = "Ahmad Santoso",
                doctorName = "Dr. Budi Dental",
                specialization = "Layanan Gigi",
                date = "14/10/2025",
                time = "08:30",
                complaint = "Pemeriksaan rutin gigi",
                diagnosis = "Karang gigi ringan",
                prescription = "Scaling gigi",
                status = BookingStatus.COMPLETED,
                createdAt = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            )
        )

        Log.d(TAG, "📤 Migrating ${bookings.size} bookings...")
        var successCount = 0

        bookings.forEach { booking ->
            val success = firebaseRepo.createBooking(booking)
            if (success) {
                successCount++
                Log.d(TAG, "✅ Migrated: ${booking.id}")
            } else {
                Log.e(TAG, "❌ Failed to migrate: ${booking.id}")
            }
        }

        Log.d(TAG, "✅ Bookings migration: $successCount/${bookings.size} successful")
    }

    /**
     * 🔍 Check migration status
     */
    suspend fun checkMigrationStatus(): MigrationStatus = withContext(Dispatchers.IO) {
        try {
            val doctors = firebaseRepo.getAllDoctors()
            val patients = firebaseRepo.getAllPatients()
            val specializations = firebaseRepo.getSpecializations()

            MigrationStatus(
                hasDoctors = doctors.isNotEmpty(),
                hasPatients = patients.isNotEmpty(),
                hasSpecializations = specializations.isNotEmpty(),
                doctorCount = doctors.size,
                patientCount = patients.size,
                specializationCount = specializations.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking status: ${e.message}")
            MigrationStatus()
        }
    }

    /**
     * 🗑️ Clear all Firebase data (use with caution!)
     */
    suspend fun clearFirebaseData() = withContext(Dispatchers.IO) {
        Log.d(TAG, "⚠️ Clearing all Firebase data...")
        firebaseRepo.clearAllData()
        Log.d(TAG, "✅ Firebase data cleared")
    }

    /**
     * 📊 Migration Status Data Class
     */
    data class MigrationStatus(
        val hasDoctors: Boolean = false,
        val hasPatients: Boolean = false,
        val hasSpecializations: Boolean = false,
        val doctorCount: Int = 0,
        val patientCount: Int = 0,
        val specializationCount: Int = 0
    ) {
        val isComplete: Boolean
            get() = hasDoctors && hasPatients && hasSpecializations

        val isEmpty: Boolean
            get() = !hasDoctors && !hasPatients && !hasSpecializations

        override fun toString(): String {
            return """
                Migration Status:
                - Doctors: $doctorCount ${if (hasDoctors) "✅" else "❌"}
                - Patients: $patientCount ${if (hasPatients) "✅" else "❌"}
                - Specializations: $specializationCount ${if (hasSpecializations) "✅" else "❌"}
                - Complete: ${if (isComplete) "✅" else "❌"}
            """.trimIndent()
        }
    }
}