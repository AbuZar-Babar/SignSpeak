import pytest
import numpy as np
from src.analysis.feature_stats import FeatureAnalyzer


class TestPerLandmarkStatistics:
    def test_computes_landmark_statistics(self):
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
        
        analyzer = FeatureAnalyzer()
        result = analyzer.per_landmark_statistics(X, y, action_names)
        
        assert result["num_sequences"] == 2
        assert result["num_frames"] == 6
        assert result["num_features"] == 6
        assert "landmarks" in result
        assert len(result["landmarks"]) == 2
        
        lm0 = result["landmarks"][0]
        assert lm0["landmark_id"] == 0
        assert np.allclose(lm0["mean"], [1.0, 2.0, 3.0])
        assert np.allclose(lm0["std"], [0.0, 0.0, 0.0])


class TestTemporalEvolution:
    def test_computes_velocity(self):
        X = np.array([
            [[0.0, 0.0, 0.0] * 42,
             [0.1, 0.1, 0.1] * 42,
             [0.2, 0.2, 0.2] * 42,
            ]
        ], dtype=np.float32)
        
        y = np.array([0])
        action_names = ["moving_hand"]
        
        analyzer = FeatureAnalyzer()
        result = analyzer.temporal_evolution(X, y, action_names, action_idx=0)
        
        assert "mean_velocity" in result
        assert "max_velocity" in result
        assert "num_frames" in result
        assert result["num_frames"] == 3
        assert result["mean_velocity"] > 0
        assert result["max_velocity"] > 0
