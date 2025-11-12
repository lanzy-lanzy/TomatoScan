package com.ml.tomatoscan.data

import android.graphics.Bitmap
import com.ml.tomatoscan.models.DiagnosticReport

/**
 * Interface for caching diagnostic results to ensure consistency.
 * Uses perceptual image hashing to identify similar images and return cached results.
 *
 * This ensures that identical or very similar images receive the same diagnostic report,
 * maintaining consistency across multiple analyses.
 */
interface ResultCache {
    /**
     * Retrieves a cached diagnostic report for the given image.
     *
     * @param bitmap The input image to check for cached results
     * @return Cached DiagnosticReport if found and not expired, null otherwise
     */
    suspend fun getCachedResult(bitmap: Bitmap): DiagnosticReport?

    /**
     * Caches a diagnostic report for the given image.
     * Uses perceptual hashing to generate a fingerprint of the image.
     *
     * @param bitmap The input image to cache
     * @param report The diagnostic report to associate with this image
     */
    suspend fun cacheResult(bitmap: Bitmap, report: DiagnosticReport)

    /**
     * Clears old cache entries that have exceeded their TTL.
     *
     * @param olderThanDays Remove entries older than this many days (default: 7)
     */
    suspend fun clearOldCache(olderThanDays: Int = 7)
}
