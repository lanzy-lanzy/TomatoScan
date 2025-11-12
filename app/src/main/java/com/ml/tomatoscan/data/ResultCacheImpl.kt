package com.ml.tomatoscan.data

import android.graphics.Bitmap
import android.util.Log
import com.ml.tomatoscan.config.CacheConfig
import com.ml.tomatoscan.data.database.dao.ResultCacheDao
import com.ml.tomatoscan.data.database.entities.ResultCacheEntity
import com.ml.tomatoscan.models.DiagnosticReport
import com.ml.tomatoscan.utils.PerceptualHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Implementation of ResultCache interface.
 * Manages caching of diagnostic results using perceptual image hashing
 * to ensure consistent results for similar images.
 *
 * Features:
 * - Perceptual hashing for image similarity detection
 * - TTL-based cache expiration (7 days default)
 * - LRU eviction when cache exceeds max size (100 entries)
 * - Automatic cleanup of expired entries
 *
 * @property resultCacheDao DAO for database operations
 */
class ResultCacheImpl(
    private val resultCacheDao: ResultCacheDao
) : ResultCache {
    
    companion object {
        private const val TAG = "ResultCacheImpl"
    }
    
    /**
     * Retrieves a cached diagnostic report for the given image.
     * Uses perceptual hashing to find similar images in the cache.
     *
     * Algorithm:
     * 1. Generate perceptual hash for input image
     * 2. Check for exact hash match in cache
     * 3. If no exact match, check all cached hashes for similarity
     * 4. Return cached result if similarity exceeds threshold
     *
     * @param bitmap The input image to check for cached results
     * @return Cached DiagnosticReport if found and not expired, null otherwise
     */
    override suspend fun getCachedResult(bitmap: Bitmap): DiagnosticReport? {
        if (!CacheConfig.ENABLE_CACHING) {
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                // Generate perceptual hash for the input image
                val imageHash = PerceptualHash.generateHash(bitmap)
                val currentTime = System.currentTimeMillis()
                
                // First, try exact hash match
                var cacheEntity = resultCacheDao.getCachedResult(imageHash, currentTime)
                
                // If no exact match, check for similar hashes
                if (cacheEntity == null) {
                    cacheEntity = findSimilarCachedResult(imageHash, currentTime)
                }
                
                if (cacheEntity != null) {
                    // Update access information for LRU tracking
                    resultCacheDao.updateAccessInfo(
                        hash = cacheEntity.imageHash,
                        accessCount = cacheEntity.accessCount + 1,
                        lastAccessedAt = currentTime
                    )
                    
                    Log.d(TAG, "Cache hit for image hash: ${cacheEntity.imageHash}")
                    return@withContext cacheEntity.diagnosticReport
                }
                
                Log.d(TAG, "Cache miss for image hash: $imageHash")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving cached result", e)
                null
            }
        }
    }
    
    /**
     * Caches a diagnostic report for the given image.
     * Implements LRU eviction if cache size exceeds maximum.
     *
     * @param bitmap The input image to cache
     * @param report The diagnostic report to associate with this image
     */
    override suspend fun cacheResult(bitmap: Bitmap, report: DiagnosticReport) {
        if (!CacheConfig.ENABLE_CACHING) {
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                // Generate perceptual hash for the image
                val imageHash = PerceptualHash.generateHash(bitmap)
                val currentTime = System.currentTimeMillis()
                val expiresAt = currentTime + TimeUnit.DAYS.toMillis(CacheConfig.CACHE_TTL_DAYS.toLong())
                
                // Create cache entity
                val cacheEntity = ResultCacheEntity(
                    imageHash = imageHash,
                    diagnosticReport = report,
                    cachedAt = currentTime,
                    expiresAt = expiresAt,
                    accessCount = 1,
                    lastAccessedAt = currentTime
                )
                
                // Insert into cache
                resultCacheDao.insertCache(cacheEntity)
                Log.d(TAG, "Cached result for image hash: $imageHash")
                
                // Check cache size and evict if necessary
                val cacheCount = resultCacheDao.getCacheCount()
                if (cacheCount > CacheConfig.MAX_CACHE_SIZE) {
                    resultCacheDao.evictLRU(CacheConfig.MAX_CACHE_SIZE)
                    Log.d(TAG, "Evicted LRU entries, cache size was: $cacheCount")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error caching result", e)
            }
        }
    }
    
    /**
     * Clears old cache entries that have exceeded their TTL.
     *
     * @param olderThanDays Remove entries older than this many days (default: 7)
     */
    override suspend fun clearOldCache(olderThanDays: Int) {
        withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays.toLong())
                val deletedCount = resultCacheDao.deleteExpired(cutoffTime)
                Log.d(TAG, "Cleared $deletedCount expired cache entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing old cache", e)
            }
        }
    }
    
    /**
     * Finds a cached result with a similar perceptual hash.
     * Checks all valid cache entries for similarity above threshold.
     *
     * @param targetHash The hash to compare against
     * @param currentTime Current timestamp for filtering expired entries
     * @return Cache entity if similar hash found, null otherwise
     */
    private suspend fun findSimilarCachedResult(
        targetHash: String,
        currentTime: Long
    ): ResultCacheEntity? {
        return try {
            // Get all valid cache entries
            val allCaches = resultCacheDao.getAllValidCaches(currentTime)
            
            // Find the most similar hash above threshold
            var bestMatch: ResultCacheEntity? = null
            var bestSimilarity = 0.0f
            
            for (cache in allCaches) {
                val similarity = PerceptualHash.calculateSimilarity(targetHash, cache.imageHash)
                if (similarity >= CacheConfig.HASH_SIMILARITY_THRESHOLD && similarity > bestSimilarity) {
                    bestMatch = cache
                    bestSimilarity = similarity
                }
            }
            
            if (bestMatch != null) {
                Log.d(TAG, "Found similar cached result with similarity: $bestSimilarity")
            }
            
            bestMatch
        } catch (e: Exception) {
            Log.e(TAG, "Error finding similar cached result", e)
            null
        }
    }
}
