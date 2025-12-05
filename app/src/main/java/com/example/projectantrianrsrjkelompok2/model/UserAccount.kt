// model/UserAccount.kt
package com.example.projectantrianrsrjkelompok2.model

data class UserAccount(
    val id: String = "",
    val email: String = "",
    val password: String = "", // Di production, gunakan hashing!
    val fullName: String = "",
    val phoneNumber: String = "",
    val userType: String = "PATIENT" // PATIENT, DOCTOR, ADMIN
) {
    // Constructor kosong untuk Firebase
    constructor() : this("", "", "", "", "", "PATIENT")

    fun toUser(): User {
        return User(
            id = id,
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            userType = UserType.valueOf(userType)
        )
    }


}