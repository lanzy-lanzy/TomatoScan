# Analysis Pipeline Orchestrator

This package contains the implementation of the complete tomato leaf analysis pipeline that orchestrates YOLOv11 detection, TFLite classification, and Gemini AI validation.

## Components

### AnalysisPipeline (Interface)
The main interface defining the contract for the analysis pipeline.

**Methods:**
- `analyze(inputImage: Bitmap): AnalysisResult` - Executes the complete three-stage pipeline
- `analyzeFallback(inputImage: Bitmap): AnalysisResult` - Executes TFLite-only mode without Gemini

### AnalysisPipelineImpl (Implementation)
The concrete implementation of the analysis pipeline.

**Pipeline Stages:**
1. **Image Quality Validation** - Validates image meets minimum quality requirements
2. **Leaf Detection** - YOLOv11 detects and crops tomato leaf region
3. **Disease Classification** - TFLite classifies disease from cropped leaf
4. **Diagnostic Report Generation** - Gemini validates and generates formal report

**Features:**
- Result caching for consistency
- Comprehensive error handling at each stage
- Performance monitoring (processing time tracking)
- Graceful fallback when Gemini is unavailable
- Detailed logging for debugging

### AnalysisResult (Data Class)
Contains the complete results from all pipeline stages.

**Properties:**
- `success: Boolean` - Whether analysis completed successfully
- `detectionResult: DetectionResult?` - YOLOv11 detection result
- `classificationResult: ClassificationResult?` - TFLite classification result
- `diagnosticReport: DiagnosticReport?` - Gemini diagnostic report
- `error: AnalysisError?` - Error information if failed
- `processingTimeMs: Long` - Total processing time

### AnalysisErrorHandler (Utility)
Comprehensive error handling and logging utility.

**Features:**
- User-friendly error messages
- Detailed logging with context
- Error severity categorization
- Actionable suggestions for users
- Error report generation for debugging

## Usage Example

```kotlin
// Initialize dependencies
val leafDetector = YoloLeafDetector(context)
val diseaseClassifier = TFLiteDiseaseClassifier(context)
val geminiApi = GeminiApi(context)
val resultCache = ResultCacheImpl(context)

// Create pipeline
val pipeline = AnalysisPipelineImpl(
    context = context,
    leafDetector = leafDetector,
    diseaseClassifier = diseaseClassifier,
    geminiApi = geminiApi,
    resultCache = resultCache
)

// Analyze image
val result = pipeline.analyze(bitmap)

if (result.success) {
    // Display diagnostic report
    val report = result.diagnosticReport
    println("Disease: ${report?.diseaseName}")
    println("Report: ${report?.fullReport}")
} else {
    // Handle error
    val error = result.error
    val message = AnalysisErrorHandler.getUserFriendlyMessage(error!!)
    val suggestions = AnalysisErrorHandler.getActionableSuggestions(error)
    
    println("Error: $message")
    suggestions.forEach { println("- $it") }
}
```

## Error Handling

The pipeline handles the following error types:

1. **NoLeafDetected** - No tomato leaf found in image
2. **PoorImageQuality** - Image quality insufficient for analysis
3. **LowConfidence** - Classification confidence below threshold (0.5)
4. **GeminiUnavailable** - Gemini API unavailable (network/quota issues)
5. **InvalidImage** - Image file is invalid or corrupted
6. **UnknownError** - Unexpected error occurred

Each error includes:
- User-friendly message for UI display
- Detailed logging with context
- Actionable suggestions for resolution
- Severity level (Recoverable, Warning, Critical)

## Performance Targets

- **Detection Stage**: < 500ms
- **Classification Stage**: < 200ms
- **Report Generation**: < 3 seconds (network dependent)
- **Total Pipeline**: < 5 seconds end-to-end

## Requirements Addressed

- **1.1**: Process images through YOLOv11 detector to identify and crop leaf regions
- **1.2**: Pass cropped image to TFLite classifier for preliminary disease prediction
- **2.1**: Classify disease from validated set of conditions
- **3.1**: Gemini receives cropped image and preliminary classification for validation
- **1.5**: Complete pipeline within 5 seconds per image
- **2.5**: Execute TFLite inference within 2 seconds
- **5.1**: Provide clear feedback for poor image quality
- **5.2**: Provide user-friendly error message when no leaf detected
- **5.3**: Fall back to TFLite prediction with disclaimer when Gemini fails
- **5.4**: Log all errors with timestamps and input metadata
- **5.5**: Inform user when network connectivity is unavailable
- **6.5**: Operate in TFLite-only mode when Gemini API is unavailable

## Testing

To test the pipeline:

1. **Unit Tests**: Test each stage independently with mock dependencies
2. **Integration Tests**: Test complete pipeline flow with real models
3. **Error Tests**: Test error handling for each error type
4. **Performance Tests**: Verify processing time meets targets

## Future Enhancements

- Batch processing support for multiple images
- Parallel processing of detection and classification
- Advanced caching strategies (memory + disk)
- Retry logic with exponential backoff
- Telemetry and analytics integration
