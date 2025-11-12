# Result Caching Implementation

This directory contains the implementation of the result caching system for ensuring consistent diagnostic results.

## Components

### 1. ResultCache Interface (`ResultCache.kt`)
- Defines the contract for caching diagnostic results
- Methods: `getCachedResult()`, `cacheResult()`, `clearOldCache()`

### 2. ResultCacheImpl (`ResultCacheImpl.kt`)
- Concrete implementation of ResultCache
- Uses perceptual hashing to identify similar images
- Implements LRU eviction when cache exceeds 100 entries
- Automatic TTL-based expiration (7 days)

### 3. PerceptualHash Utility (`utils/PerceptualHash.kt`)
- Generates perceptual hashes using pHash algorithm
- Calculates similarity between image hashes
- Resistant to minor image changes (compression, scaling, etc.)

### 4. Database Components
- **ResultCacheEntity**: Room entity for storing cached results
- **ResultCacheDao**: DAO with query methods for cache operations
- **DiagnosticReportConverter**: Type converter for storing DiagnosticReport as JSON

## Usage Example

```kotlin
// Initialize cache (typically in Application or ViewModel)
val database = TomatoScanDatabase.getDatabase(context)
val resultCache: ResultCache = ResultCacheImpl(database.resultCacheDao())

// Check cache before analysis
val cachedResult = resultCache.getCachedResult(bitmap)
if (cachedResult != null) {
    // Use cached result
    return cachedResult
}

// Perform analysis...
val newReport = performAnalysis(bitmap)

// Cache the result
resultCache.cacheResult(bitmap, newReport)

// Periodic cleanup (e.g., on app startup)
resultCache.clearOldCache(olderThanDays = 7)
```

## Configuration

Cache behavior is controlled by `CacheConfig.kt`:
- `MAX_CACHE_SIZE`: 100 entries (LRU eviction)
- `CACHE_TTL_DAYS`: 7 days
- `ENABLE_CACHING`: Can be toggled via BuildConfig
- `HASH_SIMILARITY_THRESHOLD`: 0.95 (95% similarity required)
- `PHASH_SIZE`: 8x8 hash size

## Database Migration

The database version has been updated from 1 to 2 to include the `result_cache` table.
Migration is handled automatically in `TomatoScanDatabase.kt`.

## Features

1. **Perceptual Hashing**: Identifies similar images even with minor variations
2. **LRU Eviction**: Automatically removes least recently used entries when cache is full
3. **TTL Expiration**: Entries expire after 7 days
4. **Similarity Matching**: Finds cached results for similar (not just identical) images
5. **Access Tracking**: Tracks access count and last access time for LRU

## Integration with Analysis Pipeline

The cache should be checked **before** calling the Gemini API in the analysis pipeline:

```kotlin
// In AnalysisPipeline.analyze()
suspend fun analyze(inputImage: Bitmap): AnalysisResult {
    // 1. Check cache first
    val cachedReport = resultCache.getCachedResult(inputImage)
    if (cachedReport != null) {
        return AnalysisResult(
            success = true,
            diagnosticReport = cachedReport,
            // ... other fields
        )
    }
    
    // 2. Perform detection and classification
    val detectionResult = leafDetector.detectLeaves(inputImage)
    val classificationResult = classifier.classify(croppedLeaf)
    
    // 3. Generate report with Gemini
    val report = geminiAgent.generateDiagnosticReport(croppedLeaf, classificationResult)
    
    // 4. Cache the result
    resultCache.cacheResult(inputImage, report)
    
    return AnalysisResult(success = true, diagnosticReport = report, ...)
}
```

## Requirements Satisfied

- ✅ 3.2: Deterministic output through caching
- ✅ 4.4: Consistent results for identical inputs
- ✅ Cache lookup before Gemini API calls
- ✅ LRU eviction (max 100 entries)
- ✅ TTL-based expiration (7 days)
- ✅ Perceptual hashing for similarity detection
