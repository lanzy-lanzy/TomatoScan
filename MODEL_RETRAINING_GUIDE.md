# 🍅 TomatoScan Model Retraining Guide

## Overview

This guide explains how to retrain the YOLOv11 model for TomatoScan Android app using Google Colab, and how to integrate the trained model into your application.

## Current App Configuration

**Model Details:**
- Architecture: YOLOv11n (nano - 2.58M parameters)
- Input Size: 640x640 pixels
- Format: TFLite (INT8 quantized for mobile)
- Classes: 6 disease classes
- Current Model: `best_float32.tflite` (30 epochs)

**Disease Classes (in order):**
1. Index 0: Tomato_Bacterial_spot
2. Index 1: Tomato_Early_blight
3. Index 2: Tomato_Late_blight
4. Index 3: Tomato_Septoria_leaf_spot
5. Index 4: Tomato_Tomato_mosaic_virus
6. Index 5: Tomato_healthy

## Training Options

### Option 1: Quick Training (20 Epochs) - Recommended for Testing
- **Notebook**: `Tomato_Training_20_Epochs_Colab.ipynb`
- **Training Time**: ~2-3 hours on Colab GPU
- **Expected Accuracy**: 90-95% mAP50
- **Use Case**: Quick iterations, testing new augmentations

### Option 2: Production Training (30 Epochs) - Current Model
- **Notebook**: `Tomato_Training_30_Epochs_Colab.ipynb`
- **Training Time**: ~3-4 hours on Colab GPU
- **Expected Accuracy**: 95-99% mAP50
- **Use Case**: Production deployment, maximum accuracy

## Step-by-Step Retraining Process

### 1. Prepare Google Colab Environment

1. Open the training notebook in Google Colab
2. Ensure GPU runtime is enabled:
   - Runtime → Change runtime type → GPU (T4 recommended)
3. Upload your Kaggle API credentials (`kaggle.json`)

### 2. Run Training Cells

Execute cells in order:

**Step 1-4: Dataset Preparation**
- Downloads PlantVillage dataset
- Filters tomato disease classes
- Validates class names and counts

**Step 5: Manual Augmentation (Pre-Training)**
- Applies 6 augmentation types:
  - Rotation (±15°)
  - Horizontal flip
  - Zoom in/out (±10%)
  - Brightness adjustment (±20%)
  - Center crop
- Expands dataset ~7x

**Step 6-7: YOLO Format Conversion**
- Converts to YOLO format with bounding boxes
- Splits: 70% train, 20% val, 10% test
- Creates `dataset.yaml`

**Step 8: Model Training**
- Trains YOLOv11n with runtime augmentation
- Additional augmentations during training:
  - Random rotation, flips, crops
  - HSV color jitter
  - Scale variations

**Step 9-10: Validation & Export**
- Validates on test set
- Exports to multiple formats:
  - `best.pt` (PyTorch)
  - `best_int8.tflite` (Quantized - for Android)
  - `best_float32.tflite` (Full precision)
  - `best_float16.tflite` (Half precision)

**Step 11: Auto-Download**
- Downloads all models and results
- Creates comprehensive ZIP archive

### 3. Download Trained Models

After training completes, you'll have:
- `best_int8.tflite` - **Primary model for Android** (smallest, fastest)
- `best_float32.tflite` - Higher accuracy, larger size
- `results.png` - Training curves
- `confusion_matrix.png` - Performance analysis

### 4. Integrate Model into App

#### A. Add Model to Assets

```bash
# Copy the trained model to your app
cp best_int8.tflite app/src/main/assets/
# Or for higher accuracy (larger size):
cp best_float32.tflite app/src/main/assets/
```

#### B. Update ModelConfig.kt

If using INT8 model:
```kotlin
const val YOLO_MODEL_PATH = "best_int8.tflite"
const val MODEL_VERSION = "v4.0-int8-20epochs"
const val MODEL_TRAINING_EPOCHS = 20
const val MODEL_TRAINING_DATE = "2024-11-15"
```

If using Float32 model:
```kotlin
const val YOLO_MODEL_PATH = "best_float32.tflite"
const val MODEL_VERSION = "v4.0-float32-20epochs"
const val MODEL_TRAINING_EPOCHS = 20
const val MODEL_TRAINING_DATE = "2024-11-15"
```

#### C. Verify Class Order

Ensure `DISEASE_CLASSES` in ModelConfig.kt matches training order:
```kotlin
val DISEASE_CLASSES = listOf(
    "Bacterial Spot",        // Index 0
    "Early Blight",          // Index 1
    "Late Blight",           // Index 2
    "Septoria Leaf Spot",    // Index 3
    "Tomato Mosaic Virus",   // Index 4
    "Healthy"                // Index 5
)
```

