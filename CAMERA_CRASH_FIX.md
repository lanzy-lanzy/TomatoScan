# Camera Capture Crash Fix

## Problem
The app was crashing when users:
1. Took a picture using the camera
2. Clicked the "Analyze" button
3. App would close/crash unexpectedly

## Root Cause
The issue was caused by Android's `ImageDecoder.decodeBitmap()` creating HARDWARE bitmaps by default on Android 9+ (API 28+). When these HARDWARE bitmaps were passed to our image preprocessing pipeline, they failed because:

1. HARDWARE bitmaps don't support pixel access operations like `getPixels()`
2. Our preprocessing code (`ImagePreprocessor`) needs pixel access for:
   - Color normalization
   - Contrast enhancement
   - Image hashing
3. Our quality validation code (`ImageQualityValidator`) needs pixel access for quality checks

## Solution
Fixed all image loading points to force software bitmap allocation using `ImageDecoder.ALLOCATOR_SOFTWARE`.

## Files Modified

### 1. CaptureImageScreen.kt
**Location**: Camera capture result handler

**Change**: Added software allocator and error handling
```kotlin
ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
}
```

**Benefits**:
- Prevents HARDWARE bitmap creation from camera
- Adds fallback error handling
- Ensures bitmap is processable

### 2. AnalysisScreen.kt (3 locations)
**Locations**: 
- `onAnalyze` callback in main screen
- `ImagePreview` composable
- `AnalysisInProgressScreen` composable

**Change**: Applied software allocator to all ImageDecoder calls
```kotlin
ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
}
```

**Benefits**:
- Consistent bitmap format throughout the app
- Works with both camera and gallery images
- Prevents crashes during analysis

## How It Works

### Before Fix:
```
Camera → HARDWARE Bitmap → Preprocessing → CRASH (pixel access denied)
```

### After Fix:
```
Camera → Software Bitmap → Preprocessing → Success ✓
Gallery → Software Bitmap → Preprocessing → Success ✓
```

## Technical Details

### ImageDecoder.ALLOCATOR_SOFTWARE
- Forces bitmap to be stored in app memory (not GPU)
- Allows full pixel access for processing
- Slightly more memory usage but necessary for our use case
- No noticeable performance impact for our image sizes

### Backward Compatibility
- Android < 9 (API < 28): Uses `MediaStore.Images.Media.getBitmap()` (already software)
- Android ≥ 9 (API ≥ 28): Uses `ImageDecoder` with software allocator
- Both paths now produce processable bitmaps

## Testing Checklist
After this fix, verify:
- ✅ Camera capture works without crashes
- ✅ Gallery upload works without crashes
- ✅ Image preprocessing completes successfully
- ✅ Quality validation runs without errors
- ✅ Analysis results are consistent
- ✅ No "pixel access not supported" errors

## Related Fixes
This fix works together with:
1. **ImagePreprocessor.kt**: Converts any remaining HARDWARE bitmaps as a safety net
2. **ImageQualityValidator.kt**: Handles HARDWARE bitmaps in validation
3. **AnalysisCache.kt**: Uses preprocessed (software) bitmaps for caching

## Performance Impact
- **Memory**: Minimal increase (software bitmaps use ~4 bytes per pixel)
- **Speed**: No noticeable difference for typical image sizes (< 5MB)
- **Battery**: Negligible impact
- **User Experience**: Significantly improved (no crashes!)

## Prevention
To prevent similar issues in the future:
1. Always use `ALLOCATOR_SOFTWARE` when pixel access is needed
2. Add HARDWARE bitmap detection in preprocessing pipeline (already done)
3. Test on Android 9+ devices with camera capture
4. Monitor crash reports for bitmap-related errors
