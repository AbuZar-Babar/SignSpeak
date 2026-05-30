import argparse
import json
from pathlib import Path
import sys

import joblib
import tensorflow as tf
from tensorflow.keras.models import load_model

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from src.config.config import SEQUENCE_LENGTH, MODELS_DIR
import os

FEATURES_PER_FRAME = 126

MODEL_ARTIFACTS = {
    "baseline": {
        "model_path": os.path.join(MODELS_DIR, "old", "action_model_baseline_legacy_v3.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "old", "label_encoder_baseline_new.pkl"),
    },
    "augmented": {
        "model_path": os.path.join(MODELS_DIR, "old", "action_model_augmented_legacy_v3.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "old", "label_encoder_augmented_new.pkl"),
    },
    "new50": {
        "model_path": os.path.join(MODELS_DIR, "new", "action_model_laptop50_v4.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "new", "label_encoder_laptop50_v4.pkl"),
    },
    "new50_mobile20": {
        "model_path": os.path.join(MODELS_DIR, "new", "action_model_laptop50_mobile20_v4.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "new", "label_encoder_laptop50_mobile20_v4.pkl"),
    },
    "mobile_json_v5": {
        "model_path": os.path.join(MODELS_DIR, "new", "action_model_mobile_json_no_aug_v5.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "new", "label_encoder_mobile_json_no_aug_v5.pkl"),
    },
    "mobile_json_v6": {
        "model_path": os.path.join(MODELS_DIR, "new", "action_model_mobile_json_no_aug_v6.h5"),
        "encoder_path": os.path.join(MODELS_DIR, "new", "label_encoder_mobile_json_no_aug_v6.pkl"),
    },
}


def convert_to_tflite(
    model: tf.keras.Model,
    quantize: bool = False,
    use_select_tf_ops: bool = False,
) -> bytes:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    if use_select_tf_ops:
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS,
        ]
        converter._experimental_lower_tensor_list_ops = False
    else:
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
        # Enables lowering TensorList-based LSTM graphs to builtins-only TFLite
        # for fixed single-batch inference (our mobile app uses batch size 1).
        converter._experimental_default_to_single_batch_in_tensor_list_ops = True
    converter.experimental_enable_resource_variables = True
    if quantize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    return converter.convert()


def convert_model(
    model_key: str,
    output_dir: Path,
    quantize: bool,
    use_select_tf_ops: bool,
) -> None:
    artifact = MODEL_ARTIFACTS[model_key]
    model_path = Path(artifact["model_path"])
    encoder_path = Path(artifact["encoder_path"])

    if not model_path.exists():
        raise FileNotFoundError(f"Missing model file: {model_path}")
    if not encoder_path.exists():
        raise FileNotFoundError(f"Missing encoder file: {encoder_path}")

    print(f"[{model_key}] Loading model from {model_path} ...")
    model = load_model(str(model_path), compile=False)

    mode_label = "quantized" if quantize else "float"
    ops_label = "select_tf_ops" if use_select_tf_ops else "builtins_only"
    print(f"[{model_key}] Converting to TensorFlow Lite ({mode_label}, {ops_label}) ...")
    tflite_model = convert_to_tflite(
        model,
        quantize=quantize,
        use_select_tf_ops=use_select_tf_ops,
    )

    output_dir.mkdir(parents=True, exist_ok=True)
    model_suffix = "_quantized" if quantize else ""
    tflite_path = output_dir / f"action_model_{model_key}{model_suffix}.tflite"
    tflite_path.write_bytes(tflite_model)

    encoder = joblib.load(encoder_path)
    labels = [str(value) for value in encoder.classes_.tolist()]
    labels_path = output_dir / f"labels_{model_key}.json"
    labels_path.write_text(json.dumps(labels, indent=2), encoding="utf-8")

    print(
        f"[{model_key}] Done ({mode_label}): {tflite_path.name} ({len(tflite_model) / (1024 * 1024):.2f} MB), "
        f"{labels_path.name} ({len(labels)} labels)"
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Export SignSpeak Keras models to TensorFlow Lite.")
    parser.add_argument(
        "--model",
        choices=[*MODEL_ARTIFACTS.keys(), "all", "all_new"],
        default="all_new",
        help="Which model to export",
    )
    parser.add_argument(
        "--output-dir",
        default="all_models",
        help="Directory where .tflite and labels json files will be written",
    )
    parser.add_argument(
        "--quantize",
        choices=["none", "dynamic"],
        default="none",
        help="Quantization mode for TFLite export",
    )
    parser.add_argument(
        "--use-select-tf-ops",
        action="store_true",
        help="Enable SELECT_TF_OPS (requires tensorflow-lite-select-tf-ops on Android, larger APK).",
    )
    args = parser.parse_args()

    output_dir = Path(args.output_dir).resolve()
    if args.model == "all":
        model_keys = ["baseline", "augmented", "new50", "new50_mobile20", "mobile_json_v6"]
    elif args.model == "all_new":
        model_keys = ["new50", "new50_mobile20", "mobile_json_v6"]
    else:
        model_keys = [args.model]
    quantize = args.quantize == "dynamic"
    use_select_tf_ops = args.use_select_tf_ops

    for model_key in model_keys:
        convert_model(
            model_key,
            output_dir,
            quantize=quantize,
            use_select_tf_ops=use_select_tf_ops,
        )


if __name__ == "__main__":
    main()
