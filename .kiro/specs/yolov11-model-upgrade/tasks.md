# Implementation Plan

- [ ] 1. Prepare and add the new model file
  - Download `best_int8.tflite` from Google Colab training results
  - Place the model file in `app/src/main/assets/` directory
  - Verify file size is reasonable for APK distribution (<20MB)
  - Keep old `tomato_leaf_640.tflite` temporarily for rollback capability
  - _Requirements: 1.1_

- [x] 2. Update ModelConfig with new model metadata





  - Update `YOLO_MODEL_PATH` constant to `"best_int8.tflite"`
  - Update `DISEASE_CLASSES` list to match the 6 training classes in correct order
  - Add `MODEL_VERSION` constant as `"v2.0-30epochs"`
  - Add `MODEL_TRAINING_EPOCHS` constant as `30`
  - Add `MODEL_TRAINING_DATE` constant with training date
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 3. Update DiseaseClass enum to match new model classes





  - Remove `BACTERIAL_SPECK` enum value (not in new model)
  - Remove `LEAF_MOLD` enum value (not in new model)
  - Add `TOMATO_MOSAIC_VIRUS` enum value at index 4
  - Update `BACTERIAL_SPECK` to `BACTERIAL_SPOT` (correct name)
  - Reorder enum values to match training data: BACTERIAL_SPOT(0), EARLY_BLIGHT(1), LATE_BLIGHT(2), SEPTORIA_LEAF_SPOT(3), TOMATO_MOSAIC_VIRUS(4), HEALTHY(5)
  - Add `fromModelIndex()` companion function for clean index-to-enum mapping
  - Update display names and descriptions for each disease class
  - _Requirements: 1.2, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 4. Update AnalysisPipelineImpl class mapping




  - Locate `extractClassificationFromDetection()` method in AnalysisPipelineImpl
  - Update `diseaseClasses` list to use new enum values in correct order
  - Replace `BACTERIAL_SPECK` with `BACTERIAL_SPOT`
  - Replace `LEAF_MOLD` with `TOMATO_MOSAIC_VIRUS` at correct index
  - Ensure class order matches: [BACTERIAL_SPOT, EARLY_BLIGHT, LATE_BLIGHT, SEPTORIA_LEAF_SPOT, TOMATO_MOSAIC_VIRUS, HEALTHY]
  - Update logging to show class names for debugging
  - _Requirements: 1.2, 4.1, 4.2_

- [x] 5. Validate preprocessing for 640x640 input




  - Review `ImagePreprocessor.preprocessForDetection()` method
  - Verify it resizes images to exactly 640x640 pixels
  - Verify RGB color space is maintained (not BGR)
  - Verify pixel values are in 0-255 range for INT8 quantized models
  - Test with sample images to ensure output dimensions are correct
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_


- [x] 6. Update build configuration





  - Add BuildConfig fields for model version tracking in `app/build.gradle.kts`
  - Add `MODEL_VERSION` BuildConfig field
  - Add `MODEL_TRAINING_EPOCHS` BuildConfig field
  - Verify ProGuard rules keep TFLite classes
  - _Requirements: 2.4_

- [ ]* 7. Test model loading and inference
  - Create test to verify `best_int8.tflite` loads successfully
  - Create test to verify model input tensor is 640x640x3
  - Create test to verify model output includes 6 class probabilities
  - Test inference with a sample tomato leaf image
  - Verify detection confidence is reasonable (>0.5)
  - Verify class probabilities sum to approximately 1.0
  - _Requirements: 1.1, 1.5, 3.1, 3.2, 3.4_

- [ ]* 8. Test classification extraction with new class mapping
  - Create unit test for `extractClassificationFromDetection()` method
  - Test with mock DetectionResult containing class probabilities for each of the 6 classes
  - Verify BACTERIAL_SPOT (index 0) is correctly mapped
  - Verify EARLY_BLIGHT (index 1) is correctly mapped
  - Verify LATE_BLIGHT (index 2) is correctly mapped
  - Verify SEPTORIA_LEAF_SPOT (index 3) is correctly mapped
  - Verify TOMATO_MOSAIC_VIRUS (index 4) is correctly mapped
  - Verify HEALTHY (index 5) is correctly mapped
  - Verify invalid indices default to UNCERTAIN
  - _Requirements: 1.2, 3.1, 3.3, 3.5_

- [ ] 9. Validate accuracy with test images
  - Collect or download 10 test images for each of the 6 disease classes (60 total)
  - Run inference on all test images
  - Calculate accuracy for each disease class
  - Verify Bacterial Spot accuracy ≥80%
  - Verify Early Blight accuracy ≥80%
  - Verify Late Blight accuracy ≥80%
  - Verify Septoria Leaf Spot accuracy ≥80%
  - Verify Tomato Mosaic Virus accuracy ≥70%
  - Verify Healthy accuracy ≥90%
  - Verify overall accuracy ≥85%
  - Document any misclassifications for analysis
  - _Requirements: 7.4, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ]* 10. Performance testing and optimization
  - Measure inference time on mid-range Android device
  - Verify inference completes in <500ms
  - Measure memory footprint of loaded model
  - Verify model uses <50MB of memory
  - Run 100 consecutive inferences to check for memory leaks
  - Verify no memory degradation after multiple inferences
  - Test with hardware acceleration (GPU/NNAPI) if available
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_


- [ ]* 11. Update error handling for new model
  - Test model loading error scenario (missing file)
  - Verify appropriate error message is shown to user
  - Test invalid class index scenario
  - Verify UNCERTAIN is returned for invalid indices
  - Test low confidence scenario (<0.5)
  - Verify LowConfidence error is returned with actionable message
  - Test preprocessing error scenario (invalid image)
  - Verify InvalidImage error is returned with specific issue
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 12. Update Gemini integration for new disease classes
  - Verify Gemini agent prompt includes all 6 disease classes
  - Test Gemini validation with each disease class
  - Verify Gemini correctly validates TOMATO_MOSAIC_VIRUS predictions
  - Verify Gemini doesn't reference removed classes (BACTERIAL_SPECK, LEAF_MOLD)
  - Update AGENT.md if needed to reflect new disease classes
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 13. Clear result cache after model upgrade
  - Add migration logic to detect model version change
  - Clear cached results when model version changes
  - Update cache entries to include model version
  - Verify new predictions are cached with new model version
  - _Requirements: 2.4, 4.5_

- [ ] 14. Update UI to display new disease classes
  - Verify all disease class display names are shown correctly in UI
  - Test UI with TOMATO_MOSAIC_VIRUS classification
  - Verify removed classes (BACTERIAL_SPECK, LEAF_MOLD) don't appear
  - Update any hardcoded disease lists in UI components
  - Test history screen shows correct disease names
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ] 15. Integration testing with complete pipeline
  - Test complete analysis pipeline with new model
  - Test with images of each disease class
  - Verify detection → classification → Gemini validation flow works
  - Verify diagnostic reports reference correct disease names
  - Test fallback mode (TFLite-only without Gemini)
  - Verify error scenarios are handled gracefully
  - Test caching behavior with new model
  - _Requirements: 1.1, 1.2, 2.1, 3.1, 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 16. Documentation and cleanup
  - Update README or documentation with new model information
  - Document the 6 disease classes supported
  - Document model training configuration (30 epochs, 640x640)
  - Add comments explaining class order in code
  - Remove old model file after confirming new model works
  - Update app version number for release
  - _Requirements: 2.4, 7.1, 7.2, 7.3, 7.5_

