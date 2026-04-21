"""
SignSpeak - Comprehensive Validation Testing Suite
===================================================
Performs rigorous validation beyond simple train/test split:

1. Stratified K-Fold Cross-Validation (5-fold)
2. Cross-Source Validation (Laptop ↔ Mobile)
3. Per-Class Performance Deep-Dive
4. Confusion Matrix Heatmap Generation
5. Learning Curve Analysis
6. Confidence Interval Computation (95% CI)
7. Dataset Statistics & Balance Report
8. Full JSON + Visual Report Export

Usage:
    python ml_pipeline_data_collection/validation_testing.py
"""

import os
import sys
import json
import time
import numpy as np
import joblib
from datetime import datetime
from collections import Counter

from tensorflow.keras.models import load_model, clone_model
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from tensorflow.keras.optimizers import Adam

from sklearn.model_selection import StratifiedKFold, train_test_split
from sklearn.metrics import (
    classification_report, confusion_matrix,
    precision_recall_fscore_support, accuracy_score,
)
from sklearn.preprocessing import LabelEncoder

from actions_config import load_actions, SEQUENCE_LENGTH, BATCH_SIZE, EPOCHS, LEARNING_RATE
from train_combined import load_combined_data, build_model

# ──────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

DATA_SOURCES = {
    "laptop": os.path.join(SCRIPT_DIR, "MP_Data"),
    "mobile": os.path.join(SCRIPT_DIR, "MP_Data_mobile"),
}
ALL_SOURCES = list(DATA_SOURCES.values())

MODEL_AUGMENTED = os.path.join("all_models", "action_model_augmented_new.h5")
ENCODER_AUGMENTED = os.path.join("all_models", "label_encoder_augmented_new.pkl")

OUTPUT_DIR = os.path.join(SCRIPT_DIR, "validation_results")
RANDOM_STATE = 42
N_FOLDS = 5

# ──────────────────────────────────────────────────────────────
# Visualization helpers (matplotlib/seaborn optional)
# ──────────────────────────────────────────────────────────────
HAS_PLOT = False
try:
    import matplotlib
    matplotlib.use("Agg")  # Non-interactive backend
    import matplotlib.pyplot as plt
    import matplotlib.colors as mcolors
    HAS_PLOT = True
except ImportError:
    pass

try:
    import seaborn as sns
    HAS_SNS = True
except ImportError:
    HAS_SNS = False


