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
# The wrist (y 14..18) is left clear: the skin covers there open to let the links out.
PARTS = [
    ("mech", None, (0, 0, 0), [
        ("dark", -3, 6, -8, 6, 8, 2, ""),           # housing on the forearm
        ("red", -2, 6, -9, 1, 8, 1, "cable"),       # cables over the housing
        ("red", 1, 6, -9, 1, 8, 1, "cable"),
        ("light", -4, 6, -9, 8, 2, 3, "bolt"),      # clamps
        ("light", -4, 11, -9, 8, 2, 3, "bolt"),
        ("red", -5, 6, -5, 1, 8, 1, "cable"),       # cables along the sides of the arm
        ("red", -5, 6, -2, 1, 8, 1, "cable"),
        ("red", 4, 6, -5, 1, 8, 1, "cable"),
        ("red", 4, 6, -2, 1, 8, 1, "cable"),
        ("light", -6, 8, -6, 1, 3, 6, ""),          # side plates
        ("light", 5, 8, -6, 1, 3, 6, ""),
        ("dark", -2, 13, -10, 4, 2, 2, "core"),     # hinge seat, glowing core
    ]),
    ("link_lower", "mech", (0, 14, -9), [
        ("dark", -1, 0, -1, 2, 8, 2, ""),
        ("light", 1, 1, -1, 1, 6, 2, ""),
        ("light", -2, 1, -1, 1, 6, 2, ""),
        ("red", -1, 2, -2, 2, 4, 1, "cable"),
        ("light", -2, 7, -2, 4, 2, 3, "core"),      # knuckle
    ]),
    ("link_upper", "link_lower", (0, 8, 0), [
        ("dark", -1, 0, -1, 2, 7, 2, ""),
        ("light", -2, 1, -2, 4, 5, 1, ""),
        ("red", -1, 2, -3, 2, 3, 1, "cable"),
    ]),
    ("blade", "link_upper", (0, 7, 0), [
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
    "_thermal": {"steel": (84, 56, 50), "shine": (255, 150, 40), "accent": (255, 120, 30), "housing": (44, 36, 36)},
    "_thermal_hot": {"steel": (255, 140, 40), "shine": (255, 236, 150), "accent": (255, 200, 80), "housing": (70, 40, 34)},
    "_electric": {"steel": (56, 64, 84), "shine": (90, 210, 255), "accent": (70, 200, 255), "red": (34, 72, 200),
                  "housing": (30, 34, 46)},
    "_electric_hot": {"steel": (120, 215, 255), "shine": (240, 252, 255), "accent": (190, 240, 255), "red": (60, 120, 255),
                      "housing": (30, 44, 70)},
}
# Emissive pass: which marks glow for each base variant (drawn full bright on top of the model).
GLOW = {"": {"eye"}, "_thermal": {"eye", "edge", "stripe", "core"}, "_electric": {"eye", "edge", "stripe", "core", "cable"}}

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


def paint(suffix, uv, glow=None):
    """Paints the atlas; with glow set, only the texels of those marks, for the emissive pass."""
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
                        lit = None
                        if mat == "red" and xx == fw // 2 and fw > 0 and yy % 3 == 0:
                            c = shade(base, 1.35)
                            lit = "cable"
                        if marks == "edge" and xx == 0 and mat == "steel":
                            c = pal["shine"]
                            lit = "edge"
                        if marks == "stripe" and xx == fw // 2 and yy % 4 == 1:
                            c = pal["accent"]
                            lit = "stripe"
                        if marks == "core" and xx == fw // 2 and yy == fh // 2:
                            c = pal["accent"]
                            lit = "core"
                        if marks == "bolt" and xx == fw // 2 and yy == fh // 2:
                            c = shade(pal["dark"], 0.8)
                        if marks == "eye" and xx == fw // 2 and yy == 1 and (fw, fh) == (d, h):
                            c = pal["accent"]
                            lit = "eye"
                        if glow is None:
                            px[fx + xx, fy + yy] = c + (255,)
                        elif lit in glow:
                            px[fx + xx, fy + yy] = c + (255,)
    name = f"mantis_blades{suffix}{'_glow' if glow is not None else ''}.png"
    img.save(TEX / name)


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
    for suffix, marks in GLOW.items():
        paint(suffix, uv, marks)
    JAVA.write_text(java(uv))


if __name__ == "__main__":
    main()
