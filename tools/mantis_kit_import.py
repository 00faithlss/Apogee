#!/usr/bin/env python3
"""Imports the Mantis kit's GeckoLib exports into the mod.

Usage: python3 tools/mantis_kit_import.py <path/to/mantis-kit>

Copies resources/assets/mantis/{geo,animations,textures/item} into assets/apogee, renaming the
emissive masks to GeckoLib's *_glowmask.png convention, and adds the two skin pieces the kit
leaves out of its runtime export (see its LEEME.md): the hand under the `wrist` bone and the
shoulder cap under `arm_root`. They use the atlas' `skin` material, so the game draws them with
the wearer's skin like every other skin panel. Run it again after re-exporting from Blockbench.
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/apogee"
SKIN_UV = [48, 16]  # the `skin` material cell of the kit's 4x4 material atlas
# Reference cubes from the kit's .bbmodel projects, in editor coordinates (x is symmetric).
EXTRA_BONES = [
    {"name": "skin_hand", "parent": "wrist", "pivot": [0, 0, -3.6], "from": [-2, -2, -6], "to": [2, 2, -3.6]},
    # The kit's sleeve reference starts at z=5, but its skin panels stop at z=4.6: start the cap
    # there so the closed arm has no gap by the shoulder.
    {"name": "skin_shoulder", "parent": "arm_root", "pivot": [0, 0, 0], "from": [-2, -2, 4.6], "to": [2, 2, 6]},
]


def skin_cube(frm, to):
    face = {"uv": SKIN_UV, "uv_size": [16, 16]}
    return {"origin": frm, "size": [to[i] - frm[i] for i in range(3)],
            "uv": {side: dict(face) for side in ("north", "east", "south", "west", "up", "down")}}


def main(kit):
    src = Path(kit) / "resources/assets/mantis"
    for folder in ("geo", "animations"):
        (ASSETS / folder).mkdir(parents=True, exist_ok=True)
    for geo in sorted((src / "geo").glob("*.geo.json")):
        data = json.loads(geo.read_text())
        bones = data["minecraft:geometry"][0]["bones"]
        for extra in EXTRA_BONES:
            bones.append({"name": extra["name"], "parent": extra["parent"], "pivot": extra["pivot"],
                          "cubes": [skin_cube(extra["from"], extra["to"])]})
        (ASSETS / "geo" / geo.name).write_text(json.dumps(data, indent=1) + "\n")
    for anim in sorted((src / "animations").glob("*.json")):
        shutil.copy(anim, ASSETS / "animations" / anim.name)
    textures = ASSETS / "textures/mantis"
    textures.mkdir(parents=True, exist_ok=True)
    for png in sorted((src / "textures/item").glob("*.png")):
        name = png.name.replace("_emissive.png", "_glowmask.png")
        shutil.copy(png, textures / name)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
