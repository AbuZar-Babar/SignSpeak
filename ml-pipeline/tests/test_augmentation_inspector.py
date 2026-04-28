import pytest
import numpy as np
from src.analysis.augmentation_inspector import validate_augmented_samples


class TestValidateAugmentedSamples:
    def test_detects_out_of_range_augmentation(self):
        X_original = np.full((5, 10, 126), 0.5, dtype=np.float32)
        X_augmented = np.full((15, 10, 126), 0.5, dtype=np.float32)
        
        X_augmented[2] = np.full((10, 126), 1.2, dtype=np.float32)
        
        y_original = np.zeros(5)
        y_augmented = np.concatenate([np.zeros(5), np.zeros(5), np.zeros(5)])
        
        result = validate_augmented_samples(X_augmented, y_augmented)
        
        assert "valid_samples" in result
        assert "invalid_samples" in result
        assert result["invalid_samples"] > 0
    
    def test_validates_all_augmented_in_range(self):
        X_augmented2 = np.random.uniform(0, 1, (20, 10, 126)).astype(np.float32)
        y_augmented2 = np.zeros(20)
        
        result2 = validate_augmented_samples(X_augmented2, y_augmented2)
        
        assert result2["valid_samples"] == 20
        assert result2["invalid_samples"] == 0
        assert result2["validation_passed"] is True
