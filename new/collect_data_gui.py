import cv2
import os
import numpy as np
import time
import threading
import tkinter as tk
from tkinter import ttk, messagebox, simpledialog

import mediapipe as mp

from actions_config import (
    load_actions,
    ACTIONS_FILE,
    DATA_PATH,
    SEQUENCE_LENGTH,
    NUM_SEQUENCES,
    FRAME_WAIT_MS,
)


# ------------------------------
# Mediapipe setup
# ------------------------------
mp_holistic = mp.solutions.holistic
mp_drawing = mp.solutions.drawing_utils


def mediapipe_detection(image, model):
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    image_rgb.flags.writeable = False
    results = model.process(image_rgb)
    image_rgb.flags.writeable = True
    image_bgr = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2BGR)
    return image_bgr, results


def extract_keypoints(results):
    pose = np.zeros(33 * 3)
    if results.pose_landmarks:
        pose = np.array([[lm.x, lm.y, lm.z] for lm in results.pose_landmarks.landmark]).flatten()

    left = np.zeros(21 * 3)
    if results.left_hand_landmarks:
        left = np.array([[lm.x, lm.y, lm.z] for lm in results.left_hand_landmarks.landmark]).flatten()

    right = np.zeros(21 * 3)
    if results.right_hand_landmarks:
        right = np.array([[lm.x, lm.y, lm.z] for lm in results.right_hand_landmarks.landmark]).flatten()

    return np.concatenate([pose, left, right])


def draw_landmarks(image, results):
    mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS)
    mp_drawing.draw_landmarks(image, results.left_hand_landmarks, mp_holistic.HAND_CONNECTIONS)
    mp_drawing.draw_landmarks(image, results.right_hand_landmarks, mp_holistic.HAND_CONNECTIONS)


