"""Comprueba el proyecto editable y su exportación real de Blockbench."""
from pathlib import Path
from PIL import Image
import base64
import hashlib
import json
import math
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
model=json.loads((root/'blockbench/maxtac.bbmodel').read_text(encoding='utf-8'))
geo=json.loads((root/'export/maxtac.geo.json').read_text(encoding='utf-8'))['minecraft:geometry'][0]
checks=[]

def check(condition,description):
    assert condition,description
    checks.append(description)

groups={};references=[];group_ids=[]
saved_groups={g['uuid']:g for g in model.get('groups',[])}
def walk(nodes):
    for node in nodes:
        if isinstance(node,dict):
            definition=saved_groups.get(node['uuid'],node)
            check(definition['name'] not in groups,'Nombre de grupo único: '+definition['name'])
            groups[definition['name']]=definition;group_ids.append(node['uuid']);walk(node['children'])
        else:references.append(node)
walk(model['outliner'])
ids=[e['uuid'] for e in model['elements']]
check(len(ids+group_ids)==len(set(ids+group_ids)),'UUID únicos en cubos y grupos')
check(sorted(ids)==sorted(references),'Cada cubo tiene un único grupo y no hay piezas huérfanas')
check(model['meta']['model_format']=='geckolib_model' and model['geckolib_model_type']=='Armor','Formato GeckoLib de tipo Armor')
expected={'armorHead':[0,24,0],'armorBody':[0,24,0],'armorRightArm':[5,22,0],'armorLeftArm':[-5,22,0],'armorRightLeg':[2,12,0],'armorLeftLeg':[-2,12,0],'armorRightBoot':[2,12,0],'armorLeftBoot':[-2,12,0]}
check(all(groups[n]['origin']==p for n,p in expected.items()),'Ocho grupos y pivotes de la plantilla de armadura GeckoLib')
for el in model['elements']:
    check(all(math.isfinite(v) for k in ['from','to','origin','rotation'] for v in el.get(k,[0,0,0])),'Coordenadas finitas: '+el['name'])
    check(all(a<b for a,b in zip(el['from'],el['to'])),'Volumen positivo: '+el['name'])
    check(all(0<=v<=256 for face in el['faces'].values() for v in face['uv']),'UV dentro del atlas: '+el['name'])
check(sum('_cristal_' in e['name'] for e in model['elements'])==6,'Seis cristales ópticos')
check(sum('_bisel_' in e['name'] for e in model['elements'])==96,'Seis biseles circulares de dieciséis segmentos')
check(not any(e['name'] in ['mascara_inferior','barbilla'] for e in model['elements']),'Boca y mentón descubiertos')
nose=next(e for e in model['elements'] if e['name']=='puente_nasal')
check(nose['from'][1]>=26.1 and nose['to'][0]-nose['from'][0]<1 and nose['to'][2]-nose['from'][2]<0.25,'Protector nasal estrecho, fino y situado por encima del labio')
elements={e['name']:e for e in model['elements']}
def encloses(name, low, high):
    el=elements[name]
    return all(el['from'][i]<=low[i] and el['to'][i]>=high[i] for i in range(3))
check(encloses('casco_principal',[-4.5,26,-4.5],[4.5,32.5,4.5]),'El casco cubre la capa exterior de la cabeza salvo la abertura facial')
check(encloses('traje_torso',[-4.25,12,-2.25],[4.25,24,2.25]),'El traje envuelve la capa exterior del torso')
for label,sign in [('der',1),('izq',-1)]:
    bounds=sorted([sign*3.75,sign*8.25])
    check(encloses('manga_'+label,[bounds[0],11.75,-2.25],[bounds[1],24.25,2.25]),'La manga cubre brazos clásicos con capa exterior: '+label)
    bounds=sorted([sign*-0.25,sign*4.25])
    check(encloses('pantalon_muslo_'+label,[bounds[0],6.35,-2.25],[bounds[1],12.25,2.25]),'El pantalón cubre la capa exterior del muslo: '+label)
    check(encloses('pantalon_pantorrilla_'+label,[bounds[0],3.25,-2.25],[bounds[1],6.35,2.25]),'El pantalón cubre la capa exterior de la pantorrilla: '+label)
