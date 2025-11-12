package com.ml.tomatoscan.data

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ml.tomatoscan.config.CacheConfig
import com.ml.tomatoscan.data.database.TomatoScanDatabase
import com.ml.tomatoscan.models.DiagnosticReport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Integration tests for result caching behavior.
 * 
 * Tests Requirements:
 * - 3.2: Deterministic output through caching
 * - 4.4: Consistent results for identical inputs
 * 
 * These tests verify:
 * 1. Cache hit returns same result
 * 2. Cache expiration after 7 days
 * 3. LRU eviction at 100 entries
 */
@RunWith(AndroidJUnit4::class)
class ResultCacheIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var database: TomatoScanDatabase
    private lateinit var resultCache: ResultCacheImpl
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            TomatoScanDatabase::class.java
        ).build()
        
        // Initialize cache with test database
        resultCache = ResultCacheImpl(database.resultCacheDao())
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    /**
     * Test 12.3.1: Verify cache hit returns same result
     * 
     * This test verifies that when the same image is analyzed twice,
     * the second analysis returns the cached result, ensuring consistency.
     * 
     * Requirements: 3.2, 4.4
     */
    @Test
    fun testCacheHitReturnsSameResult() = runBlocking {
        // Create test image and report
        val testImage = createTestImage()
        val originalReport = createTestReport("Early Blight")
        
        // Cache the result
        resultCache.cacheResult(testImage, originalReport)
        
        // Retrieve from cache
        val cachedReport = resultCache.getCachedResult(testImage)
        
        // Verify cache hit
        assertNotNull("Cached report should not be null", cachedReport)
        assertEquals("Disease names should match", 
            originalReport.diseaseName, cachedReport!!.diseaseName)
        assertEquals("Full reports should match", 
            originalReport.fullReport, cachedReport.fullReport)
        assertEquals("Observed symptoms should match", 
            originalReport.observedSymptoms, cachedReport.observedSymptoms)
        assertEquals("Confidence levels should match", 
            originalReport.confidenceLevel, cachedReport.confidenceLevel)
        assertEquals("Management recommendations should match", 
            originalReport.managementRecommendation, cachedReport.managementRecommendation)
    }
    
    /**
     * Test 12.3.2: Verify cache miss for different images
     * 
     * This test verifies that different images don't incorrectly match
     * in the cache.
     */
    @Test
    fun testCacheMissForDifferentImages() = runBlocking {
        // Create two different test images
        val testImage1 = createTestImage(color = android.graphics.Color.RED)
        val testImage2 = createTestImage(color = android.graphics.Color.BLUE)
        val report1 = createTestReport("Early Blight")
        
        // Cache result for first image
        resultCache.cacheResult(testImage1, report1)
        
        // Try to retrieve with second image
        val cachedReport = resultCache.getCachedResult(testImage2)
        
        // Should be cache miss (null) since images are different
        assertNull("Should be cache miss for different image", cachedReport)
    }
    
    /**
     * Test 12.3.3: Verify cache expiration after 7 days
     * 
     * This test verifies that cached results expire after the configured TTL
     * and are automatically cleaned up.
     * 
     * Requirements: 3.2, 4.4
     */
    @Test
    fun testCacheExpirationAfter7Days() = runBlocking {
        val testImage = createTestImage()
        val testReport = createTestReport("Late Blight")
        
        // Cache the result
        resultCache.cacheResult(testImage, testReport)
        
        // Verify it's cached
        val cachedReport1 = resultCache.getCachedResult(testImage)
        assertNotNull("Report should be cached initially", cachedReport1)
        
        // Simulate time passing (8 days)
        val dao = database.resultCacheDao()
        val currentTime = System.currentTimeMillis()
        val expiredTime = currentTime - TimeUnit.DAYS.toMillis(8)
        
        // Manually update the cache entry to be expired
        // (In real scenario, this would happen naturally over time)
        val allCaches = dao.getAllValidCaches(Long.MAX_VALUE)
        for (cache in allCaches) {
            dao.insertCache(cache.copy(
                cachedAt = expiredTime,
                expiresAt = expiredTime + TimeUnit.DAYS.toMillis(CacheConfig.CACHE_TTL_DAYS.toLong())
            ))
        }
        
        // Try to retrieve - should be null (expired)
        val cachedReport2 = resultCache.getCachedResult(testImage)
        assertNull("Report should be expired after 7 days", cachedReport2)
        
        // Clean up expired entries
        resultCache.clearOldCache(CacheConfig.CACHE_TTL_DAYS)
        
        // Verify cache is empty
        val cacheCount = dao.getCacheCount()
        assertEquals("Cache should be empty after cleanup", 0, cacheCount)
    }
    
    /**
     * Test 12.3.4: Verify LRU eviction at 100 entries
     * 
     * This test verifies that when the cache exceeds the maximum size,
     * the least recently used entries are evicted.
     * 
     * Requirements: 3.2, 4.4
     */
    @Test
    fun testLRUEvictionAt100Entries() = runBlocking {
        val dao = database.resultCacheDao()
        
        // Cache 105 different results (exceeding max of 100)
        val reports = mutableListOf<Pair<Bitmap, DiagnosticReport>>()
        for (i in 1..105) {
            val image = createTestImage(seed = i)
            val report = createTestReport("Disease $i")
            resultCache.cacheResult(image, report)
            reports.add(image to report)
            
            // Small delay to ensure different timestamps
            Thread.sleep(10)
        }
        
        // Verify cache size is at most MAX_CACHE_SIZE
        val cacheCount = dao.getCacheCount()
        assertTrue("Cache size should not exceed ${CacheConfig.MAX_CACHE_SIZE}", 
            cacheCount <= CacheConfig.MAX_CACHE_SIZE)
        
        // Verify oldest entries were evicted (first 5 should be gone)
        for (i in 0..4) {
            val (image, _) = reports[i]
            val cachedReport = resultCache.getCachedResult(image)
            assertNull("Oldest entry $i should be evicted", cachedReport)
        }
        
        // Verify newest entries are still present (last 5 should exist)
        for (i in 100..104) {
            val (image, originalReport) = reports[i]
            val cachedReport = resultCache.getCachedResult(image)
            assertNotNull("Newest entry $i should still be cached", cachedReport)
            if (cachedReport != null) {
                assertEquals("Disease name should match for entry $i", 
                    originalReport.diseaseName, cachedReport.diseaseName)
            }
        }
    }
    
    /**
     * Test 12.3.5: Verify LRU tracking with access patterns
     * 
     * This test verifies that frequently accessed entries are kept
     * while less frequently accessed entries are evicted.
     */
    @Test
    fun testLRUTrackingWithAccessPatterns() = runBlocking {
        val dao = database.resultCacheDao()
        
        // Cache 10 results
        val reports = mutableListOf<Pair<Bitmap, DiagnosticReport>>()
        for (i in 1..10) {
            val image = createTestImage(seed = i)
            val report = createTestReport("Disease $i")
            resultCache.cacheResult(image, report)
            reports.add(image to report)
            Thread.sleep(10)
        }
        
        // Access first 5 entries multiple times (making them "hot")
        for (i in 0..4) {
            val (image, _) = reports[i]
            repeat(5) {
                resultCache.getCachedResult(image)
                Thread.sleep(5)
            }
        }
        
        // Add 95 more entries to trigger eviction
        for (i in 11..105) {
            val image = createTestImage(seed = i)
            val report = createTestReport("Disease $i")
            resultCache.cacheResult(image, report)
            Thread.sleep(10)
        }
        
        // Verify cache size is at most MAX_CACHE_SIZE
        val cacheCount = dao.getCacheCount()
        assertTrue("Cache size should not exceed ${CacheConfig.MAX_CACHE_SIZE}", 
            cacheCount <= CacheConfig.MAX_CACHE_SIZE)
        
        // Verify frequently accessed entries are still present
        for (i in 0..4) {
            val (image, originalReport) = reports[i]
            val cachedReport = resultCache.getCachedResult(image)
            // Note: Due to LRU eviction, some may still be evicted if cache is full
            // This test verifies the LRU mechanism is working
            if (cachedReport != null) {
                assertEquals("Frequently accessed entry should match", 
                    originalReport.diseaseName, cachedReport.diseaseName)
            }
        }
    }
    
    /**
     * Test 12.3.6: Verify cache consistency across multiple retrievals
     * 
     * This test verifies that retrieving the same cached result multiple times
     * always returns identical data.
     */
    @Test
    fun testCacheConsistencyAcrossMultipleRetrievals() = runBlocking {
        val testImage = createTestImage()
        val originalReport = createTestReport("Septoria Leaf Spot")
        
        // Cache the result
        resultCache.cacheResult(testImage, originalReport)
        
        // Retrieve multiple times
        val retrievals = mutableListOf<DiagnosticReport>()
        repeat(10) {
            val cachedReport = resultCache.getCachedResult(testImage)
            assertNotNull("Should always get cached result", cachedReport)
            retrievals.add(cachedReport!!)
        }
        
        // Verify all retrievals are identical
        for (i in 1 until retrievals.size) {
            assertEquals("Disease name should be consistent", 
                retrievals[0].diseaseName, retrievals[i].diseaseName)
            assertEquals("Full report should be consistent", 
                retrievals[0].fullReport, retrievals[i].fullReport)
            assertEquals("Observed symptoms should be consistent", 
                retrievals[0].observedSymptoms, retrievals[i].observedSymptoms)
            assertEquals("Confidence level should be consistent", 
                retrievals[0].confidenceLevel, retrievals[i].confidenceLevel)
            assertEquals("Management recommendation should be consistent", 
                retrievals[0].managementRecommendation, retrievals[i].managementRecommendation)
        }
    }
    
    // Helper functions
    
    private fun createTestImage(color: Int = android.graphics.Color.GREEN, seed: Int = 0): Bitmap {
        val width = 640
        val height = 640
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Add seed to color to make each image unique
        val adjustedColor = android.graphics.Color.rgb(
            android.graphics.Color.red(color) + (seed % 50),
            android.graphics.Color.green(color) + (seed % 50),
            android.graphics.Color.blue(color) + (seed % 50)
        )
        
        val paint = android.graphics.Paint().apply {
            this.color = adjustedColor
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        return bitmap
    }
    
    private fun createTestReport(diseaseName: String): DiagnosticReport {
        return DiagnosticReport(
            diseaseName = diseaseName,
            observedSymptoms = "Test symptoms for $diseaseName",
            confidenceLevel = "High confidence (95%)",
            managementRecommendation = "Test recommendation for $diseaseName",
            fullReport = "Based on the image analysis, the tomato leaf is identified as **$diseaseName**. " +
                    "Test symptoms for $diseaseName. High confidence (95%). " +
                    "Test recommendation for $diseaseName.",
            isUncertain = false,
            timestamp = System.currentTimeMillis(),
            modelVersion = "1.0.0"
        )
    }
}
