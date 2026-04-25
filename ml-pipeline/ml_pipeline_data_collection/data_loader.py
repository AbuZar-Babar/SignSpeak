import os
import joblib
import hashlib
import numpy as np
from actions_config import SEQUENCE_LENGTH

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def load_data(actions, data_sources, use_cache=True):
    """
    Isolated data loading function with smart fingerprinting.
    Detects if the dataset has changed on disk and manages cache.
    """
    # 1. Create a content fingerprint
    fingerprint_data = "".join(sorted(data_sources)) + "".join(sorted(actions))
    for source in data_sources:
        if not os.path.isdir(source): continue
        for action in actions:
            action_path = os.path.join(source, action)
            if os.path.isdir(action_path):
                seq_count = len([d for d in os.listdir(action_path) if d.isdigit()])
                mtime = os.path.getmtime(action_path)
                fingerprint_data += f"|{action}:{seq_count}:{mtime}"
    
    content_hash = hashlib.md5(fingerprint_data.encode()).hexdigest()[:12]
    cache_file = os.path.join(SCRIPT_DIR, f"data_cache_{content_hash}.joblib")
    
    # 2. Try loading from cache
    if use_cache and os.path.exists(cache_file):
        print(f"Dataset match! Loading from cache: {os.path.basename(cache_file)}")
        try:
            cached = joblib.load(cache_file)
            return cached['X'], cached['y']
        except Exception:
            print("Cache file corrupted, re-reading from disk...")
    
    # 3. Cache miss: Clean up old versions (DISABLED: was deleting valid caches for other data sources)
    # for f in os.listdir(SCRIPT_DIR):
    #     if f.startswith("data_cache_") and f.endswith(".joblib"):
    #         try: os.remove(os.path.join(SCRIPT_DIR, f))
    #         except: pass

    # 4. Manual Loading
    sequences, labels = [], []
    print("\nCache miss or dataset changed. Reading raw .npy files...")
    try:
        from tqdm import tqdm
    except ImportError:
        tqdm = lambda x, **kwargs: x

    for source_dir in data_sources:
        if not os.path.isdir(source_dir): continue
        print(f"Loading from {os.path.basename(source_dir)}...")
        
    from concurrent.futures import ThreadPoolExecutor, as_completed

    def load_sequence(action_path, seq_id, action, seq_len):
        window = []
        for frame_num in range(seq_len):
            npy = os.path.join(action_path, seq_id, f"{frame_num}.npy")
            if not os.path.exists(npy):
                return None
            window.append(np.load(npy))
        return (window, action)

    jobs = []
    for source_dir in data_sources:
        if not os.path.isdir(source_dir): continue
        for action in actions:
            action_path = os.path.join(source_dir, action)
            if not os.path.isdir(action_path): continue
            seq_ids = sorted([d for d in os.listdir(action_path) if d.isdigit()], key=int)
            for seq_id in seq_ids:
                jobs.append((action_path, seq_id, action, SEQUENCE_LENGTH))

    print(f"Accelerating load of {len(jobs)} sequences with multithreading...")
    
    with ThreadPoolExecutor(max_workers=64) as executor:
        futures = {executor.submit(load_sequence, *job): job for job in jobs}
        for future in tqdm(as_completed(futures), total=len(jobs), desc="Loading sequences"):
            res = future.result()
            if res is not None:
                sequences.append(res[0])
                labels.append(res[1])
    
    if not sequences:
        raise RuntimeError("No sequences loaded from specified sources.")

    X, y = np.array(sequences), np.array(labels)
    
    # 5. Save new cache
    if use_cache:
        joblib.dump({'X': X, 'y': y, 'actions': actions}, cache_file)
        print(f"Saved fresh cache: {os.path.basename(cache_file)}")
    
    return X, y
