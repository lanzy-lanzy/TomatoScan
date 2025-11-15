# 🚀 Quick Integration Guide

## Overview

This guide shows how to integrate the new enhancements into your TomatoScan app UI.

## 1. Add Model Info to Settings/About Screen

```kotlin
// In your SettingsActivity or AboutActivity
import com.ml.tomatoscan.utils.ModelInfoProvider

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Display model information
        val modelInfo = ModelInfoProvider.getModelInfo()
        binding.modelInfoTextView.text = modelInfo
        
        // Or show quick summary in a card
        val quickSummary = ModelInfoProvider.getQuickSummary()
        binding.modelSummaryTextView.text = quickSummary
    }
}
```

## 2. Show Capture Tips Before Camera

```kotlin
// In your CameraActivity or before opening camera
import com.ml.tomatoscan.utils.AugmentationInfo

class CameraActivity : AppCompatActivity() {
    private fun showCaptureTips() {
        val recommendations = AugmentationInfo.getCaptureRecommendations()
        
        val tipsDialog = AlertDialog.Builder(this)
            .setTitle("📸 Tips for Best Results")
            .setMessage(recommendations.joinToString("\n\n") { "• $it" })
            .setPositiveButton("Got it!") { dialog, _ -> dialog.dismiss() }
            .create()
        
        tipsDialog.show()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show tips on first launch or when user clicks help button
        binding.helpButton.setOnClickListener {
            showCaptureTips()
        }
    }
}
```

## 3. Display Performance Statistics (Developer Mode)

```kotlin
// In your SettingsActivity or DeveloperOptionsActivity
import com.ml.tomatoscan.utils.ModelPerformanceMonitor

class DeveloperOptionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show performance statistics
        binding.showStatsButton.setOnClickListener {
            val stats = ModelPerformanceMonitor.getStatistics()
            
            val statsText = """
                Performance Statistics:
                
                Avg Detection Time: ${stats.avgDetectionTimeMs.toInt()}ms
                Avg Classification Time: ${stats.avgClassificationTimeMs.toInt()}ms
                Avg Total Time: ${stats.avgTotalTimeMs.toInt()}ms
                
                Avg Confidence: ${String.format("%.1f%%", stats.avgConfidence * 100)}
                Confidence Range: ${String.format("%.1f%%", stats.minConfidence * 100)} - ${String.format("%.1f%%", stats.maxConfidence * 100)}
                
                Success Rate: ${String.format("%.1f%%", stats.successRate * 100)}
                Total Inferences: ${stats.totalInferences}
                Sample Size: ${stats.sampleSize}
            """.trimIndent()
            
            binding.statsTextView.text = statsText
        }
        
        // Reset statistics
        binding.resetStatsButton.setOnClickListener {
            ModelPerformanceMonitor.reset()
            Toast.makeText(this, "Statistics reset", Toast.LENGTH_SHORT).show()
        }
    }
}
```

## 4. Run Diagnostics on App Start

```kotlin
// In your MainActivity or Application class
import com.ml.tomatoscan.utils.ModelDiagnostics

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Run diagnostics in background
        lifecycleScope.launch(Dispatchers.IO) {
            val report = ModelDiagnostics.runDiagnostics(this@MainActivity)
            
            withContext(Dispatchers.Main) {
                when (report.status) {
                    DiagnosticStatus.ERROR -> {
                        // Show error dialog
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Model Setup Error")
                            .setMessage("There are issues with the model setup:\n\n${report.issues.joinToString("\n")}")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    DiagnosticStatus.WARNING -> {
                        // Log warnings
                        Log.w("MainActivity", "Model warnings: ${report.warnings}")
                    }
                    DiagnosticStatus.OK -> {
                        // All good
                        Log.i("MainActivity", "Model setup OK")
                    }
                }
            }
        }
    }
}
```

## 5. Show Model Info in Results Screen

```kotlin
// In your ResultsActivity
import com.ml.tomatoscan.utils.ModelInfoProvider

class ResultsActivity : AppCompatActivity() {
    private fun displayResults(diagnosticReport: DiagnosticReport) {
        // Show main results
        binding.diseaseNameTextView.text = diagnosticReport.diseaseName
        binding.reportTextView.text = diagnosticReport.fullReport
        
        // Add model version info at bottom
        val modelVersion = "Analyzed with ${ModelConfig.MODEL_VERSION}"
        binding.modelVersionTextView.text = modelVersion
        
        // Show confidence interpretation
        val confidence = diagnosticReport.confidenceLevel
        val interpretation = when {
            confidence.contains("70") || confidence.contains("80") || confidence.contains("90") -> 
                "High confidence - Reliable diagnosis"
            confidence.contains("50") || confidence.contains("60") -> 
                "Medium confidence - Consider retaking photo"
            else -> 
                "Low confidence - Please retake with better lighting"
        }
        binding.confidenceInterpretationTextView.text = interpretation
    }
}
```

