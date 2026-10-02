"""Generates the Berserk's art: inventory icons for the three variants and their six parts, the
implant's Blockbench project (art/berserk/berserk.bbmodel) and its GeckoLib export (geometry,
animation, one texture per variant and their glow masks).

Run from the repository root: python3 tools/berserk_assets.py
Needs Pillow. After editing the model in Blockbench, export it with the GeckoLib plugin over the
files this script writes, and stop running the script for the parts you changed by hand.
"""
import base64
import io
import json
import os
import uuid

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/apogee")
ART = os.path.join(ROOT, "art/berserk")

VARIANTS = {
    # metal, highlight, shadow, light, light core, outline
    "zetatech": ("#5b6270", "#8f98a8", "#383d48", "#ff2a3a", "#ffb3b8", "#16181d"),
    "biodyne": ("#d4d8de", "#ffffff", "#9ba1ab", "#ff2a3a", "#ffc2c6", "#2a2d33"),
    "militech": ("#4c5239", "#727a55", "#30331f", "#ff7417", "#ffd09a", "#15160e"),
}


def rgba(hex_color, alpha=255):
    hex_color = hex_color.lstrip("#")
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def grid_image(rows, palette):
    image = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == image.width, (row, len(row))
        for x, key in enumerate(row):
            if key != ".":
                image.putpixel((x, y), rgba(palette[key]))
    return image


def save(image, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path)


# ---------------------------------------------------------------------------------- item icons

BERSERK_ICON = [
    "......kkkk......",
    ".....khhhhk.....",
    "...kkkmllmkkk...",
    "..khhhhhhhhhhk..",
    "..kmmmmmmmmmmk..",
    "...kkkdmmdkkk...",
    ".....khhhhk.....",
    "...kkkmllmkkk...",
    "..khhhhhhhhhhk..",
    "..kmmmmmmmmmmk..",
    "...kkkdmmdkkk...",
    ".....khhhhk.....",
    "...kkkmllmkkk...",
    "..khhhhhhhhhhk..",
    "..kddddddddddk..",
    "...kkkkkkkkkk...",
]

