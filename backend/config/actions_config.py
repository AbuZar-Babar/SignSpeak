import os

# Path to actions file (must exist in same folder)
ACTIONS_FILE = os.path.join(os.path.dirname(__file__), "../data/actions.txt")

# Where data will be stored
DATA_PATH = os.path.join(os.path.dirname(__file__), "../data/MP_Data")

# Recording params
SEQUENCE_LENGTH = 20          # number of frames per sequence
NUM_SEQUENCES = 5            # how many sequences per action
FRAME_WAIT_MS = 100           # delay between frames during collection

# Model params
BATCH_SIZE = 16
EPOCHS = 200
LEARNING_RATE = 0.001

# Inference params
PREDICTION_THRESHOLD = 0.7


def load_actions():
    """Load actions from actions.txt (one per line). Returns list of strings."""
    if not os.path.exists(ACTIONS_FILE):
        raise FileNotFoundError(f"{ACTIONS_FILE} not found. Create it and put one action per line.")

    with open(ACTIONS_FILE, "r", encoding="utf-8") as f:
        actions = [line.strip() for line in f if line.strip()]

    if not actions:
        raise ValueError("actions.txt is empty. Add one action per line.")

    return actions
