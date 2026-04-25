import numpy as np
import pytest
from src.features.feature_engineering import SignLanguageAugmenter

def test_time_warp():
    augmenter = SignLanguageAugmenter(sequence_length=30)
    # create dummy sequence of length 30 and 126 features
    sequence = np.ones((30, 126))
    augmented = augmenter.time_warp(sequence, speed_range=(0.9, 1.1))
    
    # Assert shape is maintained
    assert augmented.shape == sequence.shape
    # We should not error

def test_spatial_scale():
    augmenter = SignLanguageAugmenter(sequence_length=30)
    sequence = np.ones((30, 126))
    augmented = augmenter.spatial_scale(sequence, scale_range=(0.95, 1.05))
    
    assert augmented.shape == sequence.shape

def test_add_noise():
    augmenter = SignLanguageAugmenter(sequence_length=30)
    sequence = np.zeros((30, 126))
    augmented = augmenter.add_noise(sequence, noise_std=0.01)
    
    assert augmented.shape == sequence.shape
    # Ensure noise was added
    assert not np.array_equal(augmented, sequence)

def test_augment_method():
    augmenter = SignLanguageAugmenter(sequence_length=30)
    sequence = np.ones((30, 126))
    augmented = augmenter.augment(sequence)
    
    assert augmented.shape == sequence.shape
