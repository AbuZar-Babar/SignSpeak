try:
    import mediapipe as mp
    import numpy as np
    import cv2
    HAS_MEDIAPIPE = True
except ImportError:
    HAS_MEDIAPIPE = False
    import numpy as np

class MediaPipeService:
    def __init__(self):
        if HAS_MEDIAPIPE:
            self.mp_holistic = mp.solutions.holistic
            self.holistic = self.mp_holistic.Holistic(
                min_detection_confidence=0.5, 
                min_tracking_confidence=0.5
            )
        else:
            print("MediaPipe not available. Using mock service.")

    def process_frame(self, image_bytes: bytes):
        if not HAS_MEDIAPIPE:
            # Return dummy landmarks
            return np.zeros(258).tolist()

        try:
            # Convert bytes to numpy array
            nparr = np.frombuffer(image_bytes, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            
            if img is None:
                raise ValueError("Could not decode image")

            # Convert BGR to RGB
            img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
            
            # Process the image
            results = self.holistic.process(img_rgb)
            
            return self.extract_keypoints(results)
        except Exception as e:
            print(f"MediaPipe processing error: {e}")
            return None

    def extract_keypoints(self, results):
        pose = np.zeros(33*4)
        if results.pose_landmarks:
            pose = np.array([[lm.x, lm.y, lm.z, lm.visibility] for lm in results.pose_landmarks.landmark]).flatten()
        
        left = np.zeros(21*3)
        if results.left_hand_landmarks:
            left = np.array([[lm.x, lm.y, lm.z] for lm in results.left_hand_landmarks.landmark]).flatten()
        
        right = np.zeros(21*3)
        if results.right_hand_landmarks:
            right = np.array([[lm.x, lm.y, lm.z] for lm in results.right_hand_landmarks.landmark]).flatten()
            
        return np.concatenate([pose, left, right]).tolist()

    def close(self):
        if HAS_MEDIAPIPE:
            self.holistic.close()
