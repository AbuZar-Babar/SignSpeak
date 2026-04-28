"""
Data Quality Analyzer
======================
Validates data integrity for the PSL sign language dataset.
"""

import numpy as np
from typing import Dict, List, Any
from collections import Counter
import json


class DataQualityAnalyzer:
    """
    Validates data quality for sign language datasets.
    """
    
    def __init__(self, sequence_length: int = 60, coordinate_dim: int = 126):
        """Initialize analyzer."""
        self.sequence_length = sequence_length
        self.coordinate_dim = coordinate_dim
    
    def detect_empty_landmarks(self, X: np.ndarray, y: np.ndarray,
                              action_names: List[str], 
                              threshold: float = 0.95) -> Dict[str, Any]:
        """
        Detect frames where landmarks are missing/zeroed out.
        
        Args:
            X: Data array of shape (N, seq_len, features)
            y: Labels array
            action_names: List of action names
            threshold: Percentage threshold for flagging empty frames
        
        Returns:
            Dict with empty frame statistics
        """
        empty_sequences = []
        total_frames = 0
        empty_frames = 0
        
        for seq_idx, seq in enumerate(X):
            n_frames = seq.shape[0]
            total_frames += n_frames
            
            empty_frame_count = 0
            for frame_idx, frame in enumerate(seq):
                zero_ratio = np.sum(frame == 0) / len(frame)
                if zero_ratio >= threshold:
                    empty_frame_count += 1
                    empty_frames += 1
            
            if empty_frame_count > 0:
                action = action_names[y[seq_idx]] if y[seq_idx] < len(action_names) else f"unknown_{y[seq_idx]}"
                empty_sequences.append({
                    "sequence_id": seq_idx,
                    "action": action,
                    "total_frames": n_frames,
                    "empty_frames": empty_frame_count,
                    "empty_ratio": empty_frame_count / n_frames
                })
        
        report = {
            "total_frames_analyzed": total_frames,
            "empty_frames_count": empty_frames,
            "empty_frames_ratio": empty_frames / total_frames if total_frames > 0 else 0,
            "sequences_with_empty_frames": len(empty_sequences),
            "problematic_sequences": empty_sequences
        }
        
        return report
    
    def validate_coordinate_ranges(self, X: np.ndarray) -> Dict[str, Any]:
        """Ensure all coordinates are within valid range [0, 1]."""
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
        
        report = {
            "min_coordinate": float(np.min(X)),
            "max_coordinate": float(np.max(X)),
            "mean_coordinate": float(np.mean(X)),
            "std_coordinate": float(np.std(X)),
            "valid_range": [0.0, 1.0],
            "out_of_range_sequences": len(issues),
            "problematic_sequences": issues
        }
        
        return report
    
    def report_class_distribution(self, y: np.ndarray, 
                                 action_names: List[str]) -> Dict[str, Any]:
        """Generate class distribution and imbalance report."""
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
        
        report = {
            "total_samples": total_samples,
            "num_classes": len(class_counts),
            "samples_per_class": class_dist,
            "imbalance_ratio": round(float(imbalance_ratio), 2),
            "most_common": class_dist[0]["action"] if class_dist else None,
            "least_common": class_dist[-1]["action"] if class_dist else None,
            "minority_class_count": int(counts_array.min()) if len(counts_array) > 0 else 0,
            "majority_class_count": int(counts_array.max()) if len(counts_array) > 0 else 0
        }
        
        return report
