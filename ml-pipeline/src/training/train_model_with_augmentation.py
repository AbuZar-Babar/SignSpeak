# train_model_with_augmentation.py
"""
Enhanced training script with data augmentation support.

Features:
- Optional data augmentation (3x multiplier by default)
- Progress tracking
- Model checkpointing
- Training history visualization
"""

import numpy as np
import os
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping, ReduceLROnPlateau
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import joblib
import argparse
import time

from src.config.actions_config import (
    load_actions, DATA_PATH, SEQUENCE_LENGTH, 
    NUM_SEQUENCES, BATCH_SIZE, EPOCHS, LEARNING_RATE, AUGMENTATION_MULTIPLIER, MODELS_DIR
)
from src.training.data_augmentation import create_augmented_dataset
from src.training.training_logger import log_training_session


def load_data(actions):
    """Load collected sequence data."""
    sequences, labels = [], []
    
    print("\n📂 Loading data...")
    for action in actions:
        action_path = os.path.join(DATA_PATH, action)
        if not os.path.exists(action_path):
            raise FileNotFoundError(f"Data folder missing for action: {action}")
        
        # Count actual sequences
        actual_seqs = len([d for d in os.listdir(action_path) if d.isdigit()])
        print(f"  • {action}: {actual_seqs} sequences")
        
        for seq in range(actual_seqs):
            window = []
            for frame_num in range(SEQUENCE_LENGTH):
                npy_path = os.path.join(action_path, str(seq), f"{frame_num}.npy")
                if not os.path.exists(npy_path):
                    raise FileNotFoundError(f"Missing file: {npy_path}")
                frame = np.load(npy_path)
                window.append(frame)
            sequences.append(window)
            labels.append(action)
    
    return np.array(sequences), np.array(labels)