def _save_fig(fig, name):
    """Save figure to validation_results/."""
    path = os.path.join(OUTPUT_DIR, f"{name}.png")
    fig.savefig(path, dpi=150, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print(f"  📊 Saved: {os.path.basename(path)}")
    return path


# ──────────────────────────────────────────────────────────────
# 1. Dataset Statistics
# ──────────────────────────────────────────────────────────────
def dataset_statistics(actions, data_sources):
    """Analyze dataset balance, feature stats, and source distribution."""
    print("\n" + "=" * 70)
    print("  1. DATASET STATISTICS & BALANCE REPORT")
    print("=" * 70)

    X, y = load_combined_data(actions, data_sources)

    class_counts = Counter(y)
    counts = np.array([class_counts[a] for a in actions])

    print(f"\n  Total Sequences  : {len(X)}")
    print(f"  Total Classes    : {len(actions)}")
    print(f"  Sequence Length  : {X.shape[1]} frames")
    print(f"  Features/Frame   : {X.shape[2]}")
    print(f"  Min Samples/Class: {counts.min()} ({actions[counts.argmin()]})")
    print(f"  Max Samples/Class: {counts.max()} ({actions[counts.argmax()]})")
    print(f"  Mean ± Std       : {counts.mean():.1f} ± {counts.std():.1f}")

    # Imbalance ratio
    imbalance_ratio = counts.max() / counts.min()
    print(f"  Imbalance Ratio  : {imbalance_ratio:.2f}x")

    if imbalance_ratio > 2.0:
        print(f"  [!] Dataset is IMBALANCED (ratio > 2.0)")
    else:
        print(f"  [OK] Dataset is reasonably BALANCED")

    # Feature statistics
    flat = X.reshape(-1, X.shape[-1])
    zero_ratio = np.mean(flat == 0) * 100
    print(f"\n  Feature Range    : [{flat.min():.4f}, {flat.max():.4f}]")
    print(f"  Feature Mean     : {flat.mean():.4f}")
    print(f"  Feature Std      : {flat.std():.4f}")
    print(f"  Zero-Values      : {zero_ratio:.1f}% (inactive landmarks)")

    # Per-class table
    print(f"\n  {'Class':<22} {'Samples':>8} {'Pct':>7}")
    print(f"  {'-' * 40}")
    for action in sorted(actions):
        c = class_counts[action]
        pct = c / len(y) * 100
        bar = "#" * int(pct * 2)
        print(f"  {action:<22} {c:>8} {pct:>6.1f}% {bar}")

    # Visualize class distribution
    if HAS_PLOT:
        fig, ax = plt.subplots(figsize=(14, 6))
        sorted_actions = sorted(actions)
        sorted_counts = [class_counts[a] for a in sorted_actions]

        colors = plt.cm.viridis(np.linspace(0.2, 0.8, len(sorted_actions)))
        ax.barh(sorted_actions, sorted_counts, color=colors, edgecolor="white", linewidth=0.5)
        ax.set_xlabel("Number of Sequences", fontsize=12)
        ax.set_title("Dataset Class Distribution", fontsize=14, fontweight="bold")
        ax.invert_yaxis()
        ax.tick_params(axis="y", labelsize=7)
        for i, v in enumerate(sorted_counts):
            ax.text(v + 0.3, i, str(v), va="center", fontsize=7)
        fig.tight_layout()
        _save_fig(fig, "class_distribution")

    return {
        "total_sequences": len(X),
        "num_classes": len(actions),
        "sequence_length": int(X.shape[1]),
        "features_per_frame": int(X.shape[2]),
        "min_samples_per_class": int(counts.min()),
        "max_samples_per_class": int(counts.max()),
        "mean_samples": float(counts.mean()),
        "std_samples": float(counts.std()),
        "imbalance_ratio": float(imbalance_ratio),
        "zero_value_pct": float(zero_ratio),
        "class_counts": {a: int(class_counts[a]) for a in actions},
    }


# ──────────────────────────────────────────────────────────────
# 2. Stratified K-Fold Cross-Validation
# ──────────────────────────────────────────────────────────────
def kfold_cross_validation(actions, data_sources, n_folds=N_FOLDS):
    """Run stratified K-Fold CV - the gold standard for model validation."""
    print("\n" + "=" * 70)
    print(f"  2. STRATIFIED {n_folds}-FOLD CROSS-VALIDATION")
    print("=" * 70)

    X, y = load_combined_data(actions, data_sources)

    le = LabelEncoder()
    y_encoded = le.fit_transform(y)
    num_classes = len(le.classes_)

    skf = StratifiedKFold(n_splits=n_folds, shuffle=True, random_state=RANDOM_STATE)

    fold_results = []
    all_y_true = []
    all_y_pred = []

    for fold_idx, (train_idx, test_idx) in enumerate(skf.split(X, y_encoded), 1):
        print(f"\n  -- Fold {fold_idx}/{n_folds} --")

        X_train, X_test = X[train_idx], X[test_idx]
        y_train_enc, y_test_enc = y_encoded[train_idx], y_encoded[test_idx]
        y_train_cat = to_categorical(y_train_enc, num_classes)
        y_test_cat = to_categorical(y_test_enc, num_classes)

        print(f"  Train: {len(X_train)} | Test: {len(X_test)}")

        # Build a fresh model for each fold
        model = build_model(
            (X_train.shape[1], X_train.shape[2]),
            num_classes,
            use_dropout=True,
        )

        callbacks = [
            EarlyStopping(
                monitor="val_accuracy", patience=25, mode="max",
                restore_best_weights=True, verbose=0,
            ),
            ReduceLROnPlateau(
                monitor="val_loss", factor=0.5, patience=8,
                min_lr=1e-6, verbose=0,
            ),
        ]

        t0 = time.time()
        history = model.fit(
            X_train, y_train_cat,
            epochs=EPOCHS,
            batch_size=BATCH_SIZE,
            validation_data=(X_test, y_test_cat),
            callbacks=callbacks,
            verbose=0,
        )
        duration = time.time() - t0

        # Evaluate
        predictions = model.predict(X_test, verbose=0)
        y_pred = np.argmax(predictions, axis=1)

        acc = accuracy_score(y_test_enc, y_pred)
        prec, rec, f1, _ = precision_recall_fscore_support(
            y_test_enc, y_pred, average="macro", zero_division=0,
        )

        actual_epochs = len(history.history["accuracy"])

        print(f"  Accuracy : {acc * 100:.2f}%")
        print(f"  Macro F1 : {f1 * 100:.2f}%")
        print(f"  Epochs   : {actual_epochs} | Time: {duration:.1f}s")

        fold_results.append({
            "fold": fold_idx,
            "accuracy": float(acc),
            "precision": float(prec),
            "recall": float(rec),
            "f1_score": float(f1),
            "epochs_trained": actual_epochs,
            "duration_s": round(duration, 1),
        })

        all_y_true.extend(y_test_enc.tolist())
        all_y_pred.extend(y_pred.tolist())

        # Free memory
        del model, X_train, X_test

    # Aggregate results
    accs = [r["accuracy"] for r in fold_results]
    f1s = [r["f1_score"] for r in fold_results]
    precs = [r["precision"] for r in fold_results]
    recs = [r["recall"] for r in fold_results]

    mean_acc = np.mean(accs)
    std_acc = np.std(accs)
    mean_f1 = np.mean(f1s)
    std_f1 = np.std(f1s)

    # 95% Confidence Interval (t-distribution approximation)
    from scipy import stats
    ci_multiplier = stats.t.ppf(0.975, df=n_folds - 1)
    ci_acc = ci_multiplier * std_acc / np.sqrt(n_folds)
    ci_f1 = ci_multiplier * std_f1 / np.sqrt(n_folds)

    print(f"\n  {'-' * 60}")
    print(f"  CROSS-VALIDATION SUMMARY ({n_folds}-Fold)")
    print(f"  {'-' * 60}")
    print(f"  {'Metric':<25} {'Mean':>10} {'Std':>10} {'95% CI':>18}")
    print(f"  {'-' * 60}")
    print(f"  {'Accuracy':<25} {mean_acc * 100:>9.2f}% {std_acc * 100:>9.2f}% ±{ci_acc * 100:.2f}%")
    print(f"  {'Macro Precision':<25} {np.mean(precs) * 100:>9.2f}% {np.std(precs) * 100:>9.2f}%")
    print(f"  {'Macro Recall':<25} {np.mean(recs) * 100:>9.2f}% {np.std(recs) * 100:>9.2f}%")
    print(f"  {'Macro F1-Score':<25} {mean_f1 * 100:>9.2f}% {std_f1 * 100:>9.2f}% ±{ci_f1 * 100:.2f}%")

    print(f"\n  Per-Fold Breakdown:")
    for r in fold_results:
        print(f"    Fold {r['fold']}: Acc={r['accuracy'] * 100:.2f}%  F1={r['f1_score'] * 100:.2f}%  ({r['epochs_trained']} epochs, {r['duration_s']}s)")

    # Aggregated confusion matrix across all folds
    cm_agg = confusion_matrix(all_y_true, all_y_pred)

    if HAS_PLOT:
        _plot_confusion_matrix(cm_agg, le.classes_, "kfold_confusion_matrix")

        # Plot fold-by-fold accuracy
        fig, ax = plt.subplots(figsize=(8, 5))
        folds = [r["fold"] for r in fold_results]
        ax.bar(folds, [a * 100 for a in accs], color="#4CAF50", edgecolor="white", width=0.6)
        ax.axhline(mean_acc * 100, color="#FF5722", linestyle="--", linewidth=2, label=f"Mean: {mean_acc * 100:.2f}%")
        ax.fill_between(
            [0.5, n_folds + 0.5],
            (mean_acc - ci_acc) * 100, (mean_acc + ci_acc) * 100,
            alpha=0.15, color="#FF5722", label=f"95% CI: ±{ci_acc * 100:.2f}%",
        )
        ax.set_xlabel("Fold", fontsize=12)
        ax.set_ylabel("Accuracy (%)", fontsize=12)
        ax.set_title(f"Stratified {n_folds}-Fold Cross-Validation", fontsize=14, fontweight="bold")
        ax.set_xticks(folds)
        ax.set_ylim(max(0, min(accs) * 100 - 5), 101)
        ax.legend(fontsize=10)
        fig.tight_layout()
        _save_fig(fig, "kfold_accuracy_per_fold")

    return {
        "n_folds": n_folds,
        "mean_accuracy": float(mean_acc),
        "std_accuracy": float(std_acc),
        "ci_95_accuracy": float(ci_acc),
        "mean_f1": float(mean_f1),
        "std_f1": float(std_f1),
        "ci_95_f1": float(ci_f1),
        "mean_precision": float(np.mean(precs)),
        "mean_recall": float(np.mean(recs)),
        "fold_results": fold_results,
        "aggregated_confusion_matrix": cm_agg.tolist(),
    }


# ──────────────────────────────────────────────────────────────
# 3. Cross-Source Validation (Laptop ↔ Mobile)
# ──────────────────────────────────────────────────────────────
def cross_source_validation(actions):
    """Train on one source, test on another - tests real generalization."""
    print("\n" + "=" * 70)
    print("  3. CROSS-SOURCE VALIDATION (Laptop ↔ Mobile)")
    print("=" * 70)

    laptop_dir = DATA_SOURCES.get("laptop")
    mobile_dir = DATA_SOURCES.get("mobile")

    if not os.path.isdir(laptop_dir) or not os.path.isdir(mobile_dir):
        print("  [!] Both MP_Data and MP_Data_mobile required. Skipping.")
        return None

    results = {}

    for train_name, train_dir, test_name, test_dir in [
        ("Laptop", laptop_dir, "Mobile", mobile_dir),
        ("Mobile", mobile_dir, "Laptop", laptop_dir),
    ]:
        print(f"\n  -- Train: {train_name} → Test: {test_name} --")

        X_train, y_train = load_combined_data(actions, [train_dir])
        X_test, y_test = load_combined_data(actions, [test_dir])

        # Align classes
        le = LabelEncoder()
        le.fit(np.concatenate([y_train, y_test]))

        # Filter to common classes only
        common = set(y_train) & set(y_test)
        if len(common) < len(actions):
            print(f"  Note: {len(common)}/{len(actions)} classes present in both sources")

        mask_train = np.isin(y_train, list(common))
        mask_test = np.isin(y_test, list(common))
        X_train, y_train = X_train[mask_train], y_train[mask_train]
        X_test, y_test = X_test[mask_test], y_test[mask_test]

        y_train_enc = le.transform(y_train)
        y_test_enc = le.transform(y_test)
        num_classes = len(le.classes_)
        y_train_cat = to_categorical(y_train_enc, num_classes)

        print(f"  Train: {len(X_train)} seqs | Test: {len(X_test)} seqs | Classes: {len(common)}")

        model = build_model(
            (X_train.shape[1], X_train.shape[2]),
            num_classes,
            use_dropout=True,
        )

        callbacks = [
            EarlyStopping(
                monitor="val_loss", patience=25,
                restore_best_weights=True, verbose=0,
            ),
            ReduceLROnPlateau(
                monitor="val_loss", factor=0.5, patience=8,
                min_lr=1e-6, verbose=0,
            ),
        ]

        # Use a small validation split from training data for early stopping
        X_tr, X_val, y_tr, y_val = train_test_split(
            X_train, y_train_cat, test_size=0.15, random_state=RANDOM_STATE,
        )

        t0 = time.time()
        model.fit(
            X_tr, y_tr,
            epochs=EPOCHS,
            batch_size=BATCH_SIZE,
            validation_data=(X_val, y_val),
            callbacks=callbacks,
            verbose=0,
        )
        duration = time.time() - t0

        predictions = model.predict(X_test, verbose=0)
        y_pred = np.argmax(predictions, axis=1)
        acc = accuracy_score(y_test_enc, y_pred)
        prec, rec, f1, _ = precision_recall_fscore_support(
            y_test_enc, y_pred, average="macro", zero_division=0,
        )

        key = f"{train_name.lower()}_to_{test_name.lower()}"
        print(f"  Accuracy : {acc * 100:.2f}%")
        print(f"  Macro F1 : {f1 * 100:.2f}%")
        print(f"  Time     : {duration:.1f}s")

        results[key] = {
            "train_source": train_name,
            "test_source": test_name,
            "train_size": len(X_train),
            "test_size": len(X_test),
            "common_classes": len(common),
            "accuracy": float(acc),
            "precision": float(prec),
            "recall": float(rec),
            "f1_score": float(f1),
            "duration_s": round(duration, 1),
        }
        del model

    # Summary
    print(f"\n  {'-' * 60}")
    print(f"  CROSS-SOURCE SUMMARY")
    print(f"  {'-' * 60}")
    for key, r in results.items():
        print(f"  {r['train_source']:>6} → {r['test_source']:<6}  "
              f"Acc: {r['accuracy'] * 100:.2f}%  F1: {r['f1_score'] * 100:.2f}%")

    return results


# ──────────────────────────────────────────────────────────────
# 4. Confusion Matrix Heatmap (for pre-trained model)
# ──────────────────────────────────────────────────────────────
def _plot_confusion_matrix(cm, class_names, filename):
    """Generate a publication-quality confusion matrix heatmap."""
    if not HAS_PLOT:
        print("  [!] matplotlib not installed, skipping visualization.")
        return

    n = len(class_names)
    fig_size = max(10, n * 0.25)
    fig, ax = plt.subplots(figsize=(fig_size, fig_size))

    if HAS_SNS:
        sns.heatmap(
            cm, annot=(n <= 30), fmt="d",
            xticklabels=class_names, yticklabels=class_names,
            cmap="YlOrRd", linewidths=0.3, linecolor="white",
            ax=ax, square=True,
            cbar_kws={"shrink": 0.6, "label": "Count"},
        )
    else:
        im = ax.imshow(cm, interpolation="nearest", cmap="YlOrRd")
        fig.colorbar(im, ax=ax, shrink=0.6, label="Count")
        ax.set_xticks(range(n))
        ax.set_yticks(range(n))
        ax.set_xticklabels(class_names, rotation=90, fontsize=5)
        ax.set_yticklabels(class_names, fontsize=5)

    ax.set_xlabel("Predicted Label", fontsize=12)
    ax.set_ylabel("True Label", fontsize=12)
    ax.set_title("Confusion Matrix", fontsize=14, fontweight="bold")
    ax.tick_params(axis="both", labelsize=max(4, 10 - n // 10))
    fig.tight_layout()
    _save_fig(fig, filename)


def pretrained_model_evaluation(actions):
    """Run comprehensive evaluation on the pre-trained augmented model."""
    print("\n" + "=" * 70)
    print("  4. PRE-TRAINED MODEL DEEP EVALUATION")
    print("=" * 70)

    if not os.path.isfile(MODEL_AUGMENTED):
        print(f"  [!] Model not found: {MODEL_AUGMENTED}")
        return None

    model = load_model(MODEL_AUGMENTED)
    le = joblib.load(ENCODER_AUGMENTED)
    class_names = le.classes_

    X, y = load_combined_data(actions, ALL_SOURCES)
    y_enc = le.transform(y)

    _, X_test, _, y_test = train_test_split(
        X, y_enc, test_size=0.2, random_state=RANDOM_STATE, stratify=y_enc,
    )

    predictions = model.predict(X_test, verbose=0)
    y_pred = np.argmax(predictions, axis=1)
    max_conf = np.max(predictions, axis=1)
    correct = (y_pred == y_test)

    # Per-class analysis
    report = classification_report(
        y_test, y_pred, target_names=class_names, output_dict=True, zero_division=0,
    )
    cm = confusion_matrix(y_test, y_pred)

    # Top confused pairs
    confusions = []
    for i in range(len(class_names)):
        for j in range(len(class_names)):
            if i != j and cm[i, j] > 0:
                confusions.append({
                    "true": class_names[i],
                    "predicted": class_names[j],
                    "count": int(cm[i, j]),
                })
    confusions.sort(key=lambda x: x["count"], reverse=True)

    # Weakest classes (lowest F1)
    class_f1s = []
    for cls in class_names:
        if cls in report:
            class_f1s.append((cls, report[cls]["f1-score"]))
    class_f1s.sort(key=lambda x: x[1])

    print(f"\n  Overall Accuracy : {report['accuracy'] * 100:.2f}%")
    print(f"  Macro F1         : {report['macro avg']['f1-score'] * 100:.2f}%")
    print(f"  Weighted F1      : {report['weighted avg']['f1-score'] * 100:.2f}%")

    print(f"\n  CONFIDENCE ANALYSIS")
    print(f"  {'-' * 50}")
    print(f"  Mean Confidence (all)     : {np.mean(max_conf) * 100:.1f}%")
    print(f"  Mean Confidence (correct) : {np.mean(max_conf[correct]) * 100:.1f}%")
    if (~correct).sum() > 0:
        print(f"  Mean Confidence (wrong)   : {np.mean(max_conf[~correct]) * 100:.1f}%")
    low_conf_pct = np.mean(max_conf < 0.7) * 100
    print(f"  Low Confidence (<70%)     : {low_conf_pct:.1f}% of predictions")

    print(f"\n  TOP-5 WEAKEST CLASSES (by F1)")
    print(f"  {'-' * 50}")
    for cls, f1 in class_f1s[:5]:
        print(f"    {cls:<22} F1: {f1 * 100:.1f}%")

    if confusions:
        print(f"\n  TOP-5 MISCLASSIFICATION PAIRS")
        print(f"  {'-' * 50}")
        for c in confusions[:5]:
            print(f"    '{c['true']}' → '{c['predicted']}' ({c['count']}x)")

    if HAS_PLOT:
        _plot_confusion_matrix(cm, class_names, "pretrained_confusion_matrix")

        # Confidence distribution histogram
        fig, ax = plt.subplots(figsize=(10, 5))
        ax.hist(max_conf[correct], bins=50, alpha=0.7, color="#4CAF50", label="Correct", density=True)
        if (~correct).sum() > 0:
            ax.hist(max_conf[~correct], bins=50, alpha=0.7, color="#F44336", label="Incorrect", density=True)
        ax.axvline(0.7, color="#FF9800", linestyle="--", linewidth=2, label="Threshold (70%)")
        ax.set_xlabel("Prediction Confidence", fontsize=12)
        ax.set_ylabel("Density", fontsize=12)
        ax.set_title("Confidence Distribution: Correct vs Incorrect", fontsize=14, fontweight="bold")
        ax.legend(fontsize=10)
        fig.tight_layout()
        _save_fig(fig, "confidence_distribution")

        # Per-class F1 horizontal bar chart
        fig, ax = plt.subplots(figsize=(10, max(6, len(class_names) * 0.22)))
        names = [c[0] for c in class_f1s]
        scores = [c[1] * 100 for c in class_f1s]
        colors = ["#F44336" if s < 90 else "#FF9800" if s < 95 else "#4CAF50" for s in scores]
        ax.barh(names, scores, color=colors, edgecolor="white", linewidth=0.5)
        ax.set_xlabel("F1-Score (%)", fontsize=12)
        ax.set_title("Per-Class F1-Score", fontsize=14, fontweight="bold")
        ax.set_xlim(max(0, min(scores) - 10), 101)
        ax.tick_params(axis="y", labelsize=7)
        fig.tight_layout()
        _save_fig(fig, "per_class_f1_scores")

    return {
        "accuracy": report["accuracy"],
        "macro_f1": report["macro avg"]["f1-score"],
        "weighted_f1": report["weighted avg"]["f1-score"],
        "mean_confidence": float(np.mean(max_conf)),
        "low_confidence_pct": float(low_conf_pct),
        "weakest_classes": class_f1s[:5],
        "top_confusions": confusions[:10],
        "confusion_matrix": cm.tolist(),
    }


# ──────────────────────────────────────────────────────────────
# 5. Learning Curve Analysis
# ──────────────────────────────────────────────────────────────
def learning_curve_analysis(actions, data_sources):
    """Train on increasing data fractions to check if more data helps."""
    print("\n" + "=" * 70)
    print("  5. LEARNING CURVE ANALYSIS")
    print("=" * 70)

    X, y = load_combined_data(actions, data_sources)

    le = LabelEncoder()
    y_enc = le.fit_transform(y)
    num_classes = len(le.classes_)

    # Split off a consistent test set
    X_pool, X_test, y_pool, y_test = train_test_split(
        X, y_enc, test_size=0.2, random_state=RANDOM_STATE, stratify=y_enc,
    )

    fractions = [0.2, 0.4, 0.6, 0.8, 1.0]
    curve_results = []

    for frac in fractions:
        n_samples = int(len(X_pool) * frac)
        print(f"\n  Training with {frac * 100:.0f}% data ({n_samples} sequences)...")

        if frac < 1.0:
            X_train, _, y_train, _ = train_test_split(
                X_pool, y_pool, train_size=frac, random_state=RANDOM_STATE, stratify=y_pool,
            )
        else:
            X_train, y_train = X_pool, y_pool

        y_train_cat = to_categorical(y_train, num_classes)
        y_test_cat = to_categorical(y_test, num_classes)

        model = build_model(
            (X_train.shape[1], X_train.shape[2]),
            num_classes,
            use_dropout=True,
        )

        callbacks = [
            EarlyStopping(
                monitor="val_accuracy", patience=20, mode="max",
                restore_best_weights=True, verbose=0,
            ),
        ]

        model.fit(
            X_train, y_train_cat,
            epochs=EPOCHS,
            batch_size=BATCH_SIZE,
            validation_data=(X_test, y_test_cat),
            callbacks=callbacks,
            verbose=0,
        )

        _, train_acc = model.evaluate(X_train, y_train_cat, verbose=0)
        _, test_acc = model.evaluate(X_test, y_test_cat, verbose=0)

        gap = train_acc - test_acc

        print(f"  Train: {train_acc * 100:.2f}% | Test: {test_acc * 100:.2f}% | Gap: {gap * 100:.2f}%")

        curve_results.append({
            "fraction": frac,
            "n_samples": n_samples,
            "train_accuracy": float(train_acc),
            "test_accuracy": float(test_acc),
            "gap": float(gap),
        })
        del model

    # Interpretation
    test_accs = [r["test_accuracy"] for r in curve_results]
    gaps = [r["gap"] for r in curve_results]
    improvement = test_accs[-1] - test_accs[0]
    final_gap = gaps[-1]

    print(f"\n  {'-' * 60}")
    print(f"  LEARNING CURVE INTERPRETATION")
    print(f"  {'-' * 60}")
    if improvement < 0.02 and final_gap < 0.05:
        print(f"  [OK] Model has CONVERGED - performance plateaued with current data")
        print(f"     More data unlikely to significantly improve results")
    elif final_gap > 0.10:
        print(f"  [!] OVERFITTING detected (gap: {final_gap * 100:.1f}%)")
        print(f"     Consider more data, regularization, or augmentation")
    else:
        print(f"  📈 Performance is still IMPROVING - more data could help")

    if HAS_PLOT:
        fig, ax = plt.subplots(figsize=(10, 6))
        fracs = [r["fraction"] * 100 for r in curve_results]
        trains = [r["train_accuracy"] * 100 for r in curve_results]
        tests = [r["test_accuracy"] * 100 for r in curve_results]

        ax.plot(fracs, trains, "o-", color="#2196F3", linewidth=2.5, markersize=8, label="Training Accuracy")
        ax.plot(fracs, tests, "s-", color="#F44336", linewidth=2.5, markersize=8, label="Test Accuracy")
        ax.fill_between(fracs, tests, trains, alpha=0.1, color="#9C27B0", label="Generalization Gap")
        ax.set_xlabel("Training Data Used (%)", fontsize=12)
        ax.set_ylabel("Accuracy (%)", fontsize=12)
        ax.set_title("Learning Curve Analysis", fontsize=14, fontweight="bold")
        ax.legend(fontsize=10)
        ax.set_ylim(max(0, min(tests) - 10), 101)
        ax.grid(True, alpha=0.3)
        fig.tight_layout()
        _save_fig(fig, "learning_curve")

    return curve_results


# ──────────────────────────────────────────────────────────────
# 6. Robustness Analysis (Noise Injection)
# ──────────────────────────────────────────────────────────────
def noise_robustness_test(actions, data_sources):
    """Test model's tolerance to noisy inputs (simulates real-world variance)."""
    print("\n" + "=" * 70)
    print("  6. NOISE ROBUSTNESS ANALYSIS")
    print("=" * 70)

    if not os.path.isfile(MODEL_AUGMENTED):
        print(f"  [!] Model not found. Skipping.")
        return None

    model = load_model(MODEL_AUGMENTED)
    le = joblib.load(ENCODER_AUGMENTED)

    X, y = load_combined_data(actions, ALL_SOURCES)
    y_enc = le.transform(y)

    _, X_test, _, y_test = train_test_split(
        X, y_enc, test_size=0.2, random_state=RANDOM_STATE, stratify=y_enc,
    )

    noise_levels = [0.0, 0.005, 0.01, 0.02, 0.03, 0.05]
    noise_results = []

    for noise_std in noise_levels:
        if noise_std == 0:
            X_noisy = X_test
        else:
            noise = np.random.normal(0, noise_std, X_test.shape)
            X_noisy = X_test + noise

        preds = model.predict(X_noisy, verbose=0)
        y_pred = np.argmax(preds, axis=1)
        acc = accuracy_score(y_test, y_pred)
        _, _, f1, _ = precision_recall_fscore_support(
            y_test, y_pred, average="macro", zero_division=0,
        )

        print(f"  Noise σ={noise_std:.3f} → Accuracy: {acc * 100:.2f}%  F1: {f1 * 100:.2f}%")

        noise_results.append({
            "noise_std": noise_std,
            "accuracy": float(acc),
            "f1_score": float(f1),
        })

    # Degradation analysis
    base_acc = noise_results[0]["accuracy"]
    worst_acc = noise_results[-1]["accuracy"]
    drop = base_acc - worst_acc

    print(f"\n  {'-' * 50}")
    if drop < 0.05:
        print(f"  [OK] Model is ROBUST to noise (max drop: {drop * 100:.1f}%)")
    elif drop < 0.15:
        print(f"  [!] MODERATE sensitivity to noise (drop: {drop * 100:.1f}%)")
    else:
        print(f"  ❌ Model is FRAGILE to noise (drop: {drop * 100:.1f}%)")

    if HAS_PLOT:
        fig, ax = plt.subplots(figsize=(10, 5))
        sigmas = [r["noise_std"] for r in noise_results]
        accs = [r["accuracy"] * 100 for r in noise_results]
        f1s = [r["f1_score"] * 100 for r in noise_results]

        ax.plot(sigmas, accs, "o-", color="#2196F3", linewidth=2.5, markersize=8, label="Accuracy")
        ax.plot(sigmas, f1s, "s-", color="#FF9800", linewidth=2.5, markersize=8, label="Macro F1")
        ax.set_xlabel("Noise Standard Deviation (σ)", fontsize=12)
        ax.set_ylabel("Score (%)", fontsize=12)
        ax.set_title("Model Robustness to Noise", fontsize=14, fontweight="bold")
        ax.legend(fontsize=10)
        ax.grid(True, alpha=0.3)
        fig.tight_layout()
        _save_fig(fig, "noise_robustness")

    return noise_results


# ──────────────────────────────────────────────────────────────
# Main Orchestrator
# ──────────────────────────────────────────────────────────────
def main():
    total_t0 = time.time()
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    print("\n" + "#" * 70)
    print("  SignSpeak -- Comprehensive Validation Testing Suite")
    print(f"  Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("#" * 70)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    actions = load_actions()
    print(f"\n  Actions: {len(actions)} signs loaded")

    full_report = {
        "generated_at": datetime.now().isoformat(),
        "random_state": RANDOM_STATE,
        "n_folds": N_FOLDS,
    }

    # ── Run all validation tests ──
    print("\n\n" + "*" * 70)
    print("  Running Test 1/6: Dataset Statistics")
    print("*" * 70)
    full_report["dataset_statistics"] = dataset_statistics(actions, ALL_SOURCES)

    print("\n\n" + "*" * 70)
    print("  Running Test 2/6: K-Fold Cross-Validation")
    print("  [!] This will train 5 models from scratch -- may take 15-30 minutes")
    print("*" * 70)
    full_report["kfold_cv"] = kfold_cross_validation(actions, ALL_SOURCES)

    print("\n\n" + "*" * 70)
    print("  Running Test 3/6: Cross-Source Validation")
    print("  [!] Training 2 models (laptop->mobile, mobile->laptop)")
    print("*" * 70)
    full_report["cross_source"] = cross_source_validation(actions)

    print("\n\n" + "*" * 70)
    print("  Running Test 4/6: Pre-Trained Model Deep Evaluation")
    print("*" * 70)
    full_report["pretrained_eval"] = pretrained_model_evaluation(actions)

    print("\n\n" + "*" * 70)
    print("  Running Test 5/6: Learning Curve Analysis")
    print("  [!] Training 5 models at different data fractions")
    print("*" * 70)
    full_report["learning_curve"] = learning_curve_analysis(actions, ALL_SOURCES)

    print("\n\n" + "*" * 70)
    print("  Running Test 6/6: Noise Robustness")
    print("*" * 70)
    full_report["noise_robustness"] = noise_robustness_test(actions, ALL_SOURCES)

    # ── Save full report ──
    total_duration = time.time() - total_t0
    full_report["total_duration_seconds"] = round(total_duration, 1)

    report_path = os.path.join(OUTPUT_DIR, f"validation_report_{timestamp}.json")
    with open(report_path, "w") as f:
        json.dump(full_report, f, indent=2, default=str)

    # Summary
    m, s = divmod(int(total_duration), 60)
    print("\n\n" + "#" * 70)
    print("  VALIDATION TESTING COMPLETE")
    print("#" * 70)

    kfold = full_report.get("kfold_cv", {})
    print(f"\n  K-Fold CV Accuracy  : {kfold.get('mean_accuracy', 0) * 100:.2f}% ± {kfold.get('ci_95_accuracy', 0) * 100:.2f}% (95% CI)")
    print(f"  K-Fold CV F1-Score  : {kfold.get('mean_f1', 0) * 100:.2f}% ± {kfold.get('ci_95_f1', 0) * 100:.2f}%")

    cross = full_report.get("cross_source")
    if cross:
        for key, r in cross.items():
            print(f"  Cross-Source ({r['train_source']}→{r['test_source']}): {r['accuracy'] * 100:.2f}%")

    print(f"\n  Total Time          : {m}m {s}s")
    print(f"  Full Report         : {report_path}")

    if HAS_PLOT:
        print(f"  Visualizations      : {OUTPUT_DIR}/")
        charts = [f for f in os.listdir(OUTPUT_DIR) if f.endswith(".png")]
        for c in sorted(charts):
            print(f"    [PLOT] {c}")
    else:
        print(f"\n  [TIP] Install matplotlib & seaborn for visualizations:")
        print(f"     pip install matplotlib seaborn")

    print("\n  Done! [OK]\n")


if __name__ == "__main__":
    main()
