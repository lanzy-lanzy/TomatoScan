# Design Document: YOLOv11 Model Upgrade

## Overview

This design document outlines the integration of a newly trained YOLOv11n model (`best_int8.tflite`) into the TomatoScan Android application. The new model was trained for 30 epochs on 6 tomato disease classes using the PlantVillage dataset, achieving improved accuracy over the previous model.

### Key Improvements

1. **Better Accuracy**: 30-epoch training with data augmentation provides more reliable disease detection
2. **INT8 Quantization**: Reduced model size and faster inference with minimal accuracy loss
3. **Unified Architecture**: Single model handles both detection and classification
4. **Six Disease Classes**: Focused on the most common tomato diseases for practical farming use

### Training Configuration Reference

- **Model**: YOLOv11n (nano - optimized for mobile)
- **Epochs**: 30
- **Image Size**: 640x640
- **Batch Size**: 16
- **Dataset**: PlantVillage tomato disease images
- **Classes**: Bacterial_spot, Early_blight, Late_blight, Septoria_leaf_spot, Tomato_mosaic_virus, Healthy

## Architecture

### System Flow

```
User Image Input
      ↓
[ImagePreprocessor] → Resize to 640x640, normalize
      ↓
[LeafDetector] → Load best_int8.tflite, run inference
      ↓
[DetectionResult] → Bounding box + class probabilities
      ↓
[AnalysisPipeline] → Extract classification from detection
      ↓
[GeminiApi] → Validate and generate report
      ↓
Final Diagnostic Report
```

### Changes Required

1. **Model File**: Replace `tomato_leaf_640.tflite` with `best_int8.tflite` in assets
2. **Class Mapping**: Update disease class indices to match new training order
3. **Configuration**: Update `ModelConfig.kt` with new model metadata
4. **Data Model**: Update `DiseaseClass` enum to match 6 classes
5. **Pipeline**: Update `extractClassificationFromDetection()` method


## Components and Interfaces

### 1. ModelConfig

**File**: `app/src/main/java/com/ml/tomatoscan/config/ModelConfig.kt`

**Changes**:
```kotlin
object ModelConfig {
    const val YOLO_MODEL_PATH = "best_int8.tflite"  // Updated
    const val YOLO_INPUT_SIZE = 640  // Already correct
    
    // Updated disease classes to match training order
    val DISEASE_CLASSES = listOf(
        "Bacterial Spot",
        "Early Blight",
        "Late Blight",
        "Septoria Leaf Spot",
        "Tomato Mosaic Virus",
        "Healthy"
    )
    
    // New metadata
    const val MODEL_VERSION = "v2.0-30epochs"
    const val MODEL_TRAINING_EPOCHS = 30
}
```

### 2. DiseaseClass Enum

**File**: `app/src/main/java/com/ml/tomatoscan/models/DiseaseClass.kt`

**Changes**:
```kotlin
enum class DiseaseClass(val displayName: String) {
    BACTERIAL_SPOT("Bacterial Spot"),           // Index 0
    EARLY_BLIGHT("Early Blight"),               // Index 1
    LATE_BLIGHT("Late Blight"),                 // Index 2
    SEPTORIA_LEAF_SPOT("Septoria Leaf Spot"),   // Index 3
    TOMATO_MOSAIC_VIRUS("Tomato Mosaic Virus"), // Index 4
    HEALTHY("Healthy"),                         // Index 5
    UNCERTAIN("Uncertain");
    
    companion object {
        fun fromModelIndex(index: Int): DiseaseClass {
            return when (index) {
                0 -> BACTERIAL_SPOT
                1 -> EARLY_BLIGHT
                2 -> LATE_BLIGHT
                3 -> SEPTORIA_LEAF_SPOT
                4 -> TOMATO_MOSAIC_VIRUS
                5 -> HEALTHY
                else -> UNCERTAIN
            }
        }
    }
}
```

**Removed Classes**: BACTERIAL_SPECK, LEAF_MOLD (not in new model)
**Added Classes**: TOMATO_MOSAIC_VIRUS (new in training data)


### 3. AnalysisPipelineImpl

