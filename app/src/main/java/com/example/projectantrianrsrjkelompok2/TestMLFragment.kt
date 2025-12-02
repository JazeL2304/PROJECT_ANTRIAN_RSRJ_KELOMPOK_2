package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projectantrianrsrjkelompok2.ml.EnhancedQueuePredictionModel
import com.example.projectantrianrsrjkelompok2.ml.ModelAnalyzer
import java.util.*

/**
 * ✅ Test Fragment untuk ML Model
 * Gunakan untuk debug dan test model Anda
 */
class TestMLFragment : Fragment() {

    private lateinit var tvResults: TextView
    private lateinit var btnAnalyze: Button
    private lateinit var btnTest: Button
    private lateinit var btnTestRealData: Button
    private lateinit var scrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Title
        val title = TextView(requireContext()).apply {
            text = "🧪 ML Model Test Console"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        layout.addView(title)

        // Analyze Button
        btnAnalyze = Button(requireContext()).apply {
            text = "🔍 Analyze Model Structure"
            setOnClickListener { analyzeModel() }
        }
        layout.addView(btnAnalyze)

        // Test Button
        btnTest = Button(requireContext()).apply {
            text = "🧪 Test with Sample Data"
            setOnClickListener { testModel() }
        }
        layout.addView(btnTest)

        // Test Real Data Button
        btnTestRealData = Button(requireContext()).apply {
            text = "📊 Test with Real Queue Data"
            setOnClickListener { testRealData() }
        }
        layout.addView(btnTestRealData)

        // ScrollView with Results
        scrollView = ScrollView(requireContext())
        tvResults = TextView(requireContext()).apply {
            text = "Press a button to run tests..."
            textSize = 12f
            setTypeface(android.graphics.Typeface.MONOSPACE)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }
        scrollView.addView(tvResults)

        val scrollParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0
        ).apply {
            weight = 1f
            topMargin = 16
        }
        scrollView.layoutParams = scrollParams
        layout.addView(scrollView)

