# SignSpeak Android App

The primary mobile interface for the SignSpeak project, built with Kotlin and Jetpack Compose. This app is designed to break down communication barriers by providing Deaf/Hard-of-Hearing users and hearing individuals with real-time Pakistan Sign Language (PSL) translation directly on their mobile devices.

## Features

- **Real-Time PSL Translation:** Leverages device camera and on-device ML for live sign recognition.
- **PSL Dictionary:** A fully searchable sign vocabulary dictionary complete with categories and bookmarking capabilities.
- **Translation History:** Allows users to view and manage their previously saved translations.
- **Authentication:** Secure user login and profile management powered by Supabase.
- **Complaint Submission:** Let users report incorrect predictions or offer dictionary feedback directly to administrators.
- **Profile Management:** Users can manage their account settings seamlessly within the app.

## Tech Stack

- **UI Framework:** Jetpack Compose
- **Language:** Kotlin
- **Camera API:** CameraX
- **On-Device Machine Learning:** TensorFlow Lite, MediaPipe Tasks
- **Backend & Auth:** Supabase (Auth, Postgres)

## Prerequisites

Before running the application, ensure you have:
- **Android Studio** with the Android SDK installed.
- A functional **JDK** configured (either embedded in Android Studio or system-wide).
- An Android device or an active emulator.
- Access to the overarching SignSpeak **Supabase Project ecosystem**.

## Project Configuration

- **Application ID:** `com.example.kotlinfrontend`
- **Minimum SDK:** 26
- **Target SDK:** 36
- **App Name:** `SignSpeak`

## Setup & Running Guide

### 1. Environment Configuration

To communicate with the backend, you must provide your Supabase credentials. You can set them either via system environment variables or locally in a `local.properties` or `gradle.properties` file:

```properties
SUPABASE_URL=your_supabase_url
SUPABASE_PUBLISHABLE_KEY=your_publishable_key
```

### 2. Build and Install via Command Line

If you are using the terminal from the `kotlin app/` directory:

#### On Windows:
```powershell
.\gradlew.bat app:assembleDebug
.\gradlew.bat app:installDebug
```

#### On Linux / macOS:
```bash
./gradlew app:assembleDebug
./gradlew app:installDebug
```

### 3. Build via Android Studio

Alternatively, open the `kotlin app/` folder directly in Android Studio:
1. Wait for Gradle sync to finish.
2. Select an emulator or connected physical device.
3. Click the **Run** (Play) button in the toolbar.

## Architecture Context

The Android app communicates tightly with the central Supabase database. It relies heavily on tables such as `profiles`, `dictionary_entries`, `translation_history`, and `complaints`. Any moderation action recorded in the admin portal originated from this mobile application.