COMPONENTS = {
    "subdermal_armor": ([
        "................",
        "...kkkkkkkkkk...",
        "..khhhkhhhkhhk..",
        "..khmmkhmmkhmk..",
        "..kkkkkkkkkkkk..",
        "..kmhhhkhhhkmk..",
        "..kmhmmkhmmkmk..",
        "..kkkkkkkkkkkk..",
        "..khhhkhhhkhhk..",
        "..khmmkhmmkhmk..",
        "...kkkkkkkkkk...",
        "....kmhhhhmk....",
        ".....kmmmmk.....",
        "......kddk......",
        ".......kk.......",
        "................",
    ], {"k": "#1b2028", "h": "#9fb3c8", "m": "#5d7088", "d": "#3a4656"}),
    "adrenaline_booster": ([
        "..........kk....",
        ".........kggk...",
        "........kgggk...",
        ".......kkgkk....",
        "......kwwkk.....",
        ".....kwyywk.....",
        "....kwyyowk.....",
        "...kwyyoowk.....",
        "..kwyyoowk......",
        "..kwyoowk.......",
        "..kwoowk........",
        "..kkwwk.........",
        ".kmkkk..........",
        "kmk.............",
        "kk..............",
        "................",
    ], {"k": "#1d1a16", "g": "#8a929c", "w": "#e8f4f8", "y": "#ffd23a", "o": "#ff7a1a", "m": "#b9c0c8"}),
    "biomonitor": ([
        "................",
        "..kkkkkkkkkkkk..",
        ".kmmmmmmmmmmmmk.",
        ".kmssssssssssmk.",
        ".kmssssssrsssmk.",
        ".kmssssssrrssmk.",
        ".kmssrssrsrssmk.",
        ".kmgggrgrssgggk.",
        ".kmssssrssssssk.",
        ".kmssssssssssmk.",
        ".kmmmmmmmmmmmmk.",
        "..kkkkkkkkkkkk..",
        "....kmllllmk....",
        "....kkkkkkkk....",
        "................",
        "................",
    ], {"k": "#15181c", "m": "#6c7480", "s": "#0f2a24", "g": "#3dff8f", "r": "#ff3a4a", "l": "#9aa3ad"}),
    "fortified_ankles": ([
        "......kkkk......",
        ".....kmmmmk.....",
        "......kkkk......",
        "......krwk......",
        "......kwrk......",
        "......krwk......",
        ".....kkwrkk.....",
        "....krwkkrwk....",
        "....kwrk.kwrk...",
        "...krwk...krwk..",
        "...kwrk...kwrk..",
        "..krwk.....krk..",
        "..kmmk.....kmmk.",
        ".kmmmmk...kmmmmk",
        ".kkkkkk...kkkkkk",
        "................",
    ], {"k": "#1c1416", "m": "#8f98a4", "r": "#c21d32", "w": "#e8dccd"}),
    "bionic_joints": ([
        "................",
        "....kk....kk....",
        "...kbbk..kbbk...",
        "...kbbbkkbbbk...",
        "....kbbbbbbk....",
        ".....kbbbbk.....",
        "......kmmk......",
        ".....kmhhmk.....",
        ".....kmhcmk.....",
        "......kmmk......",
        ".....kbbbbk.....",
        "....kbbbbbbk....",
        "...kbbbkkbbbk...",
        "...kbbk..kbbk...",
        "....kk....kk....",
        "................",
    ], {"k": "#211d18", "b": "#e9e2cf", "m": "#6d7684", "h": "#b9c3cf", "c": "#2ad4f0"}),
    "synaptic_accelerator": ([
        "................",
        "...l.l.l.l.l....",
        "..kkkkkkkkkkk...",
        "l.kddddddddk.l..",
        "..kdppdddcdk....",
        "l.kdpcpdcddk.l..",
        "..kddpcpcddk....",
        "l.kdddcpcpdk.l..",
        "..kddcdpdcpk....",
        "l.kdcddddpdk.l..",
        "..kddddddddk....",
        "..kkkkkkkkkkk...",
        "...l.l.l.l.l....",
        "................",
        "................",
        "................",
    ], {"k": "#16121f", "d": "#2b2340", "p": "#b04cff", "c": "#33e6ff", "l": "#c8a75a"}),
}


def berserk_icon(variant):
    metal, high, shadow, light, core, outline = VARIANTS[variant]
    return grid_image(BERSERK_ICON, {"k": outline, "h": high, "m": metal, "d": shadow, "l": light})


# ---------------------------------------------------------------------------------- implant model
# A 32x32 atlas of 8x8 cells. Cells: (0,0) metal, (1,0) highlight, (2,0) shadow, (3,0) light,
# (0,1) dark core.
CELL = 8
TEXTURE = 32
CELLS = {"metal": (0, 0), "high": (1, 0), "shadow": (2, 0), "light": (3, 0), "core": (0, 1)}


def implant_texture(variant):
    metal, high, shadow, light, core, outline = VARIANTS[variant]
    image = Image.new("RGBA", (TEXTURE, TEXTURE), (0, 0, 0, 0))
    fills = {"metal": metal, "high": high, "shadow": shadow, "light": light, "core": outline}
    for name, (cx, cy) in CELLS.items():
        base = rgba(fills[name])
        for y in range(CELL):
            for x in range(CELL):
                color = base
                edge = x in (0, CELL - 1) or y in (0, CELL - 1)
                if name == "light":
                    color = rgba(core) if 2 <= x <= 5 and 2 <= y <= 5 else base
                elif edge:
                    color = tuple(max(0, c - 28) for c in base[:3]) + (255,)
                elif (x * 7 + y * 3) % 11 == 0:
                    color = tuple(min(255, c + 14) for c in base[:3]) + (255,)
                image.putpixel((cx * CELL + x, cy * CELL + y), color)
    return image