def build_model(input_shape, num_classes, use_dropout=True):
    """
    Build LSTM model with optional dropout for regularization.
    
    Args:
        input_shape: (sequence_length, num_features)
        num_classes: Number of sign classes
        use_dropout: Add dropout layers (recommended for augmented data)
    """
    model = Sequential()
    
    # LSTM layers (tanh is standard/stable for LSTMs)
    model.add(LSTM(64, return_sequences=True, activation='tanh', input_shape=input_shape))
    if use_dropout:
        model.add(Dropout(0.2))
    
    model.add(LSTM(128, return_sequences=True, activation='tanh'))
    if use_dropout:
        model.add(Dropout(0.2))
    
    model.add(LSTM(64, return_sequences=False, activation='tanh'))
    if use_dropout:
        model.add(Dropout(0.2))
    
    # Dense layers
    model.add(Dense(64, activation='relu'))
    if use_dropout:
        model.add(Dropout(0.3))
    
    model.add(Dense(32, activation='relu'))
    
    # Output layer
    model.add(Dense(num_classes, activation='softmax'))
    
    # Compile
    optimizer = Adam(learning_rate=LEARNING_RATE)
    model.compile(
        optimizer=optimizer,
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model


def main():
    parser = argparse.ArgumentParser(description='Train PSL recognition model')
    parser.add_argument(
        '--augment',
        action='store_true',
        help='Use data augmentation (recommended for small datasets)'
    )
    parser.add_argument(
        '--augment-multiplier',
        type=int,
        default=AUGMENTATION_MULTIPLIER,
        help=f'Augmentation multiplier (default: {AUGMENTATION_MULTIPLIER}x)'
    )
    parser.add_argument(
        '--no-dropout',
        action='store_true',
        help='Disable dropout layers'
    )
    parser.add_argument(
        '--epochs',
        type=int,
        default=EPOCHS,
        help=f'Number of epochs (default: {EPOCHS})'
    )
    
    args = parser.parse_args()
    
    # Load actions
    actions = load_actions()
    print(f"\n🎯 Training model for {len(actions)} actions:")
    print(f"   {', '.join(actions)}")
    
    # Load data
    X, y = load_data(actions)
    print(f"\n📊 Original dataset shape: {X.shape}")
    print(f"   • {len(X)} sequences")
    print(f"   • {X.shape[1]} frames per sequence")
    print(f"   • {X.shape[2]} features per frame")
    
    # Apply augmentation if requested
    if args.augment:
        print(f"\n🔄 Applying data augmentation (x{args.augment_multiplier})...")
        X_augmented, y_augmented = create_augmented_dataset(
            X, y, 
            augmentation_multiplier=args.augment_multiplier
        )
        print(f"   ✅ Augmented dataset: {X_augmented.shape}")
        print(f"   ✅ Increased from {len(X)} to {len(X_augmented)} sequences!")
        X, y = X_augmented, y_augmented
    else:
        print("\n⚠️  Training WITHOUT augmentation")
        print("   Tip: Use --augment for better performance with small datasets")
    
    # Encode labels
    le = LabelEncoder()
    y_encoded = le.fit_transform(y)
    y_cat = to_categorical(y_encoded).astype(int)
    
    # Train/test split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y_cat,
        test_size=0.2,
        random_state=42,
        stratify=y_encoded
    )
    
    print(f"\n📈 Train/Test Split:")
    print(f"   • Training: {len(X_train)} sequences")
    print(f"   • Testing: {len(X_test)} sequences")
    
    # Build model
    input_shape = (X_train.shape[1], X_train.shape[2])
    use_dropout = not args.no_dropout
    
    print(f"\n🏗️  Building model...")
    print(f"   • Input shape: {input_shape}")
    print(f"   • Num classes: {y_cat.shape[1]}")
    print(f"   • Dropout: {'Enabled' if use_dropout else 'Disabled'}")
    
    model = build_model(input_shape, y_cat.shape[1], use_dropout=use_dropout)
    model.summary()
    
    # Callbacks
    callbacks = []
    
    # Model checkpoint (save best model)
    best_model_path = os.path.join(MODELS_DIR, 'best_action_model.h5')
    checkpoint = ModelCheckpoint(
        best_model_path,
        monitor='val_accuracy',
        save_best_only=True,
        mode='max',
        verbose=1
    )
    callbacks.append(checkpoint)
    
    # Early stopping (stop if no improvement)
    early_stop = EarlyStopping(
        monitor='val_accuracy',
        patience=30,
        mode='max',
        verbose=1,
        restore_best_weights=True
    )
    callbacks.append(early_stop)
    
    # Reduce learning rate on plateau
    reduce_lr = ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=10,
        min_lr=1e-6,
        verbose=1
    )
    callbacks.append(reduce_lr)
    
    # Train
    print(f"\n🚀 Starting training for {args.epochs} epochs...")
    print("=" * 60)
    
    start_time = time.time()
    history = model.fit(
        X_train, y_train,
        epochs=args.epochs,
        batch_size=BATCH_SIZE,
        validation_data=(X_test, y_test),
        callbacks=callbacks,
        verbose=1
    )
    training_duration = time.time() - start_time
    
    # Save final model
    model_path = os.path.join(MODELS_DIR, "action_model.h5")
    encoder_path = os.path.join(MODELS_DIR, "label_encoder.pkl")
    model.save(model_path)
    joblib.dump(le, encoder_path)
    
    # Print results
    print("\n" + "=" * 60)
    print("✅ Training complete!")
    print("=" * 60)
    
    final_train_acc = history.history['accuracy'][-1]
    final_val_acc = history.history['val_accuracy'][-1]
    
    print(f"\n📊 Final Results:")
    print(f"   • Training Accuracy: {final_train_acc * 100:.2f}%")
    print(f"   • Validation Accuracy: {final_val_acc * 100:.2f}%")
    
    print(f"\n💾 Saved files:")
    print(f"   • {model_path} (final model)")
    print(f"   • {best_model_path} (best model during training)")
    print(f"   • {encoder_path}")
    
    # Log the session
    log_training_session(
        duration_seconds=training_duration,
        num_words=len(actions),
        training_acc=final_train_acc,
        val_acc=final_val_acc,
        epochs=len(history.history['accuracy']), # actual epochs run if early stopping triggered
        batch_size=BATCH_SIZE,
        augmented=args.augment,
        model_path=model_path
    )
    
    # Tips
    print(f"\n💡 Tips:")
    if not args.augment:
        print("   • Try training with --augment for better generalization")
    if final_val_acc < 0.85:
        print("   • Low accuracy? Try collecting more data or increasing augmentation")
    if abs(final_train_acc - final_val_acc) > 0.15:
        print("   • Large train/val gap? Model may be overfitting")
        print("   • Consider using dropout or more augmentation")
    
    print("\n🎉 Ready for inference!")


if __name__ == "__main__":
    main()
