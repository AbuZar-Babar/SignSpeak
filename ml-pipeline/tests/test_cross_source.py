import pytest
import numpy as np
from src.analysis.cross_source import compare_sources


class TestCompareSourceDistributions:
    def test_detects_distribution_differences(self):
        X_laptop = np.random.uniform(0.3, 0.7, (20, 5, 126)).astype(np.float32)
        X_mobile = np.random.uniform(0.2, 0.8, (20, 5, 126)).astype(np.float32)
        
        y_laptop = np.zeros(20)
        y_mobile = np.zeros(20)
        
        action_names = ["hello"]
        
        result = compare_sources(X_laptop, y_laptop, X_mobile, y_mobile, action_names)
        
        assert "domain_gap_score" in result
        assert "ks_statistic" in result
        assert "p_value" in result
        assert result["domain_gap_score"] > 0
        assert len(result["landmark_comparisons"]) > 0
    
    def test_identical_distributions_have_low_gap(self):
        X_shared = np.random.uniform(0.4, 0.6, (20, 5, 126)).astype(np.float32)
        X_laptop = X_shared.copy()
        X_mobile = X_shared.copy()
        
        y = np.zeros(20)
        action_names = ["hello"]
        
        result = compare_sources(X_laptop, y, X_mobile, y, action_names)
        
        assert result["domain_gap_score"] < 0.5
