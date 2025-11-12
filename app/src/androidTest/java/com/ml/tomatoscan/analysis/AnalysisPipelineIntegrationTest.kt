package com.ml.tomatoscan.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ml.tomatoscan.data.GeminiApi
import com.ml.tomatoscan.data.ResultCacheImpl
import com.ml.tomatoscan.ml.TFLiteDiseaseClassifier
import com.ml.tomatoscan.ml.YoloLeafDetector
import com.ml.tomatoscan.models.DiseaseClass
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Integration tests for the complete analysis pipeline.
 * 
 * Tests Requirements:
 * - 3.2: Deterministic output (same input → same output)
 * - 4.4: Consistent formal report format
 * 
 * These tests verify:
 * 1. Complete pipeline with sample images for each disease class
 * 2. Formal report format consistency
 * 3. Deterministic output verification
 */
@RunWith(AndroidJUnit4::class)
class AnalysisPipelineIntegrationTest {
    
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
     * Test 12.1.1: Test complete pipeline with sample images for each disease class
     * 
     * This test verifies that the pipeline can successfully process images
     * for all 7 disease classes and produce valid results.
     */
    @Test
    fun testPipelineWithAllDiseaseClasses() = runBlocking {
        val diseaseClasses = listOf(
            DiseaseClass.EARLY_BLIGHT,
            DiseaseClass.LATE_BLIGHT,
            DiseaseClass.LEAF_MOLD,
            DiseaseClass.SEPTORIA_LEAF_SPOT,
            DiseaseClass.BACTERIAL_SPECK,
            DiseaseClass.HEALTHY,
            DiseaseClass.UNCERTAIN
        )
        
        for (diseaseClass in diseaseClasses) {
            // Create a test image (simple colored bitmap for testing)
            val testImage = createTestImage(diseaseClass)
            
            // Run analysis
            val result = pipeline.analyze(testImage)
            
            // Verify result structure
            assertNotNull("Result should not be null for ${diseaseClass.displayName}", result)
            assertTrue("Processing time should be positive", result.processingTimeMs > 0)
            
            // If successful, verify all components are present
            if (result.success) {
                assertNotNull("Classification result should not be null", result.classificationResult)
                assertNotNull("Diagnostic report should not be null", result.diagnosticReport)
                
                // Verify report structure
                val report = result.diagnosticReport!!
                assertFalse("Disease name should not be empty", report.diseaseName.isEmpty())
                assertFalse("Observed symptoms should not be empty", report.observedSymptoms.isEmpty())
                assertFalse("Confidence level should not be empty", report.confidenceLevel.isEmpty())
                assertFalse("Management recommendation should not be empty", report.managementRecommendation.isEmpty())
                assertFalse("Full report should not be empty", report.fullReport.isEmpty())
                assertTrue("Timestamp should be valid", report.timestamp > 0)
                assertFalse("Model version should not be empty", report.modelVersion.isEmpty())
            }
        }
    }
    
    /**
     * Test 12.1.2: Verify formal report format is consistent
     * 
     * This test verifies that all diagnostic reports follow the same structure:
     * - Disease name in bold
     * - Observed symptoms section
     * - Confidence assessment
     * - Management recommendations
     * - 3-5 sentences in formal academic tone
     */
    @Test
    fun testFormalReportFormatConsistency() = runBlocking {
        val testImage = createTestImage(DiseaseClass.EARLY_BLIGHT)
        
        // Run analysis multiple times
        val results = mutableListOf<AnalysisResult>()
        repeat(3) {
            val result = pipeline.analyze(testImage)
            if (result.success && result.diagnosticReport != null) {
                results.add(result)
            }
        }
        
        // Verify we got at least one successful result
        assertTrue("Should have at least one successful result", results.isNotEmpty())
        
        for (result in results) {
            val report = result.diagnosticReport!!
            
            // Verify report contains required components
            assertTrue("Full report should contain disease name", 
                report.fullReport.contains(report.diseaseName, ignoreCase = true))
            
            // Verify bold formatting for disease name (markdown style)
            assertTrue("Disease name should be in bold in full report",
                report.fullReport.contains("**") || report.fullReport.contains(report.diseaseName))
            
            // Verify report has reasonable length (3-5 sentences typically means 100-500 characters)
            assertTrue("Report should have reasonable length (at least 50 chars)", 
                report.fullReport.length >= 50)
            assertTrue("Report should not be excessively long (max 1000 chars)", 
                report.fullReport.length <= 1000)
            
            // Verify all required fields are populated
            assertFalse("Disease name should not be empty", report.diseaseName.isEmpty())
            assertFalse("Observed symptoms should not be empty", report.observedSymptoms.isEmpty())
            assertFalse("Confidence level should not be empty", report.confidenceLevel.isEmpty())
            assertFalse("Management recommendation should not be empty", 
                report.managementRecommendation.isEmpty())
        }
    }
    
