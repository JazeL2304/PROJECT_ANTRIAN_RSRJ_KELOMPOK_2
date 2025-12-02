package com.example.projectantrianrsrjkelompok2.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ✅ Machine Learning Model untuk Prediksi Waktu Tunggu Antrian
 *
 * Model ini memprediksi waktu tunggu berdasarkan:
 * - Jumlah pasien di depan
 * - Waktu saat ini (jam dalam hari)
 * - Hari dalam minggu
 * - Spesialisasi dokter
 * - Rata-rata waktu layanan
 */
class QueuePredictionModel(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val TAG = "QueuePredictionModel"

    companion object {
        private const val MODEL_FILENAME = "prediksi_antrian_rumahsakit.tflite"

        // Input features size (sesuaikan dengan model Anda)
        private const val INPUT_SIZE = 5

        // Specialization encoding
        private val SPECIALIZATION_MAP = mapOf(
            "Dokter Umum" to 0,
            "Dokter Gigi" to 1,
            "Dokter Mata" to 2,
            "Dokter Anak" to 3,
            "Dokter Jantung" to 4,
            "Dokter Kandungan" to 5,
            "Layanan Umum" to 0,
            "Layanan Gigi" to 1,
            "Layanan Mata" to 2,
            "Layanan Anak" to 3,
            "Layanan Jantung" to 4,
            "Layanan Kandungan" to 5
        )
    }

    init {
        try {
            loadModel()
            Log.d(TAG, "✅ ML Model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load ML model: ${e.message}", e)
        }
    }

    /**
     * Load TensorFlow Lite model
     */
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Model initialized with input size: $INPUT_SIZE")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            throw e
        }
    }

    /**
     * Load model file from assets
     */
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * ✅ Prediksi waktu tunggu (dalam menit)
     *
     * @param patientsAhead Jumlah pasien di depan
     * @param currentHour Jam saat ini (0-23)
     * @param dayOfWeek Hari dalam minggu (1=Senin, 7=Minggu)
     * @param specialization Spesialisasi dokter
     * @param avgServiceTime Rata-rata waktu layanan (menit)
     * @return Prediksi waktu tunggu dalam menit
     */
    fun predictWaitTime(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int,
        specialization: String,
        avgServiceTime: Float = 10f
    ): Float {
        try {
            // Jika model tidak tersedia, gunakan heuristic
            if (interpreter == null) {
                return calculateHeuristicWaitTime(patientsAhead, avgServiceTime)
            }

            // Prepare input data
            val inputData = prepareInputData(
                patientsAhead,
                currentHour,
                dayOfWeek,
                specialization,
                avgServiceTime
            )

            // Prepare output buffer
            val outputData = Array(1) { FloatArray(1) }

            // Run inference
            interpreter?.run(inputData, outputData)

            val predictedTime = outputData[0][0]

            Log.d(TAG, """
                ✅ ML Prediction:
                - Patients ahead: $patientsAhead
                - Current hour: $currentHour
                - Day of week: $dayOfWeek
                - Specialization: $specialization
                - Predicted wait time: $predictedTime minutes
            """.trimIndent())

            return predictedTime.coerceAtLeast(0f)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Prediction error: ${e.message}", e)
            // Fallback to heuristic
            return calculateHeuristicWaitTime(patientsAhead, avgServiceTime)
        }
    }

    /**
     * Prepare input data untuk model
     */
    private fun prepareInputData(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int,
        specialization: String,
        avgServiceTime: Float
    ): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4) // 4 bytes per float
        inputBuffer.order(ByteOrder.nativeOrder())

        // Feature 1: Patients ahead (normalized 0-1)
        inputBuffer.putFloat(patientsAhead.toFloat() / 50f)

        // Feature 2: Current hour (normalized 0-1)
        inputBuffer.putFloat(currentHour.toFloat() / 24f)

        // Feature 3: Day of week (normalized 0-1)
        inputBuffer.putFloat(dayOfWeek.toFloat() / 7f)

        // Feature 4: Specialization (encoded)
        val specCode = SPECIALIZATION_MAP[specialization] ?: 0
        inputBuffer.putFloat(specCode.toFloat() / 5f)

        // Feature 5: Average service time (normalized 0-1)
        inputBuffer.putFloat(avgServiceTime / 30f)

        inputBuffer.rewind()
        return inputBuffer
    }

    /**
     * ✅ Heuristic calculation (fallback jika model tidak tersedia)
     */
    private fun calculateHeuristicWaitTime(
        patientsAhead: Int,
        avgServiceTime: Float
    ): Float {
        // Base calculation
        var waitTime = patientsAhead * avgServiceTime

        // Add peak hour multiplier
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val peakHourMultiplier = when (currentHour) {
            in 8..10 -> 1.3f  // Peak morning
            in 13..15 -> 1.2f // Peak afternoon
            else -> 1.0f
        }

        waitTime *= peakHourMultiplier

        return waitTime
    }

    /**
     * ✅ Prediksi waktu tunggu dengan confidence level
     */
    fun predictWithConfidence(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int,
        specialization: String,
        avgServiceTime: Float = 10f
    ): PredictionResult {
        val predictedTime = predictWaitTime(
            patientsAhead,
            currentHour,
            dayOfWeek,
            specialization,
            avgServiceTime
        )

        // Calculate confidence based on input quality
        val confidence = calculateConfidence(patientsAhead, currentHour, dayOfWeek)

        // Calculate time range
        val variance = predictedTime * 0.15f // 15% variance
        val minTime = (predictedTime - variance).coerceAtLeast(0f)
        val maxTime = predictedTime + variance

        return PredictionResult(
            predictedMinutes = predictedTime,
            minMinutes = minTime,
            maxMinutes = maxTime,
            confidence = confidence
        )
    }

    /**
     * Calculate prediction confidence
     */
    private fun calculateConfidence(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int
    ): Float {
        var confidence = 1.0f

        // Reduce confidence for extreme values
        if (patientsAhead > 30) confidence *= 0.8f
        if (patientsAhead < 3) confidence *= 0.9f

        // Reduce confidence for off-hours
        if (currentHour < 7 || currentHour > 18) confidence *= 0.85f

        // Reduce confidence for weekends
        if (dayOfWeek in 6..7) confidence *= 0.9f

        return confidence.coerceIn(0.5f, 1.0f)
    }

    /**
     * Get recommended action based on wait time
     */
    fun getRecommendation(waitTimeMinutes: Float): String {
        return when {
            waitTimeMinutes < 10 -> "🟢 Segera dipanggil! Harap bersiap di ruang tunggu."
            waitTimeMinutes < 30 -> "🟡 Waktu tunggu sedang. Anda bisa tetap di area klinik."
            waitTimeMinutes < 60 -> "🟠 Waktu tunggu cukup lama. Anda bisa keluar sebentar, tapi jangan jauh."
            else -> "🔴 Waktu tunggu sangat lama. Pertimbangkan reschedule atau datang lebih awal."
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        try {
            interpreter?.close()
            Log.d(TAG, "✅ Model resources released")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error closing model: ${e.message}", e)
        }
    }

    /**
     * Data class untuk hasil prediksi
     */
    data class PredictionResult(
        val predictedMinutes: Float,
        val minMinutes: Float,
        val maxMinutes: Float,
        val confidence: Float
    ) {
        fun getFormattedPrediction(): String {
            val hours = (predictedMinutes / 60).toInt()
            val minutes = (predictedMinutes % 60).toInt()

            return when {
                hours > 0 -> "$hours jam $minutes menit"
                else -> "$minutes menit"
            }
        }

        fun getFormattedRange(): String {
            val minH = (minMinutes / 60).toInt()
            val minM = (minMinutes % 60).toInt()
            val maxH = (maxMinutes / 60).toInt()
            val maxM = (maxMinutes % 60).toInt()

            return when {
                minH > 0 || maxH > 0 -> {
                    val minStr = if (minH > 0) "$minH jam $minM menit" else "$minM menit"
                    val maxStr = if (maxH > 0) "$maxH jam $maxM menit" else "$maxM menit"
                    "$minStr - $maxStr"
                }
                else -> "$minM - $maxM menit"
            }
        }

        fun getConfidenceLevel(): String {
            return when {
                confidence >= 0.9f -> "Sangat Akurat"
                confidence >= 0.75f -> "Akurat"
                confidence >= 0.6f -> "Cukup Akurat"
                else -> "Estimasi"
            }
        }
    }
}