def glowmask(variant):
    texture = implant_texture(variant)
    mask = Image.new("RGBA", texture.size, (0, 0, 0, 0))
    cx, cy = CELLS["light"]
    for y in range(CELL):
        for x in range(CELL):
            mask.putpixel((cx * CELL + x, cy * CELL + y), texture.getpixel((cx * CELL + x, cy * CELL + y)))
    return mask


# Cubes in Blockbench units (pixels). The model sits on the back of the player's body: the body
# spans x -4..4, y 12..24 (feet at 0) and z -2..2, and the back faces +z.
def cubes():
    parts = []

    def cube(name, frm, to, cell, bone):
        parts.append((name, frm, to, cell, bone))

    cube("neck_port", (-1.5, 22.0, 1.6), (1.5, 24.2, 3.2), "shadow", "spine")
    cube("neck_light", (-0.5, 22.6, 3.2), (0.5, 23.6, 3.45), "light", "spine")
    cube("spine_core", (-0.75, 12.6, 1.8), (0.75, 22.0, 2.9), "core", "spine")
    for index, top in enumerate((21.6, 19.4, 17.2, 15.0)):
        bone = "vertebra_%d" % index
        width = 2.6 - index * 0.15
        cube(bone + "_plate", (-width, top - 1.3, 2.0), (width, top, 3.5), "metal", bone)
        cube(bone + "_ridge", (-width + 0.4, top - 0.35, 3.5), (width - 0.4, top, 3.75), "high", bone)
        cube(bone + "_light_l", (-width + 0.5, top - 1.0, 3.5), (-width + 1.3, top - 0.55, 3.7), "light", bone)
        cube(bone + "_light_r", (width - 1.3, top - 1.0, 3.5), (width - 0.5, top - 0.55, 3.7), "light", bone)
    for side in (-1, 1):
        x0, x1 = (2.7, 3.7) if side > 0 else (-3.7, -2.7)
        cube("heatsink_%s" % ("r" if side > 0 else "l"), (x0, 18.6, 2.0), (x1, 22.4, 2.7), "shadow", "spine")
        cube("heatsink_fin_%s" % ("r" if side > 0 else "l"), (x0 + 0.2, 19.0, 2.7), (x1 - 0.2, 22.0, 2.95), "high", "spine")
    cube("lumbar_box", (-2.2, 12.2, 2.0), (2.2, 13.8, 3.6), "shadow", "spine")
    cube("lumbar_light", (-1.2, 12.7, 3.6), (1.2, 13.2, 3.8), "light", "spine")
    return parts


def uv_for(cell):
    cx, cy = CELLS[cell]
    return [cx * CELL + 1, cy * CELL + 1, cx * CELL + CELL - 1, cy * CELL + CELL - 1]


def geo_json():
    bones = {"spine": {"name": "spine", "pivot": [0, 24, 0], "cubes": []}}
    order = ["spine"]
    for name, frm, to, cell, bone in cubes():
        if bone not in bones:
            top = frm[1] + (to[1] - frm[1])
            bones[bone] = {"name": bone, "parent": "spine", "pivot": [0, round(top, 3), 2.0], "cubes": []}
            order.append(bone)
        u0, v0, u1, v1 = uv_for(cell)
        face = {"uv": [u0, v0], "uv_size": [u1 - u0, v1 - v0]}
        bones[bone]["cubes"].append({
            # Bedrock geometry mirrors x.
            "origin": [round(-to[0], 4), round(frm[1], 4), round(frm[2], 4)],
            "size": [round(to[0] - frm[0], 4), round(to[1] - frm[1], 4), round(to[2] - frm[2], 4)],
            "uv": {side: dict(face) for side in ("north", "east", "south", "west", "up", "down")},
        })
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.berserk",
                "texture_width": TEXTURE,
                "texture_height": TEXTURE,
                "visible_bounds_width": 2,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": [bones[name] for name in order],
        }],
    }


