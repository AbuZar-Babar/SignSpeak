# import sys
# import os

# # === PATH SETUP (Fixes ModuleNotFoundError) ===
# # Get the path to the 'src' folder
# current_dir = os.path.dirname(os.path.abspath(__file__))
# # Get the project root (one folder up)
# project_root = os.path.dirname(current_dir)
# # Get the config folder path
# config_dir = os.path.join(project_root, 'config')

# # Add config to Python's search path so we can import actions_config
# sys.path.append(config_dir)
# # ==============================================

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import cv2
import os
import numpy as np
import tkinter as tk
from tkinter import ttk, messagebox, simpledialog
from PIL import Image, ImageTk
import mediapipe as mp

# Import config
from config.actions_config import (
    load_actions,
    ACTIONS_FILE,
    DATA_PATH,
    SEQUENCE_LENGTH,
    NUM_SEQUENCES,
)

# ==========================================
# 🎨  Styling & Constants
# ==========================================
COLOR_BG = "#2E2E2E"         # Dark Grey
COLOR_PANEL = "#3C3F41"      # Lighter Grey
COLOR_ACCENT = "#00ADB5"     # Teal
COLOR_TEXT = "#EEEEEE"       # White
COLOR_RED = "#FF5722"        # Orange/Red for Recording
COLOR_GREEN = "#4CAF50"      # Green for Success

FONT_HEADER = ("Helvetica", 16, "bold")
FONT_BODY = ("Helvetica", 12)

# ==========================================
# 🧠  MediaPipe Setup
# ==========================================
mp_holistic = mp.solutions.holistic
mp_drawing = mp.solutions.drawing_utils

