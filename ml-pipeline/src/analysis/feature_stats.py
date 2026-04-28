import numpy as np
from typing import Dict, List, Any


class FeatureAnalyzer:
    def __init__(self):
        pass
    
    def per_landmark_statistics(self, X: np.ndarray, y: np.ndarray,
                               action_names: List[str]) -> Dict[str, Any]:
        n_sequences = X.shape[0]
        n_frames_total = X.shape[0] * X.shape[1]
        n_features = X.shape[2]
        n_landmarks = n_features // 3
        
        X_flat = X.reshape(-1, n_features)
        
        landmarks = []
        for lm_idx in range(n_landmarks):
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
        action_mask = y == action_idx
        X_action = X[action_mask]
        
        n_frames = X_action.shape[1]
        
        velocities = []
        for seq in X_action:
            seq_velocity = np.diff(seq, axis=0)
            velocity_mag = np.linalg.norm(seq_velocity, axis=1)
            velocities.append(velocity_mag)
        
        velocities = np.concatenate(velocities)
        
        return {
            "action": action_names[int(action_idx)] if int(action_idx) < len(action_names) else f"unknown_{action_idx}",
            "num_frames": n_frames,
            "mean_velocity": float(np.mean(velocities)),
            "max_velocity": float(np.max(velocities)),
            "min_velocity": float(np.min(velocities)),
            "std_velocity": float(np.std(velocities)),
        }
