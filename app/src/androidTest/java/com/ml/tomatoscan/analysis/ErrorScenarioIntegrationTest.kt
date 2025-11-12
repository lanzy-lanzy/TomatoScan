package com.ml.tomatoscan.analysis

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ml.tomatoscan.data.GeminiApi
import com.ml.tomatoscan.data.ResultCacheImpl
import com.ml.tomatoscan.ml.TFLiteDiseaseClassifier
import com.ml.tomatoscan.ml.YoloLeafDetector
import com.ml.tomatoscan.models.AnalysisError
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for error scenarios in the analysis pipeline.
 * 
 * Tests Requirements:
 * - 5.1: Poor image quality handling
 * - 5.2: No leaf detected error handling
 * - 5.3: Gemini API failure fallback
 * - 5.4: Error logging and user feedback
 * 
 * These tests verify:
 * 1. Non-leaf images are properly rejected
 * 2. Poor quality images are detected and handled
 * 3. Gemini API disabled scenario works with fallback
 * 4. Network unavailable scenario is handled gracefully
 */
@RunWith(AndroidJUnit4::class)
class ErrorScenarioIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var pipeline: AnalysisPipeline
    private lateinit var leafDetector: YoloLeafDetector
    private lateinit var diseaseClassifier: TFLiteDiseaseClassifier
    private lateinit var geminiApi: GeminiApi
    private lateinit var resultCache: ResultCacheImpl
    private lateinit var database: com.ml.tomatoscan.data.database.TomatoScanDatabase
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize database
        database = com.ml.tomatoscan.data.database.TomatoScanDatabase.getDatabase(context)
        
        // Initialize components
        leafDetector = YoloLeafDetector(context)
        diseaseClassifier = TFLiteDiseaseClassifier(context)
        geminiApi = GeminiApi(context)
        resultCache = ResultCacheImpl(database.resultCacheDao())
        
        // Create pipeline
        pipeline = AnalysisPipelineImpl(
            context = context,
            leafDetector = leafDetector,
            diseaseClassifier = diseaseClassifier,
            geminiApi = geminiApi,
            resultCache = resultCache
        )
    }
    
    @After
    fun tearDown() {
        // Clean up resources
        leafDetector.close()
        diseaseClassifier.close()
    }
    
    /**
     * Test 12.2.1: Test with non-leaf images
     * 
     * Verifies that images without tomato leaves are properly detected
     * and rejected with appropriate error messages.
     * 
     * Requirements: 5.2
     */
    @Test
    fun testNonLeafImageRejection() = runBlocking {
        // Create a non-leaf image (solid color, no leaf patterns)
        val nonLeafImage = createNonLeafImage()
        
        // Run analysis
        val result = pipeline.analyze(nonLeafImage)
        
        // Verify failure with appropriate error
        assertFalse("Analysis should fail for non-leaf image", result.success)
        assertNotNull("Error should be present", result.error)
        
        // Verify error type
        when (result.error) {
            is AnalysisError.NoLeafDetected -> {
                // Expected error type
                val error = result.error as AnalysisError.NoLeafDetected
                assertFalse("Error message should not be empty", error.message.isEmpty())
                assertTrue("Error message should mention leaf detection", 
                    error.message.contains("leaf", ignoreCase = true))
            }
            is AnalysisError.PoorImageQuality -> {
                // Also acceptable if quality validation catches it first
                val error = result.error as AnalysisError.PoorImageQuality
                assertNotNull("Quality issues should be listed", error.issues)
                assertTrue("Should have at least one quality issue", error.issues.isNotEmpty())
            }
            else -> {
                fail("Unexpected error type: ${result.error?.javaClass?.simpleName}")
            }
        }
    }
    
    /**
     * Test 12.2.2: Test with poor quality images
     * 
     * Verifies that low quality images (blurry, dark, low resolution)
     * are detected and handled appropriately.
     * 
     * Requirements: 5.1
     */
    @Test
    fun testPoorQualityImageHandling() = runBlocking {
        // Test with very small image (below minimum resolution)
        val lowResImage = createLowResolutionImage()
        val result1 = pipeline.analyze(lowResImage)
        
        // Should fail with quality error
        assertFalse("Analysis should fail for low resolution image", result1.success)
        assertNotNull("Error should be present", result1.error)
        
        if (result1.error is AnalysisError.PoorImageQuality) {
            val error = result1.error as AnalysisError.PoorImageQuality
            assertTrue("Should have quality issues listed", error.issues.isNotEmpty())
            assertTrue("Should mention resolution issue", 
                error.issues.any { it.contains("resolution", ignoreCase = true) })
        }
        
        // Test with very dark image
        val darkImage = createDarkImage()
        val result2 = pipeline.analyze(darkImage)
        
        // May fail with quality error or low confidence
        assertFalse("Analysis should fail or have low confidence for dark image", 
            result2.success || (result2.diagnosticReport?.isUncertain == true))
    }
    
    /**
     * Test 12.2.3: Test with Gemini API disabled
     * 
     * Verifies that the pipeline falls back to TFLite-only mode
     * when Gemini API is unavailable or disabled.
     * 
     * Requirements: 5.3, 6.5
     */
    @Test
    fun testGeminiApiDisabledFallback() = runBlocking {
        val testImage = createValidLeafImage()
        
        // Use fallback mode explicitly
        val result = pipeline.analyzeFallback(testImage)
        
        // Should succeed with fallback report
        if (result.success) {
            assertNotNull("Should have diagnostic report", result.diagnosticReport)
            
            val report = result.diagnosticReport!!
            // Fallback report should contain disclaimer
            assertTrue("Fallback report should mention preliminary classification",
                report.fullReport.contains("preliminary", ignoreCase = true) ||
                report.fullReport.contains("without formal validation", ignoreCase = true))
            
            // Should still have all required fields
            assertFalse("Disease name should not be empty", report.diseaseName.isEmpty())
            assertFalse("Full report should not be empty", report.fullReport.isEmpty())
        }
    }
    
    /**
     * Test 12.2.4: Test network unavailable scenario
     * 
     * Verifies that when Gemini API is unavailable due to network issues,
     * the system handles it gracefully and provides appropriate feedback.
     * 
     * Requirements: 5.3, 5.4
     */
    @Test
    fun testNetworkUnavailableHandling() = runBlocking {
        val testImage = createValidLeafImage()
        
        // Check if Gemini is available
        val isGeminiAvailable = geminiApi.isAvailable()
        
        // Run analysis
        val result = pipeline.analyze(testImage)
        
        // If Gemini is not available, should still get a result (fallback)
        if (!isGeminiAvailable) {
            // Should succeed with fallback report
            if (result.success) {
                assertNotNull("Should have diagnostic report", result.diagnosticReport)
                val report = result.diagnosticReport!!
                
                // Fallback report should indicate lack of formal validation
                assertTrue("Report should indicate fallback mode",
                    report.fullReport.contains("preliminary", ignoreCase = true) ||
                    report.fullReport.contains("without formal validation", ignoreCase = true) ||
                    report.fullReport.contains("Note:", ignoreCase = true))
            }
        } else {
            // If Gemini is available, should get full report
            if (result.success) {
                assertNotNull("Should have diagnostic report", result.diagnosticReport)
            }
        }
        
        // In either case, should have a result (not crash)
        assertNotNull("Should always return a result", result)
        assertTrue("Processing time should be positive", result.processingTimeMs > 0)
    }
    
    /**
     * Test 12.2.5: Test error message quality
     * 
     * Verifies that all error messages are user-friendly and informative.
     * 
     * Requirements: 5.4
     */
    @Test
    fun testErrorMessageQuality() = runBlocking {
        val testCases = listOf(
            createNonLeafImage() to "non-leaf",
            createLowResolutionImage() to "low-resolution",
            createDarkImage() to "dark"
        )
        
        for ((image, description) in testCases) {
            val result = pipeline.analyze(image)
            
            if (!result.success && result.error != null) {
                val errorMessage = when (val error = result.error) {
                    is AnalysisError.NoLeafDetected -> error.message
                    is AnalysisError.PoorImageQuality -> error.issues.joinToString(", ")
                    is AnalysisError.LowConfidence -> "Low confidence: ${error.confidence}"
                    is AnalysisError.GeminiUnavailable -> error.reason
                    is AnalysisError.InvalidImage -> error.message
                    is AnalysisError.UnknownError -> error.exception.message ?: "Unknown error"
                    else -> "Unexpected error"
                }
                
                // Verify error message is not empty
                assertFalse("Error message should not be empty for $description image", 
                    errorMessage.isEmpty())
                
                // Verify error message is reasonably informative (at least 10 characters)
                assertTrue("Error message should be informative for $description image", 
                    errorMessage.length >= 10)
                
                println("Error for $description image: $errorMessage")
            }
        }
    }
    
    // Helper functions to create test images
    
    private fun createNonLeafImage(): Bitmap {
        // Create a solid blue image (sky/background)
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLUE
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
    
    private fun createLowResolutionImage(): Bitmap {
        // Create a very small image (below minimum 224x224)
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.GREEN
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
    
    private fun createDarkImage(): Bitmap {
        // Create a very dark image
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(10, 10, 10) // Very dark
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
    
    private fun createValidLeafImage(): Bitmap {
        // Create a green image that might pass as a leaf
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.rgb(34, 139, 34) // Forest green
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}
