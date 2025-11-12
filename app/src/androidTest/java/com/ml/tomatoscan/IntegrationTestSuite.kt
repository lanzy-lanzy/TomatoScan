package com.ml.tomatoscan

import com.ml.tomatoscan.analysis.AnalysisPipelineIntegrationTest
import com.ml.tomatoscan.analysis.ErrorScenarioIntegrationTest
import com.ml.tomatoscan.analysis.PerformanceIntegrationTest
import com.ml.tomatoscan.data.ResultCacheIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive integration test suite for the Tomato Leaf Analysis AI Agent.
 * 
 * This test suite covers all aspects of the system as specified in task 12:
 * 
 * 12.1 - Complete pipeline testing with all disease classes
 * 12.2 - Error scenario testing (non-leaf images, poor quality, API failures)
 * 12.3 - Caching behavior testing (cache hits, expiration, LRU eviction)
 * 12.4 - Performance testing (latency, memory usage, load testing)
 * 
 * Requirements Addressed:
 * - 1.5: Complete pipeline within 5 seconds per image
 * - 2.5: Execute TFLite inference within 2 seconds
 * - 3.2: Deterministic output (same input → same output)
 * - 4.4: Consistent formal report format
 * - 5.1: Poor image quality handling
 * - 5.2: No leaf detected error handling
 * - 5.3: Gemini API failure fallback
 * - 5.4: Error logging and user feedback
 * 
 * To run this test suite:
 * ```
 * ./gradlew connectedAndroidTest
 * ```
 * 
 * Or run specific test classes:
 * ```
 * ./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.AnalysisPipelineIntegrationTest"
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AnalysisPipelineIntegrationTest::class,
    ErrorScenarioIntegrationTest::class,
    ResultCacheIntegrationTest::class,
    PerformanceIntegrationTest::class
)
class IntegrationTestSuite
