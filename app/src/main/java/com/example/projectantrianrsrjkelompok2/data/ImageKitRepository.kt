package com.example.projectantrianrsrjkelompok2.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import com.example.projectantrianrsrjkelompok2.api.ImageKitApiService
import com.example.projectantrianrsrjkelompok2.model.ImageKitUploadResponse
import com.example.projectantrianrsrjkelompok2.utils.ImageKitConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * ✅ ImageKit Repository (Fixed Authentication)
 */
class ImageKitRepository(private val context: Context) {

    private val TAG = "ImageKitRepository"

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
                        Log.d(TAG, "Method: ${request.method}")
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

    suspend fun uploadProfilePicture(
        imageUri: Uri,
        userId: String
    ): ImageKitUploadResponse? {
        return try {
            Log.d(TAG, "📤 Starting image upload for user: $userId")

            // Step 1: Compress image
            Log.d(TAG, "🔄 Compressing image...")
            val compressedFile = compressImage(imageUri)
            if (compressedFile == null) {
                Log.e(TAG, "❌ Failed to compress image")
                return null
            }

            Log.d(TAG, "📦 Image compressed: ${compressedFile.length() / 1024}KB")

            // Step 2: Prepare upload parameters
            val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val folder = ImageKitConfig.PROFILE_FOLDER

            Log.d(TAG, "📝 File name: $fileName")
            Log.d(TAG, "📂 Folder: $folder")

            // Step 3: Create multipart request
            val filePart = MultipartBody.Part.createFormData(
                "file",
                fileName,
                compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )

            val fileNamePart = fileName.toRequestBody("text/plain".toMediaTypeOrNull())
            val folderPart = folder.toRequestBody("text/plain".toMediaTypeOrNull())
            val publicKeyPart = ImageKitConfig.PUBLIC_KEY.toRequestBody("text/plain".toMediaTypeOrNull())
            val useUniqueFileNamePart = "true".toRequestBody("text/plain".toMediaTypeOrNull())
            val tagsPart = "profile,user,$userId".toRequestBody("text/plain".toMediaTypeOrNull())

            // Step 4: Upload to ImageKit
            Log.d(TAG, "☁️ Uploading to ImageKit...")
            Log.d(TAG, "Public Key: ${ImageKitConfig.PUBLIC_KEY}")

            val response = apiService.uploadImage(
                file = filePart,
                fileName = fileNamePart,
                folder = folderPart,
                publicKey = publicKeyPart,
                useUniqueFileName = useUniqueFileNamePart,
                tags = tagsPart
            )

            // Step 5: Clean up temp file
            compressedFile.delete()

            Log.d(TAG, "📡 Response received: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val uploadResponse = response.body()!!
                Log.d(TAG, "✅ Upload successful!")
                Log.d(TAG, "   - File ID: ${uploadResponse.fileId}")
                Log.d(TAG, "   - URL: ${uploadResponse.url}")
                Log.d(TAG, "   - Size: ${uploadResponse.size / 1024}KB")

                uploadResponse
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Upload failed: ${response.code()} - ${response.message()}")
                Log.e(TAG, "   Error body: $errorBody")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload error: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

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

            // Fix orientation
            bitmap = fixImageOrientation(uri, bitmap)

            // Resize if too large
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
                Log.d(TAG, "📏 Resized to: ${newWidth}x${newHeight}")
            }

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                ImageKitConfig.COMPRESSION_QUALITY,
                outputStream
            )

            val compressedData = outputStream.toByteArray()
            outputStream.close()

            // Save to temp file
            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(tempFile)
            fos.write(compressedData)
            fos.close()

            bitmap.recycle()

            Log.d(TAG, "✅ Compression complete: ${tempFile.length() / 1024}KB")
            tempFile

        } catch (e: Exception) {
            Log.e(TAG, "❌ Compression error: ${e.message}", e)
            null
        }
    }

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
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
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

    fun isValidImageType(mimeType: String?): Boolean {
        return mimeType in ImageKitConfig.ALLOWED_MIME_TYPES
    }

    fun isValidFileSize(uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available() ?: 0
            inputStream?.close()

            val isValid = size <= ImageKitConfig.MAX_FILE_SIZE

            if (!isValid) {
                Log.w(TAG, "File too large: ${size / 1024 / 1024}MB (max: ${ImageKitConfig.MAX_FILE_SIZE / 1024 / 1024}MB)")
            }

            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file size: ${e.message}")
            false
        }
    }
}