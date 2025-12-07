# SignSpeak

A small data-collection and sign-language recognition project. This repository contains code to collect gesture data, train a model, and run realtime inference.

Repository layout (important files):

- `data/` - collected .npy feature files (excluded from git by default)
- `config/actions_config.py` - action / label configuration
- `models/` - saved models and encoders (model files are excluded by .gitignore)
- `src/` - application code
  - `collect_data.py` / `collect_data_gui.py` - scripts to collect and label data
  - `train_model.py` - training script (produces a .h5 model)
  - `realtime_inference.py` - script for realtime camera inference
- `TODO.md` - project TODOs

Quick start
-----------
1. Create and activate a virtual environment (recommended):

```bash
python3 -m venv .venv
source .venv/bin/activate
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Collect data (example):

```bash
python src/collect_data.py
# or use the GUI
python src/collect_data_gui.py
```

4. Train model:

```bash
python src/train_model.py
```

5. Run realtime inference (requires camera and a trained model in `models/`):

```bash
python src/realtime_inference.py
```

Notes
-----
- Large binary files (models, .npy datasets) are ignored in `.gitignore`. Store them outside the repo or use Git LFS if you want them in the remote.
- If you need a GPU-enabled TensorFlow build, install the appropriate `tensorflow` package for your platform.

Contributing
------------
Feel free to open issues or pull requests. If you'd like, I can add CI (GitHub Actions) to run linting and unit tests.

License
-------
Add a LICENSE file if you want to set a license for this project.
