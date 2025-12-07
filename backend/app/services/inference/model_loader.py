try:
    import tensorflow as tf
    HAS_TENSORFLOW = True
except ImportError:
    HAS_TENSORFLOW = False
    print("TensorFlow not available. Using mock model loader.")

import os

class ModelLoader:
    _instance = None
    _model = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ModelLoader, cls).__new__(cls)
        return cls._instance

    def load_model(self, model_path: str):
        if self._model is None:
            if HAS_TENSORFLOW and os.path.exists(model_path):
                try:
                    # Try loading as Keras model
                    self._model = tf.keras.models.load_model(model_path)
                    print(f"Model loaded from {model_path}")
                except Exception as e:
                    print(f"Failed to load model: {e}")
                    # Fallback or re-raise depending on requirements
                    raise e
            else:
                if not HAS_TENSORFLOW:
                     print("TensorFlow missing, skipping model load.")
                else:
                    print(f"Model file not found at {model_path}")
                
                # For development, we might want to allow starting without a model
                self._model = None

    def get_model(self):
        return self._model
