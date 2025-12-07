package com.example.projectantrianrsrjkelompok2

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.projectantrianrsrjkelompok2.data.FirebaseRepository
import com.example.projectantrianrsrjkelompok2.data.ImageKitRepository
import com.example.projectantrianrsrjkelompok2.utils.ImageKitConfig
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.projectantrianrsrjkelompok2.utils.PasswordHasher

/**
 * ✅ Profile Fragment with ImageKit Integration
 * - Upload foto profil ke ImageKit.io cloud storage
 * - Delete foto profil dari ImageKit & Firebase
 */
class ProfileFragment : Fragment() {

    private val TAG = "ProfileFragment"

    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var firebaseRepo: FirebaseRepository
    private lateinit var imageKitRepo: ImageKitRepository

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnEditName: View
    private lateinit var btnEditPassword: View
    private lateinit var btnVerifyIdentity: View
    private lateinit var btnLogout: View

    private var progressBar: ProgressBar? = null

    private var selectedImageUri: Uri? = null
    private var isUploading = false

    // ✅ Temp file untuk camera
    private var tempCameraFile: File? = null

    // Image picker launcher (untuk galeri)
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "========== Image Picker Result ==========")
        Log.d(TAG, "Result code: ${result.resultCode}")
        Log.d(TAG, "RESULT_OK: ${Activity.RESULT_OK}")
        Log.d(TAG, "Match: ${result.resultCode == Activity.RESULT_OK}")

        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            Log.d(TAG, "Data: ${result.data}")
            Log.d(TAG, "URI: $uri")

            if (uri != null) {
                Log.d(TAG, "✅ Valid URI received, handling...")
                handleImageSelected(uri)
            } else {
                Log.e(TAG, "❌ URI is NULL!")
            }
        } else {
            Log.w(TAG, "⚠️ Result not OK. Code: ${result.resultCode}")
        }
        Log.d(TAG, "=========================================")
    }

    // ✅ Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        Log.d(TAG, "========== Camera Result ==========")
        Log.d(TAG, "Success: $success")

        if (success && tempCameraFile != null) {
            val uri = Uri.fromFile(tempCameraFile)
            Log.d(TAG, "✅ Photo captured: $uri")
            handleImageSelected(uri)
        } else {
            Log.e(TAG, "❌ Camera failed or file is null")
            Toast.makeText(context, "❌ Gagal mengambil foto", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "===================================")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize
        preferencesHelper = PreferencesHelper(requireContext())
        firebaseRepo = FirebaseRepository()
        imageKitRepo = ImageKitRepository(requireContext())

        // DEBUG SECTION
        Log.d(TAG, "========== ProfileFragment DEBUG ==========")
        val userId = preferencesHelper.getUserId()
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "User Email: ${preferencesHelper.getUserEmail()}")
        Log.d(TAG, "ImageKit URL: ${ImageKitConfig.URL_ENDPOINT}")
        Log.d(TAG, "ImageKit Public Key: ${ImageKitConfig.PUBLIC_KEY}")
        Log.d(TAG, "ImageKit isConfigured: ${ImageKitConfig.isConfigured()}")
        Log.d(TAG, "==========================================")

        // Check ImageKit configuration
        if (!ImageKitConfig.isConfigured()) {
            Log.w(TAG, "⚠️ ImageKit not configured! Please set credentials in ImageKitConfig.kt")
            Toast.makeText(
                context,
                "⚠️ ImageKit belum dikonfigurasi. Foto profil tidak bisa diupload.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d(TAG, "✅ ImageKit configured and ready!")
        }

        // Initialize views
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnEditName = view.findViewById(R.id.btnEditName)
        btnEditPassword = view.findViewById(R.id.btnEditPassword)
        btnVerifyIdentity = view.findViewById(R.id.btnVerifyIdentity)
        btnLogout = view.findViewById(R.id.btnLogout)

        progressBar = view.findViewById(R.id.progressBar)

        // Load user data
        loadUserData()

        // Load profile photo from ImageKit/Firebase
        loadProfilePhoto()

        // Setup click listeners
        setupClickListeners()
    }

    private fun loadUserData() {
        val userRole = preferencesHelper.getUserRole()

        val userName = if (userRole == "dokter") {
            "Dr. Ahmad Santoso"
        } else {
            preferencesHelper.getUserFullName() ?: "User"
        }

        val userEmail = if (userRole == "dokter") {
            "dokter@rumahsakit.com"
        } else {
            preferencesHelper.getUserEmail() ?: "email@example.com"
        }

        tvUserName.text = userName
        tvUserEmail.text = userEmail
    }

    private fun loadProfilePhoto() {
        val userId = preferencesHelper.getUserId()

        if (userId.isNullOrEmpty()) {
            Log.w(TAG, "❌ User ID is null or empty")
            loadProfilePhotoFromLocal()
            return
        }

        Log.d(TAG, "📥 Loading profile photo for user: $userId")

        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    firebaseRepo.getUserById(userId)
                }

                if (user != null && user.hasProfilePicture()) {
                    // Load from ImageKit URL
                    ivProfilePhoto.load(user.getProfileImageUrl(width = 200, height = 200)) {
                        crossfade(true)
                        placeholder(android.R.drawable.ic_menu_myplaces)
                        error(android.R.drawable.ic_menu_myplaces)
                        transformations(CircleCropTransformation())
                    }

                    Log.d(TAG, "✅ Profile photo loaded from ImageKit: ${user.profileImageUrl}")
                } else {
                    Log.d(TAG, "ℹ️ User has no profile picture, loading from local")
                    loadProfilePhotoFromLocal()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading profile photo: ${e.message}", e)
                loadProfilePhotoFromLocal()
            }
        }
    }

    private fun loadProfilePhotoFromLocal() {
        val photoPath = preferencesHelper.getProfilePhotoPath()

        if (photoPath != null && File(photoPath).exists()) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            ivProfilePhoto.setImageBitmap(bitmap)
            Log.d(TAG, "📁 Profile photo loaded from local: $photoPath")
        } else {
            ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_myplaces)
            Log.d(TAG, "🖼️ Using default profile icon")
        }
    }

    private fun setupClickListeners() {
        // Click profile photo to change
        ivProfilePhoto.setOnClickListener {
            Log.d(TAG, "📸 Profile photo clicked!")
            Log.d(TAG, "isUploading: $isUploading")
            Log.d(TAG, "ImageKit configured: ${ImageKitConfig.isConfigured()}")

            if (!isUploading) {
                showImagePickerDialog()
            } else {
                Toast.makeText(context, "⏳ Upload sedang berlangsung...", Toast.LENGTH_SHORT).show()
            }
        }

        // Edit Name
        btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        // Edit Password
        btnEditPassword.setOnClickListener {
            showEditPasswordDialog()
        }

        // Verify Identity
        btnVerifyIdentity.setOnClickListener {
            showImagePickerDialog()
        }

        // Logout
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showImagePickerDialog() {
        Log.d(TAG, "🎨 showImagePickerDialog() called")

        if (!ImageKitConfig.isConfigured()) {
            Log.e(TAG, "❌ ImageKit not configured!")
            Toast.makeText(
                context,
                "ImageKit belum dikonfigurasi. Silakan set credentials di ImageKitConfig.kt",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Log.d(TAG, "✅ ImageKit configured, showing dialog")

        // ✅ Check if user has profile picture
        val userId = preferencesHelper.getUserId()
        var hasProfilePicture = false

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                userId?.let { firebaseRepo.getUserById(it) }
            }
            hasProfilePicture = user?.hasProfilePicture() == true

            // Show dialog with or without delete option
            val options = if (hasProfilePicture) {
                arrayOf("📷 Ambil Foto", "🖼️ Pilih dari Galeri", "🗑️ Hapus Foto", "❌ Batal")
            } else {
                arrayOf("📷 Ambil Foto", "🖼️ Pilih dari Galeri", "❌ Batal")
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ubah Foto Profil")
                .setItems(options) { dialog, which ->
                    Log.d(TAG, "Dialog option selected: $which")

                    if (hasProfilePicture) {
                        // Menu dengan opsi hapus
                        when (which) {
                            0 -> {
                                Log.d(TAG, "Opening camera...")
                                openCamera()
                            }
                            1 -> {
                                Log.d(TAG, "Opening gallery...")
                                openGallery()
                            }
                            2 -> {
                                Log.d(TAG, "Delete photo...")
                                showDeleteConfirmationDialog()
                            }
                            3 -> {
                                Log.d(TAG, "Dialog cancelled")
                                dialog.dismiss()
                            }
                        }
                    } else {
                        // Menu tanpa opsi hapus
                        when (which) {
                            0 -> {
                                Log.d(TAG, "Opening camera...")
                                openCamera()
                            }
                            1 -> {
                                Log.d(TAG, "Opening gallery...")
                                openGallery()
                            }
                            2 -> {
                                Log.d(TAG, "Dialog cancelled")
                                dialog.dismiss()
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun openCamera() {
        Log.d(TAG, "📷 openCamera() called")

        try {
            // Create temp file for camera
            tempCameraFile = File(
                requireContext().cacheDir,
                "camera_${System.currentTimeMillis()}.jpg"
            )

            // ✅ Use FileProvider to get content URI
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempCameraFile!!
            )

            Log.d(TAG, "Temp file created: ${tempCameraFile?.absolutePath}")
            Log.d(TAG, "Launching camera with content URI: $uri")

            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error opening camera: ${e.message}", e)
            Toast.makeText(
                context,
                "❌ Gagal membuka kamera: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openGallery() {
        Log.d(TAG, "📂 openGallery() called")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        Log.d(TAG, "Launching image picker with intent: ${intent.action}")
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        Log.d(TAG, "========== Handle Image Selected ==========")
        Log.d(TAG, "URI: $uri")

        selectedImageUri = uri

        // ✅ Get MIME type (handle both content:// and file:// URI)
        val mimeType = when {
            uri.scheme == "content" -> {
                requireContext().contentResolver.getType(uri)
            }
            uri.scheme == "file" -> {
                // ✅ For file:// URI, detect from extension
                val extension = uri.path?.substringAfterLast('.', "")?.lowercase()
                when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    else -> "image/jpeg" // default
                }
            }
            else -> null
        }

        Log.d(TAG, "MIME type: $mimeType")

        // Validate file type
        if (!imageKitRepo.isValidImageType(mimeType)) {
            Log.e(TAG, "❌ Invalid MIME type!")
            Toast.makeText(
                context,
                "❌ Format tidak didukung. Gunakan JPEG, PNG, atau WEBP",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        Log.d(TAG, "✅ MIME type valid")

        // Validate file size
        if (!imageKitRepo.isValidFileSize(uri)) {
            Log.e(TAG, "❌ File too large!")
            val maxSizeMB = ImageKitConfig.MAX_FILE_SIZE / (1024 * 1024)
            Toast.makeText(
                context,
                "❌ Ukuran file terlalu besar. Maksimal ${maxSizeMB}MB",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        Log.d(TAG, "✅ File size valid")

        Log.d(TAG, "📸 Loading preview with Coil...")

        // Preview image
        ivProfilePhoto.load(uri) {
            crossfade(true)
            transformations(CircleCropTransformation())
        }

        Log.d(TAG, "✅ Preview loaded, showing confirmation dialog...")
        Log.d(TAG, "==========================================")

        // Confirm upload
        showUploadConfirmationDialog(uri)
    }

    private fun showUploadConfirmationDialog(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Upload Foto Profil")
            .setMessage("Upload foto ini ke cloud storage?")
            .setPositiveButton("Upload") { dialog, _ ->
                uploadProfilePicture(uri)
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                loadProfilePhoto()
                dialog.dismiss()
            }
            .show()
    }

    private fun uploadProfilePicture(uri: Uri) {
        Log.d(TAG, "========== Upload Profile Picture ==========")

        val userId = preferencesHelper.getUserId()
        Log.d(TAG, "User ID: $userId")

        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "❌ User ID is null or empty!")
            Toast.makeText(context, "❌ User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        if (isUploading) {
            Log.w(TAG, "⚠️ Already uploading!")
            Toast.makeText(context, "⏳ Upload sedang berlangsung...", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        showLoading(true)

        Log.d(TAG, "🔄 Starting upload to ImageKit...")
        Log.d(TAG, "URI: $uri")

        lifecycleScope.launch {
            try {
                // Step 1: Upload to ImageKit
                Log.d(TAG, "📤 Calling imageKitRepo.uploadProfilePicture()...")

                val uploadResponse = withContext(Dispatchers.IO) {
                    imageKitRepo.uploadProfilePicture(uri, userId)
                }

                Log.d(TAG, "📥 Upload response received")
                Log.d(TAG, "Response: $uploadResponse")

                if (uploadResponse != null) {
                    Log.d(TAG, "✅ Upload successful!")
                    Log.d(TAG, "   - File ID: ${uploadResponse.fileId}")
                    Log.d(TAG, "   - URL: ${uploadResponse.url}")
                    Log.d(TAG, "   - Size: ${uploadResponse.size / 1024}KB")

                    // Step 2: Save URL to Firebase
                    Log.d(TAG, "💾 Saving to Firebase...")

                    val success = withContext(Dispatchers.IO) {
                        firebaseRepo.updateUserProfileImage(
                            userId = userId,
                            imageUrl = uploadResponse.url,
                            fileId = uploadResponse.fileId
                        )
                    }

                    Log.d(TAG, "Firebase update result: $success")

                    if (success) {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "✅ Complete success!")
                            Toast.makeText(
                                context,
                                "✅ Foto profil berhasil diupload!",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadProfilePhoto()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Log.e(TAG, "❌ Firebase update failed!")
                            Toast.makeText(
                                context,
                                "❌ Gagal update database",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "❌ Upload response is NULL!")
                        Toast.makeText(
                            context,
                            "❌ Gagal upload gambar",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadProfilePhoto()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload error: ${e.message}", e)
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadProfilePhoto()
                }
            } finally {
                isUploading = false
                showLoading(false)
                Log.d(TAG, "==========================================")
            }
        }
    }

    // ✅ NEW: Show delete confirmation dialog
    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Foto Profil")
            .setMessage("Apakah Anda yakin ingin menghapus foto profil?")
            .setPositiveButton("Hapus") { dialog, _ ->
                deleteProfilePicture()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // ✅ NEW: Delete profile picture
    private fun deleteProfilePicture() {
        Log.d(TAG, "========== Delete Profile Picture ==========")

        val userId = preferencesHelper.getUserId()
        Log.d(TAG, "User ID: $userId")

        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "❌ User ID is null or empty!")
            Toast.makeText(context, "❌ User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        if (isUploading) {
            Log.w(TAG, "⚠️ Upload in progress!")
            Toast.makeText(context, "⏳ Tunggu proses upload selesai...", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        showLoading(true)

        Log.d(TAG, "🗑️ Starting delete process...")

        lifecycleScope.launch {
            try {
                // Step 1: Get user data to get fileId
                Log.d(TAG, "📥 Getting user data...")

                val user = withContext(Dispatchers.IO) {
                    firebaseRepo.getUserById(userId)
                }

                if (user == null) {
                    Log.e(TAG, "❌ User not found!")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ User tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val fileId = user.profileImageFileId // ✅ FIXED: Pakai profileImageFileId
                Log.d(TAG, "File ID: $fileId")

                if (fileId.isNullOrEmpty()) {
                    Log.w(TAG, "⚠️ No file ID found, skipping ImageKit delete")
                } else {
                    // Step 2: Delete from ImageKit
                    Log.d(TAG, "🗑️ Deleting from ImageKit...")

                    val imageKitSuccess = withContext(Dispatchers.IO) {
                        imageKitRepo.deleteProfilePicture(fileId)
                    }

                    Log.d(TAG, "ImageKit delete result: $imageKitSuccess")

                    if (!imageKitSuccess) {
                        Log.w(TAG, "⚠️ ImageKit delete failed, but continuing...")
                    }
                }

                // Step 3: Delete from Firebase
                Log.d(TAG, "🗑️ Deleting from Firebase...")

                val firebaseSuccess = withContext(Dispatchers.IO) {
                    firebaseRepo.deleteUserProfileImage(userId)
                }

                Log.d(TAG, "Firebase delete result: $firebaseSuccess")

                if (firebaseSuccess) {
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "✅ Delete complete!")
                        Toast.makeText(
                            context,
                            "✅ Foto profil berhasil dihapus!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reset to default image
                        ivProfilePhoto.setImageResource(android.R.drawable.ic_menu_myplaces)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "❌ Firebase delete failed!")
                        Toast.makeText(
                            context,
                            "❌ Gagal menghapus foto profil",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Delete error: ${e.message}", e)
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isUploading = false
                showLoading(false)
                Log.d(TAG, "==========================================")
            }
        }
    }

    // Ganti method showEditNameDialog dengan ini:
    private fun showEditNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_name, null)
        val etNewName = dialogView.findViewById<EditText>(R.id.etNewName)

        etNewName.setText(preferencesHelper.getUserFullName())

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Nama")
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val newName = etNewName.text.toString().trim()

                if (validateName(newName)) {
                    updateNameToFirebase(newName)
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 🆕 NEW: Update nama ke Firebase
    private fun updateNameToFirebase(newName: String) {
        val userId = preferencesHelper.getUserId()

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "❌ User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Updating name to Firebase...")
                Log.d(TAG, "User ID: $userId")
                Log.d(TAG, "New Name: $newName")

                // Get current user data
                val user = withContext(Dispatchers.IO) {
                    firebaseRepo.getUserById(userId)
                }

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ User tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Update user with new name
                val updatedUser = user.copy(
                    fullName = newName,
                    updatedAt = System.currentTimeMillis()
                )

                val success = withContext(Dispatchers.IO) {
                    firebaseRepo.updateUserAccount(updatedUser)
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        // Update local storage
                        preferencesHelper.saveUserFullName(newName)
                        tvUserName.text = newName

                        Toast.makeText(context, "✅ Nama berhasil diubah!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "✅ Name updated successfully!")
                    } else {
                        Toast.makeText(context, "❌ Gagal mengubah nama", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "❌ Failed to update name")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating name: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                showLoading(false)
            }
        }
    }
    private fun validateName(name: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (name.length < 3) {
            Toast.makeText(context, "Nama minimal 3 karakter", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    // Ganti method showEditPasswordDialog dengan ini:
    private fun showEditPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etOldPassword = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Ubah Password")
            .setView(dialogView)
            .setPositiveButton("Simpan") { dialog, _ ->
                val oldPassword = etOldPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (validatePasswordChange(oldPassword, newPassword, confirmPassword)) {
                    updatePasswordToFirebase(oldPassword, newPassword)
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // 🆕 NEW: Update password ke Firebase
    private fun updatePasswordToFirebase(oldPassword: String, newPassword: String) {
        val userId = preferencesHelper.getUserId()

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "❌ User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Updating password to Firebase...")
                Log.d(TAG, "User ID: $userId")

                // Step 1: Get current user data
                val user = withContext(Dispatchers.IO) {
                    firebaseRepo.getUserById(userId)
                }

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ User tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Step 2: Verify old password
                val isOldPasswordCorrect = if (PasswordHasher.isBCryptHash(user.password)) {
                    // Password is hashed, verify with BCrypt
                    PasswordHasher.verifyPassword(oldPassword, user.password)
                } else {
                    // Password is plain text (old data), compare directly
                    user.password == oldPassword
                }

                if (!isOldPasswordCorrect) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "❌ Password lama salah!", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "❌ Old password is incorrect")
                    }
                    return@launch
                }

                Log.d(TAG, "✅ Old password verified")

                // Step 3: Hash new password
                val hashedPassword = withContext(Dispatchers.IO) {
                    PasswordHasher.hashPassword(newPassword)
                }

                Log.d(TAG, "✅ New password hashed")

                // Step 4: Update user with new hashed password
                val updatedUser = user.copy(
                    password = hashedPassword,
                    updatedAt = System.currentTimeMillis()
                )

                val success = withContext(Dispatchers.IO) {
                    firebaseRepo.updateUserAccount(updatedUser)
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            context,
                            "✅ Password berhasil diubah!",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "✅ Password updated successfully!")
                    } else {
                        Toast.makeText(
                            context,
                            "❌ Gagal mengubah password",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "❌ Failed to update password")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating password: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validatePasswordChange(oldPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (oldPassword.isEmpty()) {
            Toast.makeText(context, "Password lama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(context, "Password baru tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.length < 6) {
            Toast.makeText(context, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(context, "Password baru tidak cocok", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        (activity as? MainActivity)?.logout()
        Toast.makeText(context, "Berhasil logout", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE

        btnEditName.isEnabled = !show
        btnEditPassword.isEnabled = !show
        btnVerifyIdentity.isEnabled = !show
        btnLogout.isEnabled = !show
    }

    override fun onResume() {
        super.onResume()
        loadProfilePhoto()
    }
}