# ------------------------------
# GUI Class
# ------------------------------
class DataCollectorGUI:
    def __init__(self):
        self.window = tk.Tk()
        self.window.title("SignSpeak – Advanced Data Collector")
        self.window.geometry("720x520")

        self.stop_flag = False

        self.actions = load_actions()
        self.current_action = tk.StringVar(value=self.actions[0])

        self.build_ui()
        self.refresh_table()

        self.window.mainloop()

    # --------------------------
    # UI Builder
    # --------------------------
    def build_ui(self):
        top_frame = tk.Frame(self.window)
        top_frame.pack(pady=10)

        tk.Label(top_frame, text="Select Action:", font=("Arial", 12)).grid(row=0, column=0, padx=5)
        self.dropdown = ttk.Combobox(top_frame, values=self.actions, textvariable=self.current_action, width=22)
        self.dropdown.grid(row=0, column=1, padx=5)

        tk.Button(top_frame, text="Add Action", command=self.add_action, bg="#3AAFA9", fg="white").grid(row=0, column=2, padx=5)
        tk.Button(top_frame, text="Remove Action", command=self.remove_action, bg="#D9534F", fg="white").grid(row=0, column=3, padx=5)

        # Table
        tk.Label(self.window, text="Actions and Sequence Counts", font=("Arial", 12)).pack()
        columns = ("action", "collected", "needed")
        self.tree = ttk.Treeview(self.window, columns=columns, show="headings", height=8)
        for col in columns:
            self.tree.heading(col, text=col.capitalize())
            self.tree.column(col, anchor="center", width=180)
        self.tree.pack(pady=10)

        # Progress bar
        self.progress_label = tk.Label(self.window, text="Progress: 0%", font=("Arial", 12))
        self.progress_label.pack(pady=5)

        self.progress = ttk.Progressbar(self.window, orient="horizontal", length=500, mode="determinate")
        self.progress.pack(pady=5)

        # Buttons
        tk.Button(self.window, text="Start Collecting", font=("Arial", 14),
                  command=self.start_collection_thread, bg="#4CAF50", fg="white").pack(pady=10)

        tk.Button(self.window, text="Stop", font=("Arial", 12),
                  command=self.stop_collection, bg="red", fg="white").pack()

    # --------------------------
    # Add / Remove Actions
    # --------------------------
    def add_action(self):
        new_action = simpledialog.askstring("Add Action", "Enter new action name:")
        if new_action and new_action.strip():
            new_action = new_action.lower().replace(" ", "_")

            with open(ACTIONS_FILE, "a") as f:
                f.write(new_action + "\n")

            self.actions = load_actions()
            self.dropdown.config(values=self.actions)
            self.current_action.set(self.actions[-1])
            self.refresh_table()

            messagebox.showinfo("Added", f"Action '{new_action}' added successfully!")

    def remove_action(self):
        action = self.current_action.get()
        if messagebox.askyesno("Remove Action", f"Delete '{action}' from actions?"):
            self.actions = [a for a in self.actions if a != action]

            with open(ACTIONS_FILE, "w") as f:
                for a in self.actions:
                    f.write(a + "\n")

            self.dropdown.config(values=self.actions)
            if self.actions:
                self.current_action.set(self.actions[0])

            self.refresh_table()
            messagebox.showinfo("Removed", f"Action '{action}' removed.")

    # --------------------------
    # Table Refresh
    # --------------------------
    def count_sequences(self, action):
        folder = os.path.join(DATA_PATH, action)
        if not os.path.exists(folder):
            return 0
        return len([seq for seq in os.listdir(folder) if seq.isdigit()])

    def refresh_table(self):
        for row in self.tree.get_children():
            self.tree.delete(row)

        for action in self.actions:
            collected = self.count_sequences(action)
            needed = NUM_SEQUENCES
            self.tree.insert("", tk.END, values=(action, collected, needed))

    # --------------------------
    # Data Collection
    # --------------------------
    def start_collection_thread(self):
        thread = threading.Thread(target=self.collect)
        thread.daemon = True
        thread.start()

    def stop_collection(self):
        self.stop_flag = True
        self.progress_label.config(text="Stopping...")

    def create_folders(self, action):
        action_path = os.path.join(DATA_PATH, action)
        os.makedirs(action_path, exist_ok=True)

    def collect(self):
        action = self.current_action.get()
        self.stop_flag = False

        self.create_folders(action)

        # Count how many sequences are done
        start_seq = self.count_sequences(action)

        total = NUM_SEQUENCES
        self.progress["maximum"] = total

        cap = cv2.VideoCapture(0)

        with mp_holistic.Holistic(min_detection_confidence=0.5,
                                  min_tracking_confidence=0.5) as holistic:

            for seq in range(start_seq, NUM_SEQUENCES):
                if self.stop_flag:
                    break

                # Countdown
                for i in range(3, 0, -1):
                    self.progress_label.config(text=f"Seq {seq+1}/{NUM_SEQUENCES} starts in {i}...")
                    time.sleep(1)

                seq_folder = os.path.join(DATA_PATH, action, str(seq))
                os.makedirs(seq_folder, exist_ok=True)

                for frame_num in range(SEQUENCE_LENGTH):
                    if self.stop_flag:
                        break

                    ret, frame = cap.read()
                    if not ret:
                        self.progress_label.config(text="Camera error!")
                        return

                    image, results = mediapipe_detection(frame, holistic)
                    draw_landmarks(image, results)

                    key = extract_keypoints(results)
                    np.save(os.path.join(seq_folder, f"{frame_num}.npy"), key)

                    cv2.imshow("SignSpeak – Recording", image)
                    if cv2.waitKey(FRAME_WAIT_MS) & 0xFF == ord('q'):
                        self.stop_flag = True
                        break

                # update progress bar
                self.progress["value"] = seq + 1
                percent = int(((seq + 1) / total) * 100)
                self.progress_label.config(text=f"Progress: {percent}%")

                self.refresh_table()

        cap.release()
        cv2.destroyAllWindows()

        self.refresh_table()

        if self.stop_flag:
            self.progress_label.config(text="Stopped.")
        else:
            self.progress_label.config(text="Completed!")
            messagebox.showinfo("Done", f"All sequences recorded for '{action}'")


if __name__ == "__main__":
    DataCollectorGUI()