#### D. Clear Cache (Important!)

When updating the model, clear the analysis cache:
```kotlin
// In your app initialization or settings
val cacheDir = File(context.cacheDir, "analysis_cache")
if (cacheDir.exists()) {
    cacheDir.deleteRecursively()
}
```

### 5. Test the New Model

#### A. Build and Install
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### B. Test with Sample Images

Test each disease class:
1. Bacterial Spot - Look for small dark spots with yellow halos
2. Early Blight - Look for concentric ring patterns
3. Late Blight - Look for water-soaked lesions
4. Septoria Leaf Spot - Look for small circular spots
5. Tomato Mosaic Virus - Look for mottled patterns
6. Healthy - Uniform green leaves

#### C. Verify Metrics

Check in logs:
- Inference time < 500ms
- Confidence scores > 0.5 for clear images
- Correct disease classification

## Training Customization

### Adjust Epochs

For faster training (lower accuracy):
```python
results = model.train(
    epochs=15,  # Reduce from 20
    # ... other params
)
```

For better accuracy (longer training):
```python
results = model.train(
    epochs=40,  # Increase from 20
    # ... other params
)
```

### Adjust Augmentation

Increase augmentation for more robustness:
```python
results = model.train(
    degrees=20.0,      # More rotation (was 15)
    scale=0.15,        # More zoom (was 0.1)
    hsv_v=0.3,         # More brightness variation (was 0.2)
    # ... other params
)
```

Decrease for faster training:
```python
results = model.train(
    degrees=10.0,      # Less rotation
    scale=0.05,        # Less zoom
    # ... other params
)
```

### Change Model Size

For better accuracy (larger model):
```python
model = YOLO('yolo11s.pt')  # Small instead of nano
# or
model = YOLO('yolo11m.pt')  # Medium
```

For faster inference (smaller model):
```python
model = YOLO('yolo11n.pt')  # Nano (current)
```

## Troubleshooting

### Issue: Low Accuracy on Specific Class

**Solution**: Collect more images of that class or increase augmentation
```python
# In Step 5, increase augmentation iterations for specific class
for aug_type in AUGMENTATIONS * 2:  # Double augmentation
    # ... augmentation code
```

### Issue: Model Too Large for APK

**Solution**: Use INT8 quantization (already default)
```python
model.export(format='tflite', int8=True, data=str(yaml_path))
```

### Issue: Slow Inference on Device

**Solutions**:
1. Use INT8 model instead of Float32
2. Enable NNAPI acceleration in app
3. Reduce input size (not recommended - affects accuracy)

### Issue: Overfitting (High train accuracy, low val accuracy)

**Solutions**:
1. Increase augmentation strength
2. Add more diverse training images
3. Reduce epochs
4. Add dropout (requires model architecture changes)

## Model Performance Comparison

| Model Type | Size | Inference Time | Accuracy | Recommended Use |
|------------|------|----------------|----------|-----------------|
| INT8 | ~5MB | ~200ms | 90-95% | Production (mobile) |
| Float16 | ~10MB | ~300ms | 93-97% | High-end devices |
| Float32 | ~20MB | ~400ms | 95-99% | Testing/validation |

## Best Practices

1. **Always validate on test set** before deploying
2. **Keep training logs** for reproducibility
3. **Version your models** with date and epoch count
4. **Test on real device** before production release
5. **Monitor inference time** on target devices
6. **Clear app cache** after model updates
7. **Backup old models** before replacing

## Next Steps After Retraining

1. ✅ Download trained model from Colab
2. ✅ Copy to `app/src/main/assets/`
3. ✅ Update `ModelConfig.kt` with new version
4. ✅ Clear analysis cache
5. ✅ Build and test app
6. ✅ Validate accuracy with test images
7. ✅ Monitor performance metrics
8. ✅ Deploy to production

## Additional Resources

- **Training Notebook**: `Tomato_Training_20_Epochs_Colab.ipynb`
- **Model Config**: `app/src/main/java/com/ml/tomatoscan/config/ModelConfig.kt`
- **Gemini Integration**: `app/src/main/java/com/ml/tomatoscan/data/GeminiApi.kt`
- **YOLOv11 Docs**: https://docs.ultralytics.com/models/yolo11/

## Questions?

Check the implementation specs:
- `.kiro/specs/yolov11-model-upgrade/requirements.md`
- `.kiro/specs/yolov11-model-upgrade/tasks.md`
