# Implementation Plan

- [x] 1. Set up configuration and constants





  - Create `GeminiConfig.kt` with deterministic parameters (temperature=0.0, topP=0.1, topK=1)
  - Create `ModelConfig.kt` with model paths and thresholds
  - Create `CacheConfig.kt` with caching parameters
  - Add configuration loading from BuildConfig and local.properties
  - _Requirements: 6.1, 6.2, 6.3, 6.4_
-

- [x] 2. Implement formal diagnostic report data models




  - [x] 2.1 Create `DiagnosticReport` data class with disease name, symptoms, confidence, and recommendation fields


    - Define fields: diseaseName, observedSymptoms, confidenceLevel, managementRecommendation, fullReport, isUncertain
    - Add timestamp and model version tracking
    - _Requirements: 3.3, 4.1, 4.2, 4.3_
  
  - [x] 2.2 Create `DiseaseClass` enum with all seven valid disease types


    - Define enum values: EARLY_BLIGHT, LATE_BLIGHT, LEAF_MOLD, SEPTORIA_LEAF_SPOT, BACTERIAL_SPECK, HEALTHY, UNCERTAIN
    - Add display name mapping for UI
    - _Requirements: 2.1, 3.4_
  
  - [x] 2.3 Create `AnalysisError` sealed class for error handling


    - Define error types: NoLeafDetected, PoorImageQuality, LowConfidence, GeminiUnavailable, InvalidImage, UnknownError
    - Add user-friendly error messages
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 3. Implement YOLOv11 leaf detector service





  - [x] 3.1 Create `LeafDetector` interface with detectLeaves and cropLeaf methods


    - Define `DetectionResult` data class with bounding box and confidence
    - Document interface contract and expected behavior
    - _Requirements: 1.1, 1.4_
  
  - [x] 3.2 Implement `YoloLeafDetector` class using TFLite or ONNX Runtime


    - Load YOLOv11 model from assets
    - Implement preprocessing for 640x640 input
    - Implement post-processing with NMS (Non-Maximum Suppression)
    - Return highest confidence detection
    - _Requirements: 1.1, 1.5_
  
  - [x] 3.3 Implement leaf cropping logic


    - Extract bounding box region from original image
    - Add padding around detected region (10% margin)
    - Handle edge cases (detection near image boundaries)
    - _Requirements: 1.1, 1.2_
-

- [x] 4. Enhance TFLite disease classifier




  - [x] 4.1 Create `DiseaseClassifier` interface


    - Define classify method returning `ClassificationResult`
    - Define getSupportedClasses method
    - _Requirements: 2.1, 2.3_
  
  - [x] 4.2 Refactor existing TFLite classifier to implement interface


    - Update to use existing `tomato_disease_model.tflite`
    - Ensure 224x224 input preprocessing
    - Return all class probabilities in result
    - _Requirements: 2.1, 2.2, 2.3, 2.5_
  
  - [x] 4.3 Implement confidence threshold validation


    - Check if confidence < 0.5 and mark as UNCERTAIN
    - Log classification results for debugging
    - _Requirements: 2.4_
-

- [x] 5. Refactor Gemini agent for formal diagnostic reports




  - [x] 5.1 Update `GeminiApi` to use deterministic generation config


    - Set temperature to 0.0
    - Set topP to 0.1
    - Set topK to 1
    - _Requirements: 3.2, 4.4_
  
  - [x] 5.2 Create formal diagnostic report prompt template

    - Include role definition: "You are a plant pathology expert"
    - Include TFLite prediction context
    - Specify exact report structure (4 components in 3-5 sentences)
    - Enforce bold formatting for disease names
    - Add examples of expected output format
    - _Requirements: 3.3, 4.1, 4.2, 4.3_
  
  - [x] 5.3 Implement report parsing and validation

    - Parse Gemini response into `DiagnosticReport` structure
    - Validate report contains all required components
    - Extract disease name, symptoms, confidence, and recommendation
    - Handle malformed responses gracefully
    - _Requirements: 3.3, 3.4_
  

  - [x] 5.4 Implement Uncertain template for poor quality images

    - Create standard template: "The analysis result is **Uncertain** due to poor image quality, lighting, or focus. A clearer photo is recommended for a more reliable diagnosis."
    - Apply when image quality validation fails or confidence is low
    - _Requirements: 5.1, 3.5_
  

  - [x] 5.5 Add isAvailable check for Gemini service

    - Verify API key is configured
    - Check network connectivity
    - Return availability status
    - _Requirements: 6.1, 6.5_
-

- [x] 6. Implement result caching for consistency




  - [x] 6.1 Create `ResultCache` interface


    - Define getCachedResult, cacheResult, and clearOldCache methods
    - _Requirements: 3.2, 4.4_
  
  - [x] 6.2 Implement perceptual image hashing


    - Use pHash algorithm to generate image fingerprints
    - Handle hash collisions with similarity threshold
    - _Requirements: 3.2, 4.4_
  
  - [x] 6.3 Create Room database entities for cache


    - Create `ResultCacheEntity` with imageHash, diagnosticReportJson, cachedAt, expiresAt
    - Create `ResultCacheDao` with query methods
    - Set TTL to 7 days
    - _Requirements: 3.2, 4.4_
  
  - [x] 6.4 Implement cache lookup and storage logic


    - Check cache before calling Gemini API
    - Store successful results in cache
    - Implement LRU eviction (max 100 entries)
    - _Requirements: 3.2, 4.4_

