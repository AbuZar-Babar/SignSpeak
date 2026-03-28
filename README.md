# SignSpeak

**Developed by [Mohib Ullah Khan Sherwani](https://github.com/MohibUllahKhanSherwani) & [AbuZar Babar](https://github.com/AbuZar-Babar)**

SignSpeak is a Final Year Project focused on Pakistan Sign Language (PSL) dataset and its recognition on a mobile app.
- We contributed a [Pakistan Sign Language (PSL) dataset](https://www.kaggle.com/datasets/mohib123456/dynamic-word-level-pakistan-sign-language-dataset/data) for dynamic word-level recognition.

## Modules & Deep Dives

This repository is a multi-module workspace. **For detailed instructions, architecture, and advanced setup, please refer to the respective module READMEs:**

| Module | Purpose | README |
| --- | --- | --- |
| **Kotlin App** | Android app for real-time translation and PSL dictionary | [`kotlin app/README.md`](./kotlin%20app/README.md) |
| **Admin Portal** | React + Vite portal for complaint review | [`front-end-web/README.md`](./front-end-web/README.md) |
| **ML Pipeline** | Python pipeline for data collection, training, and inference | [`ml-pipeline/README.md`](./ml-pipeline/README.md) |
| **Backend** | Supabase schema, seed data, and SQL helpers | [`supabase/README.md`](./supabase/README.md) |

## Quick Running Guide

Below is the minimal setup to get each module running locally. For comprehensive setup guide, prerequisites and detailed troubleshooting, see the individual module READMEs linked above.

### 1. Clone the Repository

```bash
git clone https://github.com/AbuZar-Babar/SignSpeak.git
cd SignSpeak
```

### 2. Configure Supabase Backend

1. Create a Supabase project.
2. Run migrations in order (`20260327_signspeak_v1.sql` -> `20260327_admin_complaints_portal.sql` -> `seed.sql`) from the [`supabase/`](./supabase/) folder.
3. Keep your `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` handy.

### 3. Run the Admin Portal (React/Vite)

> *For the full setup guide, environment mapping, and deployment details, see [`front-end-web/README.md`](./front-end-web/README.md).*

```bash
cd front-end-web
npm install
# Create .env from .env.example with your Supabase credentials
npm run dev
```

### 4. Run the Android App (Kotlin)

> *For full setup guide, package structure, and troubleshooting, see [`kotlin app/README.md`](./kotlin%20app/README.md).*

Open `kotlin app/` in Android Studio, or run via Gradle (ensure Supabase credentials are set in Gradle properties or env vars):

```bash
cd "kotlin app"
# On Windows use .\gradlew.bat app:installDebug
./gradlew app:installDebug
```

### 5. Run the ML Pipeline (Python)

> *For full setup guide, refer to [`ml-pipeline/README.md`](./ml-pipeline/README.md).*

```bash
cd ml-pipeline
python -m venv venv
# Activate venv (e.g., `source venv/bin/activate` or `.\venv\Scripts\Activate.ps1`)
pip install -r requirements.txt

# Run real-time inference
python ml_pipeline_data_collection/realtime_inference_minimal.py
```

## Team

- **AbuZar Babar** ([GitHub](https://github.com/AbuZar-Babar/SignSpeak))
- **Mohib Ullah Khan Sherwani** ([GitHub](https://github.com/MohibUllahKhanSherwani))
- **M. Abdullah Umar**

**Supervisor:** Dr. Rab Nawaz Jadoon

