"""Comprueba la exportación de Blockbench, la bisagra, el atlas y los recursos integrados."""
from pathlib import Path
from PIL import Image
import base64, hashlib, json, math, xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
assets=root.parents[1]/'src/main/resources/assets/apogee'
model=json.loads((root/'blockbench/trauma_team.bbmodel').read_text(encoding='utf-8'))
geo=json.loads((root/'export/trauma_team.geo.json').read_text(encoding='utf-8'))['minecraft:geometry'][0]
animations=json.loads((root/'export/trauma_team.animation.json').read_text(encoding='utf-8'))['animations']
checks=[]
def check(pass_,message):
    assert pass_,message
    checks.append(message)
defs={g['uuid']:g for g in model.get('groups',[])};groups={};refs=[];group_ids=[]
def walk(nodes,parent=None):
    for node in nodes:
        if isinstance(node,dict):
            g=defs.get(node['uuid'],node);groups[g['name']]={**g,'parent_name':parent};group_ids.append(g['uuid']);walk(node['children'],g['name'])
        else:refs.append(node)
walk(model['outliner'])
ids=[e['uuid'] for e in model['elements']]
check(len(ids+group_ids)==len(set(ids+group_ids)),'UUID únicos en cubos y grupos')
check(sorted(ids)==sorted(refs),'Cada cubo pertenece a un único grupo')
check(model['meta']['model_format']=='geckolib_model' and model['geckolib_model_type']=='Armor','Proyecto GeckoLib de armadura')
expected={'armorHead':[0,24,0],'armorBody':[0,24,0],'armorRightArm':[5,22,0],'armorLeftArm':[-5,22,0],'armorRightLeg':[2,12,0],'armorLeftLeg':[-2,12,0],'armorRightBoot':[2,12,0],'armorLeftBoot':[-2,12,0]}
check(all(groups[n]['origin']==p for n,p in expected.items()),'Ocho ranuras y pivotes de la plantilla')
for el in model['elements']:
    check(all(math.isfinite(v) for k in ('from','to','origin','rotation') for v in el.get(k,[0,0,0])),'Coordenadas finitas: '+el['name'])
    check(all(a<b for a,b in zip(el['from'],el['to'])),'Volumen positivo: '+el['name'])
    check(all(0<=v<=256 for face in el['faces'].values() for v in face['uv']),'UV dentro del atlas: '+el['name'])
elements={e['name']:e for e in model['elements']}
check(groups['frontal_abatible']['origin']==[0,31.1,-2.4],'Pivote delantero junto a las sienes para recoger la máscara sobre la frente')
plate=elements['frontal_blanco_central'];angle=math.radians(60)
open_y=[31.1+(y-31.1)*math.cos(angle)-(z+2.4)*math.sin(angle) for y in (plate['from'][1],plate['to'][1]) for z in (plate['from'][2],plate['to'][2])]
check(min(open_y)>31 and max(open_y)<34.5,'La máscara abierta despeja los ojos y queda junto a la corona del casco')
check(plate['from'][1]<=26.55 and elements['mascara_base']['to'][1]>plate['from'][1],'La placa cerrada se solapa con el respirador y oculta por completo los ojos')
check(all(elements['frontal_ala_'+str(s)]['from'][1]<=26.6 for s in (-1,1)),'Las alas blancas también tapan los extremos de los ojos')
check(groups['lamina_interior']['parent_name']=='armorHead' and groups['respirador']['parent_name']=='armorHead','El cristal y el respirador no se levantan con la carcasa')
check(sum(n.startswith('indicador_rojo_') for n in elements)==4,'Cuatro indicadores rojos en la carcasa')
check(sum(n.startswith('brazo_bisagra_') for n in elements)==2,'Dos brazos mantienen unida la carcasa al eje')
def encloses(name,lo,hi):
    e=elements[name]
    return all(e['from'][i]<=lo[i] and e['to'][i]>=hi[i] for i in range(3))
