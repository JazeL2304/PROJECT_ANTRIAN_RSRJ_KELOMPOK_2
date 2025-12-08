package com.example.projectantrianrsrjkelompok2.model

/**
 * ✅ User Model - FIXED VERSION
 * Compatible dengan Firebase Realtime Database
 *
 * PENTING: Semua field harus punya default value untuk Firebase deserialization
 */
data class User(
    val id: String = "",                          // Firebase key akan di-map ke sini
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String? = null,              // Bisa String atau null
    val profilePicture: String? = null,
    val userType: UserType = UserType.PATIENT,
    val password: String = "",                    // Tambahan untuk login check
    val createdAt: Long = System.currentTimeMillis(),   // ✅ Long, bukan String
    val updatedAt: Long = System.currentTimeMillis()    // ✅ Long, bukan String
) {
    // ✅ Firebase membutuhkan no-arg constructor
    constructor() : this(
        id = "",
        email = "",
        fullName = "",
        phoneNumber = null,
        profilePicture = null,
        userType = UserType.PATIENT,
        password = "",
        createdAt = 0L,
        updatedAt = 0L
    )
}

enum class UserType {
    PATIENT,      // Pasien/User
    DOCTOR,       // Dokter
    ADMIN         // Admin/Asisten Dokter/Nurse
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignUpRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val phoneNumber: String? = null
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null,
    val token: String? = null
)