package com.example.projectantrianrsrjkelompok2.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val profilePicture: String? = null,
    val userType: UserType = UserType.PATIENT,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserType {
    PATIENT,      // Pasien/User
    DOCTOR,       // Dokter
    ADMIN         // Admin/Asisten Dokter/Nurse (digabung jadi satu)
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