- [x] 7. Implement analysis pipeline orchestrator




  - [x] 7.1 Create `AnalysisPipeline` interface


    - Define analyze method for full pipeline
    - Define analyzeFallback method for TFLite-only mode
    - Define `AnalysisResult` data class
    - _Requirements: 1.1, 1.2, 2.1, 3.1_
  
  - [x] 7.2 Implement pipeline orchestration logic


    - Stage 1: Call YOLOv11 detector to crop leaf
    - Stage 2: Call TFLite classifier for preliminary prediction
    - Stage 3: Call Gemini agent for formal report
    - Handle errors at each stage with appropriate fallbacks
    - Track processing time for performance monitoring
    - _Requirements: 1.1, 1.2, 2.1, 3.1, 1.5, 2.5_
  
  - [x] 7.3 Implement fallback mode when Gemini unavailable

    - Generate basic report from TFLite prediction
    - Add disclaimer that formal validation is unavailable
    - Maintain consistent report structure
    - _Requirements: 5.3, 6.5_
  


  - [ ] 7.4 Add comprehensive error handling
    - Map errors to `AnalysisError` types


    - Provide user-friendly error messages


    - Log errors with context for debugging
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 8. Enhance image preprocessing and validation



  - [ ] 8.1 Update `ImagePreprocessor` for YOLOv11 input
    - Add preprocessForDetection method (640x640 resize)
    - Maintain existing preprocessForClassification method (224x224)



    - Normalize brightness and contrast
    - _Requirements: 1.3, 2.3_
  
  - [ ] 8.2 Enhance image quality validation
    - Check minimum resolution (224x224)
    - Detect blur using Laplacian variance
    - Check brightness levels
    - Return `QualityReport` with score and issues
    - _Requirements: 1.3, 5.1_

- [x] 9. Update ViewModel to use new pipeline


  - [x] 9.1 Refactor `TomatoScanViewModel.analyzeImage` method

    - Replace direct Gemini call with pipeline orchestrator
    - Handle `AnalysisResult` and map to `ScanResult`
    - Update loading states appropriately
    - _Requirements: 1.1, 1.2, 2.1, 3.1_
  
  - [x] 9.2 Add error state handling in ViewModel


    - Map `AnalysisError` to UI-friendly messages
    - Emit error states to UI
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  
  - [x] 9.3 Update history saving to include formal reports

    - Save `DiagnosticReport` to Room database
    - Maintain backward compatibility with existing history
    - _Requirements: 3.3, 4.1_






- [ ] 10. Update UI to display formal diagnostic reports

  - [ ] 10.1 Create `DiagnosticReportCard` composable
    - Display disease name in bold
    - Show observed symptoms section
    - Show confidence assessment


    - Show management recommendations
    - Use formal, professional styling
    - _Requirements: 4.1, 4.2, 4.3_
  


  - [x] 10.2 Update results screen to show formal report






    - Replace or enhance existing result display
    - Add section for full diagnostic paragraph
    - Maintain existing UI elements (image, confidence, etc.)
    - _Requirements: 4.1, 4.3_


  
  - [ ] 10.3 Update history screen to show report summaries
    - Display disease name and confidence in list items
    - Show full report when item is expanded or tapped


    - _Requirements: 4.1_







- [ ] 11. Add configuration and deployment setup

  - [ ] 11.1 Update build.gradle.kts with BuildConfig fields
    - Add GEMINI_API_KEY from local.properties


    - Add ENABLE_GEMINI flag
    - Add model version constants
    - _Requirements: 6.1, 6.2_
  
  - [x] 11.2 Add ProGuard rules for new components

    - Keep TFLite and ONNX Runtime classes

    - Keep Gemini SDK classes
    - Keep data model classes for serialization
    - _Requirements: 6.4_


  
  - [ ] 11.3 Add YOLOv11 model to assets
    - Download or convert YOLOv11 model to TFLite format
    - Place in `app/src/main/assets/models/` directory
    - Update ModelConfig with correct path
    - _Requirements: 6.4_

- [ ] 12. Integration and end-to-end testing

  - [ ] 12.1 Test complete pipeline with sample images
    - Test with each disease class (7 total)
    - Verify formal report format is consistent
    - Verify deterministic output (same input → same output)
    - _Requirements: 3.2, 4.4_
  
  - [ ] 12.2 Test error scenarios
    - Test with non-leaf images
    - Test with poor quality images
    - Test with Gemini API disabled
    - Test with network unavailable
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [ ] 12.3 Test caching behavior
    - Verify cache hit returns same result
    - Verify cache expiration after 7 days
    - Verify LRU eviction at 100 entries
    - _Requirements: 3.2, 4.4_
  
  - [ ] 12.4 Performance testing
    - Measure end-to-end latency (target < 5 seconds)
    - Measure memory usage during analysis
    - Test on mid-range Android device
    - _Requirements: 1.5, 2.5_
