// model/UserAccount.kt
package com.example.projectantrianrsrjkelompok2.model

/**
 * ✅ User Account Model (Updated with Profile Image)
 * Digunakan untuk Firebase Authentication & Storage
 */
data class UserAccount(
    val id: String = "",
    val email: String = "",
    val password: String = "", // Di production, gunakan hashing!
    val fullName: String = "",
    val phoneNumber: String = "",
    val userType: String = "PATIENT", // PATIENT, DOCTOR, ADMIN
    val profileImageUrl: String? = null, // 🆕 URL foto profil dari ImageKit
    val profileImageFileId: String? = null, // 🆕 File ID dari ImageKit (untuk delete)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()

) {
    // Constructor kosong untuk Firebase
    constructor() : this("", "", "", "", "", "PATIENT", null, null, 0L, 0L)

    /**
     * Convert to User model
     */
    fun toUser(): User {
        return User(
            id = id,
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            profilePicture = profileImageUrl, // Map to User.profilePicture
            userType = UserType.valueOf(userType),
            createdAt = createdAt
        )
    }

    /**
     * Check if user has profile picture
     */
    fun hasProfilePicture(): Boolean {
        return !profileImageUrl.isNullOrEmpty()
    }

    /**
     * Get profile image URL with transformations
     * @param width Optional width for image transformation
     * @param height Optional height for image transformation
     */
    fun getProfileImageUrl(width: Int? = 200, height: Int? = 200): String? {
        if (profileImageUrl.isNullOrEmpty()) return null

        // If already has transformations, return as is
        if (profileImageUrl.contains("?tr=")) return profileImageUrl

        // Add transformations
        val transformations = mutableListOf<String>()
        if (width != null) transformations.add("w-$width")
        if (height != null) transformations.add("h-$height")
        transformations.add("q-80")

        return "$profileImageUrl?tr=${transformations.joinToString(",")}"
    }

    /**
     * Copy with new profile image
     */
    fun withProfileImage(imageUrl: String, fileId: String): UserAccount {
        return copy(
            profileImageUrl = imageUrl,
            profileImageFileId = fileId,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Copy without profile image
     */
    fun withoutProfileImage(): UserAccount {
        return copy(
            profileImageUrl = null,
            profileImageFileId = null,
            updatedAt = System.currentTimeMillis()
        )
    }
}