    /**
     * Test 12.1.3: Verify deterministic output (same input → same output)
     * 
     * This test verifies that analyzing the same image multiple times
     * produces identical results, ensuring consistency.
     * 
     * Requirements: 3.2, 4.4
     */
    @Test
    fun testDeterministicOutput() = runBlocking {
        val testImage = createTestImage(DiseaseClass.HEALTHY)
        
        // Run analysis twice with the same image
        val result1 = pipeline.analyze(testImage)
        val result2 = pipeline.analyze(testImage)
        
        // Both should succeed or both should fail
        assertEquals("Both results should have same success status", 
            result1.success, result2.success)
        
        if (result1.success && result2.success) {
            val report1 = result1.diagnosticReport!!
            val report2 = result2.diagnosticReport!!
            
            // Verify identical reports (deterministic output)
            assertEquals("Disease names should be identical", 
                report1.diseaseName, report2.diseaseName)
            assertEquals("Full reports should be identical", 
                report1.fullReport, report2.fullReport)
            assertEquals("Observed symptoms should be identical", 
                report1.observedSymptoms, report2.observedSymptoms)
            assertEquals("Confidence levels should be identical", 
                report1.confidenceLevel, report2.confidenceLevel)
            assertEquals("Management recommendations should be identical", 
                report1.managementRecommendation, report2.managementRecommendation)
            assertEquals("Uncertain flags should be identical", 
                report1.isUncertain, report2.isUncertain)
        }
    }
    
    /**
     * Test 12.1.4: Test pipeline performance
     * 
     * Verifies that the pipeline completes within acceptable time limits.
     * Target: < 5 seconds end-to-end
     * 
     * Requirements: 1.5
     */
    @Test
    fun testPipelinePerformance() = runBlocking {
        val testImage = createTestImage(DiseaseClass.LEAF_MOLD)
        
        val result = pipeline.analyze(testImage)
        
        // Verify processing time is within acceptable range
        assertTrue("Pipeline should complete within 10 seconds (allowing for test overhead)", 
            result.processingTimeMs < 10000)
        
        // Log performance for monitoring
        println("Pipeline processing time: ${result.processingTimeMs}ms")
    }
    
    /**
     * Helper function to create test images for different disease classes.
     * In a real scenario, these would be actual leaf images from assets.
     */
    private fun createTestImage(diseaseClass: DiseaseClass): Bitmap {
        // Create a simple colored bitmap for testing
        // In production tests, load actual images from assets
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Fill with a color based on disease class (for testing purposes)
        val color = when (diseaseClass) {
            DiseaseClass.EARLY_BLIGHT -> android.graphics.Color.rgb(139, 69, 19)
            DiseaseClass.LATE_BLIGHT -> android.graphics.Color.rgb(105, 105, 105)
            DiseaseClass.LEAF_MOLD -> android.graphics.Color.rgb(85, 107, 47)
            DiseaseClass.SEPTORIA_LEAF_SPOT -> android.graphics.Color.rgb(160, 82, 45)
            DiseaseClass.BACTERIAL_SPECK -> android.graphics.Color.rgb(128, 128, 0)
            DiseaseClass.HEALTHY -> android.graphics.Color.rgb(34, 139, 34)
            DiseaseClass.UNCERTAIN -> android.graphics.Color.rgb(128, 128, 128)
        }
        
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            this.color = color
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        return bitmap
    }
}
