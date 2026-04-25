import os
import json
import time
import numpy as np
import joblib
from datetime import datetime
from collections import Counter
from tensorflow.keras.models import load_model
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from sklearn.model_selection import StratifiedKFold, train_test_split
from sklearn.metrics import (
    classification_report, confusion_matrix,
    precision_recall_fscore_support, accuracy_score,
)
from sklearn.preprocessing import LabelEncoder
from actions_config import load_actions, SEQUENCE_LENGTH, BATCH_SIZE, EPOCHS, LEARNING_RATE
from data_loader import load_data
from train_combined import build_model

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_SOURCES = {
    "laptop": os.path.join(SCRIPT_DIR, "MP_Data"),
    "mobile": os.path.join(SCRIPT_DIR, "MP_Data_mobile"),
}
MODEL_AUGMENTED = os.path.join("all_models", "action_model_augmented_legacy.h5")
ENCODER_AUGMENTED = os.path.join("all_models", "label_encoder_augmented_new.pkl")
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "validation_results")
RANDOM_STATE = 42
N_FOLDS = 5

HAS_PLOT = False
try:
    import matplotlib.pyplot as plt
    HAS_PLOT = True
except ImportError:
    pass

HAS_SNS = False
try:
    import seaborn as sns
    HAS_SNS = True
except ImportError:
    pass

