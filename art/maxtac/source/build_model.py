"""Prepara la geometría y el atlas original para la edición final en Blockbench."""
from pathlib import Path
from PIL import Image, ImageDraw
import base64
import copy
import io
import json
import math
import random
import uuid

ROOT = Path(__file__).resolve().parents[1]
SIZE, TILE = 256, 64
NAMESPACE = uuid.UUID('42c9daba-e930-4ac1-86bf-9ba3395318d5')

def uid(name):
    return str(uuid.uuid5(NAMESPACE, name))

materials = [
    ('tejido', (23, 34, 52)), ('placa', (37, 50, 63)),
    ('canto', (69, 85, 97)), ('goma', (12, 18, 25)),
    ('metal', (87, 102, 109)), ('violeta', (188, 120, 255)),
    ('lente', (12, 68, 60)), ('ncpd', (37, 50, 63)),
    ('maxtac', (37, 50, 63)), ('casco', (32, 35, 38)),
    ('hebilla', (101, 111, 109)), ('lente_auxiliar', (12, 68, 60)),
    ('panel', (29, 40, 52)), ('azul', (30, 53, 79)),
    ('suelas', (15, 21, 29)), ('insignia', (33, 47, 63)),
]
atlas = Image.new('RGBA', (SIZE, SIZE))
rng = random.Random(2077)
glyphs = {
 'N':['10001','11001','10101','10011','10001'],
 'C':['01111','10000','10000','10000','01111'],
 'P':['11110','10001','11110','10000','10000'],
 'D':['11110','10001','10001','10001','11110'],
 'M':['10001','11011','10101','10001','10001'],
 'A':['01110','10001','11111','10001','10001'],
 'X':['10001','01010','00100','01010','10001'],
 'T':['11111','00100','00100','00100','00100'],
}

def lettering(draw, word, y, color, scale=2):
    x = (TILE - (len(word)*6-1)*scale)//2
    for char in word:
        for row, bits in enumerate(glyphs[char]):
            for col, bit in enumerate(bits):
                if bit == '1':
                    draw.rectangle((x+col*scale,y+row*scale,x+(col+1)*scale-1,y+(row+1)*scale-1),fill=color)
        x += 6*scale

