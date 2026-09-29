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
MATERIALS = ["black", "steel", "silver", "edge", "cable", "accent", "core", "skin",
             "sleeve", "bolt", "recess", "warning", "ceramic", "copper", "capacitor", "hazard"]


def cell(material):
    """UV origin of a material's 16x16 cell in the kit's 4x4 material atlas."""
    index = MATERIALS.index(material)
    return [index % 4 * 16, index // 4 * 16]


SKIN_UV = cell("skin")
# Reference cubes from the kit's .bbmodel projects, in editor coordinates (x is symmetric).
EXTRA_BONES = [
    {"name": "skin_hand", "parent": "wrist", "pivot": [0, 0, -3.6], "from": [-2, -2, -6], "to": [2, 2, -3.6]},
    # The kit's sleeve reference starts at z=5, but its skin panels stop at z=4.6: start the cap
    # there so the closed arm has no gap by the shoulder.
    {"name": "skin_shoulder", "parent": "arm_root", "pivot": [0, 0, 0], "from": [-2, -2, 4.6], "to": [2, 2, 6]},
]


# Cyberware under the housing floor, in the band between the floor (y=-0.76) and the top of the
# closed lower covers (y=-0.9): flat cables along the forearm held by clamps, and two small
# modules. The cables use the `cable` material, whose colour follows the variant.
UNDERSIDE = [("cable", [x, -0.88, -2.3], [x + 0.22, -0.77, 3.3]) for x in (-1.05, -0.62, -0.19, 0.24, 0.67)] + [
    ("silver", [-1.15, -0.89, z], [1.15, -0.74, z + 0.2]) for z in (-1.7, -0.2, 1.3, 2.7)] + [
    ("copper", [-0.5, -0.89, 0.25], [0.1, -0.76, 0.85]),
    ("capacitor", [0.35, -0.89, 1.65], [0.9, -0.76, 2.2]),
]
# The standard variant's cables are grey rather than red.
GREY_CABLES = {"mantis_normal.png"}


def skin_cube(frm, to):
    return material_cube("skin", frm, to)


def material_cube(material, frm, to):
    face = {"uv": cell(material), "uv_size": [16, 16]}
    return {"origin": frm, "size": [to[i] - frm[i] for i in range(3)],
            "uv": {side: dict(face) for side in ("north", "east", "south", "west", "up", "down")}}


def main(kit):
    src = Path(kit) / "resources/assets/mantis"
    for folder in ("geo", "animations"):
        (ASSETS / folder).mkdir(parents=True, exist_ok=True)
    for geo in sorted((src / "geo").glob("*.geo.json")):
        data = json.loads(geo.read_text())
        bones = data["minecraft:geometry"][0]["bones"]
        housing = next(bone for bone in bones if bone["name"] == "housing")
        housing["cubes"].extend(material_cube(material, frm, to) for material, frm, to in UNDERSIDE)
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
        if name in GREY_CABLES:
            grey_cable_cell(textures / name)


def grey_cable_cell(path):
    """Desaturates the `cable` cell, keeping its shading."""
    from PIL import Image
    image = Image.open(path).convert("RGBA")
    u0, v0 = cell("cable")
    for y in range(v0, v0 + 16):
        for x in range(u0, u0 + 16):
            r, g, b, a = image.getpixel((x, y))
            level = int(0.35 * r + 0.45 * g + 0.2 * b) + 40
            image.putpixel((x, y), (level, level + 3, level + 8, a))
    image.save(path)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
