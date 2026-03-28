import numpy as np
from fastapi import FastAPI, HTTPException, File, UploadFile, Query
import os
import cv2
import time
import threading
import mediapipe as mp
from contextlib import asynccontextmanager
from typing import List, Optional
import joblib

# Shared utilities
from src.utils.keras_compat import load_model
from src.config.actions_config import SEQUENCE_LENGTH, MODELS_DIR, load_actions
from src.utils.mediapipe_utils import mediapipe_detection, extract_keypoints
from src.server.data_models import SequenceData

MODEL_ARTIFACTS = {
    "baseline": {
        "model_path": os.path.join(MODELS_DIR, "action_model_baseline.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "label_encoder_baseline.pkl"),
    },
    "augmented": {
        "model_path": os.path.join(MODELS_DIR, "action_model_augmented.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "label_encoder_augmented.pkl"),
    },
}
DEFAULT_MODEL_KEY = "baseline"
PREVIEW_ENABLED = os.getenv("SIGNSPEAK_PREVIEW_FRAMES", "1").strip() == "1"
PREVIEW_WINDOW_NAME = "SignSpeak Backend Preview"
PREVIEW_MAX_WIDTH = int(os.getenv("SIGNSPEAK_PREVIEW_MAX_WIDTH", "720"))
preview_lock = threading.Lock()

ml_models = {}


def show_preview_frame(frame: np.ndarray, frame_idx: int, total_frames: int) -> None:
    """Render received mobile frames on the backend laptop before inference."""
    global PREVIEW_ENABLED
    if not PREVIEW_ENABLED:
        return

    try:
        with preview_lock:
            display = frame.copy()
            label = f"Received frame {frame_idx + 1}/{total_frames}"
            cv2.putText(
                display,
                label,
                (16, 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (0, 255, 0),
                2,
                cv2.LINE_AA,
            )

            h, w = display.shape[:2]
            if w > PREVIEW_MAX_WIDTH:
                scale = PREVIEW_MAX_WIDTH / float(w)
                display = cv2.resize(display, (int(w * scale), int(h * scale)))

            cv2.imshow(PREVIEW_WINDOW_NAME, display)
            cv2.waitKey(1)
    except Exception as preview_error:
        PREVIEW_ENABLED = False
        print(f"Preview disabled due to OpenCV window error: {preview_error}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    #load resources
    registry = {}

    baseline_paths = MODEL_ARTIFACTS[DEFAULT_MODEL_KEY]
    if not os.path.exists(baseline_paths["model_path"]):
        raise FileNotFoundError(f"Model file not found at {baseline_paths['model_path']}")
    if not os.path.exists(baseline_paths["encoder_path"]):
        raise FileNotFoundError(f"Encoder file not found at {baseline_paths['encoder_path']}")

    baseline_model = load_model(baseline_paths["model_path"])
    baseline_encoder = joblib.load(baseline_paths["encoder_path"])
    registry[DEFAULT_MODEL_KEY] = {
        "model": baseline_model,
        "encoder": baseline_encoder,
    }

    augmented_paths = MODEL_ARTIFACTS["augmented"]
    if os.path.exists(augmented_paths["model_path"]) and os.path.exists(augmented_paths["encoder_path"]):
        registry["augmented"] = {
            "model": load_model(augmented_paths["model_path"]),
            "encoder": joblib.load(augmented_paths["encoder_path"]),
        }
        print("Loaded augmented model artifacts.")
    else:
        print("Augmented model artifacts not found. 'augmented' selection will be unavailable.")

    # Keep legacy keys for /predict compatibility.
    ml_models["model"] = baseline_model
    ml_models["encoder"] = baseline_encoder
    ml_models["registry"] = registry
    print(f"Resources loaded successfully. Available models: {', '.join(registry.keys())}")
    yield
    if PREVIEW_ENABLED:
        try:
            with preview_lock:
                cv2.destroyWindow(PREVIEW_WINDOW_NAME)
        except Exception:
            pass
    ml_models.clear()


def resolve_model_key(requested_model: Optional[str]) -> str:
    """Resolve and validate model selection."""
    if requested_model is None or requested_model.strip() == "":
        return DEFAULT_MODEL_KEY

    model_key = requested_model.strip().lower()
    if model_key not in MODEL_ARTIFACTS:
        raise HTTPException(
            status_code=400,
            detail="Invalid model selection. Use 'baseline' or 'augmented'.",
        )

    registry = ml_models.get("registry", {})
    if model_key not in registry:
        raise HTTPException(
            status_code=503,
            detail=f"Requested model '{model_key}' is unavailable on this server.",
        )

    return model_key


def get_model_bundle(model_key: str):
    registry = ml_models.get("registry", {})
    bundle = registry.get(model_key)
    if not bundle:
        raise HTTPException(status_code=500, detail=f"Model '{model_key}' is not loaded")
    return bundle

app = FastAPI(lifespan = lifespan, title = "SignSpeak API")


@app.post("/predict")
async def predict(
    data: SequenceData,
    model: Optional[str] = Query(default=None, description="Model selector: baseline|augmented"),
):
    start_time = time.time()

    model_key = resolve_model_key(model)
    bundle = get_model_bundle(model_key)
    selected_model = bundle["model"]
    selected_encoder = bundle["encoder"]

    sequence = np.array(data.landmarks, dtype=np.float32)
    expected_shape = (SEQUENCE_LENGTH, 126)
    if sequence.shape != expected_shape:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid input shape. Expected {expected_shape}, got {sequence.shape}",
        )

    input_data = np.expand_dims(sequence, axis=0)
    prediction = selected_model.predict(input_data, verbose=0)[0]
    predicted_index = np.argmax(prediction)
    confidence = float(prediction[predicted_index])
    action_label = selected_encoder.inverse_transform([predicted_index])[0]

    left_present = np.any(sequence[:, :63] != 0, axis=1)
    right_present = np.any(sequence[:, 63:] != 0, axis=1)
    hands_detected = int(np.sum(left_present | right_present))

    processing_time = (time.time() - start_time) * 1000  # ms
    print(
        f"[predict-landmarks] Model: {model_key}, Action: {action_label}, Confidence: {confidence:.3f}, "
        f"Hands detected: {hands_detected}/{SEQUENCE_LENGTH}, Processing: {processing_time:.0f}ms"
    )

    return {
        "action": action_label,
        "confidence": confidence,
        "all_probabilities": {
            label: float(prob)
            for label, prob in zip(selected_encoder.classes_, prediction)
        },
        "model_used": model_key,
        "processing_time_ms": round(processing_time),
        "frames_processed": int(sequence.shape[0]),
        "hands_detected": hands_detected,
    }

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.post("/debug-echo")
async def debug_echo(data: SequenceData):
    """Echo back tensor statistics for parity validation.
    
    Use this endpoint to verify that Flutter preprocessed landmarks
    match the expected value ranges from Python real-time inference.
    """
    sequence = np.array(data.landmarks)
    
    if sequence.shape != (SEQUENCE_LENGTH, 126):
        raise HTTPException(
            status_code=400, 
            detail=f"Invalid shape. Expected ({SEQUENCE_LENGTH}, 126), got {sequence.shape}"
        )
    
    lh = sequence[:, :63]   # Left hand columns
    rh = sequence[:, 63:]   # Right hand columns
    
    # Check if hand is present (non-zero)
    lh_present = np.any(lh != 0)
    rh_present = np.any(rh != 0)
    
    return {
        "shape": list(sequence.shape),
        "left_hand": {
            "present": bool(lh_present),
            "min": float(lh.min()) if lh_present else 0.0,
            "max": float(lh.max()) if lh_present else 0.0,
            "mean": float(lh.mean()) if lh_present else 0.0,
            "wrist_xyz_frame0": lh[0, :3].tolist(),
        },
        "right_hand": {
            "present": bool(rh_present),
            "min": float(rh.min()) if rh_present else 0.0,
            "max": float(rh.max()) if rh_present else 0.0,
            "mean": float(rh.mean()) if rh_present else 0.0,
            "wrist_xyz_frame0": rh[0, :3].tolist(),
        },
        "x_value_ranges": {
            "lh_x_min": float(lh[:, 0::3].min()) if lh_present else 0.0,
            "lh_x_max": float(lh[:, 0::3].max()) if lh_present else 0.0,
            "rh_x_min": float(rh[:, 0::3].min()) if rh_present else 0.0,
            "rh_x_max": float(rh[:, 0::3].max()) if rh_present else 0.0,
        }
    }


@app.post("/predict-frames")
async def predict_frames(
    frames: List[UploadFile] = File(...),
    model: Optional[str] = Query(default=None, description="Model selector: baseline|augmented"),
):
    """
    Accept a batch of JPEG frames, extract landmarks server-side using
    the EXACT same MediaPipe pipeline used during training, and predict.
    
    Expects exactly SEQUENCE_LENGTH (60) JPEG images.
    Returns the predicted action + confidence.
    """
    start_time = time.time()
    
    model_key = resolve_model_key(model)
    bundle = get_model_bundle(model_key)
    selected_model = bundle["model"]
    selected_encoder = bundle["encoder"]
    
    # Validate frame count
    if len(frames) != SEQUENCE_LENGTH:
        raise HTTPException(
            status_code=400,
            detail=f"Expected {SEQUENCE_LENGTH} frames, got {len(frames)}"
        )
    
    # Process each frame through MediaPipe (matching training config EXACTLY)
    sequence = []
    hands_detected = 0
    
    with mp.solutions.holistic.Holistic(
        min_detection_confidence=0.4,
        min_tracking_confidence=0.4
    ) as holistic:
        for i, frame_file in enumerate(frames):
            # Read JPEG bytes and decode to BGR
            contents = await frame_file.read()
            nparr = np.frombuffer(contents, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if frame is None:
                raise HTTPException(
                    status_code=400,
                    detail=f"Frame {i} could not be decoded as an image"
                )
            
            # --- MOBILE PIVOT FIX: ORIENTATION ---
            # 1. Rotate 90 CCW to make it upright (Phone is held Portrait, but sensor is Landscape)
            frame = cv2.rotate(frame, cv2.ROTATE_90_COUNTERCLOCKWISE)
            
            # 2. Horizontal Flip (MediaPipe front camera usually needs mirroring)
            frame = cv2.flip(frame, 1)

            # Preview incoming stream on backend laptop before inference.
            show_preview_frame(frame, i, len(frames))
            
            # Use EXACT same pipeline as training data collection
            image, results = mediapipe_detection(frame, holistic)
            
            keypoints = extract_keypoints(results)
            sequence.append(keypoints)
            
            # Track hand detection for diagnostics
            if results.left_hand_landmarks or results.right_hand_landmarks:
                hands_detected += 1
    
    # Build tensor: (1, 60, 126)
    sequence_np = np.array(sequence)
    input_data = np.expand_dims(sequence_np, axis=0)
    
    # Predict
    prediction = selected_model.predict(input_data, verbose=0)[0]
    predicted_index = np.argmax(prediction)
    confidence = float(prediction[predicted_index])
    action_label = selected_encoder.inverse_transform([predicted_index])[0]
    
    processing_time = (time.time() - start_time) * 1000  # ms
    
    print(f"[predict-frames] Model: {model_key}, Action: {action_label}, Confidence: {confidence:.3f}, "
          f"Hands detected: {hands_detected}/{SEQUENCE_LENGTH}, "
          f"Processing: {processing_time:.0f}ms")
    
    return {
        "action": action_label,
        "confidence": confidence,
        "all_probabilities": {
            label: float(prob)
            for label, prob in zip(selected_encoder.classes_, prediction)
        },
        "model_used": model_key,
        "processing_time_ms": round(processing_time),
        "frames_processed": len(frames),
        "hands_detected": hands_detected,
    }
