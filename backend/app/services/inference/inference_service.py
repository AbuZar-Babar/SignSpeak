import numpy as np
import joblib
import os
from app.services.inference.model_loader import ModelLoader

class InferenceService:
    def __init__(self):
        self.loader = ModelLoader()
        self.model_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "models", "action_model.h5")
        self.le_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "models", "label_encoder.pkl")
        
        # Load model
        self.loader.load_model(self.model_path)
        
        # Load Label Encoder
        try:
            self.le = joblib.load(self.le_path)
            self.labels = self.le.classes_
        except Exception as e:
            print(f"Failed to load label encoder: {e}")
            self.le = None
            self.labels = []

        # Sequence buffer: Dictionary to handle multiple users if needed, 
        # but for simplicity assuming single stream or handling via session_id in future.
        # For now, let's just use a simple list and assume one active user per service instance 
        # (Note: In a real API, this needs to be per-user/session, likely stored in Redis or similar)
        # We will implement a simple in-memory dict keyed by user_id
        self.sequences = {} 
        self.predictions = {}
        self.sentence = {}
        
        self.sequence_length = 20
        self.prediction_threshold = 0.7

    def predict(self, user_id: str, keypoints: list) -> dict:
        model = self.loader.get_model()
        if model is None or self.le is None:
            return {"label": "Model not loaded", "confidence": 0.0}

        if user_id not in self.sequences:
            self.sequences[user_id] = []
            self.predictions[user_id] = []
            self.sentence[user_id] = []

        # Add keypoints to sequence
        self.sequences[user_id].append(keypoints)
        
        # Keep only last 20 frames
        if len(self.sequences[user_id]) > self.sequence_length:
            self.sequences[user_id] = self.sequences[user_id][-self.sequence_length:]

        # Predict if we have enough frames
        if len(self.sequences[user_id]) == self.sequence_length:
            try:
                if model is None:
                    # Mock prediction for testing without model
                    import random
                    mock_labels = ["Hello", "Thanks", "Yes", "No", "Please"]
                    return {"label": random.choice(mock_labels), "confidence": 0.95}

                input_data = np.expand_dims(self.sequences[user_id], axis=0)
                res = model.predict(input_data)[0]
                predicted_index = np.argmax(res)
                confidence = float(res[predicted_index])
                
                if confidence >= self.prediction_threshold:
                    predicted_label = self.le.inverse_transform([predicted_index])[0]
                    return {"label": predicted_label, "confidence": confidence}
                else:
                    return {"label": "Uncertain", "confidence": confidence}
                    
            except Exception as e:
                print(f"Prediction error: {e}")
                return {"label": "Error", "confidence": 0.0}
        
        return {"label": "Collecting frames...", "confidence": 0.0}
