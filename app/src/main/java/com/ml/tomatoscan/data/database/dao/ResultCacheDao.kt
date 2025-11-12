package com.ml.tomatoscan.data.database.dao

import androidx.room.*
import com.ml.tomatoscan.data.database.entities.ResultCacheEntity

/**
 * Data Access Object for result cache operations.
 * Provides methods for caching, retrieving, and managing cached diagnostic results.
 */
@Dao
interface ResultCacheDao {
    
    /**
     * Retrieves a cached result by image hash if not expired.
     *
     * @param hash The perceptual hash of the image
     * @param currentTime Current timestamp to check expiration
     * @return Cached result if found and not expired, null otherwise
     */
    @Query("SELECT * FROM result_cache WHERE imageHash = :hash AND expiresAt > :currentTime")
    suspend fun getCachedResult(hash: String, currentTime: Long): ResultCacheEntity?
    
    /**
     * Retrieves all cached results (for similarity checking).
     * Only returns non-expired entries.
     *
     * @param currentTime Current timestamp to filter expired entries
     * @return List of all valid cache entries
     */
    @Query("SELECT * FROM result_cache WHERE expiresAt > :currentTime")
    suspend fun getAllValidCaches(currentTime: Long): List<ResultCacheEntity>
    
    /**
     * Inserts or updates a cache entry.
     *
     * @param cache The cache entry to insert/update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: ResultCacheEntity)
    
    /**
     * Updates the access count and last accessed time for a cache entry.
     * Used for LRU tracking.
     *
     * @param hash The image hash
     * @param accessCount New access count
     * @param lastAccessedAt New last accessed timestamp
     */
    @Query("UPDATE result_cache SET accessCount = :accessCount, lastAccessedAt = :lastAccessedAt WHERE imageHash = :hash")
    suspend fun updateAccessInfo(hash: String, accessCount: Int, lastAccessedAt: Long)
    
    /**
     * Deletes expired cache entries.
     *
     * @param currentTime Current timestamp
     * @return Number of deleted entries
     */
    @Query("DELETE FROM result_cache WHERE expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long): Int
    
    /**
     * Gets the total count of cache entries.
     *
     * @return Total number of cached results
     */
    @Query("SELECT COUNT(*) FROM result_cache")
    suspend fun getCacheCount(): Int
    
    /**
     * Deletes the least recently used cache entries.
     * Used for LRU eviction when cache size exceeds limit.
     *
     * @param limit Number of entries to keep
     */
    @Query("DELETE FROM result_cache WHERE imageHash NOT IN (SELECT imageHash FROM result_cache ORDER BY lastAccessedAt DESC LIMIT :limit)")
    suspend fun evictLRU(limit: Int)
    
    /**
     * Deletes a specific cache entry by hash.
     *
     * @param hash The image hash to delete
     */
    @Query("DELETE FROM result_cache WHERE imageHash = :hash")
    suspend fun deleteByHash(hash: String)
    
    /**
     * Clears all cache entries.
     */
    @Query("DELETE FROM result_cache")
    suspend fun clearAll()
}
