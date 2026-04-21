#!/usr/bin/env python3
"""
Resize sources to exact Play Store canvas sizes.

Sources:
  - screenshot_01.jpg … screenshot_10.jpg
  - first screen.png, second Screen.png (marketing / poster frames)

Uses independent width/height scaling (stretch) so the ENTIRE image maps to the
full canvas — no cropping, no letterboxing (aspect ratio adjusts to each target:
phone 1080×1920, tablet 7″ 1080×1920, tablet 10″ 1200×1920).
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Install Pillow: python3 -m venv .venv && .venv/bin/pip install Pillow", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1] / "App Screenshot"
SRC_DIR = ROOT
OUT_GOOGLE = ROOT / "google_play"


def stretch_to_canvas(im: Image.Image, canvas_w: int, canvas_h: int) -> Image.Image:
    """Scale image to exactly canvas_w×canvas_h; every source pixel contributes, nothing is cut off."""
    if im.mode not in ("RGB",):
        im = im.convert("RGB")
    return im.resize((canvas_w, canvas_h), Image.Resampling.LANCZOS)


def write_play_triplet(im: Image.Image, phone_name: str, tab7_name: str, tab10_name: str) -> None:
    """Write phone / tablet7 / tablet10 JPEGs from one opened image."""
    phone = stretch_to_canvas(im, 1080, 1920)
    phone.save(OUT_GOOGLE / "phone_1080x1920" / phone_name, quality=92, optimize=True)

    tab7 = stretch_to_canvas(im, 1080, 1920)
    tab7.save(OUT_GOOGLE / "tablet_7in_1080x1920" / tab7_name, quality=92, optimize=True)

    tab10 = stretch_to_canvas(im, 1200, 1920)
    tab10.save(OUT_GOOGLE / "tablet_10in_1200x1920" / tab10_name, quality=92, optimize=True)


# Extra marketing-style full frames (PNG), same export rules as screenshots.
EXTRA_SOURCES: list[tuple[str, str]] = [
    ("first screen.png", "first_screen"),
    ("second Screen.png", "second_screen"),
]


def main() -> None:
    os.makedirs(OUT_GOOGLE / "phone_1080x1920", exist_ok=True)
    os.makedirs(OUT_GOOGLE / "tablet_7in_1080x1920", exist_ok=True)
    os.makedirs(OUT_GOOGLE / "tablet_10in_1200x1920", exist_ok=True)

    for i in range(1, 11):
        src = SRC_DIR / f"screenshot_{i:02d}.jpg"
        if not src.is_file():
            print(f"Missing {src}", file=sys.stderr)
            sys.exit(1)
        im = Image.open(src)
        write_play_triplet(
            im,
            f"phone_{i:02d}_1080x1920.jpg",
            f"tablet_7in_{i:02d}_1080x1920.jpg",
            f"tablet_10in_{i:02d}_1200x1920.jpg",
        )
        print(f"Wrote {i:02d} -> phone, tablet_7in, tablet_10in")

    for filename, stem in EXTRA_SOURCES:
        src = SRC_DIR / filename
        if not src.is_file():
            print(f"Missing (optional) {src}", file=sys.stderr)
            continue
        im = Image.open(src)
        write_play_triplet(
            im,
            f"phone_{stem}_1080x1920.jpg",
            f"tablet_7in_{stem}_1080x1920.jpg",
            f"tablet_10in_{stem}_1200x1920.jpg",
        )
        print(f"Wrote {stem} <- {filename}")


if __name__ == "__main__":
    main()
