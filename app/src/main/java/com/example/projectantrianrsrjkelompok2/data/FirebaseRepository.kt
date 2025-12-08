package com.example.projectantrianrsrjkelompok2.data

import android.util.Log
import com.example.projectantrianrsrjkelompok2.*
import com.example.projectantrianrsrjkelompok2.model.UserAccount
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.example.projectantrianrsrjkelompok2.utils.PasswordHasher

/**
 * ✅ FIXED: Firebase Repository dengan parsing yang lebih toleran
 * 🔧 Mengatasi error konversi tipe data (Long to String, dll)
 */
class FirebaseRepository {

    private val TAG = "FirebaseRepository"

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val doctorsRef: DatabaseReference = database.getReference("doctors")
    private val patientsRef: DatabaseReference = database.getReference("patients")
    private val bookingsRef: DatabaseReference = database.getReference("bookings")
    private val specializationsRef: DatabaseReference = database.getReference("specializations")
    private val usersRef: DatabaseReference = database.getReference("users")
    private val countersRef: DatabaseReference = database.getReference("counters")

    // ===============================
    // 🔐 USER AUTHENTICATION
    // ===============================

    /**
     * ✅ FIXED: Parse user dari Firebase dengan handling berbagai tipe data
     */
    private fun parseUserFromSnapshot(snapshot: DataSnapshot): UserAccount? {
        return try {
            val id = snapshot.child("id").value?.toString() ?: snapshot.key ?: return null
            val email = snapshot.child("email").value?.toString() ?: return null
            val password = snapshot.child("password").value?.toString() ?: return null
            val fullName = snapshot.child("fullName").value?.toString() ?: ""
            val userType = snapshot.child("userType").value?.toString() ?: "PATIENT"

            // ✅ Handle phoneNumber - bisa String atau Number
            val phoneNumber = when (val phone = snapshot.child("phoneNumber").value) {
                is String -> phone
                is Number -> phone.toString()
                null -> ""
                else -> phone.toString()
            }

            // ✅ Handle createdAt - bisa Long atau String
            val createdAt = when (val created = snapshot.child("createdAt").value) {
                is Long -> created
                is String -> created.toLongOrNull() ?: System.currentTimeMillis()
                else -> System.currentTimeMillis()
            }

            // ✅ Handle updatedAt - bisa Long atau String
            val updatedAt = when (val updated = snapshot.child("updatedAt").value) {
                is Long -> updated
                is String -> updated.toLongOrNull() ?: System.currentTimeMillis()
                else -> System.currentTimeMillis()
            }

            // ✅ Handle profile image fields
            val profileImageUrl = snapshot.child("profileImageUrl").value?.toString()
            val profileImageFileId = snapshot.child("profileImageFileId").value?.toString()

            UserAccount(
                id = id,
                email = email,
                password = password,
                fullName = fullName,
                phoneNumber = phoneNumber,
                userType = userType,
                profileImageUrl = profileImageUrl,
                profileImageFileId = profileImageFileId,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing user from snapshot: ${e.message}", e)
            Log.e(TAG, "   Snapshot key: ${snapshot.key}")
            null
        }
    }

    /**
     * Login - Cari user berdasarkan email dan password (dengan hash verification)
     */
    suspend fun loginUser(email: String, password: String): UserAccount? {
        return suspendCoroutine { continuation ->
            usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) {
                            // ✅ FIXED: Gunakan parseUserFromSnapshot
                            val user = parseUserFromSnapshot(child)
                            if (user != null) {
                                val isPasswordValid = if (PasswordHasher.isBCryptHash(user.password)) {
                                    PasswordHasher.verifyPassword(password, user.password)
                                } else {
                                    Log.w(TAG, "⚠️ Warning: User ${user.email} still using plain text password")
                                    user.password == password
                                }

                                if (isPasswordValid) {
                                    Log.d(TAG, "✅ Login successful: ${user.email}")
                                    continuation.resume(user)
                                    return
                                }
                            }
                        }
                        Log.w(TAG, "❌ Login failed: Invalid credentials")
                        continuation.resume(null)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Login error: ${error.message}")
                        continuation.resume(null)
                    }
                })
        }
    }

    /**
     * Register user baru (dengan password hashing)
     */
    suspend fun registerUser(userAccount: UserAccount): Boolean {
        return suspendCoroutine { continuation ->
            usersRef.orderByChild("email").equalTo(userAccount.email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.w(TAG, "❌ Email already exists: ${userAccount.email}")
                            continuation.resume(false)
                        } else {
                            try {
                                val hashedPassword = PasswordHasher.hashPassword(userAccount.password)
                                val userWithHashedPassword = userAccount.copy(password = hashedPassword)

                                usersRef.child(userWithHashedPassword.id).setValue(userWithHashedPassword)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "✅ User registered with hashed password: ${userAccount.email}")
                                        continuation.resume(true)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "❌ Registration failed: ${e.message}")
                                        continuation.resume(false)
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error hashing password: ${e.message}")
                                continuation.resume(false)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Registration error: ${error.message}")
                        continuation.resume(false)
                    }
                })
        }
    }

    /**
     * Add user account (for seeding)
     */
    suspend fun addUserAccount(user: UserAccount): Boolean {
        return suspendCoroutine { continuation ->
            usersRef.child(user.id).setValue(user)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ User account added: ${user.email}")
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to add user: ${e.message}")
                    continuation.resume(false)
                }
        }
    }

    /**
     * ✅ FIXED: Get all users dengan parsing yang lebih robust
     */
    suspend fun getAllUsers(): List<UserAccount> {
        return suspendCoroutine { continuation ->
            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = snapshot.children.mapNotNull { child ->
                        parseUserFromSnapshot(child)
                    }
                    Log.d(TAG, "✅ Loaded ${users.size} users from Firebase")
                    continuation.resume(users)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Error getting all users: ${error.message}")
                    continuation.resume(emptyList())
                }
            })
        }
    }

    // ===============================
    // 🖼️ USER PROFILE IMAGE METHODS
    // ===============================

    suspend fun updateUserProfileImage(
        userId: String,
        imageUrl: String,
        fileId: String
    ): Boolean {
        return try {
            val updates = mapOf(
                "profileImageUrl" to imageUrl,
                "profileImageFileId" to fileId,
                "updatedAt" to System.currentTimeMillis()
            )
            usersRef.child(userId).updateChildren(updates).await()
            Log.d(TAG, "✅ Profile image updated for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating profile image: ${e.message}", e)
            false
        }
    }

    suspend fun removeUserProfileImage(userId: String): Boolean {
        return try {
            val updates = mapOf(
                "profileImageUrl" to null,
                "profileImageFileId" to null,
                "updatedAt" to System.currentTimeMillis()
            )
            usersRef.child(userId).updateChildren(updates).await()
            Log.d(TAG, "✅ Profile image removed for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error removing profile image: ${e.message}", e)
            false
        }
    }

    suspend fun deleteUserProfileImage(userId: String): Boolean {
        return removeUserProfileImage(userId)
    }

    /**
     * ✅ FIXED: Get user by ID dengan parsing yang lebih robust
     */
    suspend fun getUserById(userId: String): UserAccount? {
        return suspendCoroutine { continuation ->
            usersRef.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = parseUserFromSnapshot(snapshot)
                        if (user != null) {
                            Log.d(TAG, "✅ User found: ${user.email}")
                            continuation.resume(user)
                        } else {
                            Log.w(TAG, "❌ User not found: $userId")
                            continuation.resume(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error getting user: ${error.message}")
                        continuation.resume(null)
                    }
                })
        }
    }

    suspend fun updateUserAccount(userAccount: UserAccount): Boolean {
        return try {
            usersRef.child(userAccount.id).setValue(userAccount).await()
            Log.d(TAG, "✅ User account updated: ${userAccount.email}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating user account: ${e.message}", e)
            false
        }
    }

    // ===============================
    // 👨‍⚕️ DOCTORS
    // ===============================

    suspend fun getAllDoctors(): List<Doctor> {
        return try {
            Log.d(TAG, "📥 Fetching all doctors from Firebase...")
            val snapshot = doctorsRef.get().await()
            val doctors = snapshot.children.mapNotNull { it.getValue(Doctor::class.java) }
            Log.d(TAG, "✅ Loaded ${doctors.size} doctors from Firebase")
            doctors
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting doctors: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getDoctorsBySpecialization(specialization: String): List<Doctor> {
        return try {
            Log.d(TAG, "📥 Fetching doctors for specialization: $specialization")
            val snapshot = doctorsRef.get().await()
            val doctors = snapshot.children.mapNotNull {
                it.getValue(Doctor::class.java)
            }.filter { doctor ->
                doctor.specialization.equals(specialization, ignoreCase = true) ||
                        doctor.specialization.contains(specialization, ignoreCase = true)
            }
            Log.d(TAG, "✅ Found ${doctors.size} doctors for $specialization")
            doctors
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting doctors by spec: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addDoctor(doctor: Doctor): Boolean {
        return try {
            val id = doctor.id.toString()
            doctorsRef.child(id).setValue(doctor).await()
            Log.d(TAG, "✅ Doctor added: ${doctor.name} (ID: $id)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding doctor: ${e.message}", e)
            false
        }
    }

    suspend fun updateDoctor(doctor: Doctor): Boolean {
        return try {
            val id = doctor.id.toString()
            doctorsRef.child(id).setValue(doctor).await()
            Log.d(TAG, "✅ Doctor updated: ${doctor.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating doctor: ${e.message}", e)
            false
        }
    }

    suspend fun deleteDoctor(doctorId: Int): Boolean {
        return try {
            doctorsRef.child(doctorId.toString()).removeValue().await()
            Log.d(TAG, "✅ Doctor deleted: ID $doctorId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting doctor: ${e.message}", e)
            false
        }
    }

    // ===============================
    // 🧍‍♀️ PATIENTS
    // ===============================

    suspend fun getAllPatients(): List<Patient> {
        return try {
            Log.d(TAG, "📥 Fetching all patients from Firebase...")
            val snapshot = patientsRef.get().await()
            val patients = snapshot.children.mapNotNull { it.getValue(Patient::class.java) }
            Log.d(TAG, "✅ Loaded ${patients.size} patients from Firebase")
            patients
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting patients: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addPatient(patient: Patient): Boolean {
        return try {
            val id = patient.id.toString()
            patientsRef.child(id).setValue(patient).await()
            Log.d(TAG, "✅ Patient added: ${patient.name} (ID: $id)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding patient: ${e.message}", e)
            false
        }
    }

    suspend fun updatePatient(patient: Patient): Boolean {
        return try {
            val id = patient.id.toString()
            patientsRef.child(id).setValue(patient).await()
            Log.d(TAG, "✅ Patient updated: ${patient.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating patient: ${e.message}", e)
            false
        }
    }

    suspend fun deletePatient(patientId: Int): Boolean {
        return try {
            patientsRef.child(patientId.toString()).removeValue().await()
            Log.d(TAG, "✅ Patient deleted: ID $patientId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting patient: ${e.message}", e)
            false
        }
    }

    // ===============================
    // 🏥 SPECIALIZATIONS
    // ===============================

    suspend fun getSpecializations(): List<Specialization> {
        return try {
            Log.d(TAG, "📥 Fetching specializations from Firebase...")
            val snapshot = specializationsRef.get().await()
            val specializations = snapshot.children.mapNotNull { it.getValue(Specialization::class.java) }

            if (specializations.isEmpty()) {
                Log.w(TAG, "⚠️ No specializations in Firebase, returning defaults")
                return getDefaultSpecializations()
            }

            Log.d(TAG, "✅ Loaded ${specializations.size} specializations from Firebase")
            specializations
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting specializations: ${e.message}", e)
            getDefaultSpecializations()
        }
    }

    private fun getDefaultSpecializations(): List<Specialization> {
        return listOf(
            Specialization(1, "Layanan Umum", "Pelayanan kesehatan umum", "🏥"),
            Specialization(2, "Layanan Gigi", "Perawatan gigi dan mulut", "🦷"),
            Specialization(3, "Layanan Mata", "Kesehatan mata dan penglihatan", "👁️"),
            Specialization(4, "Layanan Anak", "Kesehatan bayi dan anak-anak", "👶"),
            Specialization(5, "Layanan Jantung", "Kesehatan jantung dan pembuluh darah", "❤️"),
            Specialization(6, "Layanan Kandungan", "Kesehatan ibu dan anak", "🤰")
        )
    }

    suspend fun addSpecialization(specialization: Specialization): Boolean {
        return try {
            val id = specialization.id.toString()
            specializationsRef.child(id).setValue(specialization).await()
            Log.d(TAG, "✅ Specialization added: ${specialization.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding specialization: ${e.message}", e)
            false
        }
    }

    // ===============================
    // 📋 BOOKINGS
    // ===============================

    suspend fun getBookingHistory(): List<Booking> {
        return try {
            Log.d(TAG, "📥 Fetching booking history from Firebase...")
            val snapshot = bookingsRef.orderByChild("createdAt").get().await()
            val bookings = snapshot.children.mapNotNull { it.getValue(Booking::class.java) }
                .sortedByDescending { it.createdAt }
            Log.d(TAG, "✅ Loaded ${bookings.size} bookings from Firebase")
            bookings
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting booking history: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun createBooking(booking: Booking): Boolean {
        return try {
            bookingsRef.child(booking.id).setValue(booking).await()
            Log.d(TAG, "✅ Booking created: ${booking.id} for ${booking.patientName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating booking: ${e.message}", e)
            false
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Boolean {
        return try {
            bookingsRef.child(bookingId).child("status").setValue(status.name).await()
            Log.d(TAG, "✅ Booking status updated: $bookingId -> $status")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating booking status: ${e.message}", e)
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
            Log.d(TAG, "✅ Booking diagnosis updated: $bookingId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating diagnosis: ${e.message}", e)
            false
        }
    }

    suspend fun getTodayBookings(): List<Booking> {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        return try {
            Log.d(TAG, "📥 Fetching today's bookings ($today)...")
            val snapshot = bookingsRef.orderByChild("date").equalTo(today).get().await()
            val bookings = snapshot.children.mapNotNull { it.getValue(Booking::class.java) }
            Log.d(TAG, "✅ Found ${bookings.size} bookings for today")
            bookings
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting today bookings: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getActiveQueues(): List<Booking> {
        return try {
            Log.d(TAG, "📥 Fetching active queues...")
            val allBookings = getBookingHistory()
            val activeQueues = allBookings.filter {
                it.status == BookingStatus.WAITING || it.status == BookingStatus.CALLED
            }
            Log.d(TAG, "✅ Found ${activeQueues.size} active queues")
            activeQueues
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting active queues: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getNextQueueNumber(): Int {
        return try {
            val snapshot = bookingsRef.orderByChild("queueNumber").limitToLast(1).get().await()
            val maxQueue = snapshot.children.mapNotNull {
                it.getValue(Booking::class.java)?.queueNumber
            }.maxOrNull() ?: 0
            val nextNumber = maxQueue + 1
            Log.d(TAG, "✅ Next queue number: $nextNumber")
            nextNumber
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting next queue number: ${e.message}", e)
            1
        }
    }

    suspend fun clearAllData() {
        try {
            Log.w(TAG, "⚠️ Clearing all Firebase data...")
            doctorsRef.removeValue().await()
            patientsRef.removeValue().await()
            bookingsRef.removeValue().await()
            specializationsRef.removeValue().await()
            Log.d(TAG, "✅ All Firebase data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing data: ${e.message}", e)
        }
    }

    suspend fun checkConnection(): Boolean {
        return try {
            Log.d(TAG, "🔍 Checking Firebase connection...")
            val snapshot = database.getReference(".info/connected").get().await()
            val isConnected = snapshot.getValue(Boolean::class.java) ?: false
            Log.d(TAG, if (isConnected) "✅ Firebase connected" else "❌ Firebase disconnected")
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking connection: ${e.message}", e)
            false
        }
    }

    suspend fun getNextUserId(): String {
        return suspendCoroutine { continuation ->
            countersRef.child("userIdCounter").get()
                .addOnSuccessListener { snapshot ->
                    val currentCounter = snapshot.getValue(Int::class.java) ?: 0
                    val nextCounter = currentCounter + 1
                    val userId = "user${String.format("%03d", nextCounter)}"

                    countersRef.child("userIdCounter").setValue(nextCounter)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Next user ID generated: $userId")
                            continuation.resume(userId)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Failed to update counter: ${e.message}")
                            continuation.resume("user_${System.currentTimeMillis()}")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to get counter: ${e.message}")
                    continuation.resume("user_${System.currentTimeMillis()}")
                }
        }
    }

    suspend fun initializeUserIdCounter(): Boolean {
        return try {
            Log.d(TAG, "🔄 Initializing user ID counter...")
            val allUsers = getAllUsers()
            var maxNumber = 0

            for (user in allUsers) {
                val match = Regex("user(\\d+)").find(user.id)
                if (match != null) {
                    val number = match.groupValues[1].toIntOrNull() ?: 0
                    if (number > maxNumber) {
                        maxNumber = number
                    }
                }
            }

            countersRef.child("userIdCounter").setValue(maxNumber).await()
            Log.d(TAG, "✅ Counter initialized to: $maxNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing counter: ${e.message}", e)
            false
        }
    }
}