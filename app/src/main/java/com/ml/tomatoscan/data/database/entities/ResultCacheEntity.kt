package com.ml.tomatoscan.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ml.tomatoscan.data.database.converters.DiagnosticReportConverter
import com.ml.tomatoscan.models.DiagnosticReport

/**
 * Room entity for caching diagnostic results.
 * Uses perceptual image hash as the primary key to identify similar images.
 *
 * @property imageHash Perceptual hash of the image (primary key)
 * @property diagnosticReport The cached diagnostic report
 * @property cachedAt Timestamp when the result was cached
 * @property expiresAt Timestamp when the cache entry expires
 * @property accessCount Number of times this cache entry has been accessed (for LRU)
 * @property lastAccessedAt Timestamp of last access (for LRU eviction)
 */
@Entity(tableName = "result_cache")
@TypeConverters(DiagnosticReportConverter::class)
data class ResultCacheEntity(
    @PrimaryKey
    val imageHash: String,
    val diagnosticReport: DiagnosticReport,
    val cachedAt: Long,
    val expiresAt: Long,
    val accessCount: Int = 0,
    val lastAccessedAt: Long = System.currentTimeMillis()
)
