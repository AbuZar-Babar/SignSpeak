"""
Training Session Logger

Logs training session details to a CSV file for tracking experiments.
"""

import os
import csv
from datetime import datetime


def _get_log_path():
    """Get the path to the training history CSV file."""
    project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    return os.path.join(project_root, "training_history.csv")


def log_training_session(
    duration_seconds: float,
    num_words: int,
    training_acc: float,
    val_acc: float,
    epochs: int,
    batch_size: int,
    augmented: bool,
    model_path: str,
):
    """Log a training session to the training history CSV."""
    log_path = _get_log_path()
    file_exists = os.path.exists(log_path)

    with open(log_path, "a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow([
                "timestamp", "duration_s", "num_words", "train_acc",
                "val_acc", "epochs", "batch_size", "augmented", "model_path",
            ])
        writer.writerow([
            datetime.now().isoformat(),
            round(duration_seconds, 1),
            num_words,
            round(training_acc, 4),
            round(val_acc, 4),
            epochs,
            batch_size,
            augmented,
            model_path,
        ])

    print(f"📝 Training session logged to {log_path}")


def log_comparison_session(**kwargs):
    """Stub for comparison logging (originally in archived compare_models.py)."""
    print(f"📝 Comparison session: {kwargs}")
