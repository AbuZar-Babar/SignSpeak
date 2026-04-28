import numpy as np
from typing import Dict, List, Any


def validate_augmented_samples(X_augmented: np.ndarray, 
                               y_augmented: np.ndarray) -> Dict[str, Any]:
    n_samples = X_augmented.shape[0]
    valid_count = 0
    invalid_samples = []
    
    for idx, sample in enumerate(X_augmented):
        if np.all(sample >= 0) and np.all(sample <= 1):
            valid_count += 1
        else:
            invalid_samples.append({
                "sample_id": idx,
                "min_value": float(np.min(sample)),
                "max_value": float(np.max(sample))
            })
    
    return {
        "total_samples": n_samples,
        "valid_samples": valid_count,
        "invalid_samples": len(invalid_samples),
        "validation_passed": len(invalid_samples) == 0,
        "invalid_details": invalid_samples
    }


def preview_augmentations(X: np.ndarray, seq_id: int, 
                          num_augmented: int = 3) -> Dict[str, Any]:
    preview = {
        "sequence_id": seq_id,
        "original": X.tolist() if isinstance(X, np.ndarray) else X,
        "num_frames": X.shape[0] if isinstance(X, np.ndarray) else len(X),
        "num_features": X.shape[1] if isinstance(X, np.ndarray) else len(X[0]) if X else 0
    }
    return preview
