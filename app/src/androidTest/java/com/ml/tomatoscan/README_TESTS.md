# Integration Test Suite Documentation

## Overview

This directory contains comprehensive integration tests for the Tomato Leaf Analysis AI Agent system. The tests verify the complete pipeline from image input to diagnostic report generation, including error handling, caching, and performance characteristics.

## Test Structure

### 1. Analysis Pipeline Integration Tests
**File:** `analysis/AnalysisPipelineIntegrationTest.kt`

Tests the complete three-stage analysis pipeline:
- YOLOv11 leaf detection and cropping
- TFLite disease classification
- Gemini AI diagnostic report generation

**Test Cases:**
- `testPipelineWithAllDiseaseClasses()` - Verifies pipeline works for all 7 disease classes
- `testFormalReportFormatConsistency()` - Validates report structure and formatting
- `testDeterministicOutput()` - Ensures same input produces same output
- `testPipelinePerformance()` - Measures end-to-end processing time

**Requirements Tested:** 3.2, 4.4

### 2. Error Scenario Integration Tests
**File:** `analysis/ErrorScenarioIntegrationTest.kt`

Tests error handling and edge cases:
- Non-leaf image rejection
- Poor quality image detection
- Gemini API unavailability
- Network failure scenarios

**Test Cases:**
- `testNonLeafImageRejection()` - Verifies non-leaf images are rejected
- `testPoorQualityImageHandling()` - Tests low resolution and dark images
- `testGeminiApiDisabledFallback()` - Validates TFLite-only fallback mode
- `testNetworkUnavailableHandling()` - Tests graceful degradation
- `testErrorMessageQuality()` - Ensures user-friendly error messages

**Requirements Tested:** 5.1, 5.2, 5.3, 5.4

### 3. Result Cache Integration Tests
**File:** `data/ResultCacheIntegrationTest.kt`

Tests caching behavior for consistency:
- Cache hit/miss scenarios
- TTL-based expiration
- LRU eviction policy
- Access pattern tracking

**Test Cases:**
- `testCacheHitReturnsSameResult()` - Verifies cached results are identical
- `testCacheMissForDifferentImages()` - Ensures different images don't match
- `testCacheExpirationAfter7Days()` - Validates TTL expiration
- `testLRUEvictionAt100Entries()` - Tests LRU eviction at max size
- `testLRUTrackingWithAccessPatterns()` - Verifies frequently accessed items are kept
- `testCacheConsistencyAcrossMultipleRetrievals()` - Ensures consistency

**Requirements Tested:** 3.2, 4.4

### 4. Performance Integration Tests
**File:** `analysis/PerformanceIntegrationTest.kt`

Tests performance characteristics:
- End-to-end latency
- TFLite inference speed
- Memory usage
- Load testing

**Test Cases:**
- `testEndToEndLatency()` - Measures complete pipeline time (target: < 5s)
- `testTFLiteClassificationPerformance()` - Measures inference time (target: < 2s)
- `testMemoryUsageDuringAnalysis()` - Monitors memory consumption (target: < 200MB)
- `testPerformanceWithDifferentImageSizes()` - Tests various image dimensions
- `testPerformanceUnderLoad()` - Sequential analysis performance
- `testCachePerformanceImpact()` - Measures cache speedup

**Requirements Tested:** 1.5, 2.5

## Running the Tests

### Run All Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Run Specific Test Class
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.AnalysisPipelineIntegrationTest"
```

### Run Specific Test Method
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.AnalysisPipelineIntegrationTest.testDeterministicOutput"
```

