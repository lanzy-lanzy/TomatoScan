# 🚀 Quick Reference Card

## New Utilities at a Glance

### 📊 ModelPerformanceMonitor
```kotlin
// Get statistics
val stats = ModelPerformanceMonitor.getStatistics()
println("Avg time: ${stats.avgTotalTimeMs}ms")
println("Success rate: ${stats.successRate * 100}%")

// Log statistics
ModelPerformanceMonitor.logStatistics()

// Reset
ModelPerformanceMonitor.reset()
```

### 📚 ModelInfoProvider
```kotlin
// Full model info
val info = ModelInfoProvider.getModelInfo()

// Quick summary
val summary = ModelInfoProvider.getQuickSummary()

// Usage tips
val tips = ModelInfoProvider.getUsageRecommendations()

// Training details
val training = ModelInfoProvider.getTrainingDetails()

// Disease info
val diseases = ModelInfoProvider.getDiseaseClassInfo()
```

### 💡 AugmentationInfo
```kotlin
// Get capture recommendations
val tips = AugmentationInfo.getCaptureRecommendations()

// Get augmentation description
val description = AugmentationInfo.getAugmentationDescription()

// Access constants
val expansion = AugmentationInfo.DATASET_EXPANSION_FACTOR // 7
val epochs = AugmentationInfo.TRAINING_EPOCHS // 20
```

### 🔍 ModelDiagnostics
```kotlin
// Run full diagnostics
val report = ModelDiagnostics.runDiagnostics(context)

// Log diagnostics
ModelDiagnostics.logDiagnostics(context)

// Quick check
val status = ModelDiagnostics.quickCheck(context)
// Returns: "✅ Model setup OK" or "❌ Model setup has errors"
```

## Common Use Cases

### Show Model Info in Settings
```kotlin
binding.modelInfoTextView.text = ModelInfoProvider.getModelInfo()
```

### Display Capture Tips
```kotlin
val tips = AugmentationInfo.getCaptureRecommendations()
    .joinToString("\n") { "• $it" }
AlertDialog.Builder(this)
    .setTitle("📸 Tips for Best Results")
    .setMessage(tips)
    .show()
```

### Check Performance
```kotlin
val stats = ModelPerformanceMonitor.getStatistics()
if (stats.avgTotalTimeMs > 1000) {
    Log.w("Performance", "Slow inference detected!")
}
```

### Verify Setup on Launch
```kotlin
lifecycleScope.launch {
    val report = ModelDiagnostics.runDiagnostics(this@MainActivity)
    if (report.status == DiagnosticStatus.ERROR) {
        // Show error to user
    }
}
```

## Model Configuration

```kotlin
// Current model
ModelConfig.MODEL_VERSION // "v4.0-float32-20epochs-dual-aug"
ModelConfig.YOLO_MODEL_PATH // "best_float32.tflite"
ModelConfig.YOLO_INPUT_SIZE // 640
ModelConfig.MODEL_TRAINING_EPOCHS // 20
ModelConfig.MODEL_MAP50_95 // 0.95f (95% accuracy)

// Thresholds
ModelConfig.DETECTION_CONFIDENCE_THRESHOLD // 0.6f
ModelConfig.CONFIDENCE_THRESHOLD // 0.5f

// Disease classes (6 total)
ModelConfig.DISEASE_CLASSES // List of disease names
```

## Performance Expectations

| Metric | Value |
|--------|-------|
| Detection | 200-300ms |
| Classification | 100-200ms |
| Total | <500ms |
| Accuracy | 95%+ |
| Confidence | 70-85% avg |

## Robustness

| Factor | Tolerance |
|--------|-----------|
| Rotation | ±15° |
| Brightness | ±20% |
| Scale | ±10% |
| Position | Off-center OK |

## Quick Diagnostics

```kotlin
// In MainActivity.onCreate()
ModelDiagnostics.logDiagnostics(this)
```

Check Logcat for:
```
I/ModelDiagnostics: === Model Diagnostics Report ===
I/ModelDiagnostics: Status: OK
I/ModelDiagnostics: ✓ Model file found: best_float32.tflite
I/ModelDiagnostics: ✓ Model size: 19.50 MB
I/ModelDiagnostics: ✓ Input size matches training: 640x640
```

## Quick Performance Check

```kotlin
// After 10+ analyses
ModelPerformanceMonitor.logStatistics()
```

Check Logcat for:
```
I/ModelPerformance: === Model Performance Statistics ===
I/ModelPerformance: Avg Detection Time: 250ms
I/ModelPerformance: Avg Total Time: 450ms
I/ModelPerformance: Avg Confidence: 78.50%
I/ModelPerformance: Success Rate: 92.0%
```

## Files Reference

| File | Purpose |
|------|---------|
| `ModelPerformanceMonitor.kt` | Track performance metrics |
| `ModelInfoProvider.kt` | Display model information |
| `AugmentationInfo.kt` | Document training approach |
| `ModelDiagnostics.kt` | Verify model setup |
| `ModelConfig.kt` | Model configuration (updated) |
| `YoloLeafDetector.kt` | Detection with monitoring (updated) |
| `AnalysisPipelineImpl.kt` | Pipeline with monitoring (updated) |

## Documentation

| File | Content |
|------|---------|
| `ENHANCEMENT_SUMMARY.md` | Overview of all changes |
| `APP_ENHANCEMENTS.md` | Detailed feature docs |
| `INTEGRATION_GUIDE.md` | UI integration examples |
| `MODEL_RETRAINING_GUIDE.md` | How to retrain model |
| `QUICK_REFERENCE.md` | This file |

## Need Help?

1. **Model not loading?** → Run `ModelDiagnostics.logDiagnostics(context)`
2. **Slow performance?** → Check `ModelPerformanceMonitor.getStatistics()`
3. **Low confidence?** → Show `AugmentationInfo.getCaptureRecommendations()`
4. **Want model info?** → Use `ModelInfoProvider.getModelInfo()`

## One-Liner Checks

```kotlin
// Is model OK?
ModelDiagnostics.quickCheck(context)

// How's performance?
ModelPerformanceMonitor.getStatistics().avgTotalTimeMs

// What's the model version?
ModelConfig.MODEL_VERSION

// How many inferences?
ModelPerformanceMonitor.getStatistics().totalInferences
```

---

**That's it!** All enhancements are ready to use. Check `INTEGRATION_GUIDE.md` for UI integration examples.
