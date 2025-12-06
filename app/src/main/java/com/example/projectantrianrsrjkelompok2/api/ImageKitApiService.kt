package com.example.projectantrianrsrjkelompok2.api

import com.example.projectantrianrsrjkelompok2.model.ImageKitUploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * ✅ ImageKit API Service
 * Upload endpoint dengan proper authentication
 */
interface ImageKitApiService {

    /**
     * Upload image to ImageKit
     * Endpoint: https://upload.imagekit.io/api/v1/files/upload
     *
     * Authentication: Public Key only (client-side upload)
     */
    @Multipart
    @POST("api/v1/files/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("fileName") fileName: RequestBody,
        @Part("folder") folder: RequestBody,
        @Part("publicKey") publicKey: RequestBody,
        @Part("useUniqueFileName") useUniqueFileName: RequestBody,
        @Part("tags") tags: RequestBody? = null
    ): Response<ImageKitUploadResponse>
}