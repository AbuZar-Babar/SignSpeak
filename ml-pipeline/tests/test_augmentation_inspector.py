"""
Tests for Augmentation Inspector - Preview and Validate Augmented Samples
"""

import pytest
import numpy as np
from src.analysis.augmentation_inspector import validate_augmented_samples


class TestValidateAugmentedSamples:
    """Test validating quality of augmented data."""
    
    def test_detects_out_of_range_augmentation(self):
        """Should flag augmented samples with coordinates outside [0, 1]."""
        # ARRANGE: Augmented data with some out-of-range values
        X_original = np.full((5, 10, 126), 0.5, dtype=np.float32)
        X_augmented = np.full((15, 10, 126), 0.5, dtype=np.float32)
        
        # Corrupt one augmented sample
        X_augmented[2] = np.full((10, 126), 1.2, dtype=np.float32)  # Out of range
        
        y_original = np.zeros(5)
        y_augmented = np.concatenate([np.zeros(5), np.zeros(5), np.zeros(5)])
        
        # ACT
        result = validate_augmented_samples(X_augmented, y_augmented)
        
        # ASSERT
        assert "valid_samples" in result
        assert "invalid_samples" in result
        assert result["invalid_samples"] > 0
    
    def test_validates_all_augmented_in_range(self):
        """Should pass when all augmented samples are valid."""
        # ARRANGE: All valid augmented data
        X_augmented = np.random.uniform(0, 1, (20, 10, 126)).astype(np.float32)
        y_augmented = np.zeros(20)
        
        # ACT
        result = validate_augmented_samples(X_augmented, y_augmented)
        
        # ASSERT
        assert result["valid_samples"] == 20
        assert result["invalid_samples"] == 0
        assert result["validation_passed"] is True


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