**File**: `app/src/main/java/com/ml/tomatoscan/analysis/AnalysisPipelineImpl.kt`

**Method to Update**: `extractClassificationFromDetection()`

**Current Code**:
```kotlin
val diseaseClasses = listOf(
    DiseaseClass.BACTERIAL_SPECK,     // Wrong
    DiseaseClass.EARLY_BLIGHT,
    DiseaseClass.LATE_BLIGHT,
    DiseaseClass.LEAF_MOLD,           // Wrong
    DiseaseClass.SEPTORIA_LEAF_SPOT,
    DiseaseClass.HEALTHY
)
```

**Updated Code**:
```kotlin
val diseaseClasses = listOf(
    DiseaseClass.BACTERIAL_SPOT,       // Index 0
    DiseaseClass.EARLY_BLIGHT,         // Index 1
    DiseaseClass.LATE_BLIGHT,          // Index 2
    DiseaseClass.SEPTORIA_LEAF_SPOT,   // Index 3
    DiseaseClass.TOMATO_MOSAIC_VIRUS,  // Index 4
    DiseaseClass.HEALTHY               // Index 5
)
```

### 4. LeafDetector

**File**: Likely `app/src/main/java/com/ml/tomatoscan/ml/YoloLeafDetector.kt`

**Changes Needed**:
- Update model loading to use `ModelConfig.YOLO_MODEL_PATH` (which now points to `best_int8.tflite`)
- Verify INT8 quantization handling is correct
- Ensure output parsing extracts 6 class probabilities

**No code changes required** if the detector already uses `ModelConfig.YOLO_MODEL_PATH` dynamically.

### 5. ImagePreprocessor

**File**: `app/src/main/java/com/ml/tomatoscan/utils/ImagePreprocessor.kt`

**Current Status**: Already supports 640x640 preprocessing

**Validation Needed**:
- Verify `preprocessForDetection()` outputs exactly 640x640
- Verify RGB color space is maintained
- Verify pixel values are in 0-255 range for INT8 models

**No changes required** - existing implementation is compatible.


## Data Models

### DetectionResult
```kotlin
data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float,
    val classProbabilities: Map<Int, Float>?
)
```

### ClassificationResult
```kotlin
data class ClassificationResult(
    val diseaseClass: DiseaseClass,
    val confidence: Float,
    val allProbabilities: Map<DiseaseClass, Float>
)
```

### DiagnosticReport
```kotlin
data class DiagnosticReport(
    val diseaseName: String,
    val observedSymptoms: String,
    val confidenceLevel: String,
    val managementRecommendation: String,
    val fullReport: String,
    val isUncertain: Boolean,
    val timestamp: Long,
    val modelVersion: String
)
```

## Error Handling

### Model Loading Errors
- **Scenario**: `best_int8.tflite` missing or corrupted
- **Handling**: Catch IOException, log error, show user-friendly message
- **Recovery**: Prompt user to reinstall app

### Class Mapping Errors
- **Scenario**: Model outputs invalid class index (not 0-5)
- **Handling**: Default to UNCERTAIN, log warning
- **Recovery**: Continue with uncertain classification

### Low Confidence Predictions
- **Scenario**: Confidence < 0.5
- **Handling**: Return LowConfidence error
- **Recovery**: Suggest retaking photo with better lighting

### Preprocessing Errors
- **Scenario**: Invalid input image
- **Handling**: Validate before preprocessing, return InvalidImage error
- **Recovery**: Provide actionable feedback to user


## Testing Strategy

### Unit Tests

1. **ModelConfig Tests**
   - Verify DISEASE_CLASSES has 6 entries
   - Verify YOLO_MODEL_PATH = "best_int8.tflite"
   - Verify YOLO_INPUT_SIZE = 640

2. **DiseaseClass Tests**
   - Test fromModelIndex() for indices 0-5
   - Test fromModelIndex() returns UNCERTAIN for invalid indices
   - Verify display names are correct

3. **ImagePreprocessor Tests**
   - Test preprocessForDetection() outputs 640x640
   - Test RGB color space is maintained
   - Test normalization doesn't clip values

### Integration Tests

1. **Model Loading Test**
   - Verify model loads without errors
   - Verify interpreter is initialized

