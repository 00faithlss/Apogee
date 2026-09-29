#!/usr/bin/env python3
"""Generates the Mantis Blade mechanism: Java mesh + textures from one parts list.

Units are texels (half a Minecraft model pixel); the renderer scales the mesh by 0.5.
Arm-local axes: +Y towards the hand (the arm ends at y=20), -Z out of the forearm's
front face (the face at z=-4). Run from the repository root; requires Pillow.
"""
import random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/dev/apogee/client/model/MantisMechanismMesh.java"
TEX = ROOT / "src/main/resources/assets/apogee/textures/entity"
SIZE = 128

# (part, parent, pivot, [(material, x, y, z, w, h, d, marks)])
PARTS = [
    ("mech", None, (0, 0, 0), [
        ("dark", -3, 6, -8, 6, 11, 2, ""),          # frame on the forearm
        ("red", -2, 6, -9, 1, 11, 1, ""),           # cables over the frame
        ("red", 1, 6, -9, 1, 11, 1, ""),
        ("light", -4, 7, -9, 8, 2, 3, "bolt"),      # clamps
        ("light", -4, 12, -9, 8, 2, 3, "bolt"),
        ("red", -5, 6, -5, 1, 10, 1, ""),           # cables along the sides of the arm
        ("red", -5, 6, -2, 1, 10, 1, ""),
        ("red", 4, 6, -5, 1, 10, 1, ""),
        ("red", 4, 6, -2, 1, 10, 1, ""),
        ("light", -6, 9, -6, 1, 3, 6, ""),          # side plates
        ("light", 5, 9, -6, 1, 3, 6, ""),
        ("dark", -5, 15, -8, 10, 2, 3, ""),         # wrist collar
        ("light", -2, 15, -10, 4, 2, 2, "bolt"),    # hinge seat
    ]),
    ("support", "mech", (0, 16, -9), [
        ("dark", -1, 0, -1, 2, 14, 2, ""),
        ("light", 1, 2, -1, 1, 10, 2, ""),
        ("light", -2, 2, -1, 1, 10, 2, ""),
        ("red", -1, 4, -2, 2, 6, 1, ""),
        ("light", -2, 12, -2, 4, 3, 3, "bolt"),     # knuckle
    ]),
    ("blade", "support", (0, 14, 0), [
        ("light", -2, -4, -2, 4, 5, 3, "eye"),      # hooked head
        ("light", -1, -6, -1, 2, 2, 2, ""),
        ("dark", -1, -4, 1, 2, 2, 2, ""),           # beak of the hook, towards the arm
        ("housing", -1, 1, -1, 2, 14, 2, "stripe"),
        ("light", -1, 3, -2, 2, 10, 1, ""),         # spine plate
    ]),
    ("edge", "blade", (0, 15, 0), [
        ("steel", -0.5, 0, -1, 1, 22, 2, "edge"),
        ("steel", -0.5, 1, 1, 1, 1, 1, ""),         # serrations on the arm side
        ("steel", -0.5, 3, 1, 1, 1, 1, ""),
        ("steel", -0.5, 5, 1, 1, 1, 1, ""),
        ("steel", -0.5, 7, 1, 1, 1, 1, ""),
        ("steel", -0.5, 9, 1, 1, 1, 1, ""),
        ("steel", -0.5, 22, -1, 1, 2, 1, "edge"),   # tip, flush with the spine side
        ("steel", -0.5, 24, -1, 1, 1, 1, "edge"),
    ]),
]

PALETTES = {
    "": {"dark": (52, 55, 63), "light": (178, 181, 186), "red": (158, 26, 34),
         "housing": (38, 40, 46), "steel": (150, 156, 166), "accent": (214, 40, 48), "shine": (226, 232, 240)},
    "_thermal": {"steel": (120, 70, 50), "shine": (255, 150, 60), "accent": (255, 110, 30)},
    "_thermal_hot": {"steel": (255, 140, 40), "shine": (255, 236, 150), "accent": (255, 200, 80), "housing": (70, 40, 34)},
    "_electric": {"steel": (60, 80, 110), "shine": (90, 200, 255), "accent": (60, 170, 255)},
    "_electric_hot": {"steel": (120, 215, 255), "shine": (240, 252, 255), "accent": (190, 240, 255), "housing": (30, 44, 70)},
}


