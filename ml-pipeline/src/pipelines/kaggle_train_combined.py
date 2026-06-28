"""
kaggle_train_combined.py
========================
Kaggle-ready training script: Combined dataset (laptop_data + webcam_data).

Dataset structure expected at /kaggle/input/:
  PakistanSignLanguageDataset/
    PakistanSignLanguage/
      laptop_data/   <action>/<seq_id>/{0..59}.npy
      webcam_data/   <action>/<seq_id>/{0..59}.npy
    links_to_words_final.txt

Outputs (saved to /kaggle/working/):
  psl_combined_model.keras
  psl_combined_encoder.pkl
"""

import os
import time
import numpy as np
import joblib
import matplotlib.pyplot as plt

import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Input
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

# ============================================================
# CONFIG
# ============================================================
SEQUENCE_LENGTH  = 60
BATCH_SIZE       = 16
EPOCHS           = 200
LEARNING_RATE    = 0.001

OUTPUT_DIR       = "/kaggle/working"
MODEL_PATH       = os.path.join(OUTPUT_DIR, "psl_combined_model.keras")
ENCODER_PATH     = os.path.join(OUTPUT_DIR, "psl_combined_encoder.pkl")

# ============================================================
# STEP 1: LOCATE DATASET
# ============================================================
print("=" * 65)
print("STEP 1: Locating dataset...")
print("=" * 65)

DATASET_ROOT = None
CANDIDATE_ROOTS = [
    "/kaggle/input/pakistan-sign-language-dataset-v2/PakistanSignLanguageDataset/PakistanSignLanguage",
    "/kaggle/input/pakistansignlanguagedataset/PakistanSignLanguageDataset/PakistanSignLanguage",
    "/kaggle/input/PakistanSignLanguageDataset/PakistanSignLanguage",
]

for p in CANDIDATE_ROOTS:
    print(f"  Checking: {p}  -> {'FOUND' if os.path.isdir(p) else 'not found'}")
    if os.path.isdir(p) and DATASET_ROOT is None:
        DATASET_ROOT = p

# Fallback: walk /kaggle/input looking for the sub-folders
if DATASET_ROOT is None:
    print("\n  Searching /kaggle/input for laptop_data / webcam_data ...")
    for root, dirs, _ in os.walk("/kaggle/input"):
        if "laptop_data" in dirs and "webcam_data" in dirs:
            DATASET_ROOT = root
            print(f"  Found via search: {root}")
            break

if DATASET_ROOT is None:
    print("\n  FATAL: Could not locate dataset. Tree dump:")
    for root, dirs, files in os.walk("/kaggle/input", topdown=True):
        lvl = root.replace("/kaggle/input", "").count(os.sep)
        if lvl < 5:
            print("  " + "  " * lvl + os.path.basename(root) + "/")
    raise FileNotFoundError("Dataset root not found — check the folder structure above.")

LAPTOP_DIR  = os.path.join(DATASET_ROOT, "laptop_data")
WEBCAM_DIR  = os.path.join(DATASET_ROOT, "webcam_data")
WORDS_FILE  = os.path.join(os.path.dirname(DATASET_ROOT), "links_to_words_final.txt")

print(f"\n  Dataset root : {DATASET_ROOT}")
print(f"  laptop_data  : {'OK' if os.path.isdir(LAPTOP_DIR) else 'MISSING'}")
print(f"  webcam_data  : {'OK' if os.path.isdir(WEBCAM_DIR) else 'MISSING'}")
print(f"  words file   : {'OK' if os.path.isfile(WORDS_FILE) else 'MISSING'}")

# ============================================================
# STEP 2: LOAD ACTIONS FROM links_to_words_final.txt
# ============================================================
print("\n" + "=" * 65)
print("STEP 2: Loading action list...")
print("=" * 65)

if not os.path.isfile(WORDS_FILE):
    # Try sibling search
    for root, _, files in os.walk("/kaggle/input"):
        if "links_to_words_final.txt" in files:
            WORDS_FILE = os.path.join(root, "links_to_words_final.txt")
            break

with open(WORDS_FILE, "r", encoding="utf-8") as f:
    ACTIONS = [line.split(":")[0].strip() for line in f if line.strip()]

print(f"  Actions loaded: {len(ACTIONS)}")
print(f"  First 10: {ACTIONS[:10]}")

# ============================================================
# STEP 3: LOAD DATA (laptop + webcam)
# ============================================================
print("\n" + "=" * 65)
print("STEP 3: Loading landmark sequences...")
print("=" * 65)


def _load_sequence(action_path: str, seq_id: str) -> list | None:
    """Load one complete sequence of SEQUENCE_LENGTH frames."""
    window = []
    for frame_num in range(SEQUENCE_LENGTH):
        npy_path = os.path.join(action_path, seq_id, f"{frame_num}.npy")
        if not os.path.exists(npy_path):
            return None
        window.append(np.load(npy_path))
    return window


def load_from_source(source_dir: str, actions: list, label: str, per_action_limit: int = None) -> tuple:
    """Load valid sequences from one source directory, with optional per-action cap."""
    sequences, labels = [], []
    if not os.path.isdir(source_dir):
        print(f"  WARNING: {source_dir} not found — skipping.")
        return sequences, labels

    total_loaded = 0
    total_skipped = 0
    for action in actions:
        action_path = os.path.join(source_dir, action)
        if not os.path.isdir(action_path):
            continue

        seq_ids = sorted(
            [d for d in os.listdir(action_path) if d.isdigit()],
            key=int,
        )
        if per_action_limit is not None:
            seq_ids = seq_ids[:per_action_limit]

        loaded = 0
        for seq_id in seq_ids:
            seq = _load_sequence(action_path, seq_id)
            if seq is None:
                total_skipped += 1
                continue
            sequences.append(seq)
            labels.append(action)
            loaded += 1
        total_loaded += loaded

    limit_str = f", limit={per_action_limit}/action" if per_action_limit else ""
    print(f"  [{label}{limit_str}] Loaded: {total_loaded} | Skipped (incomplete): {total_skipped}")
    return sequences, labels


