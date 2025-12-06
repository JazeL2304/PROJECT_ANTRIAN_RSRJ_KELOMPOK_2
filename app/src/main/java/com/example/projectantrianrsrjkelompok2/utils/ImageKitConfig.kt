package com.example.projectantrianrsrjkelompok2.utils

/**
 * ✅ ImageKit Configuration
 * Free tier: 20GB bandwidth + storage monthly
 *
 * Setup:
 * 1. Login https://imagekit.io/
 * 2. Dashboard → Settings → URL Endpoints → Copy URL
 * 3. Dashboard → Developer Options → API Keys → Copy Public Key
 * 4. Paste di bawah ini
 */
object ImageKitConfig {

    // ===============================
    // 🔑 GANTI DENGAN CREDENTIALS KAMU!
    // ===============================

    /**
     * URL Endpoint dari ImageKit
     * Lokasi: Dashboard → Settings → URL Endpoints
     * Format: https://ik.imagekit.io/your_imagekit_id
     */
    const val URL_ENDPOINT = "https://ik.imagekit.io/tzurunaviv"  // ✅ SUDAH BENAR (remove trailing /)

    /**
     * Public Key dari ImageKit
     * Lokasi: Dashboard → Developer Options → API Keys
     * Format: public_xxxxxxxxxxxxx
     */
    const val PUBLIC_KEY = "public_nP26P9XnstuImz5AWTQmvxaX80k="  // ✅ SUDAH BENAR

    // ===============================
    // ⚙️ CONFIGURATION
    // ===============================

    /**
     * Folder untuk menyimpan foto profil
     */
    const val PROFILE_FOLDER = "profile_pictures"

    /**
     * Max file size (5MB)
     */
    const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB in bytes

    /**
     * Allowed file types
     */
    val ALLOWED_MIME_TYPES = listOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    /**
     * Image compression quality (0-100)
     */
    const val COMPRESSION_QUALITY = 80

    /**
     * Max image dimensions
     */
    const val MAX_WIDTH = 1024
    const val MAX_HEIGHT = 1024

    // ===============================
    // ✅ VALIDATION
    // ===============================

    /**
     * Check if ImageKit is configured properly
     * Returns true if both URL_ENDPOINT and PUBLIC_KEY are set
     */
    fun isConfigured(): Boolean {
        // More lenient validation
        val hasEndpoint = URL_ENDPOINT.isNotBlank() &&
                URL_ENDPOINT.startsWith("https://ik.imagekit.io/")

        val hasPublicKey = PUBLIC_KEY.isNotBlank() &&
                PUBLIC_KEY.startsWith("public_")

        return hasEndpoint && hasPublicKey
    }

    /**
     * Get clean URL endpoint (remove trailing slash if exists)
     */
    fun getCleanEndpoint(): String {
        return URL_ENDPOINT.trimEnd('/')
    }

    /**
     * Validate configuration and return error message if invalid
     */
    fun validateConfiguration(): String? {
        return when {
            URL_ENDPOINT.isBlank() -> "URL Endpoint tidak boleh kosong"
            !URL_ENDPOINT.startsWith("https://ik.imagekit.io/") -> "URL Endpoint format salah"
            PUBLIC_KEY.isBlank() -> "Public Key tidak boleh kosong"
            !PUBLIC_KEY.startsWith("public_") -> "Public Key format salah"
            else -> null // Valid
        }
    }
}