package com.example.projectantrianrsrjkelompok2.api

import com.example.projectantrianrsrjkelompok2.model.ImageKitUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * ✅ ImageKit API Service (WITH AUTHENTICATION)
 * Untuk authenticated upload, butuh signature dari backend
 */
interface ImageKitApiService {

    /**
     * Upload dengan AUTHENTICATION (butuh backend)
     */
    @Multipart
    @POST("api/v1/files/upload")
    suspend fun uploadImageAuthenticated(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: RequestBody,
        @Part("folder") folder: RequestBody,
        @Part("publicKey") publicKey: RequestBody,
        @Part("signature") signature: RequestBody,       // ✅ ADDED
        @Part("expire") expire: RequestBody,             // ✅ ADDED
        @Part("token") token: RequestBody,               // ✅ ADDED
        @Part("useUniqueFileName") useUniqueFileName: RequestBody,
        @Part("tags") tags: RequestBody? = null
    ): Response<ImageKitUploadResponse>

    /**
     * Upload TANPA authentication (unsigned upload)
     * Hanya jalan kalau account support unsigned upload
     */
    @Multipart
    @POST("api/v1/files/upload")
    suspend fun uploadImageUnsigned(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: RequestBody,
        @Part("folder") folder: RequestBody,
        @Part("publicKey") publicKey: RequestBody,
        @Part("useUniqueFileName") useUniqueFileName: RequestBody,
        @Part("tags") tags: RequestBody? = null
    ): Response<ImageKitUploadResponse>
}