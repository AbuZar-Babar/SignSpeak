"""
Tests for Cross-Source Validator - Laptop vs Mobile Comparison
"""

import pytest
import numpy as np
from src.analysis.cross_source import compare_sources


class TestCompareSourceDistributions:
    """Test comparing data distributions across sources."""
    
    def test_detects_distribution_differences(self):
        """Should detect statistical differences between laptop and mobile."""
        # ARRANGE: Two datasets with different distributions
        # Laptop: uniform [0.3, 0.7]
        # Mobile: uniform [0.2, 0.8] (wider range, different distribution)
        X_laptop = np.random.uniform(0.3, 0.7, (20, 5, 126)).astype(np.float32)
        X_mobile = np.random.uniform(0.2, 0.8, (20, 5, 126)).astype(np.float32)
        
        y_laptop = np.zeros(20)
        y_mobile = np.zeros(20)
        
        action_names = ["hello"]
        
        # ACT
        result = compare_sources(X_laptop, y_laptop, X_mobile, y_mobile, action_names)
        
        # ASSERT: Should detect differences
        assert "domain_gap_score" in result
        assert "ks_statistic" in result
        assert "p_value" in result
        assert result["domain_gap_score"] > 0  # Should have some gap
        assert len(result["landmark_comparisons"]) > 0
    
    def test_identical_distributions_have_low_gap(self):
        """Should have low domain gap when distributions are identical."""
        # ARRANGE: Identical data from both sources
        X_shared = np.random.uniform(0.4, 0.6, (20, 5, 126)).astype(np.float32)
        X_laptop = X_shared.copy()
        X_mobile = X_shared.copy()
        
        y = np.zeros(20)
        action_names = ["hello"]
        
        # ACT
        result = compare_sources(X_laptop, y, X_mobile, y, action_names)
        
        # ASSERT: Domain gap should be low
        assert result["domain_gap_score"] < 0.5  # Low gap for identical data


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
