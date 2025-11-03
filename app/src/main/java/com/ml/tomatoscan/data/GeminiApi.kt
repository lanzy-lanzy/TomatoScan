package com.ml.tomatoscan.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.ml.tomatoscan.utils.ImagePreprocessor
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

class GeminiApi(context: Context) {
    
    private val analysisCache = AnalysisCache(context)

    companion object {
        private const val API_KEY = "AIzaSyBD15s-m0ClELhAR7XbbVPRkSFlQzcu_fQ"

        private val generativeModel by lazy {
            try {
                GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = API_KEY,
                    generationConfig = generationConfig {
                        temperature = 0.1f  // Low temperature for consistent, deterministic results
                        topK = 1
                        topP = 0.8f
                    }
                )
            } catch (e: Exception) {
                Log.e("GeminiApi", "Failed to initialize Gemini model", e)
                null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun analyzeTomatoLeaf(bitmap: Bitmap): TomatoAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first for consistent results
                val cachedResult = analysisCache.getCachedResult(bitmap)
                if (cachedResult != null) {
                    Log.d("GeminiApi", "Returning cached result for consistent analysis")
                    return@withContext cachedResult
                }
                
                // Preprocess image for consistent analysis
                val preprocessedBitmap = ImagePreprocessor.preprocessForAnalysis(bitmap)
                Log.d("GeminiApi", "Image preprocessed: ${preprocessedBitmap.width}x${preprocessedBitmap.height}")
                
                val prompt = """
                    You are an expert agricultural plant pathologist with specialized training in tomato diseases.
                    Your task is to accurately identify tomato diseases from images with high consistency and precision.
                    Be conservative and precise in your diagnosis. Prioritize known symptoms and visual evidence.
                    Always provide the same diagnosis for the same visual symptoms to ensure consistency.
                    
                    Analyze this image with precision and consistency.
                    
                    STEP 1: STRICTLY VERIFY THIS IS A TOMATO LEAF (CRITICAL STEP)
                    
                    You MUST be extremely strict in identifying tomato leaves. Look for these SPECIFIC tomato leaf characteristics:
                    
                    TOMATO LEAF IDENTIFICATION FEATURES (ALL must be present):
                    - Compound leaves with 5-9 leaflets arranged alternately
                    - Leaflets have serrated (toothed) edges with irregular, pointed teeth
                    - Distinctive strong tomato plant smell (if fresh)
                    - Leaflets are oval to lance-shaped
                    - Terminal leaflet at the tip
                    - Slightly hairy or fuzzy texture on stems and leaves
                    - Medium to dark green color (unless diseased)
                    
                    REJECT if you see these NON-TOMATO characteristics:
                    - Simple leaves (not compound) - REJECT
                    - Smooth edges without serrations - REJECT
                    - Potato leaves (which have larger, more rounded leaflets) - REJECT
                    - Pepper leaves (which are simpler and more elongated) - REJECT
                    - Any other plant species - REJECT
                    - Unclear or blurry images where leaf structure cannot be confirmed - REJECT
                    - Images of fruits, flowers, or stems without clear leaf structure - REJECT
                    
                    BE EXTREMELY CONSERVATIVE: If you have ANY doubt about whether this is a tomato leaf, or if the leaf structure is not clearly visible, you MUST reject it.

                    If the image is NOT a tomato leaf, is unclear, or you cannot confidently confirm it's a tomato leaf, respond ONLY with the following JSON structure:
                    {
                        "diseaseDetected": "Not a Tomato Leaf",
                        "confidence": 100.0,
                        "severity": "Unknown",
                        "description": "This doesn't appear to be a tomato leaf. Please capture a clear photo of a tomato plant leaf for accurate disease analysis. Tomato leaves are compound with 5-9 serrated leaflets.",
                        "recommendations": ["Take a photo of an actual tomato leaf with visible compound structure", "Ensure the leaf fills most of the frame", "Use good lighting to show leaf details clearly", "Make sure leaflets and serrated edges are visible"],
                        "treatmentOptions": [],
                        "preventionMeasures": []
                    }

                    STEP 2: Identify the disease with precision
                    ONLY proceed to this step if you are 100% certain the image shows a tomato leaf with the characteristics listed above.
                    
                    If the image IS DEFINITELY a tomato leaf (confirmed by compound structure with serrated leaflets), carefully examine the visual symptoms and identify the most probable disease.
                    
                    Look for these KEY VISUAL EVIDENCE for each disease:
                    
                    1. Early Blight (Alternaria solani):
                       - Concentric ring patterns forming "target spots" or "bull's eye" lesions
                       - Brown to black spots with defined rings
                       - Usually starts on lower, older leaves
                       - Lesions may have yellow halos
                    
                    2. Late Blight (Phytophthora infestans):
                       - Water-soaked, greasy-looking lesions
                       - White fuzzy mold growth on undersides (in humid conditions)
                       - Irregular brown or black blotches
                       - Rapid spreading, can affect entire leaf quickly
                    
                    3. Mosaic Virus:
                       - Mottled yellow and green patterns on leaves
                       - Leaf distortion, curling, or malformation
                       - Stunted growth patterns
                       - No distinct spots or lesions
                    
                    4. Septoria Leaf Spot:
                       - Small circular spots (2-3mm diameter)
                       - Gray or tan centers with dark brown borders
                       - Tiny black dots (pycnidia) visible in spot centers
                       - Numerous spots covering the leaf
                    
                    5. Bacterial Leaf Spot:
                       - Small, dark brown to black spots
                       - Yellow halos around spots
                       - Spots may appear greasy or water-soaked
                       - Angular or irregular shaped lesions
                    
                    IMPORTANT: This app is trained and specialized ONLY on these 5 diseases.
                    Be conservative - only diagnose one of these 5 if the visual evidence clearly matches the symptoms. 
                    
                    If you detect any OTHER disease or condition (such as Fusarium Wilt, Powdery Mildew, Anthracnose, Leaf Curl, nutrient deficiencies, etc.), respond with:
                    {
                        "diseaseDetected": "Disease Not Supported",
                        "confidence": 0.0,
                        "severity": "Unknown",
                        "description": "This app is specialized in detecting only 5 specific tomato diseases: Early Blight, Late Blight, Mosaic Virus, Septoria Leaf Spot, and Bacterial Leaf Spot. The symptoms shown do not match these diseases or the leaf appears to have a different condition.",
                        "recommendations": ["Consult with a local agricultural expert for other diseases", "This app focuses on the 5 most common tomato leaf diseases", "Ensure the leaf shows clear symptoms if you suspect one of the supported diseases"],
                        "treatmentOptions": ["Seek professional agricultural advice for accurate diagnosis"],
                        "preventionMeasures": ["Regular monitoring of plant health", "Proper plant spacing and air circulation", "Maintain good garden hygiene"]
                    }
                    
                    STEP 3: Provide comprehensive assessment
                    If the leaf is healthy or shows one of the 5 supported diseases, provide:
                    1. Disease identification - Be precise and identify the MOST PROBABLE disease based on visual evidence
                    2. Disease stage assessment - Determine if symptoms indicate early, middle, or late stage
                    3. Severity level (Mild, Moderate, Severe, or Healthy)
                    4. Confidence in your diagnosis (0-100%) - Base this on clarity of symptoms
                    5. Detailed description - List the KEY VISUAL EVIDENCE you observed that led to your diagnosis
                    6. Treatment recommendations - Specific to the identified disease
                    7. Prevention measures - Specific to the identified disease
                    
                    Respond ONLY in valid JSON format with this exact structure:
                    {
                        "diseaseDetected": "Disease name or 'Healthy' or 'Disease Not Supported'",
                        "confidence": 85.5,
                        "severity": "Mild/Moderate/Severe/Healthy/Unknown",
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
                    image(preprocessedBitmap)
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

                val result = TomatoAnalysisResult(
                    diseaseDetected = diseaseDetected,
                    confidence = confidence,
                    severity = severity,
                    description = description,
                    recommendations = recommendations,
                    treatmentOptions = treatmentOptions,
                    preventionMeasures = preventionMeasures
                )
                
                // Cache the result for future consistency
                analysisCache.cacheResult(bitmap, result)
                Log.d("GeminiApi", "Analysis result cached for consistency")
                
                result

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
    @RequiresApi(Build.VERSION_CODES.O)
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
