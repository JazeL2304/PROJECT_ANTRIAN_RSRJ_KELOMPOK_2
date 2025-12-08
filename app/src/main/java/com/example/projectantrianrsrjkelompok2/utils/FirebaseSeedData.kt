package com.example.projectantrianrsrjkelompok2.utils

import android.util.Log
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import com.example.projectantrianrsrjkelompok2.model.UserAccount

/**
 * ✅ UPDATED Firebase Seeding - With proper data & hash support
 *
 * PERUBAHAN:
 * 1. Admin001 fullName = "angelica" (bukan "Siti Nurhaliza")
 * 2. Doc002 password = "siti123" (bukan "dokter123")
 * 3. Password otomatis di-hash dengan bcrypt
 * 4. Seed HANYA SEKALI (check existing data)
 */
object FirebaseSeedData {

    private const val TAG = "FirebaseSeedData"
    private val firebaseRepo = FirebaseRepository()

    /**
     * 🔐 Seed User Accounts - UPDATED DATA
     */
    private suspend fun seedUsers() {
        Log.d(TAG, "🔐 Preparing to seed user accounts...")

        // ✅ Data user yang sudah di-update
        val usersPlain = listOf(
            UserAccount(
                id = "user001",
                email = "user@example.com",
                password = "password123",  // Plain, akan di-hash
                fullName = "John Doe",
                phoneNumber = "081234567890",
                userType = "PATIENT"
            ),
            UserAccount(
                id = "doc001",
                email = "dokter@rumahsakit.com",
                password = "dokter123",  // Plain, akan di-hash
                fullName = "Dr. Ahmad Susanto",
                phoneNumber = "081234567891",
                userType = "DOCTOR"
            ),
            UserAccount(
                id = "doc002",
                email = "siti@rumahsakit.com",
                password = "siti123",  // ✅ PASSWORD BARU!
                fullName = "Dr. Siti Nurhaliza",
                phoneNumber = "081234567892",
                userType = "DOCTOR"
            ),
            UserAccount(
                id = "admin001",
                email = "admin@rumahsakit.com",
                password = "admin123",  // Plain, akan di-hash
                fullName = "angelica",  // ✅ NAMA BARU!
                phoneNumber = "081234567893",
                userType = "ADMIN"
            )
        )

        Log.d(TAG, "🔐 Hashing passwords...")

        // ✅ Hash semua password SEBELUM disimpan
        val usersWithHashedPassword = usersPlain.map { user ->
            val hashedPassword = PasswordHasher.hashPassword(user.password)
            Log.d(TAG, "  🔐 ${user.id}: ${user.password} → ${hashedPassword.take(30)}...")

            user.copy(password = hashedPassword)
        }

        Log.d(TAG, "📤 Seeding ${usersWithHashedPassword.size} user accounts...")
        var successCount = 0

        usersWithHashedPassword.forEach { user ->
            try {
                val success = firebaseRepo.addUserAccount(user)
                if (success) {
                    successCount++
                    Log.d(TAG, "  ✅ Added: ${user.email} (${user.userType}) - ${user.fullName}")
                } else {
                    Log.e(TAG, "  ❌ Failed to add: ${user.email}")
                }
                delay(200) // Delay untuk stabilitas Firebase
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Exception adding ${user.email}: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Users: $successCount/${usersWithHashedPassword.size} added (with hashed passwords)")
    }

    /**
     * 🌱 Seed All Data - WITH PROPER CHECKS
     */
    fun seedAllData() {
        runBlocking {
            try {
                Log.d(TAG, "🌱 Starting Firebase seed...")

                // ✅ CHECK: Apakah data sudah ada?
                val existingDoctors = firebaseRepo.getAllDoctors()

                if (existingDoctors.isNotEmpty()) {
                    Log.d(TAG, "ℹ️ Data already exists in Firebase (${existingDoctors.size} doctors)")
                    Log.d(TAG, "ℹ️ Skipping seed to prevent overwrite")
                    DataSource.forceLoadFromFirebase()
                    return@runBlocking
                }

                // ✅ Data belum ada, mulai seeding
                Log.d(TAG, "📤 No existing data found, starting seed...")

                // Seed users PERTAMA (dengan password hashed)
                seedUsers()
                delay(2000)

                // Seed specializations
                seedSpecializations()
                delay(1000)

                // Seed doctors
                seedDoctors()
                delay(1000)

                // Seed patients
                seedPatients()
                delay(1000)

                // Seed sample bookings
                seedBookings()
                delay(1000)

                // Force reload data
                DataSource.forceLoadFromFirebase()

                Log.d(TAG, "✅ Seed completed successfully!")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Seed failed: ${e.message}", e)
            }
        }
    }

    /**
     * 🏥 Seed Specializations
     */
    private suspend fun seedSpecializations() {
        val specializations = listOf(
            Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
            Specialization(2, "Layanan Gigi", "Perawatan gigi dan mulut", "🦷"),
            Specialization(3, "Layanan Mata", "Kesehatan mata dan penglihatan", "👁️"),
            Specialization(4, "Layanan Anak", "Kesehatan bayi dan anak-anak", "👶"),
            Specialization(5, "Layanan Jantung", "Kesehatan jantung dan pembuluh darah", "❤️"),
            Specialization(6, "Layanan Kandungan", "Kesehatan ibu dan anak", "🤰")
        )

        Log.d(TAG, "📤 Seeding ${specializations.size} specializations...")

        specializations.forEach { spec ->
            try {
                val success = firebaseRepo.addSpecialization(spec)
                if (success) {
                    Log.d(TAG, "  ✅ Added: ${spec.name}")
                } else {
                    Log.e(TAG, "  ❌ Failed to add: ${spec.name}")
                }
                delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Exception adding ${spec.name}: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Specializations seeding completed")
    }

    /**
     * 👨‍⚕️ Seed Doctors
     */
    private suspend fun seedDoctors() {
        val doctors = listOf(
            // ✅ Dokter Umum - SHIFT BERSELINGAN
            Doctor(1, "Dr. Ahmad Santoso", "Dokter Umum", "Senin–Jumat 08:00–20:00"),
            Doctor(2, "Dr. Siti Nurhaliza", "Dokter Umum", "Senin–Jumat 20:00–08:00"),

            // ✅ Dokter Gigi - SHIFT BERSELINGAN
            Doctor(3, "Dr. Budi Hartono", "Dokter Gigi", "Senin–Kamis 08:00–20:00"),
            Doctor(4, "Dr. Dewi Lestari", "Dokter Gigi", "Senin–Kamis 20:00–08:00"),

            // ✅ Dokter Mata - SHIFT BERSELINGAN
            Doctor(5, "Dr. Indra Wijaya", "Dokter Mata", "Senin–Jumat 08:00–20:00"),
            Doctor(6, "Dr. Maya Anggraini", "Dokter Mata", "Senin–Jumat 20:00–08:00"),

            // ✅ Dokter Anak - 24 JAM
            Doctor(7, "Dr. Ani Kusuma", "Dokter Anak", "Senin–Minggu 08:00–20:00"),
            Doctor(8, "Dr. Rina Permata", "Dokter Anak", "Senin–Minggu 20:00–08:00"),

            // ✅ Dokter Jantung - SHIFT BERSELINGAN
            Doctor(9, "Dr. Joko Widodo", "Dokter Jantung", "Senin–Jumat 08:00–20:00"),
            Doctor(10, "Dr. Andi Cahyono", "Dokter Jantung", "Senin–Jumat 20:00–08:00"),

            // ✅ Dokter Kandungan - SHIFT BERSELINGAN
            Doctor(11, "Dr. Maya Sari", "Dokter Kandungan", "Senin–Sabtu 08:00–20:00"),
            Doctor(12, "Dr. Ratna Dewi", "Dokter Kandungan", "Senin–Sabtu 20:00–08:00")
        )

        Log.d(TAG, "📤 Seeding ${doctors.size} doctors with shift schedules...")
        var successCount = 0

        doctors.forEach { doctor ->
            try {
                val success = firebaseRepo.addDoctor(doctor)
                if (success) {
                    successCount++
                    Log.d(TAG, "  ✅ Added: ${doctor.name} (${doctor.specialization})")
                } else {
                    Log.e(TAG, "  ❌ Failed to add: ${doctor.name}")
                }
                delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Exception adding ${doctor.name}: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Doctors: $successCount/${doctors.size} added")
    }

    /**
     * 🧍‍♀️ Seed Patients
     */
    private suspend fun seedPatients() {
        val patients = listOf(
            Patient(1, "Atila Falah", "Laki-laki", 21, "Jl. Anggrek No. 12, Jakarta Selatan"),
            Patient(2, "Rizky Amalia", "Perempuan", 23, "Jl. Melati No. 8, Jakarta Pusat"),
            Patient(3, "Dewi Lestari", "Perempuan", 20, "Jl. Mawar No. 5, Tangerang"),
            Patient(4, "Budi Santoso", "Laki-laki", 35, "Jl. Kenanga No. 15, Bekasi"),
            Patient(5, "Siti Nurhaliza", "Perempuan", 28, "Jl. Dahlia No. 20, Depok"),
            Patient(6, "Andi Pratama", "Laki-laki", 30, "Jl. Flamboyan No. 7, Jakarta Timur"),
            Patient(7, "Maya Anggraini", "Perempuan", 25, "Jl. Tulip No. 11, Bogor"),
            Patient(8, "Rudi Hermawan", "Laki-laki", 40, "Jl. Sakura No. 3, Jakarta Barat")
        )

        Log.d(TAG, "📤 Seeding ${patients.size} patients...")
        var successCount = 0

        patients.forEach { patient ->
            try {
                val success = firebaseRepo.addPatient(patient)
                if (success) {
                    successCount++
                    Log.d(TAG, "  ✅ Added: ${patient.name}")
                } else {
                    Log.e(TAG, "  ❌ Failed to add: ${patient.name}")
                }
                delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Exception adding ${patient.name}: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Patients: $successCount/${patients.size} added")
    }

    /**
     * 📋 Seed Sample Bookings
     */
    private suspend fun seedBookings() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val bookings = listOf(
            Booking(
                id = "B001",
                queueNumber = 1,
                patientName = "Atila Falah",
                doctorName = "Dr. Ahmad Santoso",
                specialization = "Layanan Umum",
                date = today,
                time = "08:30",
                complaint = "Demam tinggi dan batuk",
                diagnosis = "",
                prescription = "",
                status = BookingStatus.WAITING,
                createdAt = System.currentTimeMillis()
            ),
            Booking(
                id = "B002",
                queueNumber = 2,
                patientName = "Rizky Amalia",
                doctorName = "Dr. Budi Hartono",
                specialization = "Layanan Gigi",
                date = today,
                time = "09:00",
                complaint = "Sakit gigi",
                diagnosis = "",
                prescription = "",
                status = BookingStatus.WAITING,
                createdAt = System.currentTimeMillis()
            ),
            Booking(
                id = "B003",
                queueNumber = 3,
                patientName = "Dewi Lestari",
                doctorName = "Dr. Indra Wijaya",
                specialization = "Layanan Mata",
                date = today,
                time = "10:00",
                complaint = "Mata minus",
                diagnosis = "",
                prescription = "",
                status = BookingStatus.WAITING,
                createdAt = System.currentTimeMillis()
            )
        )

        Log.d(TAG, "📤 Seeding ${bookings.size} sample bookings...")
        var successCount = 0

        bookings.forEach { booking ->
            try {
                val success = firebaseRepo.createBooking(booking)
                if (success) {
                    successCount++
                    Log.d(TAG, "  ✅ Added: ${booking.id}")
                } else {
                    Log.e(TAG, "  ❌ Failed to add: ${booking.id}")
                }
                delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "  ❌ Exception adding ${booking.id}: ${e.message}")
            }
        }

        Log.d(TAG, "✅ Bookings: $successCount/${bookings.size} added")
    }

    /**
     * 🗑️ Clear all Firebase data
     */
    fun clearAllData() {
        runBlocking {
            try {
                Log.w(TAG, "⚠️ Clearing all Firebase data...")
                firebaseRepo.clearAllData()
                delay(1000)
                DataSource.invalidateCache()
                Log.d(TAG, "✅ All data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Clear failed: ${e.message}", e)
            }
        }
    }

    /**
     * 📊 Get data status
     */
    fun getDataStatus(): String {
        return runBlocking {
            try {
                val doctors = firebaseRepo.getAllDoctors()
                val patients = firebaseRepo.getAllPatients()
                val bookings = firebaseRepo.getBookingHistory()
                val specs = firebaseRepo.getSpecializations()

                """
                Firebase Data Status:
                - Doctors: ${doctors.size}
                - Patients: ${patients.size}
                - Bookings: ${bookings.size}
                - Specializations: ${specs.size}
                """.trimIndent()
            } catch (e: Exception) {
                "❌ Error: ${e.message}"
            }
        }
    }

    /**
     * ✅ REMOVED: forceSeedUsersOnly()
     * Tidak perlu lagi karena seedAllData() sudah handle dengan baik
     */
}