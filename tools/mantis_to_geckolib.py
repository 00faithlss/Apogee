#!/usr/bin/env python3
"""One-time export of the Mantis Blade model to GeckoLib/Blockbench files.

Writes assets/apogee/geo/mantis_blades.geo.json and
assets/apogee/animations/mantis_blades.animation.json from the parts in mantis_model.py.
Once those files are edited in Blockbench they are the source of truth: do not run this again
unless you mean to overwrite them.

Coordinates: mantis_model.py works in arm-local texels with +Y towards the hand and -Z out of
the forearm's front. Bedrock/Blockbench has +Y up, so y is negated and shifted by 24 so the
shoulder sits at y=24 and the hand at y=4; x is negated (Bedrock mirrors X against Java);
z is kept (-Z is north, the forearm's front). A Java xRot of a becomes a Bedrock rotation of -a.
The model is authored at twice the size of a player arm; the renderer scales it by 0.5.
"""
import json
import math
from pathlib import Path

import mantis_model as M

ROOT = Path(__file__).resolve().parent.parent
GEO = ROOT / "src/main/resources/assets/apogee/geo/mantis_blades.geo.json"
ANIM = ROOT / "src/main/resources/assets/apogee/animations/mantis_blades.animation.json"
Y_SHIFT = 24

# Folded (bind) pose as Java xRot, in degrees; see MantisArmModel.setDeployment at progress 0.
FOLDED_JAVA_XROT = {"link_lower": -180.0, "link_upper": 0.0, "blade": 180.0}


def r(v):
    return round(v, 4)


def to_bedrock_point(x, y, z):
    return [r(-x), r(Y_SHIFT - y), r(z)]


def cube(x, y, z, w, h, d, uv, ox, oy, oz):
    """Java box (relative to its part's absolute pivot) -> Bedrock cube with an absolute origin."""
    x0, y0, z0 = ox + x, oy + y, oz + z
    return {"origin": [r(-(x0 + w)), r(Y_SHIFT - (y0 + h)), r(z0)], "size": [w, h, d], "uv": list(uv)}


def geo():
    uv = M.pack()
    absolute = {}
    bones = [{"name": "mantis", "pivot": [0, Y_SHIFT, 0]}]
    # Editing aid only: the renderer never draws this bone.
    bones.append({"name": "reference_arm", "parent": "mantis", "pivot": [0, Y_SHIFT, 0],
                  "cubes": [cube(-4, -4, -4, 8, 24, 8, (0, 0), 0, 0, 0)]})
    for name, parent, pivot, boxes in M.PARTS:
        base = absolute[parent] if parent else (0, 0, 0)
        ap = tuple(base[i] + pivot[i] for i in range(3))
        absolute[name] = ap
        bone = {"name": name, "parent": parent or "mantis", "pivot": to_bedrock_point(*ap)}
        if name in FOLDED_JAVA_XROT and FOLDED_JAVA_XROT[name]:
            bone["rotation"] = [r(-FOLDED_JAVA_XROT[name]), 0, 0]
        cubes = [cube(x, y, z, w, h, d, uv[(name, i)], *ap) for i, (_, x, y, z, w, h, d, _) in enumerate(boxes)]
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
        if name in M.SKIN_BOXES:
            # Drawn in a second pass with the wearer's skin; the UVs point at the arm's own pixels.
            x, y, z, w, h, d = M.SKIN_BOXES[name]
            bones.append({"name": "skin_" + name, "parent": name, "pivot": to_bedrock_point(*ap),
                          "cubes": [cube(x, y, z, w, h, d, M.SKIN_UV["right_arm"][name], *ap)]})
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.mantis_blades",
                "texture_width": M.SIZE,
                "texture_height": M.SIZE,
                "visible_bounds_width": 4,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": bones,
        }],
    }


# Deltas from the folded pose, in Bedrock degrees / texels, for the open pose.
OPEN = {
    "link_lower": [-50, 0, 0],   # Java -180 -> -130
    "link_upper": [22, 0, 0],    # Java 0 -> -22
    "blade": [40, 0, 0],         # Java 180 -> 140
    "cover_elbow": [35, 0, 0],   # Java 0 -> -35
    "cover_under": [-40, 0, 0],  # Java 0 -> 40
}
EDGE_OUT = [0, -14, 0]


def kf(*frames):
    return {f"{t:.2f}".rstrip("0").rstrip(".") if t else "0.0": v for t, v in frames}


def scaled(v, k):
    return [r(c * k) for c in v]