        return layout
    }

    /**
     * ✅ Analyze model structure
     */
    private fun analyzeModel() {
        tvResults.text = "🔍 Analyzing model...\n\n"

        try {
            // Run analyzer
            ModelAnalyzer.analyzeModel(requireContext())

            // Get logs (displayed in Logcat)
            tvResults.append("✅ Analysis complete!\n")
            tvResults.append("Check Logcat for detailed results.\n\n")

            tvResults.append("Model file: prediksi_antrian_rumahsakit.tflite\n")
            tvResults.append("Location: app/src/main/assets/\n\n")

            tvResults.append("To view full analysis:\n")
            tvResults.append("1. Open Logcat in Android Studio\n")
            tvResults.append("2. Filter by 'ModelAnalyzer'\n")
            tvResults.append("3. Look for detailed tensor information\n")

        } catch (e: Exception) {
            tvResults.append("❌ Error: ${e.message}\n")
            tvResults.append("\n${e.stackTraceToString()}")
        }

        scrollToBottom()
    }

    /**
     * ✅ Test model with sample data
     */
    private fun testModel() {
        tvResults.text = "🧪 Testing model with sample data...\n\n"

        try {
            val model = EnhancedQueuePredictionModel(requireContext())
            val modelInfo = model.getModelInfo()

            tvResults.append("📊 Model Info:\n")
            tvResults.append("  File: ${modelInfo.fileName}\n")
            tvResults.append("  Input size: ${modelInfo.inputSize} features\n")
            tvResults.append("  Requires batch: ${modelInfo.requiresBatch}\n")
            tvResults.append("  Loaded: ${modelInfo.isLoaded}\n\n")

            tvResults.append("=" .repeat(50) + "\n\n")

            // Test cases
            val testCases = listOf(
                TestCase("Morning Peak - Dokter Umum", 10, 9, 2, "Dokter Umum", 10f),
                TestCase("Afternoon - Dokter Gigi", 5, 14, 3, "Dokter Gigi", 15f),
                TestCase("Evening - Dokter Anak", 15, 17, 5, "Dokter Anak", 8f),
                TestCase("Low Load - Dokter Mata", 2, 11, 4, "Dokter Mata", 12f),
                TestCase("High Load - Dokter Jantung", 25, 10, 1, "Dokter Jantung", 15f)
            )

            testCases.forEachIndexed { index, test ->
                tvResults.append("Test ${index + 1}: ${test.name}\n")
                tvResults.append("─".repeat(50) + "\n")
                tvResults.append("Input:\n")
                tvResults.append("  • Patients ahead: ${test.patients}\n")
                tvResults.append("  • Hour: ${test.hour}:00\n")
                tvResults.append("  • Day: ${getDayName(test.day)}\n")
                tvResults.append("  • Specialization: ${test.specialization}\n")
                tvResults.append("  • Avg service time: ${test.avgTime} min\n\n")

                val prediction = model.predictWithConfidence(
                    patientsAhead = test.patients,
                    currentHour = test.hour,
                    dayOfWeek = test.day,
                    specialization = test.specialization,
                    avgServiceTime = test.avgTime,
                    queueNumber = test.patients + 5
                )

                tvResults.append("Output:\n")
                tvResults.append("  • Predicted: ${prediction.getFormattedPrediction()}\n")
                tvResults.append("  • Range: ${prediction.getFormattedRange()}\n")
                tvResults.append("  • Confidence: ${prediction.getConfidenceLevel()}\n")
                tvResults.append("  • Source: ${if (prediction.isMLPrediction) "ML Model" else "Heuristic"}\n")

                val recommendation = model.getRecommendation(prediction.predictedMinutes)
                tvResults.append("  • Recommendation: $recommendation\n")

                tvResults.append("\n")
            }

            tvResults.append("=" .repeat(50) + "\n")
            tvResults.append("✅ All tests completed!\n")

            model.close()

        } catch (e: Exception) {
            tvResults.append("❌ Error: ${e.message}\n")
            tvResults.append("\n${e.stackTraceToString()}")
        }

        scrollToBottom()
    }

    /**
     * ✅ Test with real queue data
     */
    private fun testRealData() {
        tvResults.text = "📊 Testing with real queue data...\n\n"

        try {
            val model = EnhancedQueuePredictionModel(requireContext())

            // Get active booking
            val activeBooking = DataSource.getActiveBooking()

            if (activeBooking == null) {
                tvResults.append("⚠️ No active booking found!\n")
                tvResults.append("Please create a booking first.\n")
                return
            }

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            tvResults.append("Current Queue Info:\n")
            tvResults.append("─".repeat(50) + "\n")
            tvResults.append("  • Patient: ${activeBooking.patientName}\n")
            tvResults.append("  • Queue #: ${activeBooking.queueNumber}\n")
            tvResults.append("  • Doctor: ${activeBooking.doctorName}\n")
            tvResults.append("  • Specialization: ${activeBooking.specialization}\n")
            tvResults.append("  • Appointment: ${activeBooking.time}\n")
            tvResults.append("  • Current time: ${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}\n")
            tvResults.append("  • Day: ${getDayName(dayOfWeek)}\n\n")

            // Get current queue number
            val todayBookings = DataSource.getBookingHistory()
                .filter { it.date == activeBooking.date }
                .sortedBy { it.queueNumber }

            val currentQueue = todayBookings
                .filter { it.status == BookingStatus.CALLED }
                .minByOrNull { it.queueNumber }
                ?.queueNumber ?: 1

            val patientsAhead = activeBooking.queueNumber - currentQueue

            tvResults.append("Queue Status:\n")
            tvResults.append("─".repeat(50) + "\n")
            tvResults.append("  • Current queue: $currentQueue\n")
            tvResults.append("  • Your queue: ${activeBooking.queueNumber}\n")
            tvResults.append("  • Patients ahead: $patientsAhead\n\n")

            if (patientsAhead <= 0) {
                tvResults.append("🎉 It's your turn!\n")
                return
            }

            // Run prediction
            tvResults.append("Running ML Prediction...\n\n")

            val prediction = model.predictWithConfidence(
                patientsAhead = patientsAhead,
                currentHour = currentHour,
                dayOfWeek = dayOfWeek,
                specialization = activeBooking.specialization,
                avgServiceTime = 10f,
                queueNumber = activeBooking.queueNumber
            )

            tvResults.append("ML Prediction Results:\n")
            tvResults.append("─".repeat(50) + "\n")
            tvResults.append("  • Estimated wait: ${prediction.getFormattedPrediction()}\n")
            tvResults.append("  • Time range: ${prediction.getFormattedRange()}\n")
            tvResults.append("  • Confidence: ${prediction.getConfidenceLevel()}\n")
            tvResults.append("  • Source: ${if (prediction.isMLPrediction) "ML Model ✅" else "Heuristic 📊"}\n\n")

            val recommendation = model.getRecommendation(prediction.predictedMinutes)
            tvResults.append("Recommendation:\n")
            tvResults.append("  $recommendation\n\n")

            // Calculate expected call time
            val expectedMinutes = calendar.get(Calendar.MINUTE) + prediction.predictedMinutes.toInt()
            val expectedHour = calendar.get(Calendar.HOUR_OF_DAY) + (expectedMinutes / 60)
            val finalMinute = expectedMinutes % 60

            tvResults.append("Expected Call Time:\n")
            tvResults.append("  Around ${expectedHour}:${finalMinute.toString().padStart(2, '0')}\n")

            tvResults.append("\n" + "=" .repeat(50) + "\n")
            tvResults.append("✅ Test completed!\n")

            model.close()

        } catch (e: Exception) {
            tvResults.append("❌ Error: ${e.message}\n")
            tvResults.append("\n${e.stackTraceToString()}")
        }

        scrollToBottom()
    }

    /**
     * Helper functions
     */
    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "Sunday"
            2 -> "Monday"
            3 -> "Tuesday"
            4 -> "Wednesday"
            5 -> "Thursday"
            6 -> "Friday"
            7 -> "Saturday"
            else -> "Unknown"
        }
    }

    private fun scrollToBottom() {
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    /**
     * Data class
     */
    data class TestCase(
        val name: String,
        val patients: Int,
        val hour: Int,
        val day: Int,
        val specialization: String,
        val avgTime: Float
    )
}