2. **Inference Test**
   - Run inference on sample images
   - Verify detection confidence > 0.5
   - Verify class probabilities are present

3. **Classification Extraction Test**
   - Test extractClassificationFromDetection()
   - Verify correct class is selected
   - Verify confidence matches highest probability

### Accuracy Validation

**Test Dataset**: 60 images (10 per class) from validation set

**Target Metrics**:
- Bacterial Spot: ≥80% accuracy
- Early Blight: ≥80% accuracy
- Late Blight: ≥80% accuracy
- Septoria Leaf Spot: ≥80% accuracy
- Tomato Mosaic Virus: ≥70% accuracy
- Healthy: ≥90% accuracy
- **Overall**: ≥85% accuracy

### Performance Tests

**Target Metrics**:
- Inference time: <500ms on mid-range devices
- Memory footprint: <50MB for model
- No memory leaks after 100 inferences

**Test Procedure**:
1. Run 10 inferences and measure average time
2. Monitor memory before/after model loading
3. Run 100 consecutive inferences and check for leaks


## Deployment Considerations

### Model File Placement

1. **Location**: `app/src/main/assets/best_int8.tflite`
2. **Size**: Verify file size is reasonable for APK (<20MB)
3. **Backup**: Keep old model temporarily for rollback if needed

### Build Configuration

**File**: `app/build.gradle.kts`

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
    
    defaultConfig {
        buildConfigField("String", "MODEL_VERSION", "\"v2.0-30epochs\"")
        buildConfigField("int", "MODEL_TRAINING_EPOCHS", "30")
    }
}
```

### ProGuard Rules

Ensure TFLite classes are not obfuscated:

```proguard
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
```

### Migration Strategy

1. **Phase 1**: Add new model file alongside old model
2. **Phase 2**: Update code to use new model
3. **Phase 3**: Test thoroughly with validation dataset
4. **Phase 4**: Remove old model file after confirming new model works
5. **Phase 5**: Update app version and release

### Rollback Plan

If new model performs worse:
1. Revert `ModelConfig.YOLO_MODEL_PATH` to old model name
2. Revert `DiseaseClass` enum changes
3. Revert `extractClassificationFromDetection()` changes
4. Keep both model files in assets temporarily


## Implementation Notes

### Class Order Verification

The class order MUST match the training data. Based on the Colab notebook, the PlantVillage dataset classes are:

1. Tomato_Bacterial_spot → Index 0
2. Tomato_Early_blight → Index 1
3. Tomato_Late_blight → Index 2
4. Tomato_Septoria_leaf_spot → Index 3
5. Tomato_Tomato_mosaic_virus → Index 4
6. Tomato_healthy → Index 5

**Critical**: Verify this order by testing with known disease samples.

### INT8 Quantization Handling

The `best_int8.tflite` model uses INT8 quantization:
- **Input**: May need to be scaled from [0, 255] to INT8 range
- **Output**: May need to be dequantized from INT8 to Float
- **TFLite Interpreter**: Handles quantization/dequantization automatically if configured correctly

**Verification**: Check model metadata using TFLite Model Analyzer or Netron.

### Gemini Integration

No changes needed to Gemini integration:
- Gemini receives the same `ClassificationResult` structure
- Disease names are updated automatically via `DiseaseClass.displayName`
- Gemini will validate against the new 6 disease classes

### Caching Considerations

Result cache should continue to work:
- Cache key is based on image hash (unchanged)
- Cached reports reference disease names (updated automatically)
- Consider clearing cache after model upgrade to avoid confusion

### Logging and Debugging

Add detailed logging for model upgrade:
```kotlin
Log.d(TAG, "Model version: ${ModelConfig.MODEL_VERSION}")
Log.d(TAG, "Training epochs: ${ModelConfig.MODEL_TRAINING_EPOCHS}")
Log.d(TAG, "Supported classes: ${ModelConfig.DISEASE_CLASSES}")
```

Log class probabilities for debugging:
```kotlin
classProbabilities.entries.sortedBy { it.key }.forEach { (idx, prob) ->
    Log.d(TAG, "Class $idx: ${String.format("%.4f", prob)}")
}
```