def open_pose(extra_blade=0.0, edge=None):
    """Bone keyframe values for the open pose, optionally with the blade tipped or the edge moved."""
    pose = {name: {"rotation": list(v)} for name, v in OPEN.items()}
    pose["blade"]["rotation"] = [OPEN["blade"][0] + extra_blade, 0, 0]
    pose["edge"] = {"position": list(edge or EDGE_OUT)}
    return pose


def animations():
    deploy = {
        "link_lower": {"rotation": kf((0, [0, 0, 0]), (0.22, OPEN["link_lower"]))},
        "link_upper": {"rotation": kf((0, [0, 0, 0]), (0.22, OPEN["link_upper"]))},
        "blade": {"rotation": kf((0, [0, 0, 0]), (0.1, [0, 0, 0]), (0.32, OPEN["blade"]))},
        "cover_elbow": {"rotation": kf((0, [0, 0, 0]), (0.22, OPEN["cover_elbow"]))},
        "cover_under": {"rotation": kf((0, [0, 0, 0]), (0.22, OPEN["cover_under"]))},
        "edge": {"position": kf((0, [0, 0, 0]), (0.22, [0, 0, 0]), (0.4, EDGE_OUT)),
                 "scale": kf((0, [0, 0, 0]), (0.21, [0, 0, 0]), (0.22, [1, 1, 1]))},
    }
    retract = {
        "edge": {"position": kf((0, EDGE_OUT), (0.18, [0, 0, 0])),
                 "scale": kf((0, [1, 1, 1]), (0.18, [1, 1, 1]), (0.19, [0, 0, 0]))},
        "blade": {"rotation": kf((0, OPEN["blade"]), (0.08, OPEN["blade"]), (0.3, [0, 0, 0]))},
        "link_lower": {"rotation": kf((0, OPEN["link_lower"]), (0.18, OPEN["link_lower"]), (0.4, [0, 0, 0]))},
        "link_upper": {"rotation": kf((0, OPEN["link_upper"]), (0.18, OPEN["link_upper"]), (0.4, [0, 0, 0]))},
        "cover_elbow": {"rotation": kf((0, OPEN["cover_elbow"]), (0.18, OPEN["cover_elbow"]), (0.4, [0, 0, 0]))},
        "cover_under": {"rotation": kf((0, OPEN["cover_under"]), (0.18, OPEN["cover_under"]), (0.4, [0, 0, 0]))},
    }
    held = {name: {prop: {"0.0": v} for prop, v in props.items()} for name, props in open_pose().items()}
    # Slash: the blade cocks back, snaps through and settles (preparation, cut, return).
    slash = {name: {prop: {"0.0": v} for prop, v in props.items()} for name, props in open_pose().items()}
    slash["blade"]["rotation"] = kf((0, OPEN["blade"]), (0.06, [OPEN["blade"][0] + 14, 0, 0]),
                                    (0.14, [OPEN["blade"][0] - 16, 0, 0]), (0.3, OPEN["blade"]))
    # Thrust: the blade levels out and the edge punches further out of the housing.
    thrust = {name: {prop: {"0.0": v} for prop, v in props.items()} for name, props in open_pose().items()}
    thrust["blade"]["rotation"] = kf((0, OPEN["blade"]), (0.08, [OPEN["blade"][0] - 10, 0, 0]), (0.4, OPEN["blade"]))
    thrust["edge"]["position"] = kf((0, EDGE_OUT), (0.08, [0, -19, 0]), (0.4, EDGE_OUT))
    return {
        "format_version": "1.8.0",
        "animations": {
            "animation.mantis_blades.deploy": {"loop": "hold_on_last_frame", "animation_length": 0.4, "bones": deploy},
            "animation.mantis_blades.deployed": {"loop": True, "animation_length": 0.05, "bones": held},
            "animation.mantis_blades.slash": {"animation_length": 0.3, "bones": slash},
            "animation.mantis_blades.thrust": {"animation_length": 0.4, "bones": thrust},
            "animation.mantis_blades.retract": {"loop": "hold_on_last_frame", "animation_length": 0.4, "bones": retract},
        },
    }


def main():
    GEO.parent.mkdir(parents=True, exist_ok=True)
    ANIM.parent.mkdir(parents=True, exist_ok=True)
    GEO.write_text(json.dumps(geo(), indent=2) + "\n")
    ANIM.write_text(json.dumps(animations(), indent=2) + "\n")


if __name__ == "__main__":
    main()
