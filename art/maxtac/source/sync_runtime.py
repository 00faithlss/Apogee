"""Copia la exportación revisada y genera los iconos originales de las cuatro ranuras."""
from pathlib import Path
from PIL import Image, ImageDraw
import json
import shutil

root=Path(__file__).resolve().parents[1]
assets=root.parents[1]/'src/main/resources/assets/apogee'
(assets/'geo').mkdir(parents=True,exist_ok=True)
(assets/'textures/armor').mkdir(parents=True,exist_ok=True)
shutil.copy2(root/'export/maxtac.geo.json',assets/'geo/maxtac.geo.json')
for texture in (root/'textures').glob('*.png'):
    shutil.copy2(texture,assets/'textures/armor'/texture.name)
for piece in ['helmet','chestplate','leggings','boots']:
    im=Image.new('RGBA',(32,32),(0,0,0,0));d=ImageDraw.Draw(im)
    black=(32,35,38);edge=(64,70,76);navy=(28,42,61);purple=(205,148,255);green=(52,225,166)
    if piece=='helmet':
        d.polygon([(6,6),(10,3),(22,3),(26,6),(27,24),(21,28),(11,28),(5,24)],fill=black,outline=edge)
        d.rectangle((14,7,17,21),fill=(45,49,53))
        for x,y in [(9,12),(12,17),(9,22),(23,12),(20,17),(23,22)]:
            d.ellipse((x-3,y-3,x+2,y+2),fill=(11,30,28),outline=edge)
            d.ellipse((x-2,y-2,x+1,y+1),outline=green)
    elif piece=='chestplate':
        d.polygon([(8,5),(12,4),(14,8),(18,8),(20,4),(24,5),(30,10),(27,25),(22,24),(22,29),(10,29),(10,24),(5,25),(2,10)],fill=navy,outline=edge)
        d.rectangle((10,9,22,20),fill=black,outline=edge)
        d.line((4,13,8,13,8,17,6,17,6,24),fill=purple,width=2)
        d.line((28,13,24,13,24,17,26,17,26,24),fill=purple,width=2)
    elif piece=='leggings':
        d.polygon([(7,3),(25,3),(26,27),(18,29),(17,13),(15,13),(14,29),(6,27)],fill=navy,outline=edge)
        d.rectangle((7,4,25,7),fill=black)
        for x in (7,19):d.rectangle((x,19,x+6,24),fill=black,outline=edge)
    else:
        for x in (4,18):
            d.polygon([(x+1,8),(x+8,8),(x+8,22),(x+10,23),(x+10,28),(x,28),(x,22)],fill=black,outline=edge)
            d.line((x+2,15,x+7,15),fill=edge,width=2)
    im.save(assets/f'textures/item/maxtac_{piece}.png')
    (assets/f'models/item/maxtac_{piece}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'apogee:item/maxtac_{piece}'}},indent=2)+'\n',encoding='utf-8')
print('Recursos MaxTac sincronizados con la exportación de Blockbench.')
