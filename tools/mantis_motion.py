#!/usr/bin/env python3
"""Tracks the first-person hands in the frames of `tools/mantis_tests.sh motion`.

For each run (vanilla, punchy) and scenario, finds the hand pixels in the lower corners (anything
that is neither grass green nor sky blue, away from the hotbar), and prints per series the
centroid's travel (peak to peak, in pixels of a 960x540 frame), its mean position and the main
period in ticks. With --csv, writes every frame's values too.

    python3 tools/mantis_motion.py [build/mantis-motion] [--csv out.csv]
"""
import glob
import math
import os
import re
import sys

from PIL import Image


def hand_mask(px, x, y):
    r, g, b = px[x, y][:3]
    grass = g > r + 12 and g > b + 12
    sky = b > r + 25 and b >= g
    return not grass and not sky


def track(path, side):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()
    # Lower corner of the screen, clear of the hotbar (the middle third) and the hearts above it.
    x0, x1 = (int(w * 0.70), w) if side == "right" else (0, int(w * 0.30))
    y0, y1 = int(h * 0.45), h
    xs = ys = n = 0
    top = h
    for y in range(y0, y1, 2):
        for x in range(x0, x1, 2):
            if hand_mask(px, x, y):
                xs += x
                ys += y
                n += 1
                top = min(top, y)
    if n < 20:
        return None
    return xs / n, ys / n, top, n


def period(values):
    """Main period in frames (ticks), from the autocorrelation's first peak after zero lag."""
    m = sum(values) / len(values)
    v = [x - m for x in values]
    var = sum(x * x for x in v)
    if var < 1e-6:
        return None
    best, best_lag = 0.0, None
    falling = True
    prev = 1.0
    for lag in range(1, len(v) // 2):
        c = sum(v[i] * v[i + lag] for i in range(len(v) - lag)) / var
        if falling and c > prev:
            falling = False
        if not falling and c > best:
            best, best_lag = c, lag
        prev = c
    return best_lag if best > 0.2 else None


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    root = args[0] if args else "build/mantis-motion"
    csv = None
    if "--csv" in sys.argv:
        csv = open(sys.argv[sys.argv.index("--csv") + 1], "w")
        csv.write("run,series,side,frame,cx,cy,top,pixels\n")
    print(f"{'run':8} {'series':22} {'side':5} {'frames':>6} {'dx p-p':>7} {'dy p-p':>7} {'top p-p':>7} {'mean y':>7} {'period':>6}")
    for run in sorted(os.listdir(root)):
        shots = glob.glob(os.path.join(root, run, "screenshots", "motion-*.png"))
        series = {}
        for shot in shots:
            m = re.match(r"motion-(.+)-(\d{3})\.png", os.path.basename(shot))
            series.setdefault(m.group(1), []).append((int(m.group(2)), shot))
        for name in sorted(series):
            frames = sorted(series[name])
            for side in ("right", "left"):
                data = [(f, track(p, side)) for f, p in frames]
                data = [(f, d) for f, d in data if d]
                if len(data) < len(frames) // 2:
                    continue
                cx = [d[0] for _, d in data]
                cy = [d[1] for _, d in data]
                top = [d[2] for _, d in data]
                if csv:
                    for f, d in data:
                        csv.write(f"{run},{name},{side},{f},{d[0]:.1f},{d[1]:.1f},{d[2]},{d[3]}\n")
                per = period(cy) if not name.startswith("fold") else None
                print(f"{run:8} {name:22} {side:5} {len(data):6d} {max(cx) - min(cx):7.1f} {max(cy) - min(cy):7.1f} "
                      f"{max(top) - min(top):7d} {sum(cy) / len(cy):7.1f} {per if per else '-':>6}")


if __name__ == "__main__":
    main()
