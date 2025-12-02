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
 * ✅ Enhanced Queue Prediction Model
 * Support untuk model "prediksi_antrian_rumahsakit.tflite"
 * Dengan auto-detection input shape
 */
class EnhancedQueuePredictionModel(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val TAG = "EnhancedQueueModel"
    private var inputSize: Int = 5
    private var requiresBatch: Boolean = true

    companion object {
        private const val MODEL_FILENAME = "prediksi_antrian_rumahsakit.tflite"

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
            detectModelStructure()
            Log.d(TAG, "✅ Model loaded successfully")
            Log.d(TAG, "   Input size: $inputSize features")
            Log.d(TAG, "   Requires batch: $requiresBatch")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load model: ${e.message}", e)
        }
    }

    /**
     * ✅ Load model dari assets
     */
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
            Log.d(TAG, "Model interpreter created")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            throw e
        }
    }

    /**
     * ✅ Detect model structure otomatis
     */
    private fun detectModelStructure() {
        try {
            val inputTensor = interpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape()

            if (inputShape != null) {
                Log.d(TAG, "Input shape: ${inputShape.contentToString()}")

                // Detect if model requires batch dimension
                requiresBatch = inputShape.size >= 2

                // Get input size (number of features)
                inputSize = if (requiresBatch) {
                    inputShape[1] // [batch_size, features]
                } else {
                    inputShape[0] // [features]
                }

                Log.d(TAG, "✅ Model structure detected:")
                Log.d(TAG, "   Shape: ${inputShape.contentToString()}")
                Log.d(TAG, "   Features: $inputSize")
                Log.d(TAG, "   Batch required: $requiresBatch")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error detecting structure: ${e.message}", e)
            // Use defaults
            inputSize = 5
            requiresBatch = true
        }
    }

    /**
     * ✅ Load model file dari assets
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
     * ✅ Prediksi waktu tunggu - MAIN FUNCTION
     */
    fun predictWaitTime(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int,
        specialization: String,
        avgServiceTime: Float = 10f,
        queueNumber: Int = 0
    ): Float {
        try {
            if (interpreter == null) {
                Log.w(TAG, "Interpreter not available, using heuristic")
                return calculateHeuristicWaitTime(patientsAhead, avgServiceTime)
            }

            // Prepare input berdasarkan detected input size
            val inputArray = prepareInputArray(
                patientsAhead,
                currentHour,
                dayOfWeek,
                specialization,
                avgServiceTime,
                queueNumber
            )

            // Run inference
            val output = if (requiresBatch) {
                // Model expects: [batch_size, features]
                val input = Array(1) { inputArray }
                val output = Array(1) { FloatArray(1) }
                interpreter?.run(input, output)
                output[0][0]
            } else {
                // Model expects: [features]
                val input = Array(1) { inputArray }
                val output = FloatArray(1)
                interpreter?.run(input, output)
                output[0]
            }

            val predictedTime = output.coerceAtLeast(0f)

            Log.d(TAG, """
                ✅ ML Prediction:
                - Patients ahead: $patientsAhead
                - Hour: $currentHour
                - Day: $dayOfWeek
                - Specialization: $specialization
                - Avg service: ${avgServiceTime}min
                - Queue #: $queueNumber
                → Predicted: ${predictedTime.toInt()} minutes
            """.trimIndent())

            return predictedTime

        } catch (e: Exception) {
            Log.e(TAG, "❌ Prediction error: ${e.message}", e)
            return calculateHeuristicWaitTime(patientsAhead, avgServiceTime)
        }
    }

    /**
     * ✅ Prepare input array berdasarkan input size
     */
    private fun prepareInputArray(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int,
        specialization: String,
        avgServiceTime: Float,
        queueNumber: Int
    ): FloatArray {
        val specCode = SPECIALIZATION_MAP[specialization] ?: 0

        return when (inputSize) {
            3 -> floatArrayOf(
                patientsAhead / 50f,
                currentHour / 24f,
                dayOfWeek / 7f
            )
            4 -> floatArrayOf(
                patientsAhead / 50f,
                currentHour / 24f,
                dayOfWeek / 7f,
                specCode / 5f
            )
            5 -> floatArrayOf(
                patientsAhead / 50f,
                currentHour / 24f,
                dayOfWeek / 7f,
                specCode / 5f,
                avgServiceTime / 30f
            )
            6 -> floatArrayOf(
                patientsAhead / 50f,
                currentHour / 24f,
                dayOfWeek / 7f,
                specCode / 5f,
                avgServiceTime / 30f,
                queueNumber / 100f
            )
            7 -> floatArrayOf(
                patientsAhead / 50f,
                currentHour / 24f,
                dayOfWeek / 7f,
                specCode / 5f,
                avgServiceTime / 30f,
                queueNumber / 100f,
                java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) / 12f
            )
            else -> {
                // Fallback: fill with zeros for extra features
                FloatArray(inputSize) { idx ->
                    when (idx) {
                        0 -> patientsAhead / 50f
                        1 -> currentHour / 24f
                        2 -> dayOfWeek / 7f
                        3 -> specCode / 5f
                        4 -> avgServiceTime / 30f
                        else -> 0f
                    }
                }
            }
        }
    }

    /**
     * ✅ Prediksi dengan confidence
     */
    fun predictWithConfidence(
        patientsAhead: Int,
        currentHour: Int,
        dayOfWeek: Int,
        specialization: String,
        avgServiceTime: Float = 10f,
        queueNumber: Int = 0
    ): PredictionResult {
        val predictedTime = predictWaitTime(
            patientsAhead,
            currentHour,
            dayOfWeek,
            specialization,
            avgServiceTime,
            queueNumber
        )

        val confidence = calculateConfidence(patientsAhead, currentHour, dayOfWeek)
        val variance = predictedTime * 0.15f
        val minTime = (predictedTime - variance).coerceAtLeast(0f)
        val maxTime = predictedTime + variance

        return PredictionResult(
            predictedMinutes = predictedTime,
            minMinutes = minTime,
            maxMinutes = maxTime,
            confidence = confidence,
            isMLPrediction = interpreter != null
        )
    }

    /**
     * ✅ Calculate confidence
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
     * ✅ Heuristic fallback
     */
    private fun calculateHeuristicWaitTime(
        patientsAhead: Int,
        avgServiceTime: Float
    ): Float {
        var waitTime = patientsAhead * avgServiceTime

        val currentHour = java.util.Calendar.getInstance()
            .get(java.util.Calendar.HOUR_OF_DAY)

        val peakHourMultiplier = when (currentHour) {
            in 8..10 -> 1.3f
            in 13..15 -> 1.2f
            else -> 1.0f
        }

        waitTime *= peakHourMultiplier
        return waitTime
    }

    /**
     * ✅ Get recommendation
     */
    fun getRecommendation(waitTimeMinutes: Float): String {
        return when {
            waitTimeMinutes < 10 -> "🟢 Segera dipanggil! Harap bersiap di ruang tunggu."
            waitTimeMinutes < 30 -> "🟡 Waktu tunggu sedang. Anda bisa tetap di area klinik."
            waitTimeMinutes < 60 -> "🟠 Waktu tunggu cukup lama. Anda bisa keluar sebentar."
            else -> "🔴 Waktu tunggu sangat lama. Pertimbangkan reschedule."
        }
    }

    /**
     * ✅ Cleanup
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
     * ✅ Get model info
     */
    fun getModelInfo(): ModelInfo {
        return ModelInfo(
            fileName = MODEL_FILENAME,
            inputSize = inputSize,
            requiresBatch = requiresBatch,
            isLoaded = interpreter != null
        )
    }

    /**
     * Data classes
     */
    data class PredictionResult(
        val predictedMinutes: Float,
        val minMinutes: Float,
        val maxMinutes: Float,
        val confidence: Float,
        val isMLPrediction: Boolean = true
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
                !isMLPrediction -> "Estimasi Standar"
                confidence >= 0.9f -> "Sangat Akurat"
                confidence >= 0.75f -> "Akurat"
                confidence >= 0.6f -> "Cukup Akurat"
                else -> "Estimasi"
            }
        }
    }

    data class ModelInfo(
        val fileName: String,
        val inputSize: Int,
        val requiresBatch: Boolean,
        val isLoaded: Boolean
    )
}