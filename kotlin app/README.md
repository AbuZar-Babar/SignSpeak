# SignSpeak Collector Android App

The Android module is now a single-purpose landmark collection app for the SignSpeak PSL dataset. It uses the phone camera, CameraX, and MediaPipe Tasks to capture 60 hand-landmark frames per sign sample.

## Features

- Live camera collection with front/back camera switching.
- 2-second countdown before recording.
- 60-frame capture at 20 FPS.
- Hand landmark overlay while recording.
- Retake before saving.
- JSON export to `Downloads/SignSpeakCollector/<action>/`.
- Stable export format: `signspeak-landmarks-v1`.

## Tech Stack

- Kotlin
- Jetpack Compose
- CameraX
- MediaPipe Tasks Vision

## Build And Install

From the `kotlin app/` directory:

```powershell
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:installDebug
```

Or open `kotlin app/` in Android Studio and run the `app` configuration on a physical Android device.

## Export Flow

1. Select or type an action name.
2. Tap `Record Sample`.
3. Perform the sign during the 60-frame capture.
4. Tap `Save Sample` or `Retake`.
5. Move the exported JSON files from the phone Downloads folder to your laptop.
6. Import them into the ML dataset:

```powershell
cd ..\ml-pipeline
python -m src.data.mobile_json_importer "path\to\phone\exports"
```

Imported samples are appended under `ml-pipeline/data/raw/MP_Data_mobile/<action>/<sequence>/`.
