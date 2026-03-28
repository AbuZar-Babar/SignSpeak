"""
Quick export script: Convert new .h5 models to TFLite and generate label JSONs
for the Kotlin app.
"""
import os
import sys
import json
import joblib

# Add parent to path so we can import from existing code
script_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, script_dir)

import numpy as np
import tensorflow as tf
from tensorflow.keras.models import load_model as _load_model
from tensorflow.keras.layers import InputLayer as _OriginalInputLayer
from tensorflow.keras.layers import Dense as _OriginalDense
from tensorflow.keras.layers import LSTM as _OriginalLSTM

SEQUENCE_LENGTH = 60
FEATURES_PER_FRAME = 126

# --- Keras compatibility shim ---
def _normalize_layer_config(config):
    config = dict(config)
    config.pop("quantization_config", None)
    dtype_cfg = config.get("dtype")
    if isinstance(dtype_cfg, dict):
        cls_name = dtype_cfg.get("class_name")
        if cls_name in {"DTypePolicy", "Policy"}:
            config["dtype"] = dtype_cfg.get("config", {}).get("name", "float32")
    return config

class _CompatInputLayer(_OriginalInputLayer):
    @classmethod
    def from_config(cls, config):
        config = _normalize_layer_config(config)
        if "batch_shape" in config and "batch_input_shape" not in config:
            config["batch_input_shape"] = config.pop("batch_shape")
        config.pop("optional", None)
        return super().from_config(config)

class _CompatDense(_OriginalDense):
    @classmethod
    def from_config(cls, config):
        return super().from_config(_normalize_layer_config(config))

class _CompatLSTM(_OriginalLSTM):
    @classmethod
    def from_config(cls, config):
        return super().from_config(_normalize_layer_config(config))

def load_model(path):
    return _load_model(
        path,
        compile=False,
        custom_objects={
            "InputLayer": _CompatInputLayer,
            "Dense": _CompatDense,
            "LSTM": _CompatLSTM,
            "DTypePolicy": tf.keras.mixed_precision.Policy,
        },
    )


def convert_to_tflite(model, quantize=False):
    input_signature = [tf.TensorSpec([1, SEQUENCE_LENGTH, FEATURES_PER_FRAME], tf.float32)]
    
    @tf.function(input_signature=input_signature, autograph=False)
    def serving_fn(x):
        return model(x, training=False)
    
    concrete = serving_fn.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete], model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    converter.experimental_enable_resource_variables = True
    if quantize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    return converter.convert()


def export_model(model_key, h5_path, encoder_path, output_dir):
    print(f"\n{'='*60}")
    print(f"Exporting {model_key} model")
    print(f"{'='*60}")
    
    # Load model
    print(f"  Loading {h5_path}...")
    model = load_model(h5_path)
    
    # Load labels
    encoder = joblib.load(encoder_path)
    labels = [str(v) for v in encoder.classes_.tolist()]
    print(f"  Labels ({len(labels)}): {labels[:5]}...")
    
    # Export float TFLite
    print(f"  Converting to TFLite (float)...")
    tflite_bytes = convert_to_tflite(model, quantize=False)
    tflite_path = os.path.join(output_dir, f"action_model_{model_key}_new.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_bytes)
    print(f"  Saved: {tflite_path} ({len(tflite_bytes) / 1024:.0f} KB)")
    
    # Export quantized TFLite
    print(f"  Converting to TFLite (quantized)...")
    tflite_q_bytes = convert_to_tflite(model, quantize=True)
    tflite_q_path = os.path.join(output_dir, f"action_model_{model_key}_new_quantized.tflite")
    with open(tflite_q_path, "wb") as f:
        f.write(tflite_q_bytes)
    print(f"  Saved: {tflite_q_path} ({len(tflite_q_bytes) / 1024:.0f} KB)")
    
    # Export labels JSON
    labels_path = os.path.join(output_dir, f"labels_{model_key}_new.json")
    with open(labels_path, "w", encoding="utf-8") as f:
        json.dump(labels, f, indent=2)
    print(f"  Saved: {labels_path} ({len(labels)} labels)")
    
    return labels


if __name__ == "__main__":
    # Paths to new models
    new_models = {
        "baseline": {
            "h5": os.path.join(script_dir, "action_model_baseline_new.h5"),
            "encoder": os.path.join(script_dir, "label_encoder_baseline_new.pkl"),
        },
        "augmented": {
            "h5": os.path.join(script_dir, "action_model_augmented_new.h5"),
            "encoder": os.path.join(script_dir, "label_encoder_augmented_new.pkl"),
        },
    }
    
    # Output directly to Kotlin app assets
    kotlin_assets = os.path.join(
        script_dir, "..", "..", "kotlin app", "app", "src", "main", "assets"
    )
    kotlin_assets = os.path.normpath(kotlin_assets)
    
    print(f"Output directory: {kotlin_assets}")
    os.makedirs(kotlin_assets, exist_ok=True)
    
    for model_key, paths in new_models.items():
        if not os.path.exists(paths["h5"]):
            print(f"SKIPPING {model_key}: {paths['h5']} not found")
            continue
        if not os.path.exists(paths["encoder"]):
            print(f"SKIPPING {model_key}: {paths['encoder']} not found")
            continue
        export_model(model_key, paths["h5"], paths["encoder"], kotlin_assets)
    
    print(f"\n{'='*60}")
    print("DONE! All models exported to Kotlin app assets.")
    print(f"{'='*60}")