def _save_fig(fig, name):
    path = os.path.join(OUTPUT_DIR, f"{name}.png")
    fig.savefig(path, dpi=150, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print(f"  Saved: {os.path.basename(path)}")
    return path

def dataset_statistics(actions, X, y):
    print("\nDataset Statistics")
    print("-" * 30)
    class_counts = Counter(y)
    counts = np.array([class_counts[a] for a in actions])
    print(f"\n  Total Sequences  : {len(X)}")
    print(f"  Total Classes    : {len(actions)}")
    print(f"  Sequence Length  : {X.shape[1]} frames")
    print(f"  Features/Frame   : {X.shape[2]}")
    print(f"  Min Samples/Class: {counts.min()} ({actions[counts.argmin()]})")
    print(f"  Max Samples/Class: {counts.max()} ({actions[counts.argmax()]})")
    print(f"  Mean ± Std       : {counts.mean():.1f} ± {counts.std():.1f}")
    imbalance_ratio = counts.max() / counts.min() if counts.min() > 0 else 0
    print(f"  Imbalance Ratio  : {imbalance_ratio:.2f}x")
    flat = X.reshape(-1, X.shape[-1])
    zero_ratio = np.mean(flat == 0) * 100
    print(f"\n  Feature Range    : [{flat.min():.4f}, {flat.max():.4f}]")
    print(f"  Zero-Values      : {zero_ratio:.1f}%")
    print(f"\n  {'Class':<22} {'Samples':>8} {'Pct':>7}")
    print(f"  {'-' * 40}")
    for action in sorted(actions):
        c = class_counts[action]
        pct = c / len(y) * 100 if len(y) > 0 else 0
        print(f"  {action:<22} {c:>8} {pct:>6.1f}%")
    if HAS_PLOT:
        fig, ax = plt.subplots(figsize=(14, 6))
        sorted_actions = sorted(actions)
        sorted_counts = [class_counts[a] for a in sorted_actions]
        colors = plt.cm.viridis(np.linspace(0.2, 0.8, len(sorted_actions)))
        ax.barh(sorted_actions, sorted_counts, color=colors, edgecolor="white", linewidth=0.5)
        ax.set_xlabel("Number of Sequences")
        ax.set_title("Dataset Class Distribution")
        ax.invert_yaxis()
        fig.tight_layout()
        _save_fig(fig, "class_distribution")
    return {
        "total_sequences": len(X),
        "num_classes": len(actions),
        "class_counts": {a: int(class_counts[a]) for a in actions},
    }

def kfold_cross_validation(actions, X, y, n_folds=N_FOLDS):
    print("\nK-Fold Cross-Validation")
    print("-" * 30)
    le = LabelEncoder()
    y_encoded = le.fit_transform(y)
    num_classes = len(le.classes_)
    skf = StratifiedKFold(n_splits=n_folds, shuffle=True, random_state=RANDOM_STATE)
    fold_results = []
    all_y_true, all_y_pred = [], []
    for fold_idx, (train_idx, test_idx) in enumerate(skf.split(X, y_encoded), 1):
        print(f"\n  -- Fold {fold_idx}/{n_folds} --")
        X_train, X_test = X[train_idx], X[test_idx]
        y_train_enc, y_test_enc = y_encoded[train_idx], y_encoded[test_idx]
        y_train_cat = to_categorical(y_train_enc, num_classes)
        y_test_cat = to_categorical(y_test_enc, num_classes)
        model = build_model((X_train.shape[1], X_train.shape[2]), num_classes, use_dropout=True)
        callbacks = [EarlyStopping(monitor="val_accuracy", patience=25, restore_best_weights=True, verbose=0)]
        model.fit(X_train, y_train_cat, epochs=EPOCHS, batch_size=BATCH_SIZE, validation_data=(X_test, y_test_cat), callbacks=callbacks, verbose=0)
        y_pred = np.argmax(model.predict(X_test, verbose=0), axis=1)
        acc = accuracy_score(y_test_enc, y_pred)
        print(f"  Accuracy : {acc * 100:.2f}%")
        fold_results.append({"fold": fold_idx, "accuracy": float(acc)})
        all_y_true.extend(y_test_enc.tolist()); all_y_pred.extend(y_pred.tolist())
        del model
    mean_acc = np.mean([r["accuracy"] for r in fold_results])
    print(f"\nMean Accuracy: {mean_acc * 100:.2f}%")
    if HAS_PLOT:
        cm = confusion_matrix(all_y_true, all_y_pred)
        _plot_confusion_matrix(cm, le.classes_, "kfold_confusion_matrix")
    return {"mean_accuracy": float(mean_acc), "fold_results": fold_results}

def cross_source_validation(actions, X_lap, y_lap, X_mob, y_mob):
    print("\nCross-Source Validation")
    print("-" * 30)
    results = {}
    sources = [("Laptop", X_lap, y_lap, "Mobile", X_mob, y_mob), ("Mobile", X_mob, y_mob, "Laptop", X_lap, y_lap)]
    for t_name, X_t, y_t, s_name, X_s, y_s in sources:
        print(f"\n  -- Train: {t_name} -> Test: {s_name} --")
        le = LabelEncoder(); le.fit(np.concatenate([y_t, y_s]))
        common = list(set(y_t) & set(y_s))
        m_t, m_s = np.isin(y_t, common), np.isin(y_s, common)
        y_t_enc = le.transform(y_t[m_t]); y_s_enc = le.transform(y_s[m_s])
        num_c = len(le.classes_)
        y_t_cat = to_categorical(y_t_enc, num_c)
        model = build_model((X_t.shape[1], X_t.shape[2]), num_c, use_dropout=True)
        model.fit(X_t[m_t], y_t_cat, epochs=EPOCHS, batch_size=BATCH_SIZE, validation_split=0.15, verbose=0)
        acc = accuracy_score(y_s_enc, np.argmax(model.predict(X_s[m_s], verbose=0), axis=1))
        print(f"  Accuracy : {acc * 100:.2f}%")
        results[f"{t_name.lower()}_to_{s_name.lower()}"] = {"accuracy": float(acc)}
        del model
    return results

def _plot_confusion_matrix(cm, class_names, filename):
    if not HAS_PLOT: return
    n = len(class_names)
    fig, ax = plt.subplots(figsize=(max(10, n*0.25), max(10, n*0.25)))
    if HAS_SNS: sns.heatmap(cm, annot=(n<=30), fmt="d", xticklabels=class_names, yticklabels=class_names, cmap="YlOrRd", ax=ax, square=True)
    else: ax.imshow(cm, interpolation="nearest", cmap="YlOrRd")
    ax.set_title("Confusion Matrix"); fig.tight_layout(); _save_fig(fig, filename)

def pretrained_model_evaluation(actions, X, y):
    print("\nPre-Trained Model Evaluation")
    print("-" * 30)
    if not os.path.isfile(MODEL_AUGMENTED): return None
    model = load_model(MODEL_AUGMENTED, compile=False); le = joblib.load(ENCODER_AUGMENTED)
    y_enc = le.transform(y)
    _, X_test, _, y_test = train_test_split(X, y_enc, test_size=0.2, random_state=RANDOM_STATE, stratify=y_enc)
    y_pred = np.argmax(model.predict(X_test, verbose=0), axis=1)
    acc = accuracy_score(y_test, y_pred)
    print(f"  Accuracy : {acc * 100:.2f}%")
    if HAS_PLOT: _plot_confusion_matrix(confusion_matrix(y_test, y_pred), le.classes_, "pretrained_confusion_matrix")
    return {"accuracy": float(acc)}

def learning_curve_analysis(actions, X, y):
    print("\nLearning Curve Analysis")
    print("-" * 30)
    le = LabelEncoder(); y_enc = le.fit_transform(y); num_c = len(le.classes_)
    X_pool, X_test, y_pool, y_test = train_test_split(X, y_enc, test_size=0.2, random_state=RANDOM_STATE, stratify=y_enc)
    results = []
    for frac in [0.2, 0.4, 0.6, 0.8, 1.0]:
        X_tr, _, y_tr, _ = train_test_split(X_pool, y_pool, train_size=frac, random_state=RANDOM_STATE, stratify=y_pool) if frac < 1.0 else (X_pool, None, y_pool, None)
        model = build_model((X_tr.shape[1], X_tr.shape[2]), num_c, use_dropout=True)
        model.fit(X_tr, to_categorical(y_tr, num_c), epochs=EPOCHS, batch_size=BATCH_SIZE, validation_data=(X_test, to_categorical(y_test, num_c)), verbose=0)
        acc = accuracy_score(y_test, np.argmax(model.predict(X_test, verbose=0), axis=1))
        print(f"  Data {frac * 100:.0f}% -> Accuracy: {acc * 100:.2f}%")
        results.append({"fraction": frac, "test_accuracy": float(acc)}); del model
    return results

def noise_robustness_test(actions, X, y):
    print("\nNoise Robustness Analysis")
    print("-" * 30)
    if not os.path.isfile(MODEL_AUGMENTED): return None
    model = load_model(MODEL_AUGMENTED, compile=False); le = joblib.load(ENCODER_AUGMENTED)
    _, X_test, _, y_test = train_test_split(X, le.transform(y), test_size=0.2, random_state=RANDOM_STATE, stratify=le.transform(y))
    results = []
    for noise in [0.0, 0.01, 0.03, 0.05]:
        X_n = X_test + np.random.normal(0, noise, X_test.shape) if noise > 0 else X_test
        acc = accuracy_score(y_test, np.argmax(model.predict(X_n, verbose=0), axis=1))
        print(f"  Noise σ={noise:.3f} -> Accuracy: {acc * 100:.2f}%")
        results.append({"noise_std": noise, "accuracy": float(acc)})
    return results

def main():
    total_t0 = time.time()
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    print("\nSignSpeak Model Evaluation")
    print("-" * 30)
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    actions = load_actions()
    
    print("\n[1/2] Loading Laptop Data...")
    X_lap, y_lap = load_data(actions, [DATA_SOURCES["laptop"]])
    print("\n[2/2] Loading Mobile Data...")
    X_mob, y_mob = load_data(actions, [DATA_SOURCES["mobile"]])
    
    X_all = np.concatenate([X_lap, X_mob])
    y_all = np.concatenate([y_lap, y_mob])
    
    full_report = {"generated_at": datetime.now().isoformat()}
    full_report["dataset_statistics"] = dataset_statistics(actions, X_all, y_all)
    full_report["kfold_cv"] = kfold_cross_validation(actions, X_all, y_all)
    full_report["cross_source"] = cross_source_validation(actions, X_lap, y_lap, X_mob, y_mob)
    full_report["pretrained_eval"] = pretrained_model_evaluation(actions, X_all, y_all)
    full_report["learning_curve"] = learning_curve_analysis(actions, X_all, y_all)
    full_report["noise_robustness"] = noise_robustness_test(actions, X_all, y_all)
    
    report_path = os.path.join(OUTPUT_DIR, f"report_{timestamp}.json")
    with open(report_path, "w") as f: json.dump(full_report, f, indent=2, default=str)
    print(f"\nDone. Report saved to {report_path} | Total Time: {time.time()-total_t0:.1f}s")

if __name__ == "__main__":
    main()
