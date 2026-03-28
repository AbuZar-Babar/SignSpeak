"""
Keras Version Compatibility Shim

Models saved with newer Keras can include:
- InputLayer keys ('batch_shape', 'optional')
- Layer key 'quantization_config'
- New dtype policy serialization dicts ('DTypePolicy')

We normalize these configs so TensorFlow/Keras 2.15 can deserialize them.
"""

import tensorflow as tf
from tensorflow.keras.models import load_model as _load_model
from tensorflow.keras.layers import InputLayer as _OriginalInputLayer
from tensorflow.keras.layers import Dense as _OriginalDense
from tensorflow.keras.layers import LSTM as _OriginalLSTM


def _normalize_layer_config(config):
    config = dict(config)  # avoid mutating the original
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


def load_model(path, **kw):
    """Wrapper that injects Keras compatibility custom objects."""
    kw.setdefault("custom_objects", {})
    kw["custom_objects"].setdefault("InputLayer", _CompatInputLayer)
    kw["custom_objects"].setdefault("Dense", _CompatDense)
    kw["custom_objects"].setdefault("LSTM", _CompatLSTM)
    kw["custom_objects"].setdefault("DTypePolicy", tf.keras.mixed_precision.Policy)
    # Inference-only server; skip optimizer/loss deserialization from newer Keras.
    kw.setdefault("compile", False)
    return _load_model(path, **kw)