class DataCollectorApp:
    def __init__(self, window):
        self.window = window
        self.window.title("SignSpeak Data Collector")
        self.window.geometry("1100x700")
        self.window.configure(bg=COLOR_BG)

        # Data State
        self.actions = load_actions()
        self.current_action = self.actions[0] if self.actions else "None"
        self.sequence_num = 0
        self.frame_count = 0
        
        # Recording State: "IDLE", "COUNTDOWN", "RECORDING", "COOLDOWN"
        self.state = "IDLE"
        self.countdown_value = 0
        self.countdown_timer = 0
        
        # Camera & Model
        self.cap = cv2.VideoCapture(0)
        self.holistic = mp_holistic.Holistic(min_detection_confidence=0.5, min_tracking_confidence=0.5)

        # Setup UI
        self.setup_styles()
        self.create_layout()
        
        # Start Loop
        self.update_feed()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use('clam')
        
        # Button Styles
        style.configure("TButton", font=FONT_BODY, padding=10, background=COLOR_PANEL, foreground=COLOR_TEXT)
        style.map("TButton", background=[('active', COLOR_ACCENT)])
        
        # Action Button Style (Green)
        style.configure("Action.TButton", background=COLOR_ACCENT, foreground="white", font=("Helvetica", 12, "bold"))
        
        # Stop Button Style (Red)
        style.configure("Stop.TButton", background=COLOR_RED, foreground="white", font=("Helvetica", 12, "bold"))

        # Treeview (Table) Styles
        style.configure("Treeview", 
                        background=COLOR_PANEL, 
                        foreground=COLOR_TEXT, 
                        fieldbackground=COLOR_PANEL,
                        font=("Helvetica", 10))
        style.configure("Treeview.Heading", font=("Helvetica", 11, "bold"), background="#555")

    def create_layout(self):
        # === Main Container ===
        container = tk.Frame(self.window, bg=COLOR_BG)
        container.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)

        # === LEFT PANEL (Controls) ===
        left_panel = tk.Frame(container, bg=COLOR_PANEL, width=350)
        left_panel.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 20))
        left_panel.pack_propagate(False)  # Force width

        # Title
        tk.Label(left_panel, text="Control Panel", font=FONT_HEADER, bg=COLOR_PANEL, fg=COLOR_ACCENT).pack(pady=(20, 10))

        # Action Selector
        tk.Label(left_panel, text="Select Action:", font=FONT_BODY, bg=COLOR_PANEL, fg=COLOR_TEXT).pack(anchor="w", padx=15)
        
        self.action_var = tk.StringVar(value=self.current_action)
        self.combo_actions = ttk.Combobox(left_panel, textvariable=self.action_var, values=self.actions, state="readonly", font=FONT_BODY)
        self.combo_actions.pack(fill=tk.X, padx=15, pady=5)
        self.combo_actions.bind("<<ComboboxSelected>>", self.on_action_changed)

        # Add/Delete Buttons
        btn_frame = tk.Frame(left_panel, bg=COLOR_PANEL)
        btn_frame.pack(fill=tk.X, padx=10, pady=5)
        ttk.Button(btn_frame, text="+ New Action", command=self.add_new_action).pack(side=tk.LEFT, expand=True, padx=2)
        ttk.Button(btn_frame, text="- Delete", command=self.delete_action).pack(side=tk.LEFT, expand=True, padx=2)

        # Sequence Selector (Manual Override)
        tk.Label(left_panel, text="Target Sequence #:", font=FONT_BODY, bg=COLOR_PANEL, fg=COLOR_TEXT).pack(anchor="w", padx=15, pady=(20, 0))
        self.seq_var = tk.IntVar(value=0)
        self.spin_seq = ttk.Spinbox(left_panel, from_=0, to=NUM_SEQUENCES-1, textvariable=self.seq_var, font=FONT_BODY)
        self.spin_seq.pack(fill=tk.X, padx=15, pady=5)

        # Progress Table
        tk.Label(left_panel, text="Data Status:", font=FONT_BODY, bg=COLOR_PANEL, fg=COLOR_TEXT).pack(anchor="w", padx=15, pady=(20, 5))
        columns = ("action", "count")
        self.tree = ttk.Treeview(left_panel, columns=columns, show="headings", height=8)
        self.tree.heading("action", text="Action")
        self.tree.heading("count", text="Seqs Recorded")
        self.tree.column("action", width=180)
        self.tree.column("count", width=100, anchor="center")
        self.tree.pack(fill=tk.X, padx=15, pady=5)
        
        # Refresh Table
        self.refresh_table()

        # Spacer
        tk.Label(left_panel, text="", bg=COLOR_PANEL).pack(expand=True)

        # === RIGHT PANEL (Camera) ===
        right_panel = tk.Frame(container, bg=COLOR_BG)
        right_panel.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        # Camera Header
        self.status_label = tk.Label(right_panel, text="Camera Ready", font=("Helvetica", 14), bg=COLOR_BG, fg="grey")
        self.status_label.pack(pady=(0, 10))

        # Video Label
        self.video_label = tk.Label(right_panel, bg="black")
        self.video_label.pack(fill=tk.BOTH, expand=True)

        # Bottom Controls
        control_bar = tk.Frame(right_panel, bg=COLOR_BG)
        control_bar.pack(fill=tk.X, pady=20)

        self.btn_start = ttk.Button(control_bar, text="🔴 Start Recording Sequence", style="Action.TButton", command=self.start_recording_process)
        self.btn_start.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=5)

        # Auto-Advance Checkbox
        self.auto_advance = tk.BooleanVar(value=True)
        chk = tk.Checkbutton(control_bar, text="Auto-Next Sequence", variable=self.auto_advance, 
                             bg=COLOR_BG, fg=COLOR_TEXT, selectcolor=COLOR_PANEL, font=FONT_BODY, activebackground=COLOR_BG, activeforeground=COLOR_TEXT)
        chk.pack(side=tk.RIGHT, padx=10)

    # ==========================================
    # 🔄  Logic & Game Loop
    # ==========================================

    def update_feed(self):
        """ Main loop called every 10ms """
        ret, frame = self.cap.read()
        if ret:
            # 1. Detection
            image, results = self.mediapipe_detection(frame)
            
            # 2. Draw Landmarks
            self.draw_styled_landmarks(image, results)

            # 3. Handle Recording State
            self.handle_state_logic(image, results)

            # 4. Convert to Tkinter Image
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            img_pil = Image.fromarray(image)
            
            # Resize to fit label if needed (optional, keeping raw size for speed)
            img_tk = ImageTk.PhotoImage(image=img_pil)
            
            self.video_label.imgtk = img_tk
            self.video_label.configure(image=img_tk)

        self.window.after(10, self.update_feed)

    def handle_state_logic(self, image, results):
        h, w, _ = image.shape

        if self.state == "COUNTDOWN":
            # Draw Countdown
            elapsed = (cv2.getTickCount() - self.countdown_timer) / cv2.getTickFrequency()
            remaining = 3 - int(elapsed)
            
            if remaining > 0:
                cv2.putText(image, str(remaining), (w//2 - 50, h//2), cv2.FONT_HERSHEY_SIMPLEX, 5, (0, 255, 255), 5)
                self.status_label.config(text=f"Get Ready... {remaining}", fg="orange")
            else:
                self.state = "RECORDING"
                self.frame_count = 0
                self.status_label.config(text="🎥 RECORDING...", fg=COLOR_RED)

        elif self.state == "RECORDING":
            # Save Keypoints
            keypoints = self.extract_keypoints(results)
            self.save_frame(keypoints)
            
            # Visuals
            cv2.circle(image, (30, 30), 15, (0, 0, 255), -1) # Red Dot
            cv2.putText(image, "REC", (55, 40), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
            
            # Progress Bar on Video
            bar_width = int((self.frame_count / SEQUENCE_LENGTH) * w)
            cv2.line(image, (0, h-10), (bar_width, h-10), (0, 255, 0), 10)

            self.frame_count += 1
            if self.frame_count >= SEQUENCE_LENGTH:
                self.finish_sequence()

        elif self.state == "COOLDOWN":
            cv2.putText(image, "SAVED!", (w//2 - 100, h//2), cv2.FONT_HERSHEY_SIMPLEX, 2, (0, 255, 0), 3)
            elapsed = (cv2.getTickCount() - self.countdown_timer) / cv2.getTickFrequency()
            if elapsed > 1.0: # 1 second pause
                self.state = "IDLE"
                self.status_label.config(text="Ready", fg="grey")
                self.btn_start.config(state="normal")
                
                # Auto-Advance Logic
                if self.auto_advance.get():
                    next_seq = self.sequence_num + 1
                    if next_seq < NUM_SEQUENCES:
                        self.seq_var.set(next_seq)
                        self.start_recording_process() # Loop immediately

    def start_recording_process(self):
        # Prepare folders
        self.current_action = self.action_var.get()
        self.sequence_num = self.seq_var.get()
        
        action_path = os.path.join(DATA_PATH, self.current_action, str(self.sequence_num))
        os.makedirs(action_path, exist_ok=True)
        
        # Set state
        self.state = "COUNTDOWN"
        self.countdown_timer = cv2.getTickCount()
        self.btn_start.config(state="disabled")

    def save_frame(self, keypoints):
        action = self.current_action
        seq = self.sequence_num
        frame_num = self.frame_count
        npy_path = os.path.join(DATA_PATH, action, str(seq), f"{frame_num}.npy")
        np.save(npy_path, keypoints)

    def finish_sequence(self):
        self.state = "COOLDOWN"
        self.countdown_timer = cv2.getTickCount()
        self.refresh_table()
        # Increment UI spinner for convenience
        if self.sequence_num < NUM_SEQUENCES - 1:
            self.seq_var.set(self.sequence_num + 1)

    # ==========================================
    # 🛠  Helpers & MediaPipe
    # ==========================================
    def mediapipe_detection(self, image):
        image.flags.writeable = False
        image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        results = self.holistic.process(image_rgb)
        image.flags.writeable = True
        image_rgb = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2BGR)
        return image, results

    def draw_styled_landmarks(self, image, results):
        # Draw connections
        self.mp_draw(image, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS, (80,22,10), (80,44,121))
        self.mp_draw(image, results.left_hand_landmarks, mp_holistic.HAND_CONNECTIONS, (121,22,76), (121,44,250))
        self.mp_draw(image, results.right_hand_landmarks, mp_holistic.HAND_CONNECTIONS, (245,117,66), (245,66,230))

    def mp_draw(self, image, model, connections, c1, c2):
        mp_drawing.draw_landmarks(
            image, model, connections,
            mp_drawing.DrawingSpec(color=c1, thickness=2, circle_radius=4),
            mp_drawing.DrawingSpec(color=c2, thickness=2, circle_radius=2)
        )

    def extract_keypoints(self, results):
        pose = np.array([[res.x, res.y, res.z, res.visibility] for res in results.pose_landmarks.landmark]).flatten() if results.pose_landmarks else np.zeros(33*4)
        lh = np.array([[res.x, res.y, res.z] for res in results.left_hand_landmarks.landmark]).flatten() if results.left_hand_landmarks else np.zeros(21*3)
        rh = np.array([[res.x, res.y, res.z] for res in results.right_hand_landmarks.landmark]).flatten() if results.right_hand_landmarks else np.zeros(21*3)
        return np.concatenate([pose, lh, rh]) # Note: Excluding face for speed/size as per standard usage

    # ==========================================
    # 📂  File / Action Management
    # ==========================================
    def refresh_table(self):
        for i in self.tree.get_children():
            self.tree.delete(i)
        
        # Re-read folders
        if os.path.exists(DATA_PATH):
            for action in self.actions:
                action_path = os.path.join(DATA_PATH, action)
                count = 0
                if os.path.exists(action_path):
                    # Count folders that are numbers
                    count = len([d for d in os.listdir(action_path) if d.isdigit()])
                self.tree.insert("", "end", values=(action, f"{count} / {NUM_SEQUENCES}"))

    def add_new_action(self):
        name = simpledialog.askstring("New Action", "Enter action name:")
        if name:
            clean_name = name.strip().replace(" ", "_").lower()
            if clean_name not in self.actions:
                with open(ACTIONS_FILE, "a") as f:
                    f.write("\n" + clean_name)
                self.actions = load_actions()
                self.combo_actions.config(values=self.actions)
                self.refresh_table()

    def delete_action(self):
        target = self.action_var.get()
        if not target: return
        if messagebox.askyesno("Delete", f"Remove '{target}' from list? (Data remains)"):
            # Remove from list only
            self.actions = [a for a in self.actions if a != target]
            with open(ACTIONS_FILE, "w") as f:
                for a in self.actions:
                    f.write(a + "\n")
            self.combo_actions.config(values=self.actions)
            if self.actions: self.action_var.set(self.actions[0])
            self.refresh_table()

    def on_action_changed(self, event):
        # Reset sequence counter to first missing sequence
        action = self.action_var.get()
        action_path = os.path.join(DATA_PATH, action)
        if os.path.exists(action_path):
            existing = [int(d) for d in os.listdir(action_path) if d.isdigit()]
            if existing:
                # Suggest next number
                self.seq_var.set(max(existing) + 1)
            else:
                self.seq_var.set(0)
        else:
            self.seq_var.set(0)
            
    def close(self):
        self.cap.release()
        self.window.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = DataCollectorApp(root)
    root.protocol("WM_DELETE_WINDOW", app.close)
    root.mainloop()