## 6. Add "About Model" Dialog

```kotlin
// Reusable dialog for showing model information
import com.ml.tomatoscan.utils.ModelInfoProvider

fun showModelInfoDialog(context: Context) {
    val modelInfo = ModelInfoProvider.getModelInfo()
    val trainingDetails = ModelInfoProvider.getTrainingDetails()
    
    val dialog = AlertDialog.Builder(context)
        .setTitle("🍅 About the Model")
        .setMessage("$modelInfo\n\n$trainingDetails")
        .setPositiveButton("Close", null)
        .setNeutralButton("Usage Tips") { _, _ ->
            showUsageTipsDialog(context)
        }
        .create()
    
    dialog.show()
}

fun showUsageTipsDialog(context: Context) {
    val tips = ModelInfoProvider.getUsageRecommendations()
    
    AlertDialog.Builder(context)
        .setTitle("📸 Usage Recommendations")
        .setMessage(tips)
        .setPositiveButton("Got it!", null)
        .show()
}
```

## 7. Add Performance Monitoring to Analysis

The performance monitoring is already integrated! It automatically tracks:
- Detection times
- Classification times
- Total pipeline times
- Confidence scores
- Success/failure rates

Statistics are logged every 10 successful inferences. Check your Logcat for:
```
I/ModelPerformance: === Model Performance Statistics ===
I/ModelPerformance: Avg Detection Time: 250ms
I/ModelPerformance: Avg Classification Time: 150ms
I/ModelPerformance: Avg Total Time: 450ms
I/ModelPerformance: Avg Confidence: 85.50%
I/ModelPerformance: Confidence Range: 65.00% - 95.00%
I/ModelPerformance: Success Rate: 92.0%
I/ModelPerformance: Total Inferences: 20 (20 samples)
I/ModelPerformance: ===================================
```

## 8. Create a "Model Info" Menu Item

```kotlin
// In your main activity
override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.main_menu, menu)
    return true
}

override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
        R.id.action_model_info -> {
            showModelInfoDialog(this)
            true
        }
        R.id.action_performance_stats -> {
            showPerformanceStatsDialog(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}

private fun showPerformanceStatsDialog(context: Context) {
    val stats = ModelPerformanceMonitor.getStatistics()
    val statsText = """
        Performance Statistics:
        
        Avg Total Time: ${stats.avgTotalTimeMs.toInt()}ms
        Avg Confidence: ${String.format("%.1f%%", stats.avgConfidence * 100)}
        Success Rate: ${String.format("%.1f%%", stats.successRate * 100)}
        Total Analyses: ${stats.totalInferences}
    """.trimIndent()
    
    AlertDialog.Builder(context)
        .setTitle("📊 Performance Stats")
        .setMessage(statsText)
        .setPositiveButton("Close", null)
        .setNeutralButton("Reset") { _, _ ->
            ModelPerformanceMonitor.reset()
            Toast.makeText(context, "Stats reset", Toast.LENGTH_SHORT).show()
        }
        .show()
}
```

## 9. Add to menu XML

Create or update `res/menu/main_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/action_model_info"
        android:title="Model Info"
        android:icon="@drawable/ic_info" />
    
    <item
        android:id="@+id/action_performance_stats"
        android:title="Performance Stats"
        android:icon="@drawable/ic_stats" />
    
    <item
        android:id="@+id/action_capture_tips"
        android:title="Capture Tips"
        android:icon="@drawable/ic_help" />
</menu>
```

## 10. Test the Integration

```kotlin
// In your test or debug activity
class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Test 1: Run diagnostics
        binding.testDiagnosticsButton.setOnClickListener {
            ModelDiagnostics.logDiagnostics(this)
            val status = ModelDiagnostics.quickCheck(this)
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        }
        
        // Test 2: Show model info
        binding.testModelInfoButton.setOnClickListener {
            val info = ModelInfoProvider.getModelInfo()
            Log.d("Debug", info)
        }
        
        // Test 3: Check performance stats
        binding.testPerformanceButton.setOnClickListener {
            ModelPerformanceMonitor.logStatistics()
        }
        
        // Test 4: Show augmentation info
        binding.testAugmentationButton.setOnClickListener {
            val augInfo = AugmentationInfo.getAugmentationDescription()
            Log.d("Debug", augInfo)
        }
    }
}
```

## Summary

You now have:

✅ **Model information display** - Show users what model is being used
✅ **Performance monitoring** - Track and display inference statistics
✅ **Capture recommendations** - Help users take better photos
✅ **Diagnostics** - Verify model setup on app start
✅ **Enhanced reports** - More informative diagnostic reports
✅ **Developer tools** - Performance stats and debugging

All enhancements are backward compatible and don't break existing functionality!
