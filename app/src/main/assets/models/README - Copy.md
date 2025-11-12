# ML Models Directory

This directory contains the machine learning models used by the Tomato Leaf Analysis system.

## Required Models

### YOLOv11 Leaf Detector
- **File name**: `yolov11_tomato_leaf.tflite`
- **Purpose**: Detects and crops tomato leaf regions from images
- **Input size**: 640x640 pixels
- **Format**: TensorFlow Lite (.tflite)
- **Model path**: `models/yolov11_tomato_leaf.tflite`

#### How to Add the YOLOv11 Model

The YOLOv11 model file needs to be added to this directory. Follow these steps:

1. **Train or Obtain a YOLOv11 Model**
   - Train a YOLOv11 model on tomato leaf detection dataset
   - Or obtain a pre-trained model for leaf/plant detection
   - Ensure the model is trained to detect tomato leaves specifically

2. **Export to TensorFlow Lite Format**
   ```bash
   # Using Ultralytics YOLOv11
   from ultralytics import YOLO
   
   # Load your trained model
   model = YOLO('path/to/your/yolov11_model.pt')
   
   # Export to TFLite format
   model.export(format='tflite', imgsz=640)
   ```

3. **Place the Model File**
   - Copy the exported `yolov11_tomato_leaf.tflite` file to this directory
   - Ensure the file name matches exactly: `yolov11_tomato_leaf.tflite`

4. **Verify Model Configuration**
   - Check that `ModelConfig.kt` has the correct path: `models/yolov11_tomato_leaf.tflite`
   - Verify input size is set to 640x640

#### Alternative: Using a Placeholder

For development and testing without the actual model, you can:
- Use a generic YOLOv11 model trained on COCO dataset (will detect plants/leaves)
- Download from: https://github.com/ultralytics/assets/releases
- Note: This will have lower accuracy for tomato-specific detection

#### Model Specifications
- **Input**: 640x640x3 (RGB image)
- **Output**: Bounding boxes with class probabilities
- **Classes**: Should include "tomato_leaf" or similar plant/leaf class
- **Confidence threshold**: 0.3 (configurable in ModelConfig)
- **NMS IoU threshold**: 0.45 (configurable in ModelConfig)

## Existing Models

### TFLite Disease Classifier
- **File name**: `tomato_disease_model.tflite` (located in parent assets directory)
- **Purpose**: Classifies tomato leaf diseases
- **Input size**: 224x224 pixels
- **Classes**: Early Blight, Late Blight, Leaf Mold, Septoria Leaf Spot, Bacterial Speck, Healthy, Uncertain

## Model Configuration

Model paths and parameters are configured in:
`app/src/main/java/com/ml/tomatoscan/config/ModelConfig.kt`

## Model Versions

Current model versions are tracked in BuildConfig:
- `BuildConfig.YOLO_MODEL_VERSION`: YOLOv11 model version
- `BuildConfig.TFLITE_MODEL_VERSION`: Disease classifier model version
- `BuildConfig.MODEL_VERSION`: Overall system version
