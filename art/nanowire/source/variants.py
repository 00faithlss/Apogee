"""Build the white material palette and its editable GeckoLib/Blockbench projects.

The two implants share geometry, UVs and animation clips. The 16 solid material
swatches match the original red model; only the wire and illuminated accents differ.
"""
from pathlib import Path
import base64
import copy
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/apogee'
PALETTE = [
    (6, 8, 11), (18, 22, 28), (47, 54, 62), (77, 85, 93),
    (0, 0, 0), (30, 33, 39), (255, 255, 255), (173, 191, 203),
    (183, 142, 118), (30, 50, 65), (26, 28, 31), (194, 181, 155),
    (34, 94, 97), (83, 36, 43), (144, 153, 157), (13, 29, 38),
]


def generate():
    texture = Image.new('RGBA', (64, 64))
    glow = Image.new('RGBA', (64, 64))
    for i, rgb in enumerate(PALETTE):
        x, y = i % 4 * 16, i // 4 * 16
        ImageDraw.Draw(texture).rectangle((x, y, x + 15, y + 15), fill=(*rgb, 255))
        if i == 6:
            ImageDraw.Draw(glow).rectangle((x, y, x + 15, y + 15), fill=(*rgb, 255))
    target = ASSETS / 'textures/nanowire'
    texture.save(target / 'nanowire_white.png')
    glow.save(target / 'nanowire_white_glowmask.png')

    # A small wound/drop glyph for the normal Minecraft status-effect HUD.
    icon = Image.new('RGBA', (18, 18))
    draw = ImageDraw.Draw(icon)
    drop = [(8, 1), (10, 5), (13, 9), (13, 13), (11, 16), (6, 16), (4, 13), (4, 10), (6, 6)]
    draw.polygon(drop, fill='#5D1120')
    draw.polygon([(8, 3), (10, 7), (12, 10), (12, 13), (10, 15), (7, 15), (5, 12), (6, 9)], fill='#CA253C')
    draw.line([(8, 7), (6, 11), (6, 12)], fill='#FF7881', width=1)
    effect = ASSETS / 'textures/mob_effect'
    effect.mkdir(exist_ok=True)
    icon.save(effect / 'bleeding.png')

    source = 'data:image/png;base64,' + base64.b64encode((target / 'nanowire_white.png').read_bytes()).decode()
    for side in ('left', 'right'):
        project_dir = ROOT / 'art/nanowire/blockbench'
        project = copy.deepcopy(json.loads((project_dir / f'nanowire_{side}_attachment.bbmodel').read_text()))
        project['name'] = f'nanowire_white_{side}_attachment'
        project['textures'][0].update(name='nanowire_white.png', source=source)
        (project_dir / f'nanowire_white_{side}_attachment.bbmodel').write_text(json.dumps(project, separators=(',', ':')) + '\n')


if __name__ == '__main__':
    generate()
