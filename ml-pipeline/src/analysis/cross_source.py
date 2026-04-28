"""
Cross-Source Validator
========================
Compares hand landmark distributions across laptop and mobile data sources
to assess domain gap and statistical differences.
"""

import numpy as np
from typing import Dict, List, Any, Tuple
from scipy import stats


def compare_sources(X_laptop: np.ndarray, y_laptop: np.ndarray,
                   X_mobile: np.ndarray, y_mobile: np.ndarray,
                   action_names: List[str]) -> Dict[str, Any]:
    """
    Compare feature distributions between laptop and mobile sources.
    
    Args:
        X_laptop: Laptop data array of shape (N, seq_len, features)
        y_laptop: Laptop labels
        X_mobile: Mobile data array of shape (N, seq_len, features)
        y_mobile: Mobile labels
        action_names: List of action names
    
    Returns:
        Dict with KS test results and domain gap assessment
    """
    # Reshape to (N*seq_len, features)
    X_lap_flat = X_laptop.reshape(-1, X_laptop.shape[2])
    X_mob_flat = X_mobile.reshape(-1, X_mobile.shape[2])
    
    n_features = X_laptop.shape[2]
    n_landmarks = n_features // 3
    
    # Perform Kolmogorov-Smirnov test for each feature
    ks_stats = []
    p_values = []
    landmark_comparisons = []
    
    for feat_idx in range(n_features):
        # KS test for this feature
        ks_stat, p_val = stats.ks_2samp(X_lap_flat[:, feat_idx], 
                                        X_mob_flat[:, feat_idx])
        ks_stats.append(ks_stat)
        p_values.append(p_val)
        
        # Determine which landmark this belongs to
        lm_idx = feat_idx // 3
        coord_idx = feat_idx % 3
        
        landmark_comparisons.append({
            "feature_id": feat_idx,
            "landmark_id": lm_idx,
            "coordinate": ["x", "y", "z"][coord_idx],
            "ks_statistic": float(ks_stat),
            "p_value": float(p_val),
            "significant": p_val < 0.05
        })
    
    # Calculate overall domain gap score (mean KS statistic)
    domain_gap_score = float(np.mean(ks_stats))
    
    # Count significant differences
    n_significant = sum(1 for p in p_values if p < 0.05)
    
    return {
        "num_laptop_sequences": X_laptop.shape[0],
        "num_mobile_sequences": X_mobile.shape[0],
        "num_features": n_features,
        "num_landmarks": n_landmarks,
        "ks_statistic": float(np.mean(ks_stats)),
        "p_value": float(np.mean(p_values)),
        "domain_gap_score": domain_gap_score,
        "significant_differences": n_significant,
        "landmark_comparisons": landmark_comparisons
    }
