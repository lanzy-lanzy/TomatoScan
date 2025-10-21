package com.ml.tomatoscan.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import com.ml.tomatoscan.utils.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

class AnalysisCache(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("analysis_cache", Context.MODE_PRIVATE)
    
    companion object {
        private const val CACHE_EXPIRY_HOURS = 24 // Cache results for 24 hours
        private const val MAX_CACHE_SIZE = 100 // Maximum cached results
    }
    
    /**
     * Generate a unique key for the image based on its content
     */
    private suspend fun generateImageKey(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val preprocessed = ImagePreprocessor.preprocessForAnalysis(bitmap)
        val hash = ImagePreprocessor.generateImageHash(preprocessed)
        
        // Add image dimensions and basic properties to the key
        val metadata = "${preprocessed.width}x${preprocessed.height}_${preprocessed.config}"
        
        // Create MD5 hash of combined data
        val combined = "$hash$metadata"
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(combined.toByteArray())
        digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check if we have a cached result for this image
     */
    suspend fun getCachedResult(bitmap: Bitmap): TomatoAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            val key = generateImageKey(bitmap)
            val cachedJson = prefs.getString("result_$key", null) ?: return@withContext null
            val timestamp = prefs.getLong("timestamp_$key", 0)
            
            // Check if cache is still valid (within expiry time)
            val currentTime = System.currentTimeMillis()
            val cacheAge = (currentTime - timestamp) / (1000 * 60 * 60) // hours
            
            if (cacheAge > CACHE_EXPIRY_HOURS) {
                // Cache expired, remove it
                removeCachedResult(key)
                return@withContext null
            }
            
            // Parse cached result
            parseAnalysisResult(JSONObject(cachedJson))
        } catch (e: Exception) {
            android.util.Log.e("AnalysisCache", "Error retrieving cached result", e)
            null
        }
    }
    
    /**
     * Cache an analysis result
     */
    suspend fun cacheResult(bitmap: Bitmap, result: TomatoAnalysisResult) = withContext(Dispatchers.IO) {
        try {
            val key = generateImageKey(bitmap)
            val resultJson = serializeAnalysisResult(result)
            
            prefs.edit()
                .putString("result_$key", resultJson.toString())
                .putLong("timestamp_$key", System.currentTimeMillis())
                .apply()
                
            // Clean up old cache entries if we exceed max size
            cleanupOldEntries()
            
            android.util.Log.d("AnalysisCache", "Cached result for key: $key")
        } catch (e: Exception) {
            android.util.Log.e("AnalysisCache", "Error caching result", e)
        }
    }
    
    /**
     * Remove a specific cached result
     */
    private fun removeCachedResult(key: String) {
        prefs.edit()
            .remove("result_$key")
            .remove("timestamp_$key")
            .apply()
    }
    
    /**
     * Clean up old cache entries to maintain max cache size
     */
    private fun cleanupOldEntries() {
        val allEntries = prefs.all
        val timestampEntries = allEntries.filter { it.key.startsWith("timestamp_") }
        
        if (timestampEntries.size > MAX_CACHE_SIZE) {
            // Sort by timestamp and remove oldest entries
            val sortedEntries = timestampEntries.toList()
                .sortedBy { (_, value) -> value as Long }
            
            val entriesToRemove = sortedEntries.take(timestampEntries.size - MAX_CACHE_SIZE)
            val editor = prefs.edit()
            
            entriesToRemove.forEach { (timestampKey, _) ->
                val key = timestampKey.removePrefix("timestamp_")
                editor.remove("result_$key")
                editor.remove(timestampKey)
            }
            
            editor.apply()
        }
    }
    
    /**
     * Clear all cached results
     */
    fun clearCache() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Serialize analysis result to JSON
     */
    private fun serializeAnalysisResult(result: TomatoAnalysisResult): JSONObject {
        return JSONObject().apply {
            put("diseaseDetected", result.diseaseDetected)
            put("confidence", result.confidence)
            put("severity", result.severity)
            put("description", result.description)
            put("recommendations", JSONArray(result.recommendations))
            put("treatmentOptions", JSONArray(result.treatmentOptions))
            put("preventionMeasures", JSONArray(result.preventionMeasures))
        }
    }
    
    /**
     * Parse JSON back to analysis result
     */
    private fun parseAnalysisResult(json: JSONObject): TomatoAnalysisResult {
        return TomatoAnalysisResult(
            diseaseDetected = json.getString("diseaseDetected"),
            confidence = json.getDouble("confidence").toFloat(),
            severity = json.getString("severity"),
            description = json.getString("description"),
            recommendations = parseJsonArray(json.getJSONArray("recommendations")),
            treatmentOptions = parseJsonArray(json.getJSONArray("treatmentOptions")),
            preventionMeasures = parseJsonArray(json.getJSONArray("preventionMeasures"))
        )
    }
    
    private fun parseJsonArray(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
}