for index, (name, base) in enumerate(materials):
    tile = Image.new('RGBA', (TILE,TILE), (*base,255))
    d = ImageDraw.Draw(tile)
    for y in range(TILE):
        for x in range(TILE):
            noise = rng.choice((-2,-1,0,0,1,2))
            weave = 3 if name in ('tejido','ncpd','maxtac','panel') and (x+y)%6<2 else 0
            tile.putpixel((x,y),tuple(max(0,min(255,c+noise+weave)) for c in base)+(255,))
    if name in ('placa','panel','azul','casco'):
        d.line((4,58,4,9,10,4,54,4), fill=tuple(c+12 for c in base),width=1)
        d.line((9,60,59,60,59,10),fill=tuple(max(0,c-8) for c in base),width=2)
    if name == 'violeta':
        for y in range(3,64,6):
            d.line((2,y,61,y),fill=(219,178,255),width=1)
        d.line((2,2,61,2),fill=(243,221,255),width=2)
    if name in ('lente','lente_auxiliar'):
        tile = Image.new('RGBA',(64,64),(0,0,0,0)); d=ImageDraw.Draw(tile)
        d.ellipse((2,2,61,61),fill=(8,24,27),outline=(42,58,57),width=3)
        d.ellipse((7,7,56,56),fill=(6,45,42),outline=(47,165,137),width=3)
        d.ellipse((12,12,51,51),outline=(16,104,82),width=2)
        for j in range(32):
            x,y=rng.randrange(16,49),rng.randrange(16,49)
            d.line((x,y,x+rng.randrange(1,7),y),fill=rng.choice([(31,201,150),(65,234,181),(16,125,99)]),width=1)
        d.ellipse((22,22,42,42),fill=(24,126,104),outline=(67,221,165),width=2)
        d.rectangle((27,24,32,29),fill=(183,255,220))
        d.rectangle((35,38,38,42),fill=(91,244,190))
    if name == 'ncpd':
        lettering(d,'NCPD',22,(185,199,193))
        d.line((8,16,55,16),fill=(73,94,106),width=2)
        d.line((8,40,55,40),fill=(21,32,42),width=2)
    if name == 'maxtac':
        lettering(d,'MAXTAC',21,(172,187,185),scale=1)
        d.line((12,34,51,34),fill=(69,85,97),width=1)
    if name == 'hebilla':
        d.rectangle((9,10,54,53),outline=(142,155,151),width=5)
        d.rectangle((19,20,44,43),fill=(15,25,34))
        d.line((32,19,32,45),fill=(108,120,119),width=4)
    if name == 'aviso':
        d.polygon([(32,7),(58,53),(6,53)],fill=(27,32,34))
        d.rectangle((30,22,34,37),fill=(210,173,78))
        d.rectangle((30,42,34,46),fill=(210,173,78))
    if name == 'suelas':
        for y in range(5,62,10): d.line((0,y,63,y),fill=(34,44,52),width=3)
    if name == 'insignia':
        d.rectangle((5,6,58,57),outline=(100,119,130),width=2)
        d.rectangle((10,13,26,29),fill=(133,153,150))
        for y in (16,22,36,43,48):d.line((32 if y<30 else 11,y,53,y),fill=(141,162,166),width=2)
    atlas.paste(tile,((index%4)*64,(index//4)*64))

def uv(mat):
    i=[m[0] for m in materials].index(mat)
    x,y=(i%4)*64,(i//4)*64
    return [x+0.5,y+0.5,x+63.5,y+63.5]

groups=[]
elements=[]
by_name={}

def group(name, pivot, parent=None):
    g=dict(name=name,uuid=uid(name),origin=pivot,rotation=[0,0,0],export=True,isOpen=False,visibility=True,children=[])
    (by_name[parent]['children'] if parent else groups).append(g)
    by_name[name]=g
    return name

for biped, armor, pivot in [
 ('bipedHead','armorHead',[0,24,0]), ('bipedBody','armorBody',[0,24,0]),
 ('bipedRightArm','armorRightArm',[5,22,0]), ('bipedLeftArm','armorLeftArm',[-5,22,0]),
 ('bipedRightLeg','armorRightLeg',[2,12,0]), ('bipedLeftLeg','armorLeftLeg',[-2,12,0])]:
    group(biped,pivot);group(armor,pivot,biped)
    if 'Leg' in armor:group(armor.replace('Leg','Boot'),pivot,biped)

def cube(name, bone, a, b, mat='placa', rotation=None, pivot=None, front=None):
    faces={f:dict(uv=uv(front if f=='north' and front else mat),texture=0) for f in ['north','east','south','west','up','down']}
    el=dict(name=name,type='cube',uuid=uid('cube:'+name),from_=a,to=b,origin=pivot or by_name[bone]['origin'],rotation=rotation or [0,0,0],box_uv=False,autouv=0,color=0,visibility=True,export=True,faces=faces)
    el['from']=el.pop('from_');elements.append(el);by_name[bone]['children'].append(el['uuid'])
    return el

head=group('casco_carcasa',[0,24,0],'armorHead')
cube('casco_principal',head,[-4.6,26,-4.6],[4.6,32.55,4.6],'azul')
cube('corona_escalonada',head,[-3.9,32.55,-4],[3.9,32.95,4],'azul')
cube('placa_corona',head,[-3.15,32.95,-3.2],[3.15,33.12,3.2],'placa')
cube('frente_superior',head,[-3.8,29.3,-4.82],[3.8,31.85,-4.55],'placa',[-9,0,0],[0,30.5,-4.55])
cube('cresta_central',head,[-0.52,29.3,-4.92],[0.52,32.55,-4.72],'casco',[-4,0,0],[0,30.7,-4.72])
cube('nuca_superior',head,[-4.2,28.5,4.6],[4.2,30.75,4.9],'placa')
cube('nuca_inferior',head,[-4.3,25.7,4.5],[4.3,28.25,4.95],'azul')
cube('nuca_placa_central',head,[-1.35,26.25,4.95],[1.35,29.5,5.15],'placa')
cube('nuca_identificador',head,[-0.8,27.4,5.16],[0.8,27.85,5.24],'casco')
cube('nuca_cierre_inferior',head,[-4.6,24.25,2.55],[4.6,26.15,4.8],'casco')
for s,label in [(-1,'izq'),(1,'der')]:
    def pair(name,a,b,mat='placa',bone=head,**kw):
        if s<0:a,b=[-b[0],a[1],a[2]],[-a[0],b[1],b[2]]
        return cube(f'{name}_{label}',bone,a,b,mat,**kw)
    pair('orejera',[4.22,25.8,-1.8],[4.75,29.2,2.55],'goma')
    pair('orejera_blindaje',[4.75,26.5,-1.2],[4.98,28.55,2.0],'placa')
    pair('cierre_nuca',[3.5,25.2,2.55],[4.22,27,4.2],'metal')
    pair('carcasa_lateral_inferior',[4.35,24.35,-1.8],[4.7,26.1,3.4],'casco')
    optics=group('opticas_'+label,[0,24,0],'armorHead')
    # Tres puentes forman el soporte triangular, sin cajas cuadradas detrás de las lentes.
    centers=[(s*3.38,29.15),(s*2.02,27.8),(s*3.38,26.45)]
    for j,(a,b) in enumerate(zip(centers,centers[1:]+centers[:1])):
        mx,my=(a[0]+b[0])/2,(a[1]+b[1])/2
        length=math.dist(a,b)
        angle=math.degrees(math.atan2(b[1]-a[1],b[0]-a[0]))
        cube(f'soporte_optico_{j}_{label}',optics,[mx-length/2,my-0.32,-4.9],[mx+length/2,my+0.32,-4.18],'casco',[0,0,angle],[mx,my,-4.5])
    for j,(x,y,r) in enumerate([(3.38,29.15,0.91),(2.02,27.8,1.03),(3.38,26.45,0.91)]):
        cx=s*x
        # El tambor tiene sección circular escalonada y un bisel de dieciséis segmentos.
        for k in range(12):
            low=-r+2*r*k/12; high=-r+2*r*(k+1)/12
            half_width=math.sqrt(max(0,r*r-max(abs(low),abs(high))**2))
            if half_width>0.01:
                cube(f'lente_{j+1}_tambor_{k}_{label}',optics,[cx-half_width,y+low,-5.28],[cx+half_width,y+high,-4.2],'casco')
        for k in range(16):
            angle=360*k/16
            half_tangent=(r-0.04)*math.tan(math.pi/16)+0.015
            cube(f'lente_{j+1}_bisel_{k}_{label}',optics,[cx+r-0.16,y-half_tangent,-5.54],[cx+r+0.025,y+half_tangent,-5.19],'casco',[0,0,angle],[cx,y,-5.35])
        glass=pair(f'lente_{j+1}_cristal',[x-r+0.11,y-r+0.11,-5.56],[x+r-0.11,y+r-0.11,-5.55],'casco',bone=optics,front='lente' if j==1 else 'lente_auxiliar')
        for face in glass['faces']:
            if face!='north':glass['faces'][face]['texture']=None
    pair('anclaje_opticas',[4.45,27.1,-4.8],[4.8,28.4,-3.2],'metal',bone=optics)
    pair('mandibula_lateral',[2.75,24.45,-3.9],[4.05,26.1,-2.1],'placa')

mask=group('protector_nasal_boca_abierta',[0,24,0],'armorHead')
# El labio y el mentón pertenecen a la skin: el casco deja libre esa zona.
cube('puente_nasal',mask,[-0.42,26.1,-4.98],[0.42,29.45,-4.78],'casco',[-6,0,0],[0,29.4,-4.78])

torso=group('chaleco',[0,24,0],'armorBody')
cube('traje_torso',torso,[-4.38,11.8,-2.38],[4.38,24.1,2.38],'tejido')
cube('coraza_frontal',torso,[-4.55,17.2,-3.07],[4.55,23.3,-2.18],'placa',front='ncpd')
cube('placa_abdominal_superior',torso,[-3.95,15.35,-2.9],[3.95,17.1,-2.18],'panel',[-5,0,0],[0,16.3,-2.18])
cube('placa_abdominal_inferior',torso,[-3.55,13.35,-2.72],[3.55,15.2,-2.18],'placa',[-7,0,0],[0,14.3,-2.18])
cube('placa_espalda',torso,[-4.15,16.3,2.18],[4.15,23.35,3.0],'panel')
back_label=cube('rotulo_espalda',torso,[-3.35,20.4,3.01],[3.35,22.35,3.14],'maxtac')
back_label['faces']['south']['uv']=[8,145,55,162]
cube('bolsa_lumbar',torso,[-2.8,13.1,2.22],[2.8,15.9,3.0],'tejido')
cube('credencial_ncpd',torso,[1.75,21.9,-3.25],[3.6,22.85,-3.08],'insignia')
cube('cuello_posterior',torso,[-4.3,23.85,1.8],[4.3,25.65,3.0],'goma')
for s,label in [(-1,'izq'),(1,'der')]:
    def pc(name,x1,y1,z1,x2,y2,z2,mat='placa',bone=torso,**kw):
        return cube(name+'_'+label,bone,[min(s*x1,s*x2),y1,z1],[max(s*x1,s*x2),y2,z2],mat,**kw)
    pc('cuello_lateral',3.25,23.6,-2.45,4.35,25.5,1.8,'panel')
    pc('tirante',2.65,22.6,-3.25,3.55,24.15,2.9,'goma')
    pc('hebilla_pectoral',2.65,22.35,-3.46,3.55,23.25,-3.25,'hebilla')
    pc('bolsa_costado',4.18,13.65,-1.95,4.85,16.8,1.6,'tejido')
    pc('cierre_costado',4.85,14.4,-0.65,4.98,15.45,0.35,'metal')
    arm='armorRightArm' if s>0 else 'armorLeftArm'
    sleeves=group('manga_'+label,by_name[arm]['origin'],arm)
    pc('manga',3.65,11.7,-2.4,8.35,24.3,2.4,'tejido',bone=sleeves)
    pc('hombrera',4.35,21.8,-2.5,8.65,24.35,2.5,'placa',bone=sleeves)
    pc('hombrera_tapa',4.85,24.35,-1.9,8.15,24.62,1.9,'canto',bone=sleeves)
    pc('hombrera_faldon',8.2,20.8,-2.0,8.75,23.5,2.0,'azul',bone=sleeves)
    pc('parche_brazo',8.16,19.4,-1.25,8.24,21.1,1.25,'maxtac',bone=sleeves)
    pc('codo',4.55,16.0,2.13,7.75,18.1,2.7,'goma',bone=sleeves)
    pc('guantelete',4.35,12.45,-2.52,7.95,16,-2.13,'panel',bone=sleeves)
    pc('guantelete_placa',4.8,13,-2.75,7.5,15.5,-2.52,'placa',bone=sleeves)
    pc('banda_alta_frontal',4.5,19.2,-2.5,8.36,19.85,-2.41,'violeta',bone=sleeves)
    pc('banda_alta_exterior',8.36,19.2,-2.5,8.5,19.85,1.9,'violeta',bone=sleeves)
    pc('banda_descenso_frontal',7.6,17.85,-2.5,8.36,19.2,-2.41,'violeta',bone=sleeves)
    pc('banda_descenso_exterior',8.36,17.2,-2.5,8.5,19.2,-1.55,'violeta',bone=sleeves)
    pc('banda_codo',5.45,17.2,-2.5,8.36,17.85,-2.41,'violeta',bone=sleeves)
    pc('banda_antebrazo_superior',5.45,16.1,-2.5,6.1,17.2,-2.41,'violeta',bone=sleeves)
    pc('banda_puente_antebrazo',5.45,15.9,-2.82,6.1,16.1,-2.41,'violeta',bone=sleeves)
    pc('banda_baja_frontal',5.45,12.7,-2.82,6.1,15.9,-2.74,'violeta',bone=sleeves)
    pc('cinta_muneca',3.6,11.75,-2.5,8.4,12.5,2.5,'goma',bone=sleeves)
    pc('broche_muneca',6.2,12.27,-2.32,7.35,12.72,-2.2,'hebilla',bone=sleeves)
    leg='armorRightLeg' if s>0 else 'armorLeftLeg'
    pants=group('pantalon_'+label,by_name[leg]['origin'],leg)
    pc('pantalon_muslo',-0.3,6.35,-2.4,4.4,12.3,2.4,'tejido',bone=pants)
    pc('pantalon_pantorrilla',-0.3,3.1,-2.4,4.4,6.35,2.4,'tejido',bone=pants)
    pc('cinturon',-0.3,11.35,-2.52,4.5,12.4,2.52,'goma',bone=pants)
    pc('cinturon_hebilla',0.1,11.42,-2.67,0.85,12.17,-2.52,'metal',bone=pants)
    pc('bolsillo_muslo',4.14,8,-1.65,4.85,10.9,1.5,'panel',bone=pants)
    pc('bolsillo_solapa',4.85,10.05,-1.65,4.97,10.85,1.5,'placa',bone=pants)
    pc('correa_muslo',-0.32,7.7,-2.48,4.45,8.15,2.48,'goma',bone=pants)
    pc('rodillera_base',0.45,4.8,-2.65,3.9,7.2,-2.14,'goma',bone=pants)
    pc('rodillera_superior',0.75,6.1,-3.0,3.6,7.05,-2.65,'canto',bone=pants)
    pc('rodillera_inferior',0.9,4.9,-2.95,3.5,5.95,-2.65,'placa',bone=pants)
    pc('rodillera_rotula',1.2,5.4,-3.12,3.15,6.65,-2.96,'panel',bone=pants)
    pc('correa_rodilla',-0.3,5.1,2.4,4.4,5.65,2.6,'goma',bone=pants)
    boot='armorRightBoot' if s>0 else 'armorLeftBoot'
    boots=group('bota_'+label,by_name[boot]['origin'],boot)
    pc('bota_cana',-0.3,0.55,-2.42,4.4,3.5,2.42,'goma',bone=boots)
    pc('bota_puntera',-0.3,0.4,-3.35,4.4,1.6,-2.3,'placa',bone=boots)
    pc('bota_suela',-0.4,-0.3,-3.4,4.5,0.55,2.65,'suelas',bone=boots)
    pc('bota_talon',0.2,0.55,2.42,3.9,1.8,2.65,'placa',bone=boots)
    pc('bota_espinillera',0.55,1.8,-2.62,3.7,3.5,-2.42,'azul',bone=boots)
    for j in range(2):
        pc('bota_cierre_'+str(j),0.6,1.0+j*0.8,-2.63,3.65,1.25+j*0.8,-2.5,'metal',bone=boots)

# Todo el casco comparte el mismo negro; solo el cristal conserva el verde de las ópticas.
def descendants(node):
    for child in node['children']:
        if isinstance(child,dict):yield from descendants(child)
        else:yield child
helmet_ids=set(descendants(by_name['armorHead']))
for el in elements:
    if el['uuid'] in helmet_ids:
        for face,values in el['faces'].items():
            if not ('_cristal_' in el['name'] and face=='north'):
                values['uv']=uv('casco')

# Las placas de cada hombrera comparten la inclinación revisada en Blockbench.
for el in elements:
    for suffix, sign in [('izq',-1),('der',1)]:
        if el['name'] in [part+'_'+suffix for part in ['hombrera','hombrera_tapa','hombrera_faldon']]:
            el['origin']=[sign*4.5,23.7,0]
            el['rotation']=[0,0,-sign*10]

texture_file=ROOT/'textures/maxtac.png'
atlas.save(texture_file)
glow=Image.new('RGBA',(SIZE,SIZE),(0,0,0,0))
# La máscara solo contiene las bandas violetas y el cristal de las ópticas.
for tile_index in (5,6,11):
    tx,ty=(tile_index%4)*64,(tile_index//4)*64
    glow.paste(atlas.crop((tx,ty,tx+64,ty+64)),(tx,ty))
glow.save(ROOT/'textures/maxtac_glowmask.png')
standby=atlas.copy()
aux=atlas.crop((192,128,256,192))
aux.putdata([(round(r*0.07),round(g*0.07),round(b*0.07),a) for r,g,b,a in aux.get_flattened_data()])
standby.paste(aux,(192,128));standby.save(ROOT/'textures/maxtac_standby.png')
standby_glow=glow.copy()
ImageDraw.Draw(standby_glow).rectangle((192,128,255,191),fill=(0,0,0,0))
standby_glow.save(ROOT/'textures/maxtac_standby_glowmask.png')
buf=io.BytesIO();atlas.save(buf,format='PNG')
model=dict(meta=dict(format_version='5.0',model_format='geckolib_model',box_uv=False),name='MaxTac — Apogee',model_identifier='maxtac',geo_name='maxtac',geckolib_modid='apogee',geckolib_model_type='Armor',resolution=dict(width=SIZE,height=SIZE),elements=elements,outliner=groups,textures=[dict(name='maxtac.png',uuid=uid('texture'),id='0',mode='bitmap',internal=True,uv_width=SIZE,uv_height=SIZE,width=SIZE,height=SIZE,source='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode())],animations=[])
(ROOT/'blockbench/maxtac.bbmodel').write_text(json.dumps(model,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'source/materiales.json').write_text(json.dumps(dict(materials=materials,tile_size=TILE,atlas_size=SIZE),ensure_ascii=False,indent=2),encoding='utf-8')

# Maniquí independiente para revisar la abertura. No se incluye en la geometría exportada.
preview=copy.deepcopy(model)
skin=Image.new('RGBA',(8,8),(167,121,91,255));sd=ImageDraw.Draw(skin)
sd.rectangle((0,6,7,7),fill=(151,106,80))
sd.rectangle((2,6,5,6),fill=(102,62,54))
sd.rectangle((3,5,4,5),fill=(177,129,96))
sd.rectangle((1,3,2,3),fill=(48,55,59));sd.rectangle((5,3,6,3),fill=(48,55,59))
preview_atlas=Image.new('RGBA',(512,256),(0,0,0,0));preview_atlas.paste(atlas,(0,0));preview_atlas.paste(skin.resize((64,64),Image.Resampling.NEAREST),(256,0))
preview_buf=io.BytesIO();preview_atlas.save(preview_buf,format='PNG')
preview['textures'][0]['source']='data:image/png;base64,'+base64.b64encode(preview_buf.getvalue()).decode()
preview['resolution']['width']=512;preview['textures'][0]['width']=512;preview['textures'][0]['uv_width']=512
guide=dict(name='GUIA_cabeza_NO_EXPORTAR',type='cube',uuid=uid('preview_head'),origin=[0,24,0],rotation=[0,0,0],box_uv=False,autouv=0,visibility=True,export=False,locked=True,faces={f:dict(uv=[256.5,0.5,319.5,63.5] if f=='north' else [256,0,264,8],texture=0) for f in ['north','east','south','west','up','down']})
guide['from']=[-4,24,-4];guide['to']=[4,32,4]
preview['elements'].append(guide)
preview['outliner'][0]['children'].append(guide['uuid'])
preview['name']='MaxTac — maniquí de referencia'
(ROOT/'blockbench/maxtac_con_maniqui.bbmodel').write_text(json.dumps(preview,ensure_ascii=False,indent=2),encoding='utf-8')
preview_atlas.paste(standby,(0,0));preview_buf=io.BytesIO();preview_atlas.save(preview_buf,format='PNG')
preview['textures'][0]['source']='data:image/png;base64,'+base64.b64encode(preview_buf.getvalue()).decode()
(ROOT/'blockbench/maxtac_con_maniqui_reposo.bbmodel').write_text(json.dumps(preview,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'MaxTac: {len(elements)} cubos; atlas {SIZE} × {SIZE}; 8 grupos de armadura.')
