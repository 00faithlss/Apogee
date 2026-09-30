#!/usr/bin/env python3
"""Strips the baked wire out of the nanowire pack's per-arm exports.

The pack bakes each wire as 64 rigid segments keyed at 60 fps (about 8 MB of animation per arm).
Apogee draws the wire itself, as a continuous curve computed every frame, so the exports only
keep the housing: shutter, service cover and spool, with their animation tracks.

Usage: python3 tools/nanowire_strip_wire.py <pack>/src/main/resources/assets/apogee
Writes into src/main/resources/assets/apogee of this repository.
"""
import json
import sys
from pathlib import Path

source = Path(sys.argv[1])
target = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/apogee'

for side in ('left', 'right'):
    name = f'nanowire_{side}_attachment'
    geo = json.loads((source / 'geo' / f'{name}.geo.json').read_text())
    for model in geo['minecraft:geometry']:
        model['bones'] = [bone for bone in model['bones'] if not bone['name'].startswith('wire_')]
    (target / 'geo' / f'{name}.geo.json').write_text(json.dumps(geo, indent=1))

    animations = json.loads((source / 'animations' / f'{name}.animation.json').read_text())
    for clip in animations['animations'].values():
        clip['bones'] = {bone: tracks for bone, tracks in clip['bones'].items() if not bone.startswith('wire_')}
    (target / 'animations' / f'{name}.animation.json').write_text(json.dumps(animations, separators=(',', ':')))
    print(name, 'bones kept:', [b['name'] for b in geo['minecraft:geometry'][0]['bones']])
