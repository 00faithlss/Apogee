#!/usr/bin/env python3
"""Compare real runNanowireSmoke frames. Requires Pillow and ffmpeg, not the art rig."""
import argparse
from pathlib import Path
import subprocess

from PIL import Image, ImageDraw, ImageFont, ImageOps


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("before", type=Path)
    parser.add_argument("after", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--heavy-only", action="store_true", help="Compare only right-click attacks")
    parser.add_argument("--before-label", default="ANTES / feat/ripperdoc")
    parser.add_argument("--after-label", default="DESPUES / feat/animations")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    frames = args.output / "frames"
    frames.mkdir(exist_ok=True)
    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 22)
        small = ImageFont.truetype("DejaVuSans.ttf", 17)
    except OSError:
        font = ImageFont.load_default(size=22)
        small = ImageFont.load_default(size=17)
    with Image.open(args.before / "nanowire-12-first-lash-right-00.png") as source:
        panel_height = min(680, round(source.height * 640 / source.width))
    height = 88 + panel_height + 32
    moves = [
        ("12-first-lash-right", "Clic izquierdo / brazo derecho"),
        ("13-first-lash-left", "Clic izquierdo / brazo izquierdo"),
        ("14-first-heavy", "Clic derecho / vertical"),
        ("14b-first-heavy", "Clic derecho / S"),
        ("14c-first-heavy", "Clic derecho / X"),
        ("08-third-lash-right", "Tercera persona / brazo derecho"),
        ("09-third-lash-left", "Tercera persona / brazo izquierdo"),
        ("10-third-heavy", "Tercera persona / X"),
    ]
    if args.heavy_only:
        moves = [move for move in moves if move[0] in {"14-first-heavy", "14b-first-heavy", "14c-first-heavy", "10-third-heavy"}]
    count = 0
    for prefix, title in moves:
        for tick in range(26):
            filename = f"nanowire-{prefix}-{tick:02d}.png"
            before, after = args.before / filename, args.after / filename
            if not before.exists() or not after.exists():
                raise FileNotFoundError(f"Missing pair: {before}, {after}")
            canvas = Image.new("RGB", (1280, height), "#111820")
            draw = ImageDraw.Draw(canvas)
            draw.text((22, 15), title, font=font, fill="white")
            draw.text((22, 52), args.before_label, font=small, fill="#adb7c3")
            draw.text((662, 52), args.after_label, font=small, fill="#ff7979")
            for x, path in [(0, before), (640, after)]:
                with Image.open(path) as source:
                    panel = ImageOps.contain(source.convert("RGB"), (640, panel_height))
                canvas.paste(panel, (x + (640 - panel.width) // 2, 88 + (panel_height - panel.height) // 2))
            draw.text((22, height - 26), f"Minecraft real / captura por tick / {tick * 0.05:.2f} s", font=small, fill="#adb7c3")
            canvas.save(frames / f"{count:04d}.png")
            if tick == 6:
                canvas.save(args.output / f"{prefix}.png")
            count += 1
    subprocess.run([
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error", "-framerate", "20",
        "-i", str(frames / "%04d.png"), "-frames:v", str(count), "-c:v", "libx264",
        "-pix_fmt", "yuv420p", "-crf", "19", str(args.output / "nanowire-antes-despues.mp4"),
    ], check=True)
    subprocess.run([
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error", "-i",
        str(args.output / "nanowire-antes-despues.mp4"), "-t", "6.5", "-vf",
        "fps=15,scale=960:-1:flags=lanczos,split[a][b];[a]palettegen[p];[b][p]paletteuse",
        str(args.output / "nanowire-antes-despues.gif"),
    ], check=True)
    print(args.output / "nanowire-antes-despues.mp4")


if __name__ == "__main__":
    main()
