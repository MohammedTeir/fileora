#!/usr/bin/env python3
"""Import the Synapse v3 full-resolution master into assets/synapse/v3.

Usage:
    python3 scripts/import_synapse_v3_assets.py /path/to/Synapse_Production_Candidate_Assets_v3_Full_Resolution.zip

This script does not modify Android's active launcher resources.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
from zipfile import ZipFile

ARCHIVE_ROOT = "Synapse_Production_Candidate_Assets_v3"
DESTINATION = Path(__file__).resolve().parents[1] / "assets" / "synapse" / "v3"


def import_assets(archive: Path) -> None:
    if not archive.is_file():
        raise SystemExit(f"Archive not found: {archive}")
    with ZipFile(archive) as z:
        if z.testzip() is not None:
            raise SystemExit("ZIP integrity check failed")
        entries = [info for info in z.infolist() if not info.is_dir()]
        for info in entries:
            p = PurePosixPath(info.filename)
            if p.is_absolute() or ".." in p.parts or not p.parts or p.parts[0] != ARCHIVE_ROOT:
                raise SystemExit(f"Unexpected archive entry: {info.filename}")
        checksums = {}
        for info in entries:
            relative = PurePosixPath(info.filename).relative_to(ARCHIVE_ROOT)
            output = DESTINATION.joinpath(*relative.parts)
            output.parent.mkdir(parents=True, exist_ok=True)
            data = z.read(info)
            output.write_bytes(data)
            checksums[str(relative)] = hashlib.sha256(data).hexdigest()
    if len([p for p in (DESTINATION / "references").glob("screen_*.png")]) != 31:
        raise SystemExit("Expected exactly 31 full-resolution screen references")
    print(f"Imported {len(entries)} files into {DESTINATION}")
    print("All files preserved as original bytes; no image resizing or recompression.")
    print("Commit with: git add assets/synapse/v3 && git commit -m 'assets: import Synapse v3 full-resolution master' && git push origin main")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path)
    args = parser.parse_args()
    import_assets(args.archive)
