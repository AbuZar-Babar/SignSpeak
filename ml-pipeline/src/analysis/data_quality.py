import numpy as np
from typing import Dict, List, Any
from collections import Counter
import json


class DataQualityAnalyzer:
    def __init__(self, sequence_length: int = 60, coordinate_dim: int = 126):
        self.sequence_length = sequence_length
        self.coordinate_dim = coordinate_dim
    
    def detect_empty_landmarks(self, X: np.ndarray, y: np.ndarray,
                               action_names: List[str], 
                               threshold: float = 0.95) -> Dict[str, Any]:
        empty_sequences = []
        total_frames = 0
        empty_frames = 0
        
        for seq_idx, seq in enumerate(X):
            n_frames = seq.shape[0]
            total_frames += n_frames
            
            empty_frame_count = 0
            for frame in seq:
                zero_ratio = np.sum(frame == 0) / len(frame)
                if zero_ratio >= threshold:
                    empty_frame_count += 1
                    empty_frames += 1
            
            if empty_frame_count > 0:
                action = action_names[int(y[seq_idx])] if int(y[seq_idx]) < len(action_names) else f"unknown_{y[seq_idx]}"
                empty_sequences.append({
                    "sequence_id": seq_idx,
                    "action": action,
                    "total_frames": n_frames,
                    "empty_frames": empty_frame_count,
                    "empty_ratio": empty_frame_count / n_frames
                })
        
        return {
            "total_frames_analyzed": total_frames,
            "empty_frames_count": empty_frames,
            "empty_frames_ratio": empty_frames / total_frames if total_frames > 0 else 0,
            "sequences_with_empty_frames": len(empty_sequences),
            "problematic_sequences": empty_sequences
        }
    
    def validate_coordinate_ranges(self, X: np.ndarray) -> Dict[str, Any]:
        issues = []
        out_of_range_samples = np.where((X < 0) | (X > 1))
        
        if len(out_of_range_samples[0]) > 0:
            unique_seqs = np.unique(out_of_range_samples[0])
            for seq_idx in unique_seqs:
                seq_issues = np.where((X[seq_idx] < 0) | (X[seq_idx] > 1))
                n_issues = len(seq_issues[0])
                min_val = np.min(X[seq_idx])
                max_val = np.max(X[seq_idx])
                
                issues.append({
                    "sequence_id": int(seq_idx),
                    "out_of_range_coordinates": n_issues,
                    "min_value": float(min_val),
                    "max_value": float(max_val)
                })
        
        return {
            "min_coordinate": float(np.min(X)),
            "max_coordinate": float(np.max(X)),
            "mean_coordinate": float(np.mean(X)),
            "std_coordinate": float(np.std(X)),
            "valid_range": [0.0, 1.0],
            "out_of_range_sequences": len(issues),
            "problematic_sequences": issues
        }
    
    def report_class_distribution(self, y: np.ndarray, 
                                 action_names: List[str]) -> Dict[str, Any]:
        class_counts = Counter(y)
        total_samples = len(y)
        sorted_classes = sorted(class_counts.items(), key=lambda x: x[1], reverse=True)
        
        class_dist = []
        for class_idx, count in sorted_classes:
            if class_idx < len(action_names):
                action = action_names[class_idx]
            else:
                action = f"unknown_{class_idx}"
            
            percentage = (count / total_samples) * 100
            class_dist.append({
                "action": action,
                "class_id": int(class_idx),
                "count": int(count),
                "percentage": round(percentage, 2)
            })
        
        counts_array = np.array([count for _, count in sorted_classes])
        imbalance_ratio = counts_array.max() / counts_array.min() if counts_array.min() > 0 else float('inf')
        
        return {
            "total_samples": total_samples,
            "num_classes": len(class_counts),
            "samples_per_class": class_dist,
            "imbalance_ratio": round(float(imbalance_ratio), 2),
            "most_common": class_dist[0]["action"] if class_dist else None,
            "least_common": class_dist[-1]["action"] if class_dist else None,
            "minority_class_count": int(counts_array.min()) if len(counts_array) > 0 else 0,
            "majority_class_count": int(counts_array.max()) if len(counts_array) > 0 else 0
        }
    
    def generate_full_report(self, X: np.ndarray, y: np.ndarray,
                            action_names: List[str]) -> Dict[str, Any]:
        report = {
            "dataset_shape": {
                "n_sequences": X.shape[0],
                "frames_per_sequence": X.shape[1],
                "features_per_frame": X.shape[2]
            },
            "missing_frames": self.check_missing_frames(X, y, action_names),
            "empty_landmarks": self.detect_empty_landmarks(X, y, action_names),
            "coordinate_ranges": self.validate_coordinate_ranges(X),
            "class_distribution": self.report_class_distribution(y, action_names),
            "timestamp": np.datetime64('now').astype(str)
        }
        return report
    
    def check_missing_frames(self, X: np.ndarray, y: np.ndarray, 
                            action_names: List[str]) -> Dict[str, Any]:
        n_sequences = X.shape[0]
        expected_len = self.sequence_length
        issues = []
        
        for idx, seq in enumerate(X):
            actual_len = seq.shape[0]
            if actual_len != expected_len:
                action = action_names[int(y[idx])] if int(y[idx]) < len(action_names) else f"unknown_{y[idx]}"
                issues.append({
                    "sequence_id": idx,
                    "action": action,
                    "expected_frames": expected_len,
                    "actual_frames": actual_len,
                    "missing": expected_len - actual_len
                })
        
        return {
            "total_sequences": n_sequences,
            "valid_sequences": n_sequences - len(issues),
            "problematic_sequences": len(issues),
            "issues": issues
        }
    
    def print_summary(self):
        report = self.generate_full_report(None, None, [])
        print("\n" + "="*70)
        print("DATA QUALITY REPORT")
        print("="*70 + "\n")
