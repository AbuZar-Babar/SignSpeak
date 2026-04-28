import pytest
import numpy as np
from src.analysis.data_quality import DataQualityAnalyzer


class TestDetectEmptyLandmarks:
    def test_detects_completely_empty_frame(self):
        X = np.array([
            [
                [0.5, 0.5, 0.5] * 42,
                [0.0, 0.0, 0.0] * 42,
                [0.5, 0.5, 0.5] * 42,
            ]
        ], dtype=np.float32)
        
        y = np.array([0])
        action_names = ["hello"]
        
        analyzer = DataQualityAnalyzer()
        result = analyzer.detect_empty_landmarks(X, y, action_names, threshold=0.95)
        
        assert result["empty_frames_count"] == 1
        assert result["sequences_with_empty_frames"] == 1
        assert len(result["problematic_sequences"]) == 1
        assert result["problematic_sequences"][0]["empty_frames"] == 1


class TestValidateCoordinateRanges:
    def test_detects_out_of_range_coordinates(self):
        X = np.array([
            [
                [0.5, 0.5, 0.5] * 42,
                [1.5, 0.5, -0.1] + [0.5] * 123,
                [0.5, 0.5, 0.5] * 42,
            ]
        ], dtype=np.float32)
        
        analyzer = DataQualityAnalyzer()
        result = analyzer.validate_coordinate_ranges(X)
        
        assert result["out_of_range_sequences"] == 1
        assert len(result["problematic_sequences"]) == 1
        assert result["min_coordinate"] < 0
        assert result["max_coordinate"] > 1


class TestReportClassDistribution:
    def test_reports_class_distribution_with_imbalance(self):
        X = np.random.rand(30, 3, 126).astype(np.float32)
        y = np.array([0]*10 + [1]*20)
        action_names = ["hello", "water"]
        
        analyzer = DataQualityAnalyzer()
        result = analyzer.report_class_distribution(y, action_names)
        
        assert result["total_samples"] == 30
        assert result["num_classes"] == 2
        assert result["majority_class_count"] == 20
        assert result["minority_class_count"] == 10
        assert result["imbalance_ratio"] == 2.0
        assert len(result["samples_per_class"]) == 2
        assert result["most_common"] == "water"
        assert result["least_common"] == "hello"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
