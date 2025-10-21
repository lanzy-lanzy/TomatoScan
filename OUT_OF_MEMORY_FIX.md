# Out of Memory (OOM) Crash Fix

## Problem
The app was crashing with `OutOfMemoryError` when clicking "Analyze" after taking a picture with the camera:

```
java.lang.OutOfMemoryError: Failed to allocate a 16 byte allocation with 418752 free bytes and 408KB until OOM, 
target footprint 268435456, growth limit 268435456; giving up on allocation because <1% of heap free after GC.
at com.ml.tomatoscan.utils.ImageQualityValidator$Companion.analyzeContrast(ImageQualityValidator.kt:128)
```

## Root Cause
The `ImageQualityValidator` was trying to process the full-resolution camera image (which can be several megapixels) by:
1. Loading ALL pixels into memory at once
2. Creating multiple large arrays for brightness calculations
3. Using functional operations (`.map()`) that create intermediate collections

For a 4000x3000 pixel image, this means:
- 12,000,000 pixels
- 48MB for the pixel array alone
- Additional memory for intermediate calculations
- This exceeded the app's heap limit (256MB)

## Solution
Optimized the image quality validation to use **downsampled images** and **memory-efficient algorithms**:

### Changes Made

#### 1. analyzeBrightness() - Before:
```kotlin
val pixels = IntArray(bitmap.width * bitmap.height)  // Could be 12 million pixels!
bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
```

#### 1. analyzeBrightness() - After:
```kotlin
val sampleBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)  // Only 10,000 pixels
val pixels = IntArray(10000)
sampleBitmap.getPixels(pixels, 0, 100, 0, 0, 100, 100)
// ... process ...
sampleBitmap.recycle()  // Clean up memory
```

#### 2. analyzeContrast() - Before:
```kotlin
val pixels = IntArray(bitmap.width * bitmap.height)
bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
val brightness = pixels.map { ... }  // Creates another large array
val variance = brightness.map { ... }  // Creates yet another large array
```

#### 2. analyzeContrast() - After:
```kotlin
val sampleBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
val pixels = IntArray(10000)
val brightness = FloatArray(pixels.size)  // Pre-allocated, fixed size
// Use loops instead of .map() to avoid intermediate collections
for (i in pixels.indices) { ... }
sampleBitmap.recycle()
```

### Memory Savings

**Before:**
- Full image: 4000x3000 = 12,000,000 pixels
- Pixel array: 48MB
- Brightness array: 48MB
- Variance calculations: 48MB+
- **Total: ~150MB+**

**After:**
- Sample image: 100x100 = 10,000 pixels
- Pixel array: 40KB
- Brightness array: 40KB
- **Total: ~100KB** (1,500x less memory!)

### Why This Works
1. **Downsampling**: A 100x100 sample is statistically representative for quality metrics
2. **Memory Efficiency**: Using loops instead of functional operations avoids intermediate collections
3. **Resource Cleanup**: Explicitly recycling bitmaps frees memory immediately
4. **Accuracy**: Quality metrics (brightness, contrast) don't require full resolution

## Benefits
- ✅ No more OutOfMemoryError crashes
- ✅ Faster quality validation (less data to process)
- ✅ Works with high-resolution camera images
- ✅ Maintains accuracy of quality assessments
- ✅ Lower memory footprint overall

## Testing
After this fix:
1. Take a picture with the camera
2. Click "Analyze"
3. App should process without crashing
4. Quality validation completes successfully

## Related Files
- `ImageQualityValidator.kt` - Fixed memory-intensive operations
- `ImagePreprocessor.kt` - Already uses efficient downsampling
- `AnalysisCache.kt` - Works with preprocessed images

## Prevention
To avoid similar issues in the future:
1. Always downsample large images before pixel-level operations
2. Use loops instead of functional operations for large datasets
3. Recycle bitmaps when done
4. Test with high-resolution images (4K+)
5. Monitor memory usage during development
