"""Construye el modelo original Trauma Team para revisión y exportación en Blockbench."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import base64, copy, io, json, math, random, uuid

ROOT=Path(__file__).resolve().parents[1]
for folder in ('blockbench','textures','export','previews','evidence'): (ROOT/folder).mkdir(parents=True,exist_ok=True)
NS=uuid.UUID('13a0fb0a-9057-49e1-9297-7a1a75f3f403')
def uid(name):return str(uuid.uuid5(NS,name))
MATERIALS=[('tejido',(23,104,93)),('blanco',(217,226,220)),('junta',(29,39,43)),('rojo',(190,38,44)),
 ('metal',(115,129,130)),('sombra',(14,67,62)),('pecho',(217,226,220)),('emblema',(217,226,220)),
 ('correa',(33,43,44)),('panel',(42,52,56)),('suela',(174,36,43)),('led',(255,27,37)),
 ('lampara',(199,250,255)),('cristal',(31,101,114)),('aviso',(228,197,82)),('rotulo',(158,30,35))]
atlas=Image.new('RGBA',(256,256));rng=random.Random(207703)
font=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',9)
def emblem(draw,center=(32,32),size=20):
    x,y=center
    for angle in (90,30,150):
        a=math.radians(angle);dx,dy=math.cos(a)*size,math.sin(a)*size
        draw.line((x-dx,y-dy,x+dx,y+dy),fill=(37,45,46),width=10)
        draw.line((x-dx,y-dy,x+dx,y+dy),fill=(190,38,44),width=6)
for i,(name,color) in enumerate(MATERIALS):
    tile=Image.new('RGBA',(64,64),(*color,255));d=ImageDraw.Draw(tile)
    for y in range(64):
        for x in range(64):
            n=rng.choice((-2,-1,0,0,1,2))+(2 if name in ('tejido','sombra') and (x+y)%7<2 else 0)
            tile.putpixel((x,y),tuple(max(0,min(255,c+n)) for c in color)+(255,))
    if name in ('blanco','panel','metal'):
        d.line((4,54,4,12,12,4,52,4),fill=tuple(min(255,c+12) for c in color),width=1)
        d.line((9,60,58,60,60,9),fill=tuple(max(0,c-19) for c in color),width=2)
    if name=='tejido':
        for y in (9,53):d.line((4,y,59,y),fill=(36,122,108),width=1)
    if name=='emblema':emblem(d)
    if name=='pecho':
        d.rectangle((3,8,60,55),outline=(145,157,151),width=2)
        d.text((32,20),'TRAUMA',font=font,anchor='mm',fill=(178,29,35))
        d.text((32,39),'TEAM',font=font,anchor='mm',fill=(178,29,35))
    if name=='rotulo':d.text((32,32),'TRAUMA',font=font,anchor='mm',fill=(233,236,222))
    if name=='led':
        d.rectangle((0,0,63,63),fill=(255,27,37));d.line((0,13,63,13),fill=(255,135,137),width=5)
    if name=='lampara':d.rectangle((9,9,54,54),fill=(244,255,255))
    if name=='cristal':
        tile=Image.new('RGBA',(64,64),(23,65,76,76));d=ImageDraw.Draw(tile)
        d.line((6,52,6,12,13,6),fill=(131,209,215,126),width=2)
        d.line((49,8,56,15,56,45),fill=(107,174,183,110),width=1)
    if name=='aviso':
        d.polygon([(32,8),(56,51),(8,51)],fill=(36,42,43));d.line((32,22,32,34),fill=color,width=4);d.rectangle((30,40,34,44),fill=color)
    if name=='suela':
        for x in range(3,64,11):d.rectangle((x,3,x+4,60),fill=(91,27,31))
    atlas.paste(tile,((i%4)*64,(i//4)*64))
def uv(mat):
    i=[m[0] for m in MATERIALS].index(mat);x,y=(i%4)*64,(i//4)*64
    return [x+.5,y+.5,x+63.5,y+63.5]
groups=[];elements=[];bones={}
def group(name,pivot,parent=None):
    g=dict(name=name,uuid=uid(name),origin=pivot,rotation=[0,0,0],export=True,isOpen=False,visibility=True,children=[])
    (bones[parent]['children'] if parent else groups).append(g);bones[name]=g;return name
for b,a,p in [('bipedHead','armorHead',[0,24,0]),('bipedBody','armorBody',[0,24,0]),('bipedRightArm','armorRightArm',[5,22,0]),('bipedLeftArm','armorLeftArm',[-5,22,0]),('bipedRightLeg','armorRightLeg',[2,12,0]),('bipedLeftLeg','armorLeftLeg',[-2,12,0])]:
    group(b,p);group(a,p,b)
    if 'Leg' in a:group(a.replace('Leg','Boot'),p,b)
def cube(name,bone,a,b,mat='blanco',rotation=None,pivot=None,front=None):
    el=dict(name=name,type='cube',uuid=uid('cube:'+name),origin=pivot or bones[bone]['origin'],rotation=rotation or [0,0,0],box_uv=False,autouv=0,visibility=True,export=True,faces={f:dict(uv=uv(front if f=='north' and front else mat),texture=0) for f in ('north','east','south','west','up','down')})
    el['from']=a;el['to']=b;elements.append(el);bones[bone]['children'].append(el['uuid']);return el
def disk(name,bone,x,y,z,r,depth,mat):
    # Sección circular real en doce bandas horizontales, sin marco cuadrado.
    for k in range(12):
        lo=-r+2*r*k/12;hi=-r+2*r*(k+1)/12
        half=math.sqrt(max(0,r*r-((lo+hi)/2)**2))
        cube(name+'_'+str(k),bone,[x-half,y+lo,z],[x+half,y+hi,z+depth],mat)

head=group('casco_fijo',[0,24,0],'armorHead')
cube('nuca_oscura',head,[-4.65,23.95,3.8],[4.65,32.1,4.72],'junta')
cube('nuca_blanca',head,[-4.4,27.3,4.72],[4.4,32.4,4.95])
cube('corona_base',head,[-4.65,32,-4.65],[4.65,32.75,4.65])
cube('corona_superior',head,[-3.7,32.75,-3.4],[3.7,33.0,3.9])
cube('banda_trasera',head,[-4.5,27.6,4.96],[4.5,28.3,5.03],'rotulo')
for s,label in ((1,'der'),(-1,'izq')):
    def pc(name,x1,y1,z1,x2,y2,z2,mat='blanco',bone=head,**kw):
        return cube(name+'_'+label,bone,[min(s*x1,s*x2),y1,z1],[max(s*x1,s*x2),y2,z2],mat,**kw)
    pc('acolchado',4.5,23.95,-4.5,4.85,32.2,3.9,'junta')
    pc('lateral_carcasa',4.85,28.6,-1,5.1,32.1,3.7)
    pc('orejera',4.86,25.1,-1.8,5.42,28.5,1.8)
    pc('orejera_inserto',5.42,26.15,-1.1,5.51,27.45,1.05,'metal')
    pc('correa_roja',4.9,27.75,-1.9,5.16,28.25,3.8,'rojo')
    pc('bisagra',4.55,30.65,-3,5.3,31.55,-1.8,'metal')
    pc('bisagra_eje',4.9,30.86,-3.22,5.5,31.34,-3.02,'junta')
    pc('filtro_lateral',4.25,24.8,-4.3,5.45,26.3,-2.6,'blanco')
    pc('filtro_cinta',4.5,25.1,-4.45,5.22,25.4,-4.3,'aviso')
    for j in range(3):pc('ventilacion_'+str(j),4.92,28.55+j*.24,2,5.17,28.68+j*.24,3.7,'junta')

inner=group('lamina_interior',[0,24,0],'armorHead')
cube('lamina_cristal',inner,[-4.35,26.7,-4.68],[4.35,31.8,-4.62],'cristal')
cube('marco_lamina_superior',inner,[-4.5,31.8,-4.72],[4.5,32.08,-4.42],'junta')
for s in (-1,1):cube('marco_lamina_'+str(s),inner,sorted([s*4.25,s*4.55])[:1]+[26.6,-4.72],sorted([s*4.25,s*4.55])[1:]+[31.8,-4.4],'junta')
resp=group('respirador',[0,24,0],'armorHead')
cube('mascara_base',resp,[-3.8,23.95,-4.98],[3.8,26.85,-4.52],'junta')
cube('mascara_menton',resp,[-3.05,23.95,-5.1],[3.05,24.65,-4.7],'panel')
for s in (-1,1):
    cheek=sorted([s*3.6,s*4.55])
    cube('mascara_mejilla_'+str(s),resp,[cheek[0],23.95,-5.0],[cheek[1],27.25,-4.5],'junta')
cube('puente_respirador',resp,[-.5,24.2,-5.4],[.5,27.5,-4.92],'panel',[-8,0,0],[0,27,-4.9])
for s in (-1,1):
    for j in range(2):
        disk('valvula_marco_'+str(s)+'_'+str(j),resp,s*2.05,24.85+j*1.25,-5.26,.49,.27,'metal')
        disk('valvula_tapa_'+str(s)+'_'+str(j),resp,s*2.05,24.85+j*1.25,-5.3,.3,.04,'blanco')
disk('filtro_frontal',resp,0,25.35,-5.85,.68,.4,'junta')
disk('filtro_frontal_tapa',resp,0,25.35,-5.93,.46,.08,'metal')
# Carcasa abatible independiente del cristal y del respirador.
front=group('frontal_abatible',[0,31.1,-2.4],'armorHead')
cube('frontal_soporte',front,[-4.6,26.5,-5.02],[4.6,32.1,-4.72],'junta')
cube('frontal_blanco_central',front,[-3.45,26.55,-5.23],[3.45,31.9,-4.99])
cube('frontal_superior',front,[-4.1,31.9,-5.09],[4.1,32.4,-4.7])
for s in (-1,1):
    xs=sorted([s*3.45,s*4.65])
    rail=sorted([s*4.25,s*4.55])
    cube('brazo_bisagra_'+str(s),front,[rail[0],30.88,-4.85],[rail[1],31.32,-2.25],'metal')
    cube('frontal_ala_'+str(s),front,[xs[0],26.6,-5.02],[xs[1],31.9,-4.5],'blanco',[0,s*16,0],[s*3.45,30,-4.7])
    for j in range(2):
        x=s*2.52;y=28.75+j*2.0
        cube('indicador_marco_'+str(s)+'_'+str(j),front,[x-.38,y-.16,-5.32],[x+.38,y+.16,-5.23],'metal')
        cube('indicador_rojo_'+str(s)+'_'+str(j),front,[x-.25,y-.075,-5.35],[x+.25,y+.075,-5.32],'led')
cube('escudo_medico',front,[-.75,29.7,-5.31],[.75,31.75,-5.23],'blanco',front='emblema')
cube('lampara_soporte',head,[-5.35,28.5,-4.55],[-4.15,29.7,-3.2],'panel')
cube('lampara_blanca',head,[-5.22,28.63,-4.65],[-4.28,29.57,-4.55],'lampara')
for j,(y,z) in enumerate(((25.5,2.8),(25.0,3.8),(24.4,4.6),(23.4,4.95))):
    cube('tubo_cuello_'+str(j),head,[4.6,y-.45,z-.5],[5.35,y+.45,z+.5],'junta',[20+j*10,0,0],[4.9,y,z])

body=group('chaleco',[0,24,0],'armorBody')
cube('traje_torso',body,[-4.38,11.8,-2.38],[4.38,24.1,2.38],'tejido')
cube('chaleco_base',body,[-4.6,14.8,-2.85],[4.6,23.3,2.8],'correa')
cube('placa_pecho',body,[-4.4,19.6,-3.35],[4.4,23,-2.85],'blanco',front='pecho')
cube('faja_inferior',body,[-4.5,14.7,-3.06],[4.5,15.4,-2.7],'junta')
for j,x in enumerate((-2.9,0,2.9)):
    cube('bolsa_frontal_'+str(j),body,[x-1.27,15.5,-3.8],[x+1.27,19.35,-2.82],'blanco')
    cube('solapa_bolsa_'+str(j),body,[x-1.25,18.1,-3.94],[x+1.25,19.35,-3.8],'blanco')
    cube('cierre_bolsa_'+str(j),body,[x-.23,17.75,-4.01],[x+.23,18.5,-3.94],'metal')
cube('placa_espalda',body,[-4.4,17,2.8],[4.4,23.2,3.25],'panel')
cube('mochila_medica',body,[-2.9,15,3.25],[2.8,21.2,4.4],'sombra')
cube('mochila_solapa',body,[-2.8,19.25,4.4],[2.7,21.1,4.6],'tejido')
cube('botiquin_rojo',body,[-4.6,13.2,3.1],[-2.65,17.5,4.4],'rojo')
cube('botiquin_emblema',body,[-4.3,14,4.4],[-2.95,16.3,4.5],'emblema')
cube('cuello_posterior',body,[-4.5,23.2,1.7],[4.5,25.55,3],'tejido')
for s,label in ((1,'der'),(-1,'izq')):
    def pc(name,x1,y1,z1,x2,y2,z2,mat='blanco',bone=body,**kw):
        return cube(name+'_'+label,bone,[min(s*x1,s*x2),y1,z1],[max(s*x1,s*x2),y2,z2],mat,**kw)
    pc('cuello_lateral',3.6,23.2,-2.4,4.7,25.1,2,'tejido')
    pc('tirante',2.9,22.3,-2.9,3.9,24.4,2.9,'correa')
    pc('tirante_hebilla',2.8,22.65,-3.02,4,23.5,-2.89,'metal')
    arm='armorRightArm' if s>0 else 'armorLeftArm';sleeve=group('manga_'+label,bones[arm]['origin'],arm)
    pc('manga',3.65,11.7,-2.4,8.35,24.3,2.4,'tejido',bone=sleeve)
    pc('hombrera_base',3.7,21.4,-2.55,8.6,24.5,2.55,'junta',bone=sleeve,rotation=[0,0,-s*8],pivot=[s*4.4,23.6,0])
    pc('hombrera_blanca',4,21.8,-2.7,8.5,24.35,2.7,'blanco',bone=sleeve,rotation=[0,0,-s*8],pivot=[s*4.4,23.6,0],front='emblema')
    pc('codo_refuerzo',3.55,16.5,2.4,8.4,18.5,2.7,'panel',bone=sleeve)
    pc('antebrazo_rojo',3.6,12.2,-2.55,8.4,16.6,2.55,'rojo',bone=sleeve)
    pc('placa_antebrazo',4.2,13.2,-2.85,7.8,16.05,-2.55,'blanco',bone=sleeve)
    pc('banda_brazo',3.55,18.5,-2.5,8.4,19.05,2.5,'rojo',bone=sleeve)
    pc('puno_rojo',3.55,11.65,-2.5,8.4,12.8,2.5,'rojo',bone=sleeve)
    leg='armorRightLeg' if s>0 else 'armorLeftLeg';pants=group('pantalon_'+label,bones[leg]['origin'],leg)
    pc('pantalon_muslo',-.3,6.35,-2.4,4.4,12.3,2.4,'tejido',bone=pants)
    pc('pantalon_pantorrilla',-.3,3.1,-2.4,4.4,6.35,2.4,'tejido',bone=pants)
    pc('cinturon',-.32,11.45,-2.55,4.45,12.4,2.55,'correa',bone=pants)
    pc('bolsillo_muslo',4.4,7.7,-1.6,5.15,10.9,1.6,'sombra',bone=pants)
    pc('bolsillo_tapa',5.15,9.9,-1.6,5.26,10.9,1.6,'tejido',bone=pants)
    pc('correa_muslo',-.32,7.5,-2.5,4.45,8,2.5,'correa',bone=pants)
    pc('rodillera_base',.45,4.75,-2.68,3.9,7.1,-2.2,'junta',bone=pants)
    pc('rodillera_placa',.85,5,-2.96,3.5,6.9,-2.68,'panel',bone=pants)
    pc('rodillera_rotula',1.15,5.3,-3.12,3.2,6.5,-2.96,'metal',bone=pants)
    boot='armorRightBoot' if s>0 else 'armorLeftBoot';feet=group('bota_'+label,bones[boot]['origin'],boot)
    pc('bota_cana',-.3,.5,-2.45,4.4,3.5,2.45,'sombra',bone=feet)
    pc('bota_puntera',-.3,.3,-3.3,4.4,1.6,-2.25,'tejido',bone=feet)
    pc('bota_refuerzo_rojo',-.35,.2,-3.5,4.45,1,-3.3,'rojo',bone=feet)
    pc('bota_suela',-.4,-.3,-3.5,4.5,.4,2.65,'suela',bone=feet)
    for j in range(2):pc('bota_cierre_'+str(j),.5,1.5+j*.8,-2.57,3.6,1.75+j*.8,-2.45,'correa',bone=feet)

atlas.save(ROOT/'textures/trauma_team.png')
off=atlas.copy();off.paste(Image.new('RGBA',(64,64),(48,10,14,255)),(192,128));off.save(ROOT/'textures/trauma_team_off.png')
for name,source,indices in [('trauma_team',atlas,(11,12)),('trauma_team_off',off,(12,))]:
    mask=Image.new('RGBA',(256,256))
    for i in indices:
        x,y=(i%4)*64,(i//4)*64;mask.paste(source.crop((x,y,x+64,y+64)),(x,y))
    mask.save(ROOT/('textures/'+name+'_glowmask.png'))
def encoded(image):
    b=io.BytesIO();image.save(b,format='PNG');return 'data:image/png;base64,'+base64.b64encode(b.getvalue()).decode()
animations=[]
for name,values in [('abrir',(0,60)),('cerrar',(60,0))]:
    keys=[dict(channel='rotation',data_points=[dict(x=v,y=0,z=0)],uuid=uid(name+str(i)),time=i*.5,interpolation='catmullrom') for i,v in enumerate(values)]
    animations.append(dict(uuid=uid('animation:'+name),name='animation.trauma_team.'+name,loop='hold',length=.5,snapping=20,animators={uid(front):dict(name=front,type='bone',keyframes=keys)}))
model=dict(meta=dict(format_version='5.0',model_format='geckolib_model',box_uv=False),name='Trauma Team — Apogee',model_identifier='trauma_team',geo_name='trauma_team',geckolib_modid='apogee',geckolib_model_type='Armor',resolution=dict(width=256,height=256),elements=elements,outliner=groups,textures=[dict(name='trauma_team.png',uuid=uid('texture'),id='0',mode='bitmap',internal=True,uv_width=256,uv_height=256,width=256,height=256,source=encoded(atlas))],animations=animations)
(ROOT/'blockbench/trauma_team.bbmodel').write_text(json.dumps(model,ensure_ascii=False,indent=2),encoding='utf-8')
# Guías independientes: no exportar la cara de muestra ni la pose abierta como geometría base.
for opened in (False,True):
    preview=copy.deepcopy(model);preview['animations']=[]
    skin=Image.new('RGBA',(8,8),(172,126,94,255));d=ImageDraw.Draw(skin)
    d.rectangle((1,3,2,3),fill=(45,54,57));d.rectangle((5,3,6,3),fill=(45,54,57));d.line((2,6,5,6),fill=(104,63,57))
    wide=Image.new('RGBA',(512,256));wide.paste(atlas,(0,0));wide.paste(skin.resize((64,64),Image.Resampling.NEAREST),(256,0))
    preview['resolution']['width']=512;preview['textures'][0].update(width=512,uv_width=512,source=encoded(wide))
    guide=dict(name='GUIA_cabeza_NO_EXPORTAR',type='cube',uuid=uid('guide'),origin=[0,24,0],rotation=[0,0,0],box_uv=False,autouv=0,visibility=True,export=False,locked=True,faces={f:dict(uv=[256.5,.5,319.5,63.5],texture=0) for f in ('north','east','south','west','up','down')})
    guide['from']=[-4,24,-4];guide['to']=[4,32,4];preview['elements'].append(guide);preview['outliner'][0]['children'].append(guide['uuid'])
    def pose(nodes):
        for n in nodes:
            if isinstance(n,dict):
                if n['name']==front:n['rotation']=[60,0,0]
                pose(n['children'])
    if opened:pose(preview['outliner'])
    suffix='abierto' if opened else 'cerrado'
    (ROOT/f'blockbench/trauma_team_guia_{suffix}.bbmodel').write_text(json.dumps(preview,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'source/materiales.json').write_text(json.dumps(MATERIALS,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Trauma Team: {len(elements)} cubos; {len(bones)} huesos; casco abatible y cuatro indicadores rojos.')
