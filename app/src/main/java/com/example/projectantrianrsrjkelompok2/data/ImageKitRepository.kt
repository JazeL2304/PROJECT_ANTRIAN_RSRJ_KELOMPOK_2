package com.example.projectantrianrsrjkelompok2.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.util.Base64
import com.example.projectantrianrsrjkelompok2.api.ImageKitApiService
import com.example.projectantrianrsrjkelompok2.model.ImageKitUploadResponse
import com.example.projectantrianrsrjkelompok2.utils.ImageKitConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * ✅ ImageKit Repository WITH Backend Authentication
 * - Upload profile picture
 * - Delete profile picture
 */
class ImageKitRepository(private val context: Context) {

    private val TAG = "ImageKitRepository"

    // ⚠️ GANTI dengan URL backend Anda setelah deploy
    private val AUTH_BACKEND_URL = "https://imagekit-auth-backend.vercel.app/imagekit-auth"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://upload.imagekit.io/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request()
                        Log.d(TAG, "API Request: ${request.url}")
                        val response = chain.proceed(request)
                        Log.d(TAG, "Response code: ${response.code}")
                        response
                    }
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: ImageKitApiService by lazy {
        retrofit.create(ImageKitApiService::class.java)
    }

    /**
     * ✅ Get authentication parameters dari backend
     * 🔧 FIXED: Tambah no-cache headers dan validasi expire time
     */
    private suspend fun getAuthParams(): AuthParams? {
        return try {
            Log.d(TAG, "🔐 Getting auth params from backend...")

            // ✅ FIX 1: Tambah no-cache headers
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(AUTH_BACKEND_URL)
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val authParams = AuthParams(
                    token = json.getString("token"),
                    expire = json.getString("expire"),
                    signature = json.getString("signature"),
                    publicKey = json.getString("publicKey")
                )

                // ✅ FIX 2: Validasi expire time lebih ketat
                val expireTime = authParams.expire.toLongOrNull() ?: 0
                val currentTime = System.currentTimeMillis() / 1000
                val diff = expireTime - currentTime

                Log.d(TAG, "✅ Auth params received")
                Log.d(TAG, "   - Device time: $currentTime")
                Log.d(TAG, "   - Expire time: $expireTime")
                Log.d(TAG, "   - Time until expire: ${diff}s (should be ~3600s)")

                // ✅ FIX 3: Validasi apakah expire time valid
                if (diff < 10) {
                    Log.e(TAG, "❌ Auth token already expired or about to expire! Diff: ${diff}s")
                    return null
                }
                if (diff > 3595) {
                    Log.e(TAG, "❌ Expire time too long (${diff}s), need fresh auth from backend")
                    return null
                }

                Log.d(TAG, "✅ Auth params validation passed")
                authParams
            } else {
                Log.e(TAG, "❌ Failed to get auth params: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting auth params: ${e.message}", e)
            null
        }
    }

    /**
     * ✅ Upload profile picture to ImageKit
     * 🔧 FIXED: Tambah validasi sebelum upload dan retry mechanism
     */
    suspend fun uploadProfilePicture(
        imageUri: Uri,
        userId: String,
        retryCount: Int = 0
    ): ImageKitUploadResponse? {
        return try {
            Log.d(TAG, "📤 Starting image upload for user: $userId (attempt ${retryCount + 1})")

            // Step 1: Get FRESH authentication params dari backend
            Log.d(TAG, "🔐 Getting FRESH authentication...")
            val authParams = getAuthParams()
            if (authParams == null) {
                Log.e(TAG, "❌ Cannot get authentication parameters")

                // ✅ FIX 4: Retry jika gagal get auth (max 2 retry)
                if (retryCount < 2) {
                    Log.w(TAG, "⚠️ Retrying to get auth params...")
                    delay(1000) // Wait 1 second
                    return uploadProfilePicture(imageUri, userId, retryCount + 1)
                }
                return null
            }
            Log.d(TAG, "✅ Authentication params received")

            // ✅ FIX 5: Cek lagi sebelum mulai compress (karena compress butuh waktu)
            val expireTime = authParams.expire.toLongOrNull() ?: 0
            val currentTime = System.currentTimeMillis() / 1000
            val timeLeft = expireTime - currentTime

            if (timeLeft < 30) {
                Log.e(TAG, "❌ Not enough time to upload! Time left: ${timeLeft}s")
                if (retryCount < 2) {
                    Log.w(TAG, "⚠️ Getting new auth params...")
                    delay(500)
                    return uploadProfilePicture(imageUri, userId, retryCount + 1)
                }
                return null
            }

            // Step 2: Compress image
            Log.d(TAG, "🔄 Compressing image...")
            val compressedFile = compressImage(imageUri)
            if (compressedFile == null) {
                Log.e(TAG, "❌ Failed to compress image")
                return null
            }
            Log.d(TAG, "📦 Image compressed: ${compressedFile.length() / 1024}KB")

            // ✅ FIX 6: Cek lagi setelah compress sebelum upload
            val currentTime2 = System.currentTimeMillis() / 1000
            val timeLeft2 = expireTime - currentTime2

            Log.d(TAG, "⏱️ Time check before upload:")
            Log.d(TAG, "   - Time left: ${timeLeft2}s")

            if (timeLeft2 < 10) {
                Log.e(TAG, "❌ Auth about to expire! Getting new auth...")
                compressedFile.delete()
                if (retryCount < 2) {
                    delay(500)
                    return uploadProfilePicture(imageUri, userId, retryCount + 1)
                }
                return null
            }

            // Step 3: Prepare upload parameters
            val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val folder = ImageKitConfig.PROFILE_FOLDER

            // Step 4: Create multipart request
            val filePart = MultipartBody.Part.createFormData(
                "file",
                fileName,
                compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )

            val fileNamePart = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
            val folderPart = folder.toRequestBody("text/plain".toMediaTypeOrNull())
            val publicKeyPart = authParams.publicKey.toRequestBody("text/plain".toMediaTypeOrNull())
            val signaturePart = authParams.signature.toRequestBody("text/plain".toMediaTypeOrNull())
            val expirePart = authParams.expire.toRequestBody("text/plain".toMediaTypeOrNull())
            val tokenPart = authParams.token.toRequestBody("text/plain".toMediaTypeOrNull())
            val useUniqueFileNamePart = "true".toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsPart = "profile,user,$userId".toRequestBody("text/plain".toMediaTypeOrNull())

            // Step 5: Upload to ImageKit dengan authentication
            Log.d(TAG, "☁️ Uploading to ImageKit with authentication...")
            Log.d(TAG, "   - Time left: ${timeLeft2}s")

            val response = apiService.uploadImageAuthenticated(
                file = filePart,
                fileName = fileNamePart,
                folder = folderPart,
                publicKey = publicKeyPart,
                signature = signaturePart,
                expire = expirePart,
                token = tokenPart,
                useUniqueFileName = useUniqueFileNamePart,
                tags = tagsPart
            )

            // Step 6: Clean up temp file
            compressedFile.delete()

            Log.d(TAG, "📡 Response received: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val uploadResponse = response.body()!!
                Log.d(TAG, "✅ Upload successful!")
                Log.d(TAG, "   - File ID: ${uploadResponse.fileId}")
                Log.d(TAG, "   - URL: ${uploadResponse.url}")
                uploadResponse
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Upload failed: ${response.code()} - ${response.message()}")
                Log.e(TAG, "   Error body: $errorBody")

                // ✅ FIX 7: Jika error 400 expire, retry dengan auth baru
                if (response.code() == 400 && errorBody?.contains("expire") == true) {
                    Log.w(TAG, "⚠️ Expire error detected, retrying with new auth...")
                    if (retryCount < 2) {
                        delay(1000)
                        return uploadProfilePicture(imageUri, userId, retryCount + 1)
                    }
                }

                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload error: ${e.message}", e)
            null
        }
    }

    /**
     * ✅ Delete profile picture from ImageKit (via backend)
     */
    suspend fun deleteProfilePicture(fileId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ Deleting file from ImageKit via backend...")
                Log.d(TAG, "File ID: $fileId")

                // Backend delete endpoint
                val deleteUrl = AUTH_BACKEND_URL.replace("/imagekit-auth", "/imagekit-delete")

                // Create request body
                val jsonBody = JSONObject().apply {
                    put("fileId", fileId)
                }

                // Create delete request
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBody = jsonBody.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(deleteUrl)
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Sending delete request to: $deleteUrl")
                Log.d(TAG, "Request body: $jsonBody")

                // Execute request
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Log.d(TAG, "Delete response code: ${response.code}")
                Log.d(TAG, "Delete response body: $responseBody")

                val success = response.isSuccessful

                if (success) {
                    Log.d(TAG, "✅ File deleted successfully!")
                } else {
                    Log.e(TAG, "❌ Delete failed: ${response.message}")
                }

                response.close()
                success

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting file: ${e.message}", e)
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Compress image before upload
     */
    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "❌ Cannot open input stream")
                return null
            }

            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e(TAG, "❌ Failed to decode bitmap")
                return null
            }

            bitmap = fixImageOrientation(uri, bitmap)

            val maxWidth = ImageKitConfig.MAX_WIDTH
            val maxHeight = ImageKitConfig.MAX_HEIGHT

            if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                val scaleFactor = minOf(
                    maxWidth.toFloat() / bitmap.width,
                    maxHeight.toFloat() / bitmap.height
                )

                val newWidth = (bitmap.width * scaleFactor).toInt()
                val newHeight = (bitmap.height * scaleFactor).toInt()

                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                ImageKitConfig.COMPRESSION_QUALITY,
                outputStream
            )

            val compressedData = outputStream.toByteArray()
            outputStream.close()

            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(tempFile)
            fos.write(compressedData)
            fos.close()

            bitmap.recycle()
            tempFile

        } catch (e: Exception) {
            Log.e(TAG, "❌ Compression error: ${e.message}", e)
            null
        }
    }

    /**
     * Fix image orientation based on EXIF data
     */
    private fun fixImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (!matrix.isIdentity) {
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                bitmap.recycle()
                return rotatedBitmap
            }

            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fix orientation: ${e.message}")
            bitmap
        }
    }

    /**
     * Validate image MIME type
     */
    fun isValidImageType(mimeType: String?): Boolean {
        return mimeType in ImageKitConfig.ALLOWED_MIME_TYPES
    }

    /**
     * Validate file size
     */
    fun isValidFileSize(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available() ?: 0
            inputStream?.close()
            size <= ImageKitConfig.MAX_FILE_SIZE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Data class untuk authentication parameters
     */
    data class AuthParams(
        val token: String,
        val expire: String,
        val signature: String,
        val publicKey: String
    )
}