all_sequences, all_labels = [], []

# Matches local training_pipeline_no_aug.py: laptop 50/action, webcam all
laptop_seqs, laptop_labels = load_from_source(LAPTOP_DIR, ACTIONS, "laptop_data", per_action_limit=50)
all_sequences.extend(laptop_seqs)
all_labels.extend(laptop_labels)

webcam_seqs, webcam_labels = load_from_source(WEBCAM_DIR, ACTIONS, "webcam_data")
all_sequences.extend(webcam_seqs)
all_labels.extend(webcam_labels)

if not all_sequences:
    raise RuntimeError("No sequences loaded at all! Check dataset paths and action names.")

X = np.array(all_sequences)
y = np.array(all_labels)
print(f"\n  Total sequences: {X.shape[0]}")
print(f"  X shape        : {X.shape}")

# ============================================================
# STEP 4: FILTER CLASSES WITH < 2 SAMPLES
# ============================================================
print("\n" + "=" * 65)
print("STEP 4: Filtering low-sample classes...")
print("=" * 65)

unique, counts = np.unique(y, return_counts=True)
removed = unique[counts < 2]
if len(removed):
    print(f"  Removing {len(removed)} class(es) with < 2 samples: {list(removed)}")
    mask = np.isin(y, unique[counts >= 2])
    X, y = X[mask], y[mask]
else:
    print("  All classes have 2+ samples — no filtering needed.")

print(f"  Classes remaining : {len(np.unique(y))}")
print(f"  Samples remaining : {X.shape[0]}")

# ============================================================
# STEP 5: ENCODE & SPLIT
# ============================================================
print("\n" + "=" * 65)
print("STEP 5: Encoding labels & splitting...")
print("=" * 65)

le = LabelEncoder()
y_encoded = le.fit_transform(y)
y_cat = to_categorical(y_encoded).astype(int)

X_train, X_test, y_train, y_test = train_test_split(
    X, y_cat, test_size=0.2, random_state=42, stratify=y_encoded
)
print(f"  Train: {X_train.shape[0]} | Test: {X_test.shape[0]}")
print(f"  Input shape: {X.shape[1]} frames × {X.shape[2]} features")
print(f"  Classes: {y_cat.shape[1]}")

# ============================================================
# STEP 6: BUILD MODEL
# ============================================================
print("\n" + "=" * 65)
print("STEP 6: Building LSTM model...")
print("=" * 65)

model = Sequential([
    Input(shape=(X.shape[1], X.shape[2])),
    LSTM(64, return_sequences=True, activation="tanh"),
    LSTM(128, return_sequences=True, activation="tanh"),
    LSTM(64, return_sequences=False, activation="tanh"),
    Dense(64, activation="relu"),
    Dense(32, activation="relu"),
    Dense(y_cat.shape[1], activation="softmax"),
])
model.compile(
    optimizer=Adam(learning_rate=LEARNING_RATE),
    loss="categorical_crossentropy",
    metrics=["accuracy"],
)
model.summary()

# ============================================================
# STEP 7: TRAIN
# ============================================================
print("\n" + "=" * 65)
print(f"STEP 7: Training (up to {EPOCHS} epochs, early stopping)...")
print("=" * 65)

callbacks = [
    EarlyStopping(
        monitor="val_accuracy", patience=30, mode="max",
        restore_best_weights=True, verbose=1,
    ),
    ReduceLROnPlateau(
        monitor="val_loss", factor=0.5, patience=10,
        min_lr=1e-6, verbose=1,
    ),
]

t0 = time.time()
history = model.fit(
    X_train, y_train,
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    validation_data=(X_test, y_test),
    callbacks=callbacks,
    verbose=1,
)
elapsed = time.time() - t0

# ============================================================
# STEP 8: EVALUATE & SAVE
# ============================================================
print("\n" + "=" * 65)
print("STEP 8: Evaluation & saving artifacts...")
print("=" * 65)

train_loss, train_acc = model.evaluate(X_train, y_train, verbose=0)
test_loss,  test_acc  = model.evaluate(X_test,  y_test,  verbose=0)

print(f"  Train accuracy : {train_acc:.4f}  |  Train loss : {train_loss:.4f}")
print(f"  Test  accuracy : {test_acc:.4f}  |  Test  loss : {test_loss:.4f}")
print(f"  Training time  : {elapsed:.1f}s")

model.save(MODEL_PATH)
joblib.dump(le, ENCODER_PATH)
print(f"\n  Model   saved → {MODEL_PATH}")
print(f"  Encoder saved → {ENCODER_PATH}")

# ============================================================
# STEP 9: PLOT
# ============================================================
plt.figure(figsize=(12, 4))
plt.subplot(1, 2, 1)
plt.plot(history.history["accuracy"],     label="Train")
plt.plot(history.history["val_accuracy"], label="Val")
plt.title("Accuracy — Combined (laptop + webcam)")
plt.xlabel("Epoch"); plt.ylabel("Accuracy"); plt.legend()

plt.subplot(1, 2, 2)
plt.plot(history.history["loss"],     label="Train")
plt.plot(history.history["val_loss"], label="Val")
plt.title("Loss — Combined (laptop + webcam)")
plt.xlabel("Epoch"); plt.ylabel("Loss"); plt.legend()

plt.tight_layout()
plt.savefig(os.path.join(OUTPUT_DIR, "psl_combined_training_curves.png"), dpi=150)
plt.show()
print("\nDONE — Combined training complete.")
