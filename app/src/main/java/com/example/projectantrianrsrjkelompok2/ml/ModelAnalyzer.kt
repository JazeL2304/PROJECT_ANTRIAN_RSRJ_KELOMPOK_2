package com.example.projectantrianrsrjkelompok2.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * ✅ Model Analyzer - Inspect TFLite Model Structure
 * Gunakan ini untuk debug dan lihat struktur model Anda
 */
object ModelAnalyzer {

    private const val TAG = "ModelAnalyzer"

    /**
     * ✅ Analyze model structure
     */
    fun analyzeModel(context: Context, modelFileName: String = "prediksi_antrian_rumahsakit.tflite") {
        try {
            Log.d(TAG, "=" .repeat(60))
            Log.d(TAG, "🔍 ANALYZING MODEL: $modelFileName")
            Log.d(TAG, "=" .repeat(60))

            val modelBuffer = loadModelFile(context, modelFileName)
            val interpreter = Interpreter(modelBuffer)

            // Get input details
            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            val inputDataType = inputTensor.dataType()

            Log.d(TAG, "\n📥 INPUT TENSOR:")
            Log.d(TAG, "  Shape: ${inputShape.contentToString()}")
            Log.d(TAG, "  Data Type: $inputDataType")
            Log.d(TAG, "  Size: ${inputShape.reduce { acc, i -> acc * i }} elements")

            // Get output details
            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputDataType = outputTensor.dataType()

            Log.d(TAG, "\n📤 OUTPUT TENSOR:")
            Log.d(TAG, "  Shape: ${outputShape.contentToString()}")
            Log.d(TAG, "  Data Type: $outputDataType")
            Log.d(TAG, "  Size: ${outputShape.reduce { acc, i -> acc * i }} elements")

            // Model metadata
            Log.d(TAG, "\n📊 MODEL INFO:")
            Log.d(TAG, "  Input Count: ${interpreter.inputTensorCount}")
            Log.d(TAG, "  Output Count: ${interpreter.outputTensorCount}")

            // Infer input features
            val inputSize = inputShape.getOrNull(1) ?: inputShape.reduce { acc, i -> acc * i }
            Log.d(TAG, "\n🎯 INFERENCE:")
            Log.d(TAG, "  Expected input features: $inputSize")

            when (inputSize) {
                5 -> Log.d(TAG, "  Likely: [patients_ahead, hour, day, specialization, avg_service_time]")
                6 -> Log.d(TAG, "  Likely: [patients_ahead, hour, day, specialization, avg_service_time, queue_number]")
                7 -> Log.d(TAG, "  Likely: [patients_ahead, hour, day, specialization, avg_service_time, queue_number, month]")
                else -> Log.d(TAG, "  Custom model with $inputSize features")
            }

            Log.d(TAG, "\n" + "=" .repeat(60))
            Log.d(TAG, "✅ ANALYSIS COMPLETE")
            Log.d(TAG, "=" .repeat(60))

            interpreter.close()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error analyzing model: ${e.message}", e)
        }
    }

    /**
     * ✅ Test model with sample data
     */
    fun testModel(
        context: Context,
        modelFileName: String = "prediksi_antrian_rumahsakit.tflite",
        inputSize: Int = 5
    ) {
        try {
            Log.d(TAG, "\n🧪 TESTING MODEL WITH SAMPLE DATA")
            Log.d(TAG, "=" .repeat(60))

            val modelBuffer = loadModelFile(context, modelFileName)
            val interpreter = Interpreter(modelBuffer)

            // Prepare test cases
            val testCases = listOf(
                TestCase(
                    name = "Morning Peak - Dokter Umum",
                    patients = 10,
                    hour = 9,
                    day = 2, // Tuesday
                    specialization = 0,
                    avgTime = 10f
                ),
                TestCase(
                    name = "Afternoon - Dokter Gigi",
                    patients = 5,
                    hour = 14,
                    day = 3, // Wednesday
                    specialization = 1,
                    avgTime = 15f
                ),
                TestCase(
                    name = "Evening - Dokter Anak",
                    patients = 15,
                    hour = 17,
                    day = 5, // Friday
                    specialization = 3,
                    avgTime = 8f
                )
            )

            testCases.forEach { test ->
                val result = runInference(interpreter, test, inputSize)

                Log.d(TAG, "\n📊 Test: ${test.name}")
                Log.d(TAG, "  Input:")
                Log.d(TAG, "    - Patients ahead: ${test.patients}")
                Log.d(TAG, "    - Hour: ${test.hour}")
                Log.d(TAG, "    - Day: ${test.day}")
                Log.d(TAG, "    - Specialization: ${test.specialization}")
                Log.d(TAG, "    - Avg service time: ${test.avgTime} min")
                Log.d(TAG, "  Output:")
                Log.d(TAG, "    - Predicted wait time: ${result.predictedTime} minutes")
                Log.d(TAG, "    - Formatted: ${formatTime(result.predictedTime)}")
            }

            Log.d(TAG, "\n" + "=" .repeat(60))
            Log.d(TAG, "✅ TEST COMPLETE")
            Log.d(TAG, "=" .repeat(60))

            interpreter.close()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error testing model: ${e.message}", e)
        }
    }

    /**
     * Run inference
     */
    private fun runInference(
        interpreter: Interpreter,
        test: TestCase,
        inputSize: Int
    ): InferenceResult {
        // Prepare input
        val inputArray = when (inputSize) {
            5 -> floatArrayOf(
                test.patients / 50f,
                test.hour / 24f,
                test.day / 7f,
                test.specialization / 5f,
                test.avgTime / 30f
            )
            6 -> floatArrayOf(
                test.patients / 50f,
                test.hour / 24f,
                test.day / 7f,
                test.specialization / 5f,
                test.avgTime / 30f,
                test.patients / 100f // queue number normalized
            )
            else -> FloatArray(inputSize) { 0f }
        }

        val input = Array(1) { inputArray }
        val output = Array(1) { FloatArray(1) }

        interpreter.run(input, output)

        return InferenceResult(
            predictedTime = output[0][0],
            rawOutput = output[0][0]
        )
    }

    /**
     * Load model file
     */
    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Format time
     */
    private fun formatTime(minutes: Float): String {
        val hours = (minutes / 60).toInt()
        val mins = (minutes % 60).toInt()
        return when {
            hours > 0 -> "$hours jam $mins menit"
            else -> "$mins menit"
        }
    }

    /**
     * Data classes
     */
    data class TestCase(
        val name: String,
        val patients: Float,
        val hour: Float,
        val day: Float,
        val specialization: Float,
        val avgTime: Float
    )

    data class InferenceResult(
        val predictedTime: Float,
        val rawOutput: Float
    )
}