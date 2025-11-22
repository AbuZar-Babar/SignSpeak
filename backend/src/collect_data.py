# collect_data.py
import cv2
import os
import numpy as np
import time
import mediapipe as mp
from backend.config.actions_config import load_actions, DATA_PATH, SEQUENCE_LENGTH, NUM_SEQUENCES, FRAME_WAIT_MS

mp_holistic = mp.solutions.holistic
mp_drawing = mp.solutions.drawing_utils

def mediapipe_detection(image, model):
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    image_rgb.flags.writeable = False
    results = model.process(image_rgb)
    image_rgb.flags.writeable = True
    image_bgr = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2BGR)
    return image_bgr, results

def draw_landmarks(image, results):
    # Draw face, pose and hands (optional)
    mp_drawing.draw_landmarks(image, results.face_landmarks, mp_holistic.FACEMESH_TESSELATION,
                              mp_drawing.DrawingSpec(color=(80,110,10), thickness=1, circle_radius=1),
                              mp_drawing.DrawingSpec(color=(80,256,121), thickness=1, circle_radius=1))
    mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS,
                              mp_drawing.DrawingSpec(color=(80,22,10), thickness=2, circle_radius=4),
                              mp_drawing.DrawingSpec(color=(80,44,121), thickness=2, circle_radius=2))
    mp_drawing.draw_landmarks(image, results.left_hand_landmarks, mp_holistic.HAND_CONNECTIONS,
                              mp_drawing.DrawingSpec(color=(121,22,76), thickness=2, circle_radius=4),
                              mp_drawing.DrawingSpec(color=(121,44,250), thickness=2, circle_radius=2))
    mp_drawing.draw_landmarks(image, results.right_hand_landmarks, mp_holistic.HAND_CONNECTIONS,
                              mp_drawing.DrawingSpec(color=(245,117,66), thickness=2, circle_radius=4),
                              mp_drawing.DrawingSpec(color=(245,66,230), thickness=2, circle_radius=2))

def extract_keypoints(results):
    # Pose: 33 landmarks * 4
    pose = np.zeros(33*4)
    if results.pose_landmarks:
        pose_landmarks = results.pose_landmarks.landmark
        pose = np.array([[lm.x, lm.y, lm.z, lm.visibility] for lm in pose_landmarks]).flatten()
    # Left hand: 21 * 3
    left = np.zeros(21*3)
    if results.left_hand_landmarks:
        left_landmarks = results.left_hand_landmarks.landmark
        left = np.array([[lm.x, lm.y, lm.z] for lm in left_landmarks]).flatten()
    # Right hand: 21 * 3
    right = np.zeros(21*3)
    if results.right_hand_landmarks:
        right_landmarks = results.right_hand_landmarks.landmark
        right = np.array([[lm.x, lm.y, lm.z] for lm in right_landmarks]).flatten()

    # Combine (pose, left, right)
    return np.concatenate([pose, left, right])

def create_folders(actions):
    for action in actions:
        action_path = os.path.join(DATA_PATH, action)
        os.makedirs(action_path, exist_ok=True)
        for seq in range(NUM_SEQUENCES):
            seq_path = os.path.join(action_path, str(seq))
            os.makedirs(seq_path, exist_ok=True)

def collect():
    actions = load_actions()
    create_folders(actions)
    cap = cv2.VideoCapture(0)
    with mp_holistic.Holistic(min_detection_confidence=0.5, min_tracking_confidence=0.5) as holistic:
        for action in actions:
            print(f"\nStart collecting for action: '{action}'")
            time.sleep(1)
            for seq in range(NUM_SEQUENCES):
                print(f"  Sequence {seq}")
                frame_idx = 0
                # Countdown before starting sequence
                for countdown in range(3, 0, -1):
                    print(f"    Starting in {countdown}...")
                    time.sleep(1)
                while frame_idx < SEQUENCE_LENGTH:
                    ret, frame = cap.read()
                    if not ret:
                        print("Failed to grab frame. Exiting.")
                        break
                    image, results = mediapipe_detection(frame, holistic)
                    draw_landmarks(image, results)
                    keypoints = extract_keypoints(results)
                    npy_path = os.path.join(DATA_PATH, action, str(seq), f"{frame_idx}.npy")
                    np.save(npy_path, keypoints)
                    # show live
                    cv2.putText(image, f'Action: {action} Sequence: {seq} Frame: {frame_idx}', (10,30),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255,255,255), 2, cv2.LINE_AA)
                    cv2.imshow('Collecting', image)
                    frame_idx += 1
                    # Wait specified ms between frames
                    if cv2.waitKey(int(FRAME_WAIT_MS)) & 0xFF == ord('q'):
                        print("Interrupted by user.")
                        cap.release()
                        cv2.destroyAllWindows()
                        return
                # small pause between sequences
                time.sleep(0.5)
    cap.release()
    cv2.destroyAllWindows()
    print("Data collection complete.")

if __name__ == "__main__":
    collect()
