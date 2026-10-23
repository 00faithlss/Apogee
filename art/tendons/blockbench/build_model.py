#!/usr/bin/env python3
"""Builds the Reinforced Tendons item model from one description.

Outputs, next to the game files they feed:
  art/tendons/blockbench/reinforced_tendons.bbmodel   (open in Blockbench to edit)
  src/main/resources/assets/apogee/models/item/reinforced_tendons.json
  src/main/resources/assets/apogee/textures/item/reinforced_tendons_3d.png

The piece is the leg actuator of the reference image: a black cylinder with a red ring on its
open end, a bundle of twisted orange cables climbing to a dark housing. The implant is not drawn
on the player, so this model is only seen as an item (inventory, hand, ground, tooltip).
Requires Pillow. Run from the repository root.
"""
import base64
import io
import json
import math
import random
import uuid

from PIL import Image

ROOT = "."
BBMODEL = ROOT + "/art/tendons/blockbench/reinforced_tendons.bbmodel"
MODEL = ROOT + "/src/main/resources/assets/apogee/models/item/reinforced_tendons.json"
TEXTURE = ROOT + "/src/main/resources/assets/apogee/textures/item/reinforced_tendons_3d.png"
TEXTURE_REF = "apogee:item/reinforced_tendons_3d"

# 32x32 texture, a 4x4 grid of 8x8 swatches (4x4 in the model's 16-unit UV space).
SWATCHES = {
    "metal_dark": (0, 0),
    "metal_light": (1, 0),
    "cable": (2, 0),
    "ring_red": (3, 0),
    "inner": (0, 1),
    "port_blue": (1, 1),
    "label": (2, 1),
    "clamp": (3, 1),
}


def swatch_uv(name, u0=0.0, v0=0.0, u1=1.0, v1=1.0):
    col, row = SWATCHES[name]
    return [col * 4 + u0 * 4, row * 4 + v0 * 4, col * 4 + u1 * 4, row * 4 + v1 * 4]


def draw_texture():
    rng = random.Random(7)
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
    px = image.load()

    def fill(name, painter):
        col, row = SWATCHES[name]
        for y in range(8):
            for x in range(8):
                px[col * 8 + x, row * 8 + y] = painter(x, y)

    def jitter(base, amount):
        d = rng.randint(-amount, amount)
        return tuple(max(0, min(255, c + d)) for c in base) + (255,)

    fill("metal_dark", lambda x, y: jitter((28, 28, 32), 5) if (x + y) % 5 else (46, 46, 52, 255))
    fill("metal_light", lambda x, y: jitter((78, 80, 88), 6) if y % 4 else (104, 106, 116, 255))
    # Cable: bright orange with a darker lower edge and a highlight line, like the reference.
    fill("cable", lambda x, y: (255, 168, 90, 255) if y == 1 else ((196, 74, 18, 255) if y < 5 else (130, 44, 10, 255)))
    fill("ring_red", lambda x, y: (232, 20, 28, 255) if y < 6 else (150, 8, 16, 255))
    fill("inner", lambda x, y: jitter((10, 10, 12), 2))
    fill("port_blue", lambda x, y: (40, 150, 255, 255) if 2 <= x <= 5 and 2 <= y <= 5 else (20, 24, 34, 255))
    fill("label", lambda x, y: (210, 214, 220, 255) if y in (3, 4) and 1 <= x <= 6 else (46, 46, 52, 255))
    fill("clamp", lambda x, y: jitter((14, 14, 16), 3) if y % 3 else (62, 64, 72, 255))
    return image


class Model:
    def __init__(self):
        self.elements = []

    def box(self, name, frm, to, swatch, rotation=None, origin=None, faces=None):
        element = {
            "name": name,
            "from": [round(v, 3) for v in frm],
            "to": [round(v, 3) for v in to],
            "faces": {},
        }
        if rotation:
            axis, angle = rotation
            element["rotation"] = {"angle": angle, "axis": axis,
                                   "origin": [round(v, 3) for v in (origin or [(a + b) / 2 for a, b in zip(frm, to)])]}
        sw = faces or {}
        for face in ("north", "east", "south", "west", "up", "down"):
            element["faces"][face] = {"uv": [round(v, 3) for v in swatch_uv(sw.get(face, swatch))], "texture": "#atlas"}
        self.elements.append(element)

    def cylinder(self, name, x0, x1, radius, swatch, caps=None):
        """A cylinder along X: four boxes turned 22.5 degrees apart about X make a 16-sided prism."""
        c = 8.0
        half = radius / 1.3
        faces = {"west": caps or swatch, "east": caps or swatch}
        for k in range(4):
            self.box(f"{name}_{k}", (x0, c - half, c - half), (x1, c + half, c + half), swatch,
                     rotation=("x", (0.0, 22.5, 45.0, -22.5)[k]) if k else None, origin=(0, c, c), faces=faces)


