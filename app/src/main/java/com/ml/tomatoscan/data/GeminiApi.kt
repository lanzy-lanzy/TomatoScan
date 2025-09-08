package com.ml.tomatoscan.data

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TomatoAnalysisResult(
    val diseaseDetected: String,
    val confidence: Float,
    val severity: String,
    val description: String,
    val recommendations: List<String>,
    val treatmentOptions: List<String>,
    val preventionMeasures: List<String>
)

class GeminiApi {

    companion object {
        private const val API_KEY = "AIzaSyBD15s-m0ClELhAR7XbbVPRkSFlQzcu_fQ"

        private val generativeModel by lazy {
            try {
                GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = API_KEY
                )
            } catch (e: Exception) {
                Log.e("GeminiApi", "Failed to initialize Gemini model", e)
                null
            }
        }
    }

    suspend fun analyzeTomatoLeaf(bitmap: Bitmap): TomatoAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    You are an expert agricultural pathologist specializing in tomato plant diseases. 
                    Your first task is to determine if the uploaded image is a tomato leaf. 

                    If the image is NOT a tomato leaf or is unclear, respond ONLY with the following JSON structure:
                    {
                        "diseaseDetected": "Invalid Image",
                        "confidence": 100.0,
                        "severity": "Unknown",
                        "description": "The uploaded image does not appear to be a tomato leaf. Please upload a clear image of a tomato leaf for analysis.",
                        "recommendations": [],
                        "treatmentOptions": [],
                        "preventionMeasures": []
                    }

                    If the image IS a tomato leaf, analyze it and provide a comprehensive disease assessment.
                    Please examine the leaf for:
                    1. Disease identification (if any)
                    2. Severity level (Mild, Moderate, Severe, or Healthy)
                    3. Confidence in your diagnosis (0-100%)
                    4. Detailed description of what you observe
                    5. Treatment recommendations
                    6. Prevention measures
                    
                    Common tomato diseases to look for:
                    - Early Blight (Alternaria solani)
                    - Late Blight (Phytophthora infestans)
                    - Septoria Leaf Spot
                    - Bacterial Spot
                    - Fusarium Wilt
                    - Mosaic Virus
                    - Powdery Mildew
                    - Anthracnose
                    - Leaf Curl
                    - Nutrient Deficiencies
                    
                    Respond ONLY in valid JSON format with this exact structure:
                    {
                        "diseaseDetected": "Disease name or 'Healthy'",
                        "confidence": 85.5,
                        "severity": "Mild/Moderate/Severe/Healthy",
                        "description": "Detailed description of observations",
                        "recommendations": [
                            "Immediate action 1",
                            "Immediate action 2",
                            "Immediate action 3"
                        ],
                        "treatmentOptions": [
                            "Treatment option 1",
                            "Treatment option 2",
                            "Treatment option 3"
                        ],
                        "preventionMeasures": [
                            "Prevention measure 1",
                            "Prevention measure 2",
                            "Prevention measure 3"
                        ]
                    }
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                if (generativeModel == null) {
                    throw Exception("Gemini API not properly configured")
                }
                
                Log.d("GeminiApi", "Sending request to Gemini API...")
                val response = generativeModel!!.generateContent(inputContent)
                val responseText = response.text ?: "{}"
                Log.d("GeminiApi", "Received response: $responseText")

                // Clean the response text to ensure it's valid JSON
                val cleanedResponse = cleanJsonResponse(responseText)
                val jsonResponse = JSONObject(cleanedResponse)

                // Parse the response
                val diseaseDetected = jsonResponse.optString("diseaseDetected", "Unknown")
                val confidence = jsonResponse.optDouble("confidence", 0.0).toFloat()
                val severity = jsonResponse.optString("severity", "Unknown")
                val description = jsonResponse.optString("description", "No description available")
                
                val recommendations = parseJsonArray(jsonResponse.optJSONArray("recommendations"))
                val treatmentOptions = parseJsonArray(jsonResponse.optJSONArray("treatmentOptions"))
                val preventionMeasures = parseJsonArray(jsonResponse.optJSONArray("preventionMeasures"))

                TomatoAnalysisResult(
                    diseaseDetected = diseaseDetected,
                    confidence = confidence,
                    severity = severity,
                    description = description,
                    recommendations = recommendations,
                    treatmentOptions = treatmentOptions,
                    preventionMeasures = preventionMeasures
                )

            } catch (e: Exception) {
                Log.e("GeminiApi", "Error analyzing tomato leaf", e)
                // Return a fallback result
                TomatoAnalysisResult(
                    diseaseDetected = "Analysis Error",
                    confidence = 0f,
                    severity = "Unknown",
                    description = "Unable to analyze the image: ${e.message}",
                    recommendations = listOf("Please try again with a clearer image", "Ensure good lighting conditions"),
                    treatmentOptions = listOf("Consult with a local agricultural expert"),
                    preventionMeasures = listOf("Regular monitoring", "Proper plant spacing", "Good air circulation")
                )
            }
        }
    }

    private fun cleanJsonResponse(response: String): String {
        // Remove any markdown formatting or extra text
        var cleaned = response.trim()
        
        // Find the JSON part (between first { and last })
        val firstBrace = cleaned.indexOf('{')
        val lastBrace = cleaned.lastIndexOf('}')
        
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1)
        }
        
        return cleaned
    }

    private fun parseJsonArray(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.optString(i, ""))
        }
        return list.filter { it.isNotBlank() }
    }

    // Legacy method for backward compatibility - converts new result to old format
    suspend fun analyzeImage(bitmap: Bitmap): Pair<String, Float> {
        val result = analyzeTomatoLeaf(bitmap)
        val quality = when {
            result.diseaseDetected.equals("Healthy", ignoreCase = true) -> "Excellent"
            result.severity.equals("Mild", ignoreCase = true) -> "Good"
            result.severity.equals("Moderate", ignoreCase = true) -> "Fair"
            else -> "Poor"
        }
        return Pair(quality, result.confidence)
    }
}
