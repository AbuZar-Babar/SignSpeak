"""
Tests for FeatureAnalyzer - Feature-Level Statistics
"""

import pytest
import numpy as np
from src.analysis.feature_stats import FeatureAnalyzer


class TestPerLandmarkStatistics:
    """Test computing per-landmark statistics (min, max, mean, std)."""
    
    def test_computes_landmark_statistics(self):
        """Should compute mean/std for each landmark across sequences."""
        # ARRANGE: Simple data with known statistics
        # Create 2 sequences, 3 frames, 6 features (2 landmarks x 3 coords)
        X = np.array([
            [[1.0, 2.0, 3.0, 4.0, 5.0, 6.0],
             [1.0, 2.0, 3.0, 4.0, 5.0, 6.0],
             [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]],
            
            [[1.0, 2.0, 3.0, 4.0, 5.0, 6.0],
             [1.0, 2.0, 3.0, 4.0, 5.0, 6.0],
             [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]],
        ], dtype=np.float32)
        
        y = np.array([0, 0])
        action_names = ["test_action"]
        
        # ACT
        analyzer = FeatureAnalyzer()
        result = analyzer.per_landmark_statistics(X, y, action_names)
        
        # ASSERT: All means should be exact values [1, 2, 3, 4, 5, 6]
        # All stds should be 0 (no variation)
        assert result["num_sequences"] == 2
        assert result["num_frames"] == 6  # 2 sequences * 3 frames
        assert result["num_features"] == 6
        assert "landmarks" in result
        assert len(result["landmarks"]) == 2  # 2 landmarks
        
        # Landmark 0 should have mean around [1, 2, 3]
        lm0 = result["landmarks"][0]
        assert lm0["landmark_id"] == 0
        assert np.allclose(lm0["mean"], [1.0, 2.0, 3.0])
        assert np.allclose(lm0["std"], [0.0, 0.0, 0.0])


class TestTemporalEvolution:
    """Test analyzing how landmarks change frame-to-frame."""
    
    def test_computes_velocity(self):
        """Should compute frame-to-frame velocity (displacement)."""
        # ARRANGE: Sequence where landmarks move predictably
        X = np.array([
            [[0.0, 0.0, 0.0] * 42,  # Frame 0: origin
             [0.1, 0.1, 0.1] * 42,  # Frame 1: +0.1 displacement
             [0.2, 0.2, 0.2] * 42,  # Frame 2: +0.1 displacement
            ]
        ], dtype=np.float32)
        
        y = np.array([0])
        action_names = ["moving_hand"]
        
        # ACT
        analyzer = FeatureAnalyzer()
        result = analyzer.temporal_evolution(X, y, action_names, action_idx=0)
        
        # ASSERT
        assert "mean_velocity" in result
        assert "max_velocity" in result
        assert "num_frames" in result
        assert result["num_frames"] == 3
        # Velocity should be ~0.1 per frame in each direction
        assert result["mean_velocity"] > 0
        assert result["max_velocity"] > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
