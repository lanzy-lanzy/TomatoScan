# Quick Test Execution Guide

## Prerequisites

1. **Connect Android Device or Start Emulator**
   ```bash
   # Check connected devices
   adb devices
   ```

2. **Ensure Configuration**
   - Gemini API key in `local.properties`:
     ```properties
     gemini.api.key=YOUR_API_KEY_HERE
     enable.gemini=true
     enable.caching=true
     ```

## Running Tests

### Option 1: Run All Integration Tests
```bash
./gradlew connectedAndroidTest
```

### Option 2: Run Test Suite
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.IntegrationTestSuite"
```

### Option 3: Run Individual Test Classes

**Pipeline Tests:**
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.AnalysisPipelineIntegrationTest"
```

**Error Scenario Tests:**
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.ErrorScenarioIntegrationTest"
```

**Cache Tests:**
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.data.ResultCacheIntegrationTest"
```

**Performance Tests:**
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.PerformanceIntegrationTest"
```

### Option 4: Run Specific Test Method
```bash
./gradlew connectedAndroidTest --tests "com.ml.tomatoscan.analysis.AnalysisPipelineIntegrationTest.testDeterministicOutput"
```

## Viewing Results

### HTML Report
After tests complete, open:
```
app/build/reports/androidTests/connected/index.html
```

### Console Output
Test results are printed to console with:
- Test execution status (PASSED/FAILED)
- Performance measurements
- Error messages (if any)

### XML Results
JUnit XML results available at:
```
app/build/outputs/androidTest-results/connected/
```

## Test Execution Time

Approximate execution times:
- **Pipeline Tests**: 2-5 minutes
- **Error Tests**: 1-3 minutes
- **Cache Tests**: 2-4 minutes
- **Performance Tests**: 3-6 minutes
- **Full Suite**: 8-18 minutes

*Times vary based on device performance and network speed*

## Troubleshooting

### Tests Won't Start
```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug assembleDebugAndroidTest

# Try again
./gradlew connectedAndroidTest
```

### Gemini API Tests Fail
- Check API key in `local.properties`
- Verify internet connection
- Check API quota limits

### Performance Tests Fail
- Run on physical device (not emulator)
- Close other apps
- Ensure device has sufficient memory

### Cache Tests Fail
- Clear app data: `adb shell pm clear com.ml.tomatoscan`
- Restart tests

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Android Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          
      - name: Run Integration Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          script: ./gradlew connectedAndroidTest
          
      - name: Upload Test Reports
        uses: actions/upload-artifact@v2
        if: always()
        with:
          name: test-reports
          path: app/build/reports/androidTests/
```

## Test Coverage Summary

| Test Class | Tests | Requirements |
|------------|-------|--------------|
| AnalysisPipelineIntegrationTest | 4 | 3.2, 4.4 |
| ErrorScenarioIntegrationTest | 5 | 5.1, 5.2, 5.3, 5.4 |
| ResultCacheIntegrationTest | 6 | 3.2, 4.4 |
| PerformanceIntegrationTest | 6 | 1.5, 2.5 |
| **Total** | **21** | **All** |

## Next Steps After Running Tests

1. **Review Results**
   - Check HTML report for detailed results
   - Review performance measurements
   - Verify all tests passed

2. **Address Failures**
   - Read error messages carefully
   - Check logs for stack traces
   - Verify prerequisites are met

3. **Performance Analysis**
   - Compare against targets (5s pipeline, 2s TFLite)
   - Identify bottlenecks
   - Optimize if needed

4. **Documentation**
   - Update test results in project docs
   - Note any device-specific issues
   - Document performance baselines

## Support

For detailed test documentation, see:
- `app/src/androidTest/java/com/ml/tomatoscan/README_TESTS.md`
- `INTEGRATION_TESTS_SUMMARY.md`

For implementation details, see:
- `.kiro/specs/tomato-leaf-analysis-agent/tasks.md`
- `.kiro/specs/tomato-leaf-analysis-agent/design.md`
