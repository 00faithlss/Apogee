"""Copia la exportación de Blockbench y crea los iconos originales de las cuatro piezas."""
from pathlib import Path
from PIL import Image, ImageDraw
import json, shutil

root=Path(__file__).resolve().parents[1]
assets=root.parents[1]/'src/main/resources/assets/apogee'
shutil.copy2(root/'export/trauma_team.geo.json',assets/'geo/trauma_team.geo.json')
shutil.copy2(root/'export/trauma_team.animation.json',assets/'animations/trauma_team.animation.json')
for texture in (root/'textures').glob('*.png'):shutil.copy2(texture,assets/'textures/armor'/texture.name)
for piece in ('helmet','chestplate','leggings','boots'):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    green=(23,104,93);white=(217,226,220);black=(29,39,43);red=(190,38,44)
    if piece=='helmet':
        d.polygon([(7,5),(11,2),(21,2),(26,6),(28,23),(23,29),(9,29),(4,23)],fill=black)
        d.polygon([(7,5),(12,2),(21,2),(26,6),(25,18),(21,21),(10,21),(6,18)],fill=white)
        for a,b in [((16,7),(16,16)),((12,9),(20,14)),((12,14),(20,9))]:d.line((*a,*b),fill=red,width=2)
        for x in (10,22):
            for y in (23,27):d.ellipse((x-2,y-2,x+2,y+2),fill=white)
    elif piece=='chestplate':
        d.polygon([(8,5),(12,4),(14,7),(18,7),(20,4),(24,5),(30,10),(28,26),(22,25),(22,29),(10,29),(10,25),(4,26),(2,10)],fill=green,outline=black)
        d.rectangle((9,8,23,17),fill=white);d.line((12,12,20,12),fill=red,width=2)
        for x in (9,14,19):d.rectangle((x,18,x+4,24),fill=white,outline=black)
        d.rectangle((3,19,7,26),fill=red);d.rectangle((25,19,29,26),fill=red)
    elif piece=='leggings':
        d.polygon([(7,3),(25,3),(26,28),(18,29),(17,13),(15,13),(14,29),(6,28)],fill=green,outline=black)
        d.rectangle((7,4,25,7),fill=black)
        for x in (7,19):d.rectangle((x,19,x+6,24),fill=black,outline=white)
    else:
        for x in (4,18):
            d.rectangle((x+1,8,x+8,24),fill=green,outline=black)
            d.rectangle((x,23,x+10,28),fill=red,outline=black)
    im.save(assets/f'textures/item/trauma_team_{piece}.png')
    (assets/f'models/item/trauma_team_{piece}.json').write_text(json.dumps(dict(parent='minecraft:item/generated',textures=dict(layer0=f'apogee:item/trauma_team_{piece}')),indent=2)+'\n',encoding='utf-8')
print('Geometría, animaciones, texturas e iconos Trauma Team sincronizados.')
