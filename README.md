# SignSpeak 🤟

**SignSpeak** is a comprehensive sign language recognition and translation system designed to bridge the communication gap between sign language users and non-signers. It combines real-time AI-powered translation technology with an engaging learning platform and administrative tools.

## 📋 Project Description

SignSpeak is a full-stack application consisting of three main components:

1. **Mobile Application** - Flutter-based mobile app for end users to translate sign language in real-time
2. **Backend API** - FastAPI server providing ML inference, user management, and complaint handling
3. **Admin Dashboard** - Next.js web application for administrators to manage users, dictionary, and complaints

The system uses MediaPipe for pose and hand landmark extraction, and TensorFlow/Keras with LSTM models for sign language recognition. The entire platform features a modern, Duolingo-inspired UI with a soft color palette and intuitive navigation.

## 🌟 Features

### Mobile App
*   **Real-time Translation**: Point your camera to translate sign language gestures into text instantly
*   **Interactive Dictionary**: Browse and search a comprehensive library of signs with a clean, card-based interface
*   **Modern UI/UX**: Enjoy a friendly, "Soft" theme with pastel colors and a Duolingo-inspired layout
*   **User Profiles**: Track your progress, manage settings, and view your stats
*   **Complaint System**: Report issues and provide feedback

### Admin Dashboard
*   **User Management**: View and manage user accounts
*   **Dictionary Management**: Add, edit, and remove sign language entries
*   **Complaint Management**: Review and respond to user complaints
*   **Analytics Dashboard**: View statistics and insights
*   **Modern Interface**: Duolingo-inspired design with responsive layout

### Backend API
*   **ML Inference**: Real-time sign language recognition using trained models
*   **MediaPipe Integration**: Pose and hand landmark extraction
*   **RESTful API**: Well-structured endpoints for all operations
*   **Firebase Integration**: Authentication and database support

## 🏗️ Architecture

```
SignSpeak/
├── mobile/              # Flutter mobile application
├── backend/             # FastAPI backend server
├── admin_dashboard/     # Next.js admin web application
└── SignSpeak-main/      # ML training scripts and models
```

## 🛠️ Tech Stack

### Mobile App
*   **Framework**: Flutter
*   **Language**: Dart
*   **State Management**: Provider
*   **HTTP Client**: http package
*   **Camera**: camera package
*   **Design**: Custom "Soft" Theme (Nunito Font, Pastel Palette)

### Backend API
*   **Framework**: FastAPI
*   **Language**: Python 3.13
*   **ML Framework**: TensorFlow/Keras, MediaPipe
*   **Database**: Firestore (Firebase)
*   **Authentication**: Firebase Admin SDK
*   **Server**: Uvicorn

### Admin Dashboard
*   **Framework**: Next.js 16
*   **Language**: TypeScript
*   **Styling**: Tailwind CSS
*   **Icons**: Lucide React
*   **Design**: Duolingo-inspired theme

### ML Training
*   **Framework**: TensorFlow/Keras
*   **Model Type**: LSTM (Long Short-Term Memory)
*   **Data Processing**: MediaPipe, NumPy
*   **Training Tools**: scikit-learn

## 🚀 Getting Started

### Prerequisites

- **For Mobile App**:
  - Flutter SDK (latest stable version)
  - Android Studio / Xcode (for mobile development)
  - Android device or emulator / iOS simulator

- **For Backend**:
  - Python 3.13 or higher
  - pip (Python package manager)
  - Virtual environment (recommended)

- **For Admin Dashboard**:
  - Node.js 18+ and npm
  - Modern web browser

### Installation & Running

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd SignSpeak
```

#### 2. Backend API Setup

```bash
# Navigate to backend directory
cd backend

# Create virtual environment (Windows)
python -m venv venv
venv\Scripts\activate

# Create virtual environment (Linux/Mac)
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Note: You may need to uncomment mediapipe and tensorflow in requirements.txt
# pip install mediapipe tensorflow

# Run the server
cd app
uvicorn main:app --reload --port 8000
```

The API will be available at `http://localhost:8000`

