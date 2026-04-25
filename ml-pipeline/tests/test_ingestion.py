import pytest
import os
import numpy as np
from src.data.ingestion import load_data

def test_ingestion_imports():
    assert load_data is not None
