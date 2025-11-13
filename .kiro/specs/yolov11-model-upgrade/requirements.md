# Requirements Document

## Introduction

This feature focuses on upgrading the TomatoScan Android application to use a newly trained YOLOv11n model (`best_int8.tflite`) that was trained for 30 epochs on 6 tomato disease classes with improved accuracy. The upgrade includes replacing the existing model, updating the class mappings, enhancing the analysis pipeline to leverage the improved model capabilities, and ensuring the app maintains high accuracy in disease detection and classification.

## Glossary

- **YOLOv11n**: A nano (lightweight) version of the YOLOv11 object detection model optimized for mobile deployment
- **TFLite Model**: TensorFlow Lite model format optimized for mobile and embedded devices
- **INT8 Quantization**: 8-bit integer quantization technique that reduces model size and improves inference speed
- **Disease Classifier**: The ML component that identifies specific tomato leaf diseases from images
- **Analysis Pipeline**: The complete workflow from image capture to disease diagnosis and recommendation
- **Model Inference**: The process of running input data through a trained model to get predictions
- **Confidence Threshold**: Minimum probability score required to accept a prediction as valid
- **Class Mapping**: The correspondence between model output indices and disease names

## Requirements

### Requirement 1

**User Story:** As a farmer, I want the app to use the latest trained model with better accuracy, so that I can get more reliable disease diagnoses for my tomato plants

#### Acceptance Criteria

1. WHEN the Application starts, THE Disease Classifier SHALL load the new `best_int8.tflite` model from the assets directory
2. THE Disease Classifier SHALL support exactly six disease classes: Tomato_Bacterial_spot, Tomato_Early_blight, Tomato_Late_blight, Tomato_Septoria_leaf_spot, Tomato_Tomato_mosaic_virus, and Tomato_healthy
3. THE Disease Classifier SHALL map model output indices to the correct disease class names according to the training configuration
4. WHEN a model loading error occurs, THEN THE Disease Classifier SHALL provide a clear error message indicating the model file is missing or corrupted
5. THE Disease Classifier SHALL validate that the loaded model has the expected input dimensions of 640x640 pixels

### Requirement 2

**User Story:** As a developer, I want the model configuration to be centralized and version-tracked, so that I can easily manage model updates and maintain consistency across the application

#### Acceptance Criteria

1. THE ModelConfig object SHALL define the path to the new model as `best_int8.tflite`
2. THE ModelConfig object SHALL specify the input image size as 640x640 pixels to match the training configuration
3. THE ModelConfig object SHALL maintain a list of supported disease classes in the exact order used during model training
4. THE ModelConfig object SHALL include version metadata for the new model including training date and epoch count
5. WHERE model parameters need adjustment, THE ModelConfig object SHALL provide configurable thresholds for confidence and NMS

### Requirement 3

**User Story:** As a farmer, I want the app to provide accurate disease classifications with appropriate confidence levels, so that I can trust the diagnosis and take correct action

#### Acceptance Criteria

1. WHEN the Disease Classifier processes an image, THE Disease Classifier SHALL return confidence scores for all six disease classes
2. THE Disease Classifier SHALL apply a confidence threshold of 0.5 to determine if a prediction is reliable
3. IF the highest confidence score is below 0.5, THEN THE Disease Classifier SHALL mark the result as UNCERTAIN
4. THE Disease Classifier SHALL normalize confidence scores to ensure they sum to 1.0 across all classes
5. THE Disease Classifier SHALL return the top prediction along with the full probability distribution

### Requirement 4

**User Story:** As a farmer, I want the analysis to leverage the improved model accuracy, so that I receive more precise symptom descriptions and treatment recommendations

#### Acceptance Criteria

1. WHEN the Analysis Pipeline receives a classification result, THE Analysis Pipeline SHALL pass the disease class and confidence to the Gemini agent for validation
2. THE Analysis Pipeline SHALL include model version information in the diagnostic report for traceability
3. THE Gemini agent SHALL validate the model prediction against visual symptoms in the image
4. WHEN the model prediction has high confidence (above 0.7), THE Gemini agent SHALL prioritize confirming the prediction rather than overriding it
5. THE Analysis Pipeline SHALL generate formal diagnostic reports that reference the improved model accuracy

### Requirement 5

**User Story:** As a developer, I want comprehensive preprocessing for the new model, so that input images are correctly formatted and the model performs optimally

#### Acceptance Criteria

1. THE ImagePreprocessor SHALL resize input images to exactly 640x640 pixels using appropriate interpolation
2. THE ImagePreprocessor SHALL normalize pixel values to the range expected by the model (0-255 for INT8 quantized models)
3. THE ImagePreprocessor SHALL maintain aspect ratio during resizing by applying letterboxing with padding
4. THE ImagePreprocessor SHALL convert images to RGB color space if they are in a different format
5. WHEN preprocessing fails due to invalid input, THEN THE ImagePreprocessor SHALL return a clear error indicating the issue

### Requirement 6

**User Story:** As a farmer, I want the app to handle the six disease classes correctly, so that I get accurate diagnoses for all common tomato diseases

#### Acceptance Criteria

1. THE Disease Classifier SHALL correctly identify Tomato_Bacterial_spot with visual symptoms of small dark spots with yellow halos
2. THE Disease Classifier SHALL correctly identify Tomato_Early_blight with visual symptoms of concentric ring patterns on leaves
3. THE Disease Classifier SHALL correctly identify Tomato_Late_blight with visual symptoms of water-soaked lesions and white fungal growth
4. THE Disease Classifier SHALL correctly identify Tomato_Septoria_leaf_spot with visual symptoms of small circular spots with gray centers
5. THE Disease Classifier SHALL correctly identify Tomato_Tomato_mosaic_virus with visual symptoms of mottled yellow-green patterns
6. THE Disease Classifier SHALL correctly identify Tomato_healthy leaves with no disease symptoms present

### Requirement 7

**User Story:** As a developer, I want to validate the new model integration, so that I can ensure it performs better than the previous model

#### Acceptance Criteria

1. THE Application SHALL provide a mechanism to compare predictions between the old and new models for testing purposes
2. THE Application SHALL log model inference time to monitor performance improvements
3. THE Application SHALL track prediction confidence distributions to assess model reliability
4. WHEN testing with known disease samples, THE Disease Classifier SHALL achieve accuracy above 85% on the six disease classes
5. THE Application SHALL provide diagnostic information when model predictions differ significantly from expected results

### Requirement 8

**User Story:** As a farmer, I want the app to work efficiently with the new model, so that I can get quick results without draining my phone battery

#### Acceptance Criteria

1. THE Disease Classifier SHALL complete inference on a 640x640 image within 500 milliseconds on mid-range Android devices
2. THE Disease Classifier SHALL use INT8 quantization to minimize memory footprint and power consumption
3. THE Disease Classifier SHALL release model resources properly when not in use to prevent memory leaks
4. THE Application SHALL use hardware acceleration (GPU or NNAPI) when available to improve inference speed
5. WHEN multiple analyses are requested in sequence, THE Disease Classifier SHALL maintain consistent performance without degradation
