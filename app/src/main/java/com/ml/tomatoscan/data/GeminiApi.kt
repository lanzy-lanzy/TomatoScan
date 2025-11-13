package com.ml.tomatoscan.data

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.ml.tomatoscan.ml.ClassificationResult
import com.ml.tomatoscan.models.DiagnosticReport
import com.ml.tomatoscan.models.DiseaseClass
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

class GeminiApi(private val context: Context) {
    
    private val analysisCache = AnalysisCache(context)

    companion object {
        private const val MODEL_VERSION = "1.0.0"

        private val generativeModel by lazy {
            try {
                val apiKey = com.ml.tomatoscan.config.GeminiConfig.API_KEY
                if (apiKey.isBlank()) {
                    Log.w("GeminiApi", "Gemini API key is not configured")
                    null
                } else {
                    GenerativeModel(
                        modelName = com.ml.tomatoscan.BuildConfig.GEMINI_MODEL_NAME,
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = com.ml.tomatoscan.config.GeminiConfig.TEMPERATURE
                            topK = com.ml.tomatoscan.config.GeminiConfig.TOP_K
                            topP = com.ml.tomatoscan.config.GeminiConfig.TOP_P
                        }
                    )
                }
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
                    
                    Analyze this tomato leaf image with precision and consistency.
                    
                    STEP 1: Verify this is a tomato leaf
                    First, determine if the uploaded image is a tomato leaf. 

                    If the image is NOT a tomato leaf or is unclear, respond ONLY with the following JSON structure:
                    {
                        "diseaseDetected": "Not a Tomato Leaf",
                        "confidence": 100.0,
                        "severity": "Unknown",
                        "description": "This doesn't appear to be a tomato leaf. Please capture a clear photo of a tomato plant leaf for accurate disease analysis.",
                        "recommendations": ["Take a photo of an actual tomato leaf", "Ensure the leaf fills most of the frame", "Use good lighting for better results"],
                        "treatmentOptions": [],
                        "preventionMeasures": []
                    }

                    STEP 2: Identify the disease with precision
                    If the image IS a tomato leaf, carefully examine the visual symptoms and identify the most probable disease.
                    
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

    /**
     * Generates a formal diagnostic report based on TFLite classification result.
     * Uses deterministic Gemini parameters to ensure consistent outputs.
     *
     * @param croppedLeaf The cropped leaf image
     * @param preliminaryResult The TFLite classification result
     * @return Formal diagnostic report
     */
    suspend fun generateDiagnosticReport(
        croppedLeaf: Bitmap,
        preliminaryResult: ClassificationResult
    ): DiagnosticReport {
        return withContext(Dispatchers.IO) {
            try {
                // Check if confidence is too low
                if (preliminaryResult.confidence < 0.5f) {
                    return@withContext createUncertainReport(
                        "Low classification confidence (${String.format("%.1f%%", preliminaryResult.confidence * 100)})"
                    )
                }

                // Create formal diagnostic report prompt
                val prompt = createFormalDiagnosticPrompt(preliminaryResult)

                val inputContent = content {
                    image(croppedLeaf)
                    text(prompt)
                }

                if (generativeModel == null) {
                    throw Exception("Gemini API not properly configured")
                }

                Log.d("GeminiApi", "Generating formal diagnostic report...")
                val response = generativeModel!!.generateContent(inputContent)
                val responseText = response.text ?: ""
                Log.d("GeminiApi", "Received diagnostic report: $responseText")

                // Parse and validate the report
                parseDiagnosticReport(responseText, preliminaryResult)

            } catch (e: Exception) {
                Log.e("GeminiApi", "Error generating diagnostic report", e)
                // Return fallback report based on TFLite prediction
                createFallbackReport(preliminaryResult)
            }
        }
    }

    /**
     * Creates the formal diagnostic report prompt template.
     * Includes role definition, TFLite context, and exact report structure.
     */
    private fun createFormalDiagnosticPrompt(preliminaryResult: ClassificationResult): String {
        val diseaseClass = preliminaryResult.diseaseClass
        val confidence = String.format("%.1f%%", preliminaryResult.confidence * 100)

        return """
            You are a plant pathology expert specializing in tomato leaf diseases.
            
            A TensorFlow Lite model has analyzed this tomato leaf image and predicted:
            - Disease: ${diseaseClass.displayName}
            - Confidence: $confidence
            
            Your task is to validate this prediction and generate a formal diagnostic report.
            
            REPORT STRUCTURE (3-5 sentences):
            1. Disease Identification: Start with "Based on the image analysis, the tomato leaf is identified as **[Disease Name]**."
            2. Observed Symptoms: Describe the specific visual symptoms you observe (e.g., lesion patterns, discoloration, texture).
            3. Confidence Assessment: State your confidence level in this diagnosis (e.g., "High confidence", "Moderate confidence").
            4. Management Recommendation: Provide specific management or treatment recommendations.
            
            FORMATTING REQUIREMENTS:
            - Use bold formatting (**Disease Name**) for the disease name
            - Write in formal, academic tone
            - Be concise: 3-5 sentences total
            - Do not use conversational language
            - Focus on observable evidence
            
            EXAMPLE FORMAT:
            "Based on the image analysis, the tomato leaf is identified as **Early Blight**. The leaf exhibits characteristic concentric ring patterns forming target-like lesions with dark brown coloration and yellow halos, primarily affecting the lower leaf sections. High confidence in this diagnosis based on the distinct symptom presentation. Immediate removal of affected leaves is recommended, followed by application of copper-based fungicides and improved air circulation around plants."
            
            Generate the formal diagnostic report now:
        """.trimIndent()
    }

    /**
     * Parses Gemini response into DiagnosticReport structure.
     * Validates that all required components are present.
     */
    private fun parseDiagnosticReport(
        responseText: String,
        preliminaryResult: ClassificationResult
    ): DiagnosticReport {
        try {
            val fullReport = responseText.trim()

            // Extract disease name (look for text between ** **)
            val diseaseNameRegex = """\*\*([^*]+)\*\*""".toRegex()
            val diseaseNameMatch = diseaseNameRegex.find(fullReport)
            val diseaseName = diseaseNameMatch?.groupValues?.get(1)?.trim()
                ?: preliminaryResult.diseaseClass.displayName

            // Split report into sentences
            val sentences = fullReport.split(". ").map { it.trim() }.filter { it.isNotEmpty() }

            // Extract components (heuristic approach)
            val observedSymptoms = extractObservedSymptoms(sentences)
            val confidenceLevel = extractConfidenceLevel(sentences)
            val managementRecommendation = extractManagementRecommendation(sentences)

            // Validate report has minimum required content
            if (fullReport.length < 50) {
                throw Exception("Report too short, using fallback")
            }

            return DiagnosticReport(
                diseaseName = diseaseName,
                observedSymptoms = observedSymptoms,
                confidenceLevel = confidenceLevel,
                managementRecommendation = managementRecommendation,
                fullReport = fullReport,
                isUncertain = false,
                timestamp = System.currentTimeMillis(),
                modelVersion = MODEL_VERSION
            )

        } catch (e: Exception) {
            Log.e("GeminiApi", "Error parsing diagnostic report, using fallback", e)
            return createFallbackReport(preliminaryResult)
        }
    }

    /**
     * Extracts observed symptoms from report sentences.
     */
    private fun extractObservedSymptoms(sentences: List<String>): String {
        // Look for sentences describing visual characteristics
        val symptomKeywords = listOf("exhibit", "show", "display", "observe", "lesion", "spot", "discolor", "pattern")
        val symptomSentence = sentences.find { sentence ->
            symptomKeywords.any { keyword -> sentence.contains(keyword, ignoreCase = true) }
        }
        return symptomSentence ?: "Visual symptoms consistent with the identified disease."
    }

    /**
     * Extracts confidence level from report sentences.
     */
    private fun extractConfidenceLevel(sentences: List<String>): String {
        // Look for sentences mentioning confidence
        val confidenceKeywords = listOf("confidence", "certain", "likely", "probable")
        val confidenceSentence = sentences.find { sentence ->
            confidenceKeywords.any { keyword -> sentence.contains(keyword, ignoreCase = true) }
        }
        return confidenceSentence ?: "Moderate confidence based on visual analysis."
    }

    /**
     * Extracts management recommendation from report sentences.
     */
    private fun extractManagementRecommendation(sentences: List<String>): String {
        // Look for sentences with recommendations (usually last sentence or contains action words)
        val actionKeywords = listOf("recommend", "apply", "remove", "treat", "spray", "prune", "improve")
        val recommendationSentence = sentences.findLast { sentence ->
            actionKeywords.any { keyword -> sentence.contains(keyword, ignoreCase = true) }
        }
        return recommendationSentence ?: "Consult with agricultural expert for specific treatment recommendations."
    }

    /**
     * Creates an Uncertain diagnostic report for poor quality images or low confidence.
     */
    private fun createUncertainReport(reason: String): DiagnosticReport {
        val fullReport = "The analysis result is **Uncertain** due to poor image quality, lighting, or focus. A clearer photo is recommended for a more reliable diagnosis."
        
        return DiagnosticReport(
            diseaseName = "Uncertain",
            observedSymptoms = "Unable to clearly identify symptoms due to image quality issues.",
            confidenceLevel = "Low confidence - $reason",
            managementRecommendation = "Please capture a clearer image with better lighting and focus for accurate diagnosis.",
            fullReport = fullReport,
            isUncertain = true,
            timestamp = System.currentTimeMillis(),
            modelVersion = MODEL_VERSION
        )
    }

    /**
     * Creates a fallback diagnostic report based on TFLite prediction when Gemini is unavailable.
     */
    private fun createFallbackReport(classificationResult: ClassificationResult): DiagnosticReport {
        val diseaseName = classificationResult.diseaseClass.displayName
        val confidence = String.format("%.1f%%", classificationResult.confidence * 100)

        val fullReport = "Based on the image analysis, the tomato leaf is identified as **$diseaseName**. " +
                "Classification confidence: $confidence. " +
                "Note: This is a preliminary classification without formal validation. " +
                "Consult with an agricultural expert for detailed diagnosis and treatment recommendations."

        return DiagnosticReport(
            diseaseName = diseaseName,
            observedSymptoms = "Automated classification based on visual patterns.",
            confidenceLevel = "Classification confidence: $confidence (preliminary)",
            managementRecommendation = "Consult with agricultural expert for specific treatment recommendations.",
            fullReport = fullReport,
            isUncertain = classificationResult.confidence < 0.5f,
            timestamp = System.currentTimeMillis(),
            modelVersion = MODEL_VERSION
        )
    }

    /**
     * Checks if the Gemini service is available.
     * Verifies API key configuration and network connectivity.
     *
     * @return True if Gemini can be used, false otherwise
     */
    fun isAvailable(): Boolean {
        // Check if API key is configured
        val apiKey = com.ml.tomatoscan.config.GeminiConfig.API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE" || apiKey == "PLACEHOLDER") {
            Log.w("GeminiApi", "Gemini API key not configured")
            return false
        }

        // Check if model is initialized
        if (generativeModel == null) {
            Log.w("GeminiApi", "Gemini model not initialized")
            return false
        }

        // Check network connectivity
        if (!isNetworkAvailable()) {
            Log.w("GeminiApi", "Network not available")
            return false
        }

        return true
    }

    /**
     * Checks if network connectivity is available.
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e("GeminiApi", "Error checking network availability", e)
            false
        }
    }
    
    /**
     * Validates YOLO classification result and corrects if needed.
     * Gemini reviews the image and YOLO's prediction to catch obvious errors.
     * 
     * @param bitmap The cropped leaf image
     * @param yoloResult The YOLO classification result
     * @return Triple<Boolean, DiseaseClass?, String> - (isCorrect, correctedClass, reason)
     */
    suspend fun validateClassification(
        bitmap: Bitmap,
        yoloResult: ClassificationResult
    ): Triple<Boolean, com.ml.tomatoscan.models.DiseaseClass?, String> {
        return withContext(Dispatchers.IO) {
            try {
                val model = generativeModel
                if (model == null) {
                    Log.w("GeminiApi", "Gemini model not available for classification validation")
                    return@withContext Triple(true, null, "Gemini unavailable - accepting YOLO result")
                }
                
                val smallBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                
                val prompt = """
                    You are an expert plant pathologist. Review this tomato leaf image.
                    
                    The AI model classified this as: ${yoloResult.diseaseClass.displayName}
                    Confidence: ${String.format("%.1f%%", yoloResult.confidence * 100)}
                    
                    Is this classification correct? Respond in this EXACT format:
                    
                    CORRECT: YES or NO
                    ACTUAL_CLASS: [One of: Healthy, Early Blight, Late Blight, Tomato Mosaic Virus, Septoria Leaf Spot, Bacterial Spot]
                    REASON: [Brief explanation]
                    
                    Be especially careful to distinguish:
                    - Healthy leaves (uniform green, no spots/lesions)
                    - Diseased leaves (spots, lesions, discoloration, wilting)
                """.trimIndent()
                
                val inputContent = content {
                    image(smallBitmap)
                    text(prompt)
                }
                
                Log.d("GeminiApi", "Sending classification validation request to Gemini...")
                val response = model.generateContent(inputContent)
                val responseText = response.text?.trim() ?: ""
                
                Log.d("GeminiApi", "Gemini classification validation response: $responseText")
                
                // Parse response
                val isCorrect = responseText.contains("CORRECT: YES", ignoreCase = true)
                val actualClassLine = responseText.lines().find { it.startsWith("ACTUAL_CLASS:", ignoreCase = true) }
                val reasonLine = responseText.lines().find { it.startsWith("REASON:", ignoreCase = true) }
                
                val correctedClass = if (!isCorrect && actualClassLine != null) {
                    val className = actualClassLine.substringAfter(":").trim()
                    when {
                        className.contains("Healthy", ignoreCase = true) -> com.ml.tomatoscan.models.DiseaseClass.HEALTHY
                        className.contains("Early Blight", ignoreCase = true) -> com.ml.tomatoscan.models.DiseaseClass.EARLY_BLIGHT
                        className.contains("Late Blight", ignoreCase = true) -> com.ml.tomatoscan.models.DiseaseClass.LATE_BLIGHT
                        className.contains("Mosaic", ignoreCase = true) -> com.ml.tomatoscan.models.DiseaseClass.TOMATO_MOSAIC_VIRUS
                        className.contains("Septoria", ignoreCase = true) -> com.ml.tomatoscan.models.DiseaseClass.SEPTORIA_LEAF_SPOT
                        className.contains("Bacterial", ignoreCase = true) -> com.ml.tomatoscan.models.DiseaseClass.BACTERIAL_SPOT
                        else -> null
                    }
                } else null
                
                val reason = reasonLine?.substringAfter(":")?.trim() ?: "Gemini validation completed"
                
                smallBitmap.recycle()
                Triple(isCorrect, correctedClass, reason)
                
            } catch (e: Exception) {
                Log.e("GeminiApi", "Error in Gemini classification validation", e)
                // On error, accept YOLO result (fail-open)
                Triple(true, null, "Validation error: ${e.message}")
            }
        }
    }
    
    /**
     * Pre-validates if the image contains a tomato leaf before running YOLO detection.
     * This is a fast check using Gemini's vision capabilities to reject non-tomato images.
     * 
     * @param bitmap The image to validate
     * @return Pair<Boolean, String> - (isTomatoLeaf, reason)
     */
    suspend fun validateIsTomatoLeaf(bitmap: Bitmap): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val model = generativeModel
                if (model == null) {
                    Log.w("GeminiApi", "Gemini model not available for pre-validation")
                    return@withContext Pair(true, "Gemini unavailable - skipping pre-validation")
                }
                
                // Resize image for faster processing
                val smallBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                
                val prompt = """
                    You are an expert botanist. Analyze this image and answer with ONLY "YES" or "NO":
                    
                    Is this image a photograph of a tomato plant leaf?
                    
                    Answer YES if:
                    - The image shows a tomato leaf (healthy or diseased)
                    - The leaf is clearly visible and recognizable as a tomato leaf
                    
                    Answer NO if:
                    - The image shows a different type of plant
                    - The image shows non-plant objects (hands, keyboards, furniture, etc.)
                    - The image is too blurry or unclear to identify
                    - The image shows something other than a leaf
                    
                    Respond with ONLY one word: YES or NO
                """.trimIndent()
                
                val inputContent = content {
                    image(smallBitmap)
                    text(prompt)
                }
                
                Log.d("GeminiApi", "Sending pre-validation request to Gemini...")
                val response = model.generateContent(inputContent)
                val responseText = response.text?.trim()?.uppercase() ?: ""
                
                Log.d("GeminiApi", "Gemini pre-validation response: $responseText")
                
                // Parse response
                val isTomatoLeaf = responseText.contains("YES")
                val reason = if (isTomatoLeaf) {
                    "Gemini confirmed: Image contains a tomato leaf"
                } else {
                    "Gemini rejected: Image does not appear to be a tomato leaf"
                }
                
                smallBitmap.recycle()
                Pair(isTomatoLeaf, reason)
                
            } catch (e: Exception) {
                Log.e("GeminiApi", "Error in Gemini pre-validation", e)
                // On error, allow the image to proceed (fail-open)
                Pair(true, "Pre-validation error: ${e.message}")
            }
        }
    }
}
