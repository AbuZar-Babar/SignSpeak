Required assets for the Kotlin app:

1) MediaPipe hand landmark model:
hand_landmarker.task

2) TFLite classification models + labels exported from the *_new training files:
action_model_baseline_new.tflite
action_model_baseline_new_quantized.tflite
labels_baseline_new.json
action_model_augmented_new.tflite
action_model_augmented_new_quantized.tflite
labels_augmented_new.json

3) Face keypoint detector (nose + ears overlay):
blaze_face_short_range.tflite
