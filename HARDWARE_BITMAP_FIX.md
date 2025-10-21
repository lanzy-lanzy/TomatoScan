# Hardware Bitmap Fix

## Problem
The app was crashing with the error:
```
Unable to analyze the image: unable to getPixels(), pixel access is not supported on Config#HARDWARE bitmaps
```

## Root Cause
Android's `Bitmap.Config.HARDWARE` is a special configuration that stores bitmaps in GPU memory for better performance. However, this configuration doesn't allow direct pixel access through methods like `getPixels()`, which our image preprocessing and quality validation code requires.

## Solution
Added automatic conversion from HARDWARE bitmaps to software-accessible bitmaps (ARGB_8888) at the beginning of the processing pipeline.

## Changes Made

### 1. ImagePreprocessor.kt
- Added `convertToSoftwareBitmap()` function to detect and convert HARDWARE bitmaps
- Updated `preprocessForAnalysis()` to convert bitmap first before any processing
- Updated `generateImageHash()` to handle HARDWARE bitmaps

```kotlin
private fun convertToSoftwareBitmap(bitmap: Bitmap): Bitmap {
    if (bitmap.config != Bitmap.Config.HARDWARE) {
        return bitmap
    }
    val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    return softwareBitmap ?: bitmap
}
```

### 2. ImageQualityValidator.kt
- Added HARDWARE bitmap detection and conversion at the start of validation
- All pixel access operations now use the converted software bitmap

## Benefits
- ✅ Fixes the crash when analyzing images
- ✅ Maintains performance for non-HARDWARE bitmaps
- ✅ Transparent to the rest of the codebase
- ✅ No changes needed in UI or ViewModel code

## Testing
After this fix, the app should:
1. Successfully analyze images from camera
2. Successfully analyze uploaded images
3. No longer crash with "pixel access not supported" error
4. Maintain all consistency and accuracy features

## Technical Details
- HARDWARE bitmaps are automatically created by Android when loading images in certain scenarios
- The conversion to ARGB_8888 happens only once at the start of processing
- The conversion is fast and doesn't significantly impact performance
- Original bitmap is not modified; a copy is created only when needed
