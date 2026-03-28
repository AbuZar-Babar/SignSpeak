# SignSpeak Mobile (React Native)

Android-focused React Native frontend for continuous Pakistan Sign Language translation.

This app is translation-first and includes placeholders for:
- Dictionary
- Profile
- Complaints

## What this version does

- Uses phone camera continuously.
- Supports two transport modes:
  - `FRAMES`: buffers latest 60 frames and sends JPEGs to backend.
  - `LANDMARKS`: extracts hand landmarks on phone and sends only landmark tensors.
  - `ONDEVICE`: extracts landmarks + runs TensorFlow Lite on phone (no backend calls).
- Endpoints used:
  - `POST /predict-frames?model=augmented|baseline` (frame transport)
  - `POST /predict?model=augmented|baseline` (landmark transport)
- Shows:
  - Current prediction
  - Confidence
  - Hands detected / frames processed
  - Sentence history with smoothing
  - Landmark extractor warmup status/progress (landmark mode)
  - Landmark debug panel with local vs backend ranges (landmark mode)

Default stream settings:
- Capture tick target: ~34ms (aiming near 30 FPS when device allows)
- Request cadence: every ~900ms once 60-frame buffer is full

## Tech stack

- Expo (React Native)
- `expo-camera`
- `expo-file-system`
- `@tensorflow/tfjs`
- `@tensorflow-models/hand-pose-detection`
- `react-native-fast-tflite`

## 1) Setup

```bash
cd front-end-mobile-react-native
npm install
```

## 1.1) Export TFLite assets (from ML pipeline)

```bash
cd ../ml-pipeline/ml_pipeline_data_collection
..\venv\Scripts\python.exe export_tflite.py --model all --output-dir ..\..\front-end-mobile-react-native\assets\models
```

## 2) Start app (Android)

```bash
npm run start
```

Then press `a` in terminal, or use:

```bash
npm run android
```

For `ONDEVICE` mode you need a native dev build (not Expo Go):

```bash
npx expo run:android
```

## 3) Start backend on laptop

From `ml-pipeline/ml_pipeline_data_collection`:

```bash
python -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

## 4) Network requirements

- Phone and laptop must be on the same Wi-Fi.
- Backend URL in app defaults to:
  - `http://192.168.100.2:8000`
- Change it from the Backend section in the app UI.

## Notes

- The current backend applies fixed rotate + mirror before MediaPipe extraction.
- Landmark mode runs TensorFlow.js hand landmark extraction locally on the phone.
- On-device mode uses bundled `.tflite` models in `assets/models/`.
- TFLite assets are loaded by Metro, so `metro.config.js` includes `tflite` in `assetExts`.
- First run of landmark mode may take longer while TFJS model assets are loaded.
- Stream starts only when `/health` is reachable.

## Current MVP status

- Translation: implemented
- Dictionary: placeholder
- Profile: placeholder
- Complaints: placeholder