png=(root/'textures/maxtac.png').read_bytes()
check(base64.b64decode(model['textures'][0]['source'].split(',')[1])==png,'Textura incrustada idéntica al PNG de entrega')
check(Image.open(root/'textures/maxtac.png').size==(256,256),'Atlas de 256 × 256')
full_mask=Image.open(root/'textures/maxtac_glowmask.png').convert('RGBA')
basic_mask=Image.open(root/'textures/maxtac_standby_glowmask.png').convert('RGBA')
check(full_mask.getbbox() is not None and basic_mask.getbbox() is not None,'Máscaras emisivas presentes')
check(basic_mask.crop((192,128,256,192)).getbbox() is None,'Las cuatro ópticas auxiliares no emiten en reposo')
check(full_mask.crop((192,128,256,192)).getbbox() is not None,'Las cuatro ópticas auxiliares emiten al activar el visor')
for label in ('izq','der'):
    bands=[el for el in model['elements'] if el['name'].startswith('banda_') and el['name'].endswith('_'+label)]
    connected={0}
    def touches(a,b):
        return all(a['from'][i]<=b['to'][i]+1e-6 and b['from'][i]<=a['to'][i]+1e-6 for i in range(3))
    while True:
        added={i for i,b in enumerate(bands) if any(touches(b,bands[j]) for j in connected)}
        if added<=connected:break
        connected|=added
    check(len(connected)==len(bands), 'Banda continua en el brazo '+label)
check(sum(len(b.get('cubes',[])) for b in geo['bones'])==len(model['elements']),'La exportación de Blockbench conserva todos los cubos')
names={b['name'] for b in geo['bones']}
check(all(b.get('parent') is None or b['parent'] in names for b in geo['bones']),'Todos los padres de los huesos exportados existen')
check(set(expected)<=names,'Los ocho grupos de equipamiento están en la exportación')
check(not any('GUIA' in b['name'] for b in geo['bones']),'El maniquí no forma parte de la exportación')
runtime=root.parents[1]/'src/main/resources/assets/apogee'
check((runtime/'geo/maxtac.geo.json').read_bytes()==(root/'export/maxtac.geo.json').read_bytes(),'La geometría integrada coincide con la exportación de Blockbench')
for name in ['maxtac.png','maxtac_glowmask.png','maxtac_standby.png','maxtac_standby_glowmask.png']:
    check((runtime/'textures/armor'/name).read_bytes()==(root/'textures'/name).read_bytes(),'Textura integrada actualizada: '+name)
smoke_path=root/'evidence/maxtac-smoke.log'
smoke=smoke_path.read_text(encoding='utf-8') if smoke_path.exists() else ''
smoke_passed='MAXTAC_SMOKE_PASSED' in smoke and 'MAXTAC_SMOKE_FAILED' not in smoke
server_path=root/'evidence/gametest.xml'
server={}
if server_path.exists():
    results=ET.parse(server_path).getroot()
    cases=list(results.iter('testcase'))
    server=dict(tests=len(cases),failures=len(list(results.iter('failure'))),errors=len(list(results.iter('error'))),skipped=len(list(results.iter('skipped'))),maxtac_tests=sum(c.get('name','').startswith('maxtacgametests.') for c in cases))
report=dict(result='correcto',cubes=len(ids),bones=len(geo['bones']),checks_passed=len(checks),texture_sha256=hashlib.sha256(png).hexdigest(),minecraft_tested=smoke_passed,client_smoke='correcto' if smoke_passed else 'sin evidencia',server_gametest=server,notes=['Revisión y exportación real en Blockbench web con GeckoLib Models & Animations 4.2.5.','Prueba de cliente: skins clásicas y delgadas con capas exteriores, tecla N, sincronización con servidor integrado, dos/seis ópticas y brillo con luz ambiental cero.','Pendientes sesión de dos clientes reales, todas las poses, encantamientos, primera persona y convivencia visual con ciberware.'])
(root/'validacion.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