**API Endpoints**:
- Health check: `GET /health`
- Inference: `POST /api/v1/inference/predict`
- Complaints: `GET/POST /api/v1/complaints`
- Users: `GET /api/v1/users/me`

#### 3. Admin Dashboard Setup

```bash
# Navigate to admin dashboard directory
cd admin_dashboard

# Install dependencies
npm install

# Run development server
npm run dev
```

The admin dashboard will be available at `http://localhost:3000`

**Note**: Currently using mock authentication. See `FIREBASE_SETUP.md` for Firebase configuration when needed.

#### 4. Mobile App Setup

```bash
# Navigate to mobile app directory
cd mobile/signspeak

# Install Flutter dependencies
flutter pub get

# Run on connected device/emulator
flutter run
```

**Configuration**:
- Update the backend URL in `lib/services/api_service.dart` if needed
- For Android emulator, use `10.0.2.2:8000` instead of `localhost:8000`
- For physical device, use your computer's IP address

#### 5. ML Model Training (Optional)

```bash
# Navigate to training directory
cd SignSpeak-main/backend

# Create virtual environment and install dependencies
python -m venv venv
source venv/bin/activate  # or venv\Scripts\activate on Windows
pip install -r requirements.txt

# Collect training data
python src/collect_data.py
# or use GUI version
python src/collect_data_gui.py

# Train the model
python src/train_model.py

# The trained model will be saved to models/action_model.h5
# Copy it to backend/app/models/ for use in the API
```

## 📁 Project Structure

```
SignSpeak/
├── mobile/
│   └── signspeak/          # Flutter app source code
│       ├── lib/
│       │   ├── core/       # Theme and core utilities
│       │   ├── presentation/  # UI screens
│       │   └── services/   # API and camera services
│       └── pubspec.yaml
│
├── backend/
│   └── app/
│       ├── api/v1/         # API routes and schemas
│       ├── core/           # Configuration and auth
│       ├── services/       # Business logic (ML, MediaPipe, etc.)
│       └── main.py         # FastAPI application entry point
│
├── admin_dashboard/
│   ├── app/                # Next.js app directory
│   │   ├── dashboard/      # Dashboard pages
│   │   └── login/          # Login page
│   ├── components/         # React components
│   ├── contexts/           # React contexts (Auth)
│   └── lib/                # Utilities
│
└── SignSpeak-main/
    └── backend/            # ML training scripts
        ├── src/            # Training and data collection
        └── models/         # Trained models (gitignored)
```

## 🔧 Configuration

### Backend Configuration

Create a `.env` file in the `backend/` directory (optional):
```env
PROJECT_NAME=SignSpeak Backend
API_V1_STR=/api/v1
```

### Admin Dashboard Configuration

Firebase configuration is optional (currently using mock auth). See `admin_dashboard/FIREBASE_SETUP.md` for details.

### Mobile App Configuration

Update `mobile/signspeak/lib/services/api_service.dart`:
```dart
static const String baseUrl = 'http://YOUR_BACKEND_IP:8000/api/v1';
```

## 📱 Screenshots

| Home | Dictionary | Profile |
|------|------------|---------|
| ![Home](docs/home_placeholder.png) | ![Dictionary](docs/dict_placeholder.png) | ![Profile](docs/profile_placeholder.png) |

## 🧪 Development

### Running in Development Mode

1. **Backend**: The FastAPI server runs with `--reload` flag for auto-reload
2. **Admin Dashboard**: Next.js dev server with hot reload
3. **Mobile App**: Flutter hot reload enabled

### Building for Production

**Backend**:
```bash
cd backend/app
uvicorn main:app --host 0.0.0.0 --port 8000
```

**Admin Dashboard**:
```bash
cd admin_dashboard
npm run build
npm start
```

**Mobile App**:
```bash
cd mobile/signspeak
flutter build apk  # for Android
flutter build ios  # for iOS
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is part of a Final Year Project (FYP).

## 🙏 Acknowledgments

- MediaPipe for pose and hand landmark detection
- TensorFlow/Keras for ML model training
- Duolingo for UI/UX inspiration

---

*SignSpeak - Breaking Barriers, One Sign at a Time.*