### Run Test Suite
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.IntegrationTestSuite"
```

## Test Requirements

### Device Requirements
- Android device or emulator with API level 24+
- Minimum 2GB RAM
- Internet connection (for Gemini API tests)

### Configuration Requirements
- Gemini API key configured in `local.properties`
- TFLite models present in assets folder
- YOLOv11 model available (if testing detection)

### Test Data
Tests use synthetic test images (colored bitmaps) for consistency and reproducibility. In production testing, real tomato leaf images should be used for validation.

## Performance Targets

| Metric | Target | Test Environment Allowance |
|--------|--------|---------------------------|
| End-to-end latency | < 5 seconds | < 10 seconds |
| TFLite inference | < 2 seconds | < 2 seconds |
| Memory usage | < 200 MB peak | < 200 MB |
| Cache hit speedup | > 50% faster | Measured |

## Test Coverage

### Requirements Coverage Matrix

| Requirement | Test Class | Test Method |
|-------------|------------|-------------|
| 1.5 - Pipeline latency | PerformanceIntegrationTest | testEndToEndLatency |
| 2.5 - TFLite inference | PerformanceIntegrationTest | testTFLiteClassificationPerformance |
| 3.2 - Deterministic output | AnalysisPipelineIntegrationTest | testDeterministicOutput |
| 3.2 - Caching consistency | ResultCacheIntegrationTest | testCacheHitReturnsSameResult |
| 4.4 - Report format | AnalysisPipelineIntegrationTest | testFormalReportFormatConsistency |
| 4.4 - Cache consistency | ResultCacheIntegrationTest | All tests |
| 5.1 - Poor quality handling | ErrorScenarioIntegrationTest | testPoorQualityImageHandling |
| 5.2 - No leaf error | ErrorScenarioIntegrationTest | testNonLeafImageRejection |
| 5.3 - Gemini fallback | ErrorScenarioIntegrationTest | testGeminiApiDisabledFallback |
| 5.4 - Error messages | ErrorScenarioIntegrationTest | testErrorMessageQuality |

## Troubleshooting

### Tests Fail Due to Missing Models
Ensure all required models are present:
- `app/src/main/assets/tomato_disease_model.tflite`
- `app/src/main/assets/models/yolov11_tomato_leaf.tflite` (if using YOLO)

### Tests Fail Due to Gemini API
Check `local.properties` has valid API key:
```properties
gemini.api.key=YOUR_API_KEY_HERE
enable.gemini=true
```

### Performance Tests Fail
Performance tests may fail on slow emulators. Run on physical device for accurate results.

### Memory Tests Fail
Close other apps and ensure device has sufficient free memory before running tests.

## Continuous Integration

These tests are designed to run in CI/CD pipelines. Recommended configuration:

```yaml
# Example GitHub Actions workflow
- name: Run Integration Tests
  run: ./gradlew connectedAndroidTest
  
- name: Upload Test Reports
  uses: actions/upload-artifact@v2
  with:
    name: test-reports
    path: app/build/reports/androidTests/
```

## Test Maintenance

### Adding New Tests
1. Create test class in appropriate package
2. Follow naming convention: `*IntegrationTest.kt`
3. Add to `IntegrationTestSuite.kt`
4. Update this README with test documentation

### Updating Test Data
When adding real test images:
1. Place in `app/src/androidTest/assets/test_images/`
2. Update helper functions to load from assets
3. Document expected results for each image

## Known Limitations

1. **Synthetic Test Images**: Current tests use colored bitmaps instead of real leaf images. This tests the pipeline structure but not actual disease detection accuracy.

2. **Network Dependency**: Some tests require internet connection for Gemini API. Consider mocking for offline testing.

3. **Device Variability**: Performance tests may vary significantly across devices. Establish baseline on target device.

4. **Cache Timing**: Cache expiration tests manipulate timestamps rather than waiting 7 days. This tests the mechanism but not real-world timing.

## Future Improvements

- [ ] Add real tomato leaf images for validation testing
- [ ] Implement mock Gemini API for offline testing
- [ ] Add UI automation tests with Espresso
- [ ] Create performance benchmarking suite
- [ ] Add stress testing for concurrent analyses
- [ ] Implement visual regression testing for UI components