def animation_json():
    # The plates breathe out while the Berserk runs; the renderer plays "active" then.
    bones = {}
    for index in range(4):
        delay = index * 0.08
        bones["vertebra_%d" % index] = {"position": {
            "0.0": [0, 0, 0],
            str(round(0.2 + delay, 2)): [0, 0, 0.35],
            str(round(0.5 + delay, 2)): [0, 0, 0],
            "0.9": [0, 0, 0],
        }}
    return {"format_version": "1.8.0", "animations": {"active": {"loop": True, "animation_length": 0.9, "bones": bones}}}


def stable_uuid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "apogee/berserk/" + name))


def png_data(image):
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode()


def bbmodel():
    elements = []
    groups = {}
    for name, frm, to, cell, bone in cubes():
        u0, v0, u1, v1 = uv_for(cell)
        element_id = stable_uuid(name)
        elements.append({
            "name": name, "type": "cube", "uuid": element_id,
            "from": list(frm), "to": list(to), "origin": [0, 0, 0], "rotation": [0, 0, 0],
            "box_uv": False, "autouv": 0, "color": 0, "visibility": True, "export": True,
            "faces": {side: {"uv": [u0, v0, u1, v1], "texture": 0}
                      for side in ("north", "east", "south", "west", "up", "down")},
        })
        groups.setdefault(bone, []).append(element_id)
    spine_children = list(groups["spine"])
    for bone, children in groups.items():
        if bone == "spine":
            continue
        top = max(to[1] for name, frm, to, cell, b in cubes() if b == bone)
        spine_children.append({"name": bone, "uuid": stable_uuid("group/" + bone), "origin": [0, top, 2.0],
                               "rotation": [0, 0, 0], "export": True, "isOpen": True, "visibility": True,
                               "children": children})
    outliner = [{"name": "spine", "uuid": stable_uuid("group/spine"), "origin": [0, 24, 0], "rotation": [0, 0, 0],
                 "export": True, "isOpen": True, "visibility": True, "children": spine_children}]
    textures = []
    for index, variant in enumerate(VARIANTS):
        textures.append({"name": "berserk_%s.png" % variant, "uuid": stable_uuid("texture/" + variant), "id": str(index),
                         "mode": "bitmap", "internal": True, "uv_width": TEXTURE, "uv_height": TEXTURE,
                         "width": TEXTURE, "height": TEXTURE, "source": png_data(implant_texture(variant))})
    return {
        "meta": {"format_version": "4.10", "model_format": "animated_entity_model", "box_uv": False},
        "name": "berserk", "model_identifier": "berserk",
        "resolution": {"width": TEXTURE, "height": TEXTURE},
        "elements": elements, "outliner": outliner, "textures": textures, "animations": [],
    }


def main():
    for variant in VARIANTS:
        save(berserk_icon(variant), os.path.join(ASSETS, "textures/item/berserk_%s.png" % variant))
        save(implant_texture(variant), os.path.join(ASSETS, "textures/berserk/berserk_%s.png" % variant))
        save(glowmask(variant), os.path.join(ASSETS, "textures/berserk/berserk_%s_glowmask.png" % variant))
    for name, (rows, palette) in COMPONENTS.items():
        save(grid_image(rows, palette), os.path.join(ASSETS, "textures/item/%s.png" % name))
    os.makedirs(os.path.join(ASSETS, "geo"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "animations"), exist_ok=True)
    os.makedirs(ART, exist_ok=True)
    with open(os.path.join(ASSETS, "geo/berserk.geo.json"), "w") as out:
        json.dump(geo_json(), out, indent=2)
    with open(os.path.join(ASSETS, "animations/berserk.animation.json"), "w") as out:
        json.dump(animation_json(), out, indent=2)
    with open(os.path.join(ART, "berserk.bbmodel"), "w") as out:
        json.dump(bbmodel(), out)


if __name__ == "__main__":
    main()
