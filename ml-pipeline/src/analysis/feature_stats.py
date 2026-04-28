"""
Feature Statistics Analyzer
=============================
Computes per-landmark statistics, temporal evolution, and distribution analysis.
"""

import numpy as np
from typing import Dict, List, Any


class FeatureAnalyzer:
    """
    Analyzes feature-level characteristics of hand landmark data.
    """
    
    def __init__(self):
        """Initialize analyzer."""
        pass
    
    def per_landmark_statistics(self, X: np.ndarray, y: np.ndarray,
                               action_names: List[str]) -> Dict[str, Any]:
        """
        Compute mean, std, min, max for each landmark across sequences.
        
        Args:
            X: Data array of shape (N, seq_len, features)
            y: Labels array
            action_names: List of action names
        
        Returns:
            Dict with per-landmark statistics
        """
        n_sequences = X.shape[0]
        n_frames_total = X.shape[0] * X.shape[1]
        n_features = X.shape[2]
        n_landmarks = n_features // 3
        
        # Reshape to (N*seq_len, features)
        X_flat = X.reshape(-1, n_features)
        
        landmarks = []
        for lm_idx in range(n_landmarks):
            # Extract 3 coordinates for this landmark
            coords_idx = [lm_idx * 3 + i for i in range(3)]
            coords = X_flat[:, coords_idx]
            
            landmarks.append({
                "landmark_id": lm_idx,
                "mean": np.mean(coords, axis=0).tolist(),
                "std": np.std(coords, axis=0).tolist(),
                "min": np.min(coords, axis=0).tolist(),
                "max": np.max(coords, axis=0).tolist(),
            })
        
        return {
            "num_sequences": n_sequences,
            "num_frames": n_frames_total,
            "num_features": n_features,
            "num_landmarks": n_landmarks,
            "landmarks": landmarks
        }
    
    def temporal_evolution(self, X: np.ndarray, y: np.ndarray,
                          action_names: List[str],
                          action_idx: int = 0) -> Dict[str, Any]:
        """
        Analyze how landmarks change across frames (temporal dynamics).
        
        Args:
            X: Data array
            y: Labels array
            action_names: List of action names
            action_idx: Index of action to analyze
        
        Returns:
            Dict with temporal statistics
        """
        # Get sequences for this action
        action_mask = y == action_idx
        X_action = X[action_mask]  # shape: (n_seqs, n_frames, n_features)
        
        # Compute frame-to-frame displacement (velocity)
        n_frames = X_action.shape[1]
        n_features = X_action.shape[2]
        
        velocities = []
        for seq in X_action:
            # Frame-to-frame deltas: (n_frames-1, n_features)
            seq_velocity = np.diff(seq, axis=0)
            # Magnitude of displacement per frame
            velocity_mag = np.linalg.norm(seq_velocity, axis=1)
            velocities.append(velocity_mag)
        
        velocities = np.concatenate(velocities)  # Flatten all velocities
        
        return {
            "action": action_names[action_idx] if action_idx < len(action_names) else f"unknown_{action_idx}",
            "num_frames": n_frames,
            "mean_velocity": float(np.mean(velocities)),
            "max_velocity": float(np.max(velocities)),
            "min_velocity": float(np.min(velocities)),
            "std_velocity": float(np.std(velocities)),
        }
