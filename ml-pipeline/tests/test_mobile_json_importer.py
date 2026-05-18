import json

import numpy as np
import pytest

from src.data.mobile_json_importer import (
    EXPECTED_FRAME_DIM,
    EXPECTED_FORMAT,
    EXPECTED_SEQUENCE_LENGTH,
    MobileJsonImportError,
    import_mobile_json_dir,
    validate_payload,
)


def make_payload(action="hello", frames=None):
    if frames is None:
        frames = [
            [float(frame_index + value_index) for value_index in range(EXPECTED_FRAME_DIM)]
            for frame_index in range(EXPECTED_SEQUENCE_LENGTH)
        ]
    return {
        "format": EXPECTED_FORMAT,
        "source": "mobile",
        "action": action,
        "sequence_length": EXPECTED_SEQUENCE_LENGTH,
        "dim": EXPECTED_FRAME_DIM,
        "created_at": "2026-05-18T00:00:00Z",
        "frames": frames,
    }


def write_payload(path, payload):
    path.write_text(json.dumps(payload), encoding="utf-8")


def test_import_valid_payload_writes_npy_sequence(tmp_path):
    input_dir = tmp_path / "exports"
    output_dir = tmp_path / "MP_Data_mobile"
    input_dir.mkdir()
    write_payload(input_dir / "hello.json", make_payload())

    summary = import_mobile_json_dir(
        input_dir,
        output_dir,
        allowed_actions={"hello"},
    )

    assert len(summary.imported) == 1
    assert not summary.skipped
    sequence_dir = output_dir / "hello" / "0"
    assert sequence_dir.is_dir()
    assert len(list(sequence_dir.glob("*.npy"))) == EXPECTED_SEQUENCE_LENGTH
    assert np.load(sequence_dir / "0.npy").shape == (EXPECTED_FRAME_DIM,)


def test_import_appends_after_existing_sequence(tmp_path):
    input_dir = tmp_path / "exports"
    output_dir = tmp_path / "MP_Data_mobile"
    existing_dir = output_dir / "hello" / "0"
    input_dir.mkdir()
    existing_dir.mkdir(parents=True)
    write_payload(input_dir / "hello.json", make_payload())

    summary = import_mobile_json_dir(
        input_dir,
        output_dir,
        allowed_actions={"hello"},
    )

    assert summary.imported == [output_dir / "hello" / "1"]
    assert (output_dir / "hello" / "1" / "59.npy").exists()


def test_invalid_frame_count_is_skipped(tmp_path):
    input_dir = tmp_path / "exports"
    output_dir = tmp_path / "MP_Data_mobile"
    input_dir.mkdir()
    payload = make_payload(frames=[[0.0] * EXPECTED_FRAME_DIM] * 59)
    write_payload(input_dir / "bad.json", payload)

    summary = import_mobile_json_dir(
        input_dir,
        output_dir,
        allowed_actions={"hello"},
    )

    assert not summary.imported
    assert len(summary.skipped) == 1
    assert "invalid frame count" in summary.skipped[0][1]


def test_invalid_frame_dimension_is_rejected():
    payload = make_payload(frames=[[0.0] * 125] * EXPECTED_SEQUENCE_LENGTH)

    with pytest.raises(MobileJsonImportError, match="invalid frame shape"):
        validate_payload(payload, allowed_actions={"hello"})


def test_unknown_action_is_skipped(tmp_path):
    input_dir = tmp_path / "exports"
    output_dir = tmp_path / "MP_Data_mobile"
    input_dir.mkdir()
    write_payload(input_dir / "unknown.json", make_payload(action="not_in_actions"))

    summary = import_mobile_json_dir(
        input_dir,
        output_dir,
        allowed_actions={"hello"},
    )

    assert not summary.imported
    assert len(summary.skipped) == 1
    assert "unknown action" in summary.skipped[0][1]


def test_missing_action_is_rejected():
    payload = make_payload(action="")

    with pytest.raises(MobileJsonImportError, match="missing or invalid action"):
        validate_payload(payload, allowed_actions={"hello"})
