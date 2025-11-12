package com.ml.tomatoscan.config

import com.ml.tomatoscan.BuildConfig

/**
 * Configuration for result caching system
 * Ensures consistent results for identical inputs
 */
object CacheConfig {
    /**
     * Maximum number of cached results to store
     * Uses LRU (Least Recently Used) eviction policy
     */
    const val MAX_CACHE_SIZE = 100

    /**
     * Time-to-live for cached results in days
     * Results older than this will be automatically cleared
     */
    const val CACHE_TTL_DAYS = 7

    /**
     * Flag to enable/disable result caching
     * Can be configured in local.properties as enable.caching
     */
    val ENABLE_CACHING: Boolean
        get() = BuildConfig.ENABLE_CACHING

    /**
     * Perceptual hash similarity threshold (0.0 to 1.0)
     * Higher values require more similarity for cache hit
     */
    const val HASH_SIMILARITY_THRESHOLD = 0.95f

    /**
     * Size of perceptual hash in bits
     */
    const val PHASH_SIZE = 8

    /**
     * Cache cleanup interval in hours
     * How often to run automatic cache cleanup
     */
    const val CLEANUP_INTERVAL_HOURS = 24L
}
