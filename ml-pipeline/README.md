# SignSpeak ML Pipeline

**SignSpeak_ML_Pipeline** is a professional MLOps-driven pipeline for Pakistan Sign Language (PSL) recognition. It transitions the project from simple scripts to a modular, package-based architecture with automated CI/CD, smart caching, and robust data augmentation.

## 🏗 Project Architecture

The project follows a modular structure for better maintainability and scalability:

```
ml-pipeline/
├── src/                    # Core source code (Python package)
│   ├── config/             # Centralized configuration & path management
│   ├── data/               # Data ingestion, loading & collection GUI
│   ├── features/           # Feature engineering & augmentation logic
│   ├── models/             # Model evaluation & export utilities
│   ├── pipelines/          # End-to-end training & inference pipelines
│   ├── analysis/           # EDA & data quality analysis
│   └── utils/              # MediaPipe & logging helpers
├── data/                   # Data storage (Raw, Processed, External)
├── models/                 # Saved model artifacts (.h5, .tflite, .pkl)
├── reports/                # Automatically generated evaluation reports
├── logs/                   # Training and system logs
├── tests/                  # Unit tests for core components
└── .github/workflows/      # CI/CD pipeline (Linting & Testing)
```

---

## 🚀 Getting Started

### 1. Prerequisites
- Python 3.9 - 3.11
- A webcam for data collection and inference.

### 2. Installation
From the `ml-pipeline` root directory:

```powershell
# Create and activate virtual environment
python -m venv venv
.\venv\Scripts\Activate.ps1  # Windows

# Install dependencies
pip install -r requirements.txt
```

---

## 🛠 Usage Guide

**IMPORTANT:** Always run these commands from the `ml-pipeline` root folder using the `-m` (module) flag.

### 1. Data Collection
Launch the GUI to record hand landmarks for new signs:
```powershell
python -m src.data.collection_gui
```

### 2. Training Pipeline
Retrain both the **Baseline** and **Augmented** models. This script uses smart caching—if you haven't added new data, it will load from cache instantly.
```powershell
python -m src.pipelines.training_pipeline
```

### 3. Real-Time Inference
Test the trained models live via your webcam:
```powershell
python -m src.pipelines.inference_pipeline
```

### 4. Model Evaluation
Generate detailed performance reports, confusion matrices, and F1-score benchmarks:
```powershell
python -m src.models.evaluate
```

### 5. Exploratory Data Analysis (EDA)
Run comprehensive data quality checks, feature statistics, and cross-source validation:
```powershell
python -m src.analysis --output analysis_report.json
```
This generates a detailed JSON report including:
- **Data Quality:** Missing frames, empty landmarks, coordinate range validation
- **Feature Statistics:** Per-landmark statistics (mean, std, min, max), temporal evolution
- **Cross-Source Analysis:** Compares laptop vs mobile data distributions (Kolmogorov-Smirnov tests)
- **Augmentation Validation:** Checks augmented sample quality and validity

### 6. TFLite Export
Convert your trained Keras models to TFLite for mobile or edge deployment:
```powershell
python -m src.models.export_model
```

### 7. Testing
Run unit tests to verify core functionality, data integrity, and augmentation logic:

**Run all tests:**
```powershell
pytest tests/
```

**Run specific test modules:**
```powershell
pytest tests/test_data_quality.py          # Data quality validation tests
pytest tests/test_feature_stats.py         # Feature statistics tests
pytest tests/test_cross_source.py          # Cross-source comparison tests
pytest tests/test_augmentation_inspector.py # Augmentation validation tests
pytest tests/test_feature_engineering.py   # Augmentation techniques tests
pytest tests/test_ingestion.py             # Data ingestion tests
```

**Run with verbose output:**
```powershell
pytest tests/ -v
```

**Run with coverage report:**
```powershell
pytest tests/ --cov=src --cov-report=html
```

---

## 🧠 Core MLOps Features

### ⚡ Smart Ingestion & Caching
The pipeline implements a **fingerprinting system** in `src/data/ingestion.py`. It calculates a hash of your raw dataset. If the data hasn't changed, it loads the processed dataset from `data/processed/` in seconds, saving you minutes of waiting during every training run.

### 📈 Coordinate-Space Augmentation
Our specialized augmentation engine (`src/features/feature_engineering.py`) performs 3x-5x data expansion directly on the hand landmarks:
- **Time Warping:** Simulates faster/slower signing speeds.
- **Spatial Scaling:** Simulates distance variations from the camera.
- **Spatial Translation:** Simulates different hand positions in the frame.
- **Gaussian Noise:** Simulates sensor jitter.

### 🧪 Quality Assurance (CI/CD)
The project includes a GitHub Actions workflow (`.github/workflows/ci-cd.yml`) that automatically runs:
- **Flake8**: To ensure PEP8 code quality.
- **Pytest**: To verify data ingestion and augmentation logic on every push.

To run tests locally:
```powershell
pytest tests/
```

### 📊 Data Exploration & Quality Analysis
The EDA module (`src/analysis/`) provides comprehensive data validation:
- **Data Quality Checks:** Detects missing frames, empty landmarks, out-of-range coordinates, class imbalance
- **Feature Statistics:** Computes per-landmark statistics, temporal motion analysis
- **Cross-Source Validation:** Identifies domain gaps between laptop and mobile data using statistical tests
- **Augmentation Validation:** Ensures augmented samples maintain data integrity

Run the analysis anytime to understand your dataset:
```powershell
python -m src.analysis --output analysis_report.json
```

---

## 📊 Dataset Reference
The dataset used in this project is live and open-source:
- [**Kaggle**](https://www.kaggle.com/datasets/mohib123456/dynamic-word-level-pakistan-sign-language-dataset/data)
- [**HuggingFace**](https://huggingface.co/datasets/mohibkhansherwani/DynamicWordLevelPakistanSignLanguageDataset)