check(encloses('traje_torso',[-4.25,12,-2.25],[4.25,24,2.25]),'Cobertura de la capa exterior del torso')
check(encloses('corona_base',[-4.5,32,-4.5],[4.5,32.5,4.5]),'La corona envuelve también el borde superior de la capa de la cabeza')
for label,s in [('der',1),('izq',-1)]:
    x=sorted([s*3.75,s*8.25])
    check(encloses('manga_'+label,[x[0],11.75,-2.25],[x[1],24.25,2.25]),'Cobertura de manga clásica y delgada: '+label)
    x=sorted([s*-.25,s*4.25])
    check(encloses('pantalon_muslo_'+label,[x[0],6.35,-2.25],[x[1],12.25,2.25]),'Cobertura del muslo con capa exterior: '+label)
    check(encloses('pantalon_pantorrilla_'+label,[x[0],3.25,-2.25],[x[1],6.35,2.25]),'Cobertura de pantorrilla con capa exterior: '+label)
png=(root/'textures/trauma_team.png').read_bytes()
check(base64.b64decode(model['textures'][0]['source'].split(',')[1])==png,'Atlas incrustado idéntico al PNG')
lit=Image.open(root/'textures/trauma_team.png').convert('RGBA');off=Image.open(root/'textures/trauma_team_off.png').convert('RGBA')
check(lit.size==(256,256),'Atlas de 256 × 256')
check(lit.getpixel((96,224))[3] in range(1,255),'Lámina interna translúcida')
for name,active in [('trauma_team',True),('trauma_team_off',False)]:
    mask=Image.open(root/f'textures/{name}_glowmask.png')
    check(bool(mask.crop((192,128,256,192)).getbbox())==active,'Máscara de indicadores '+name)
    check(mask.crop((0,192,64,256)).getbbox() is not None,'Lámpara lateral emisiva '+name)
    check(mask.crop((64,192,128,256)).getbbox() is None,'El cristal no emite '+name)
check(sum(len(b.get('cubes',[])) for b in geo['bones'])==len(ids),'Todos los cubos están en la exportación')
check(not any('GUIA' in b['name'] for b in geo['bones']),'La guía no se exporta')
check(set(expected)<={b['name'] for b in geo['bones']},'Los ocho grupos se conservan al exportar')
for name in ('abrir','cerrar'):
    a=animations['animation.trauma_team.'+name]
    check(a['animation_length']==.5 and 'frontal_abatible' in a['bones'],'Animación exportada de medio segundo: '+name)
for folder,files in [('geo',['trauma_team.geo.json']),('animations',['trauma_team.animation.json']),('textures/armor',[p.name for p in (root/'textures').glob('*.png')])]:
    for name in files:
        source=root/('export' if folder in ('geo','animations') else 'textures')/name
        check(source.read_bytes()==(assets/folder/name).read_bytes(),'Recurso integrado actualizado: '+name)
smoke_path=root/'evidence/trauma-smoke.log'
smoke=smoke_path.read_text(encoding='utf-8') if smoke_path.exists() else ''
smoke_ok='TRAUMA_SMOKE_PASSED' in smoke and 'TRAUMA_SMOKE_FAILED' not in smoke
blink_pixels={}
for phase,filename in [('on','trauma-04-rojo-encendido-oscuridad.png'),('off','trauma-05-rojo-apagado-oscuridad.png')]:
    path=root/'previews'/filename
    if path.exists():
        capture=Image.open(path).convert('RGB')
        blink_pixels[phase]=sum(r>120 and g<90 and b<90 for r,g,b in capture.get_flattened_data())
if len(blink_pixels)==2:
    check(blink_pixels['on']>0 and blink_pixels['off']==0,'Las capturas reales muestran emisión roja al encender y ninguna al apagar')
server_path=root/'evidence/gametest.xml';server={}
if server_path.exists():
    r=ET.parse(server_path).getroot();cases=list(r.iter('testcase'))
    server=dict(tests=len(cases),failures=len(list(r.iter('failure'))),errors=len(list(r.iter('error'))),trauma_tests=sum(c.get('name','').startswith('traumagametests.') for c in cases))
report=dict(result='correcto',cubes=len(ids),bones=len(geo['bones']),checks_passed=len(checks),texture_sha256=hashlib.sha256(png).hexdigest(),minecraft_tested=smoke_ok,red_emissive_pixels_in_darkness=blink_pixels,server_gametest=server,notes=['Proyecto, geometría y animaciones guardados y exportados en Blockbench.','Apertura/cierre por portador, lámina interior independiente y parpadeo rojo de un segundo.','Essential fuera de alcance por indicación del usuario; compatibilidad visual con ciberware y todas las poses todavía sin comprobar.'])
(root/'validacion.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