def pack():
    """Shelf-packs every box's UV net; returns {(part, index): (u, v)}."""
    uv, x, y, shelf = {}, 0, 0, 0
    for name, _, _, boxes in PARTS:
        for i, (_, _, _, _, w, h, d, _) in enumerate(boxes):
            nw, nh = 2 * d + 2 * w, d + h
            if x + nw > SIZE:
                x, y, shelf = 0, y + shelf, 0
            uv[(name, i)] = (x, y)
            x, shelf = x + nw, max(shelf, nh)
    assert y + shelf <= SIZE, "texture atlas overflow"
    return uv


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def paint(suffix, uv):
    pal = dict(PALETTES[""])
    pal.update(PALETTES[suffix])
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(5)
    for name, _, _, boxes in PARTS:
        for i, (mat, _, _, _, w, h, d, marks) in enumerate(boxes):
            u, v = uv[(name, i)]
            faces = [(u + d, v, w, d, 1.12), (u + d + w, v, w, d, 0.7),          # top, bottom
                     (u, v + d, d, h, 0.9), (u + d, v + d, w, h, 1.0),           # sides
                     (u + d + w, v + d, d, h, 0.9), (u + 2 * d + w, v + d, w, h, 0.82)]
            base = pal[mat]
            for fx, fy, fw, fh, light in faces:
                for yy in range(fh):
                    for xx in range(fw):
                        k = light * (0.94 + rnd.random() * 0.1)
                        border = (xx == 0 or yy == 0 or xx == fw - 1 or yy == fh - 1) and fw > 2 and fh > 2
                        if border:
                            k *= 0.72
                        c = shade(base, k)
                        if mat == "red" and xx == fw // 2 and fw > 0 and yy % 3 == 0:
                            c = shade(base, 1.35)
                        if marks == "edge" and yy >= 0 and xx == 0 and mat == "steel":
                            c = pal["shine"]
                        if marks == "stripe" and xx == fw // 2 and yy % 4 == 1:
                            c = pal["accent"]
                        if marks == "bolt" and xx == fw // 2 and yy == fh // 2:
                            c = shade(pal["dark"], 0.8)
                        if marks == "eye" and xx == fw // 2 and yy == 1 and (fw, fh) == (d, h):
                            c = pal["accent"]
                        px[fx + xx, fy + yy] = c + (255,)
    img.save(TEX / f"mantis_blades{suffix}.png")


def fmt(n):
    return f"{float(n)}F"


def java(uv):
    lines = []
    for name, parent, pivot, boxes in PARTS:
        cubes = "".join(
            f"\n                        .texOffs({uv[(name, i)][0]}, {uv[(name, i)][1]})"
            f".addBox({fmt(x)}, {fmt(y)}, {fmt(z)}, {fmt(w)}, {fmt(h)}, {fmt(d)})"
            for i, (_, x, y, z, w, h, d, _) in enumerate(boxes))
        target = "parent" if parent is None else parent
        lines.append(f"        PartDefinition {name} = {target}.addOrReplaceChild(\"{name}\", CubeListBuilder.create(){cubes},\n"
                     f"                PartPose.offset({fmt(pivot[0])}, {fmt(pivot[1])}, {fmt(pivot[2])}));")
    body = "\n".join(lines)
    return f'''package dev.apogee.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Mantis Blade mechanism cubes, in texels (half a model pixel).
 * Generated by tools/mantis_model.py together with textures/entity/mantis_blades*.png; edit the
 * script and run it again rather than editing this file, so geometry and UVs stay in sync.
 */
final class MantisMechanismMesh {{
    static final int TEXTURE_SIZE = {SIZE};

    private MantisMechanismMesh() {{
    }}

    @SuppressWarnings("unused")
    static void build(PartDefinition parent) {{
{body}
    }}
}}
'''


def main():
    uv = pack()
    for suffix in PALETTES:
        paint(suffix, uv)
    JAVA.write_text(java(uv))


if __name__ == "__main__":
    main()
