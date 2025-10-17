package com.example.projectantrianrsrjkelompok2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.utils.PreferencesHelper
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraFragment : Fragment() {

    private lateinit var ivPreview: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnRetake: Button
    private lateinit var btnConfirm: Button
    private lateinit var tvInstruction: TextView
    private lateinit var preferencesHelper: PreferencesHelper

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            displayPhoto()
        } else {
            Toast.makeText(requireContext(), "Foto dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Izin kamera diperlukan untuk mengambil foto",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesHelper = PreferencesHelper(requireContext())
        initViews(view)
        setupButtons()
    }

    private fun initViews(view: View) {
        ivPreview = view.findViewById(R.id.iv_photo_preview)
        btnTakePhoto = view.findViewById(R.id.btn_take_photo)
        btnRetake = view.findViewById(R.id.btn_retake)
        btnConfirm = view.findViewById(R.id.btn_confirm)
        tvInstruction = view.findViewById(R.id.tv_instruction)
    }

    private fun setupButtons() {
        btnTakePhoto.setOnClickListener {
            checkCameraPermission()
        }

        btnRetake.setOnClickListener {
            resetCamera()
        }

        btnConfirm.setOnClickListener {
            confirmPhoto()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                ex.printStackTrace()
                Toast.makeText(requireContext(), "Error creating file: ${ex.message}", Toast.LENGTH_LONG).show()
                null
            }

            photoFile?.also {
                try {
                    photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        it
                    )

                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    takePictureLauncher.launch(takePictureIntent)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error launching camera: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error opening camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "PROFILE_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun displayPhoto() {
        currentPhotoPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            val rotatedBitmap = rotateImageIfRequired(bitmap, path)

            ivPreview.setImageBitmap(rotatedBitmap)
            ivPreview.visibility = View.VISIBLE
            tvInstruction.text = "✅ Foto berhasil diambil!\nPeriksa kembali foto Anda."

            btnTakePhoto.visibility = View.GONE
            btnRetake.visibility = View.VISIBLE
            btnConfirm.visibility = View.VISIBLE
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, path: String): Bitmap {
        val ei = ExifInterface(path)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    private fun resetCamera() {
        ivPreview.setImageDrawable(null)
        ivPreview.visibility = View.GONE
        tvInstruction.text = "📸 Ambil foto profil Anda\n\nPastikan foto jelas dan wajah terlihat"

        btnTakePhoto.visibility = View.VISIBLE
        btnRetake.visibility = View.GONE
        btnConfirm.visibility = View.GONE

        currentPhotoPath = null
        photoUri = null
    }

    // ✅ FIXED: Simpan path foto ke SharedPreferences
    private fun confirmPhoto() {
        currentPhotoPath?.let { path ->
            // Simpan path foto
            preferencesHelper.saveProfilePhotoPath(path)

            Toast.makeText(
                requireContext(),
                "✅ Foto profil berhasil disimpan!",
                Toast.LENGTH_LONG
            ).show()

            (activity as MainActivity).navigateToFragment(ProfileFragment())
        } ?: run {
            Toast.makeText(
                requireContext(),
                "❌ Error: Foto tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
