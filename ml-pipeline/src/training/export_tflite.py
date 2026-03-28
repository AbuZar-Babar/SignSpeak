import argparse
import json
import os
from pathlib import Path

import joblib
import tensorflow as tf

from src.server.api_server import MODEL_ARTIFACTS
from src.utils.keras_compat import load_model
from src.config.actions_config import SEQUENCE_LENGTH


FEATURES_PER_FRAME = 126


def convert_to_tflite(model: tf.keras.Model, quantize: bool = False) -> bytes:
    input_signature = [tf.TensorSpec([1, SEQUENCE_LENGTH, FEATURES_PER_FRAME], tf.float32)]

    @tf.function(input_signature=input_signature, autograph=False)
    def serving_fn(x):
        return model(x, training=False)

    concrete = serving_fn.get_concrete_function()
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete], model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    converter.experimental_enable_resource_variables = True
    if quantize:
        # Dynamic-range quantization keeps float I/O for app compatibility
        # while reducing weights and improving CPU throughput on many devices.
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    return converter.convert()


def convert_model(model_key: str, output_dir: Path, quantize: bool) -> None:
    artifact = MODEL_ARTIFACTS[model_key]
    model_path = Path(artifact["model_path"])
    encoder_path = Path(artifact["encoder_path"])

    if not model_path.exists():
        raise FileNotFoundError(f"Missing model file: {model_path}")
    if not encoder_path.exists():
        raise FileNotFoundError(f"Missing encoder file: {encoder_path}")

    print(f"[{model_key}] Loading model from {model_path} ...")
    model = load_model(str(model_path))

    mode_label = "quantized" if quantize else "float"
    print(f"[{model_key}] Converting to TensorFlow Lite ({mode_label}) ...")
    tflite_model = convert_to_tflite(model, quantize=quantize)

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
        choices=["baseline", "augmented", "all"],
        default="all",
        help="Which model to export",
    )
    parser.add_argument(
        "--output-dir",
        default=".",
        help="Directory where .tflite and labels json files will be written",
    )
    parser.add_argument(
        "--quantize",
        choices=["none", "dynamic"],
        default="none",
        help="Quantization mode for TFLite export",
    )
    args = parser.parse_args()

    output_dir = Path(args.output_dir).resolve()
    model_keys = ["baseline", "augmented"] if args.model == "all" else [args.model]
    quantize = args.quantize == "dynamic"

    for model_key in model_keys:
        convert_model(model_key, output_dir, quantize=quantize)


if __name__ == "__main__":
    main()
