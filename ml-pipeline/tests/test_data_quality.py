"""
Tests for DataQualityAnalyzer - Empty Landmarks Detection
"""

import pytest
import numpy as np
from src.analysis.data_quality import DataQualityAnalyzer


class TestDetectEmptyLandmarks:
    """Test detecting frames where landmarks are all zeros (missing data)."""
    
    def test_detects_completely_empty_frame(self):
        """Should flag frame with all zero coordinates."""
        # ARRANGE: Create sequence with one frame that's all zeros
        X = np.array([
            [
                [0.5, 0.5, 0.5] * 42,  # Frame 0: normal
                [0.0, 0.0, 0.0] * 42,  # Frame 1: all zeros (empty)
                [0.5, 0.5, 0.5] * 42,  # Frame 2: normal
            ]
        ], dtype=np.float32)
        
        y = np.array([0])
        action_names = ["hello"]
        
        # ACT
        analyzer = DataQualityAnalyzer()
        result = analyzer.detect_empty_landmarks(X, y, action_names, threshold=0.95)
        
        # ASSERT
        assert result["empty_frames_count"] == 1
        assert result["sequences_with_empty_frames"] == 1
        assert len(result["problematic_sequences"]) == 1
        assert result["problematic_sequences"][0]["empty_frames"] == 1


class TestValidateCoordinateRanges:
    """Test validating landmark coordinates are within [0, 1]."""
    
    def test_detects_out_of_range_coordinates(self):
        """Should flag coordinates outside [0, 1]."""
        # ARRANGE: Create sequence with one frame having out-of-range values
        X = np.array([
            [
                [0.5, 0.5, 0.5] * 42,  # Frame 0: normal
                [1.5, 0.5, -0.1] + [0.5] * 123,  # Frame 1: has 1.5 and -0.1 (out of range)
                [0.5, 0.5, 0.5] * 42,  # Frame 2: normal
            ]
        ], dtype=np.float32)
        
        # ACT
        analyzer = DataQualityAnalyzer()
        result = analyzer.validate_coordinate_ranges(X)
        
        # ASSERT
        assert result["out_of_range_sequences"] == 1
        assert len(result["problematic_sequences"]) == 1
        assert result["min_coordinate"] < 0  # Should detect -0.1
        assert result["max_coordinate"] > 1  # Should detect 1.5


class TestReportClassDistribution:
    """Test class distribution and imbalance reporting."""
    
    def test_reports_class_distribution_with_imbalance(self):
        """Should report class distribution and imbalance ratio."""
        # ARRANGE: Create imbalanced dataset
        # 10 samples of class 0 (hello), 20 of class 1 (water)
        X = np.random.rand(30, 3, 126).astype(np.float32)
        y = np.array([0]*10 + [1]*20)
        action_names = ["hello", "water"]
        
        # ACT
        analyzer = DataQualityAnalyzer()
        result = analyzer.report_class_distribution(y, action_names)
        
        # ASSERT
        assert result["total_samples"] == 30
        assert result["num_classes"] == 2
        assert result["majority_class_count"] == 20
        assert result["minority_class_count"] == 10
        assert result["imbalance_ratio"] == 2.0
        assert len(result["samples_per_class"]) == 2
        # Most common should be water (20 samples)
        assert result["most_common"] == "water"
        assert result["least_common"] == "hello"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
