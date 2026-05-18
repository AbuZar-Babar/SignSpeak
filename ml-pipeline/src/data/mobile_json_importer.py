import argparse
import json
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable, Optional, Union

import numpy as np

from src.config.config import DATASET_FOLDERS, load_actions


EXPECTED_FORMAT = "signspeak-landmarks-v1"
EXPECTED_SEQUENCE_LENGTH = 60
EXPECTED_FRAME_DIM = 126


class MobileJsonImportError(ValueError):
    pass


@dataclass
class ImportSummary:
    imported: list[Path] = field(default_factory=list)
    skipped: list[tuple[Path, str]] = field(default_factory=list)


def normalize_action_name(raw: object) -> str:
    if not isinstance(raw, str):
        return ""
    action = raw.strip().lower().replace(" ", "_")
    action = re.sub(r"[^a-z0-9_-]", "", action)
    return action


def load_allowed_actions() -> set[str]:
    return {normalize_action_name(action) for action in load_actions()}


def validate_payload(
    payload: dict,
    *,
    allowed_actions: Optional[set[str]] = None,
) -> tuple[str, np.ndarray]:
    if payload.get("format") != EXPECTED_FORMAT:
        raise MobileJsonImportError(
            f"invalid format {payload.get('format')!r}; expected {EXPECTED_FORMAT!r}"
        )

    action = normalize_action_name(payload.get("action"))
    if not action:
        raise MobileJsonImportError("missing or invalid action")
    if allowed_actions is not None and action not in allowed_actions:
        raise MobileJsonImportError(f"unknown action {action!r}")

    sequence_length = payload.get("sequence_length")
    if sequence_length != EXPECTED_SEQUENCE_LENGTH:
        raise MobileJsonImportError(
            f"invalid sequence_length {sequence_length!r}; expected {EXPECTED_SEQUENCE_LENGTH}"
        )

    dim = payload.get("dim")
    if dim != EXPECTED_FRAME_DIM:
        raise MobileJsonImportError(f"invalid dim {dim!r}; expected {EXPECTED_FRAME_DIM}")

    frames = payload.get("frames")
    if not isinstance(frames, list):
        raise MobileJsonImportError("frames must be a list")
    if len(frames) != EXPECTED_SEQUENCE_LENGTH:
        raise MobileJsonImportError(
            f"invalid frame count {len(frames)}; expected {EXPECTED_SEQUENCE_LENGTH}"
        )

    try:
        array = np.asarray(frames, dtype=np.float32)
    except (TypeError, ValueError) as error:
        raise MobileJsonImportError(f"frames must be numeric: {error}") from error

    if array.shape != (EXPECTED_SEQUENCE_LENGTH, EXPECTED_FRAME_DIM):
        raise MobileJsonImportError(
            f"invalid frame shape {array.shape}; expected "
            f"({EXPECTED_SEQUENCE_LENGTH}, {EXPECTED_FRAME_DIM})"
        )
    if not np.isfinite(array).all():
        raise MobileJsonImportError("frames contain NaN or infinite values")

    return action, array


def next_sequence_id(action_dir: Path) -> int:
    if not action_dir.exists():
        return 0

    sequence_ids = [
        int(path.name)
        for path in action_dir.iterdir()
        if path.is_dir() and path.name.isdigit()
    ]
    return max(sequence_ids, default=-1) + 1


def write_sequence(output_dir: Path, action: str, frames: np.ndarray) -> Path:
    action_dir = output_dir / action
    sequence_dir = action_dir / str(next_sequence_id(action_dir))
    sequence_dir.mkdir(parents=True, exist_ok=False)

    for frame_index, frame in enumerate(frames):
        np.save(sequence_dir / f"{frame_index}.npy", frame)

    return sequence_dir


def iter_json_files(input_dir: Path) -> Iterable[Path]:
    return sorted(path for path in input_dir.rglob("*.json") if path.is_file())


def import_mobile_json_dir(
    input_dir: Union[Path, str],
    output_dir: Optional[Union[Path, str]] = None,
    *,
    allowed_actions: Optional[set[str]] = None,
    strict: bool = False,
) -> ImportSummary:
    input_path = Path(input_dir)
    if output_dir is None:
        output_path = Path(DATASET_FOLDERS["mobile"])
    else:
        output_path = Path(output_dir)

    if not input_path.is_dir():
        raise MobileJsonImportError(f"input folder does not exist: {input_path}")

    summary = ImportSummary()
    for json_file in iter_json_files(input_path):
        try:
            payload = json.loads(json_file.read_text(encoding="utf-8"))
            if not isinstance(payload, dict):
                raise MobileJsonImportError("top-level JSON must be an object")
            action, frames = validate_payload(payload, allowed_actions=allowed_actions)
            sequence_dir = write_sequence(output_path, action, frames)
            summary.imported.append(sequence_dir)
        except Exception as error:
            if strict:
                raise
            summary.skipped.append((json_file, str(error)))

    return summary


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Import SignSpeak Collector JSON files into MP_Data_mobile."
    )
    parser.add_argument(
        "input_dir",
        type=Path,
        help="Folder containing JSON files exported by the Android collector.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(DATASET_FOLDERS["mobile"]),
        help="Destination MP_Data_mobile folder.",
    )
    parser.add_argument(
        "--allow-new-actions",
        action="store_true",
        help="Allow action folders not listed in data/external/actions.txt.",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Stop on the first invalid JSON file.",
    )
    return parser


def main() -> int:
    args = build_parser().parse_args()
    allowed_actions = None if args.allow_new_actions else load_allowed_actions()
    summary = import_mobile_json_dir(
        input_dir=args.input_dir,
        output_dir=args.output_dir,
        allowed_actions=allowed_actions,
        strict=args.strict,
    )

    for path in summary.imported:
        print(f"imported: {path}")
    for path, reason in summary.skipped:
        print(f"skipped: {path} ({reason})")
    print(f"done: imported={len(summary.imported)} skipped={len(summary.skipped)}")
    return 1 if args.strict and summary.skipped else 0


if __name__ == "__main__":
    raise SystemExit(main())
