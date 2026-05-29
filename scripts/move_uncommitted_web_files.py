#!/usr/bin/env python3
"""Move uncommitted generated/support files to model-trash."""

from __future__ import annotations

import argparse
import shutil
import subprocess
from pathlib import Path


TARGET_EXTENSIONS = {
    ".avif",
    ".bmp",
    ".css",
    ".csv",
    ".gif",
    ".html",
    ".ico",
    ".json",
    ".jpeg",
    ".jpg",
    ".js",
    ".log",
    ".markdown",
    ".md",
    ".png",
    ".properties",
    ".rst",
    ".sass",
    ".scss",
    ".svg",
    ".txt",
    ".webp",
    ".xml",
}
TRASH_DIR_NAME = "model-trash"
HANDOFF_ANALYSIS_DIR_NAME = "model-handoff-and-analysis"
HANDOFF_ANALYSIS_EXTENSIONS = {".md", ".markdown"}


def repository_root() -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        check=True,
        capture_output=True,
        text=True,
    )
    return Path(result.stdout.strip())


def uncommitted_paths(root: Path) -> list[Path]:
    result = subprocess.run(
        ["git", "status", "--porcelain=v1", "-z", "--untracked-files=all"],
        cwd=root,
        check=True,
        capture_output=True,
    )

    entries = result.stdout.split(b"\0")
    paths: list[Path] = []
    index = 0

    while index < len(entries):
        entry = entries[index]
        index += 1

        if not entry:
            continue

        status = entry[:2].decode("ascii")
        raw_path = entry[3:]

        if "R" in status or "C" in status:
            # In porcelain -z output, renamed/copied entries are followed by the
            # original path. The first path is the current destination.
            index += 1

        if status != "??":
            continue

        paths.append(root / raw_path.decode("utf-8", errors="surrogateescape"))

    return paths


def unique_destination(path: Path) -> Path:
    if not path.exists():
        return path

    counter = 1
    stem = path.stem
    suffix = path.suffix

    while True:
        candidate = path.with_name(f"{stem}-{counter}{suffix}")
        if not candidate.exists():
            return candidate
        counter += 1


def should_move(path: Path) -> bool:
    if path.name.startswith("."):
        return False

    return not path.suffix or path.suffix.lower() in TARGET_EXTENSIONS


def target_directory(root: Path, path: Path) -> Path:
    if path.suffix.lower() in HANDOFF_ANALYSIS_EXTENSIONS:
        return root / HANDOFF_ANALYSIS_DIR_NAME

    return root / TRASH_DIR_NAME


def move_files(root: Path, dry_run: bool) -> int:
    moved = 0

    for source in uncommitted_paths(root):
        if not source.exists() or not source.is_file():
            continue

        relative_source = source.relative_to(root)

        if relative_source.parts[0] in {TRASH_DIR_NAME, HANDOFF_ANALYSIS_DIR_NAME}:
            continue

        if not should_move(source):
            continue

        destination_dir = target_directory(root, source)
        destination = unique_destination(destination_dir / relative_source)
        print(f"{relative_source} -> {destination.relative_to(root)}")

        if not dry_run:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(source), str(destination))

        moved += 1

    return moved


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Move uncommitted generated/support files to model-trash."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="show what would be moved without changing files",
    )
    args = parser.parse_args()

    root = repository_root()
    moved = move_files(root, args.dry_run)

    action = "Would move" if args.dry_run else "Moved"
    print(f"{action} {moved} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
