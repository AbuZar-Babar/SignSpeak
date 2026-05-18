import os
import time
import numpy as np
import joblib

from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

from src.config.config import (
    load_actions,
    SEQUENCE_LENGTH,
    BATCH_SIZE,
    EPOCHS,
    LEARNING_RATE,
    MODELS_DIR,
    RAW_DIR,
)

V4_OUTPUT_DIR = os.path.join(MODELS_DIR, "new")

MODEL_LAPTOP_ONLY = os.path.join(V4_OUTPUT_DIR, "action_model_laptop50_v4.h5")
ENCODER_LAPTOP_ONLY = os.path.join(V4_OUTPUT_DIR, "label_encoder_laptop50_v4.pkl")

MODEL_COMBINED = os.path.join(V4_OUTPUT_DIR, "action_model_laptop50_mobile20_v4.h5")
ENCODER_COMBINED = os.path.join(V4_OUTPUT_DIR, "label_encoder_laptop50_mobile20_v4.pkl")

LAPTOP_DIR = os.path.join(RAW_DIR, "MP_Data")
MOBILE_DIR = os.path.join(RAW_DIR, "MP_Data_mobile")


def build_model(input_shape, num_classes):
    model = Sequential([
        LSTM(64, return_sequences=True, activation="tanh", input_shape=input_shape),
        LSTM(128, return_sequences=True, activation="tanh"),
        LSTM(64, return_sequences=False, activation="tanh"),
        Dense(64, activation="relu"),
        Dense(32, activation="relu"),
        Dense(num_classes, activation="softmax"),
    ])
    model.compile(
        optimizer=Adam(learning_rate=LEARNING_RATE),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
    return model


def _load_sequence(action_path, seq_id):
    window = []
    for frame_num in range(SEQUENCE_LENGTH):
        npy_path = os.path.join(action_path, seq_id, f"{frame_num}.npy")
        if not os.path.exists(npy_path):
            return None
        window.append(np.load(npy_path))
    return window


def load_limited_data(actions, source_limits):
    sequences = []
    labels = []

    for source_dir, per_action_limit in source_limits:
        if not os.path.isdir(source_dir):
            print(f"Skipping missing source: {source_dir}")
            continue

        print(f"Loading from {os.path.basename(source_dir)} (limit={per_action_limit}/action)")
        for action in actions:
            action_path = os.path.join(source_dir, action)
            if not os.path.isdir(action_path):
                continue

            seq_ids = sorted(
                [d for d in os.listdir(action_path) if d.isdigit()],
                key=int,
            )[:per_action_limit]

            for seq_id in seq_ids:
                seq = _load_sequence(action_path, seq_id)
                if seq is None:
                    continue
                sequences.append(seq)
                labels.append(action)

    if not sequences:
        raise RuntimeError("No sequences loaded. Check dataset paths and action folders.")

    return np.array(sequences), np.array(labels)


def train_and_save(run_name, source_limits, model_path, encoder_path):
    print("\n" + "=" * 70)
    print(f"Training Run: {run_name}")
    print("=" * 70)

    actions = load_actions()
    X, y = load_limited_data(actions, source_limits)

    le = LabelEncoder()
    y_encoded = le.fit_transform(y)
    y_cat = to_categorical(y_encoded).astype(int)

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y_cat,
        test_size=0.2,
        random_state=42,
        stratify=y_encoded,
    )

    print(f"Loaded sequences: {len(X)}")
    print(f"Train: {len(X_train)} | Test: {len(X_test)}")

    model = build_model((X_train.shape[1], X_train.shape[2]), y_cat.shape[1])
    callbacks = [
        EarlyStopping(
            monitor="val_accuracy",
            patience=30,
            mode="max",
            restore_best_weights=True,
            verbose=1,
        ),
        ReduceLROnPlateau(
            monitor="val_loss",
            factor=0.5,
            patience=10,
            min_lr=1e-6,
            verbose=1,
        ),
    ]

    t0 = time.time()
    model.fit(
        X_train,
        y_train,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        validation_data=(X_test, y_test),
        callbacks=callbacks,
        verbose=1,
    )
    duration = time.time() - t0

    train_loss, train_acc = model.evaluate(X_train, y_train, verbose=0)
    test_loss, test_acc = model.evaluate(X_test, y_test, verbose=0)

    model.save(model_path)
    joblib.dump(le, encoder_path)

    print(f"Saved model  : {model_path}")
    print(f"Saved encoder: {encoder_path}")
    print(
        f"Result ({run_name}) -> "
        f"train_acc={train_acc:.4f}, test_acc={test_acc:.4f}, "
        f"train_loss={train_loss:.4f}, test_loss={test_loss:.4f}, "
        f"time={duration:.1f}s"
    )


def main():
    os.makedirs(V4_OUTPUT_DIR, exist_ok=True)

    train_and_save(
        run_name="Laptop Only (50/action)",
        source_limits=[(LAPTOP_DIR, 50)],
        model_path=MODEL_LAPTOP_ONLY,
        encoder_path=ENCODER_LAPTOP_ONLY,
    )

    train_and_save(
        run_name="Combined (Laptop 50 + Mobile 20 per action)",
        source_limits=[(LAPTOP_DIR, 50), (MOBILE_DIR, 20)],
        model_path=MODEL_COMBINED,
        encoder_path=ENCODER_COMBINED,
    )

    print("\nDone. Produced v4 no-augmentation artifacts for both scenarios.")


if __name__ == "__main__":
    main()