def build():
    m = Model()
    c = 8.0

    # Main body: the black cylinder, open end (west) on the left.
    m.cylinder("body", 2.0, 9.5, 3.7, "metal_dark")
    # Lighter band around the body and a grey neck that joins the housing.
    m.cylinder("band", 5.2, 5.9, 3.95, "metal_light")
    m.cylinder("neck", 9.5, 11.0, 2.9, "metal_light")
    # The red ring on the open end, with the dark socket inside.
    m.cylinder("ring", 1.2, 2.0, 4.15, "ring_red", caps="ring_red")
    m.cylinder("socket", 0.9, 1.3, 2.6, "inner", caps="inner")

    # Dark housing with ports and a label, far end.
    m.box("housing", (11.0, c - 3.2, c - 2.6), (15.4, c + 3.2, c + 2.6), "metal_dark",
          faces={"up": "metal_light", "south": "label"})
    m.box("housing_plate", (11.4, c + 3.2, c - 1.8), (14.6, c + 3.9, c + 1.8), "clamp", faces={"up": "port_blue"})
    m.box("port_a", (15.4, c - 1.9, c - 1.4), (15.9, c - 0.7, c - 0.2), "port_blue")
    m.box("port_b", (15.4, c + 0.5, c + 0.3), (15.9, c + 1.7, c + 1.5), "port_blue")
    m.box("pin_a", (12.0, c - 3.9, c - 0.6), (13.4, c - 3.2, c + 0.6), "metal_light")

    # Three orange cables twisting around the body and climbing to the housing.
    segments = 16
    for cable in range(3):
        phase = cable * 2.0 * math.pi / 3.0
        for i in range(segments):
            t = i / (segments - 1)
            x = 3.2 + t * 8.0
            radius = 4.6 - 2.3 * t
            theta = phase + t * 5.0 - 0.6
            y = c + radius * math.cos(theta) * 0.95
            z = c + radius * math.sin(theta) * 0.95
            r = 0.95
            m.box(f"cable{cable}_{i}", (x - 0.8, y - r, z - r), (x + 0.8, y + r, z + r), "cable",
                  rotation=("x", 22.5 if i % 2 else 0.0), origin=(x, y, z))
    # Clamps holding the bundle together.
    m.cylinder("clamp_a", 7.0, 7.8, 4.4, "clamp")
    m.cylinder("clamp_b", 9.9, 10.5, 3.3, "clamp")
    return m


DISPLAY = {
    "gui": {"rotation": [25, 150, 35], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.45, 0.45, 0.45]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 12, 0], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_righthand": {"rotation": [0, 90, 55], "translation": [0, 3, 1], "scale": [0.42, 0.42, 0.42]},
    "thirdperson_lefthand": {"rotation": [0, 90, 55], "translation": [0, 3, 1], "scale": [0.42, 0.42, 0.42]},
    "firstperson_righthand": {"rotation": [0, 90, 25], "translation": [2, 1.5, 0], "scale": [0.28, 0.28, 0.28]},
    "firstperson_lefthand": {"rotation": [0, 90, 25], "translation": [2, 1.5, 0], "scale": [0.28, 0.28, 0.28]},
}


def write_game_model(model):
    data = {
        "credit": "Original 3D model made for Apogee, based on the user's reference image",
        "ambientocclusion": False,
        "gui_light": "front",
        "textures": {"atlas": TEXTURE_REF, "particle": TEXTURE_REF},
        "elements": model.elements,
        "display": DISPLAY,
    }
    with open(MODEL, "w") as out:
        json.dump(data, out, indent=2)
        out.write("\n")


def write_bbmodel(model, image):
    buffer = io.BytesIO()
    image.save(buffer, "PNG")
    source = "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()
    tex_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, "apogee-tendons-texture"))
    elements = []
    for el in model.elements:
        faces = {}
        for name, face in el["faces"].items():
            # Blockbench's UVs are in texture pixels: 16 UV units span the 32 px texture.
            faces[name] = {"uv": [v * 2 for v in face["uv"]], "texture": 0}
        out = {
            "name": el["name"],
            "type": "cube",
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "from": el["from"],
            "to": el["to"],
            "autouv": 0,
            "color": 0,
            "origin": el.get("rotation", {}).get("origin", [(a + b) / 2 for a, b in zip(el["from"], el["to"])]),
            "faces": faces,
            "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "apogee-tendons-" + el["name"])),
        }
        if "rotation" in el:
            rot = [0.0, 0.0, 0.0]
            rot["xyz".index(el["rotation"]["axis"])] = el["rotation"]["angle"]
            out["rotation"] = rot
        elements.append(out)
    doc = {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": "reinforced_tendons",
        "model_identifier": "",
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 32, "height": 32},
        "elements": elements,
        "outliner": [e["uuid"] for e in elements],
        "textures": [{
            "path": "",
            "name": "reinforced_tendons_3d.png",
            "folder": "item",
            "namespace": "apogee",
            "id": "0",
            "width": 32,
            "height": 32,
            "uv_width": 32,
            "uv_height": 32,
            "particle": True,
            "use_as_default": True,
            "layers_enabled": False,
            "sync_to_project": "",
            "render_mode": "default",
            "render_sides": "auto",
            "frame_time": 1,
            "frame_order_type": "loop",
            "frame_order": "",
            "frame_interpolate": False,
            "visible": True,
            "internal": True,
            "saved": True,
            "uuid": tex_uuid,
            "source": source,
        }],
        "display": DISPLAY,
    }
    with open(BBMODEL, "w") as out:
        json.dump(doc, out, indent=2)
        out.write("\n")


if __name__ == "__main__":
    model = build()
    texture = draw_texture()
    texture.save(TEXTURE)
    write_game_model(model)
    write_bbmodel(model, texture)
    print(len(model.elements), "elements")
