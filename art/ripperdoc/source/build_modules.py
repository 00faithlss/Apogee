from pathlib import Path
import json,copy,uuid,sys,base64,io
import numpy as np
from scipy.spatial.transform import Rotation
from PIL import Image,ImageDraw,ImageFont
R=Path(__file__).resolve().parents[1]
SRC=R/'combat-source'
for n in ['modules','states','combat-source']: (R/n).mkdir(exist_ok=True)
def save(d,p):p.write_text(json.dumps(d,ensure_ascii=False,indent=2))
def uid():return str(uuid.uuid4())
base=json.loads((R/'jugador-rayos-x.bbmodel').read_text())
ig=next((g['uuid'] for g in base['groups'] if g['name']=='implants'),None);removed=set()
def clean(nodes):
 out=[]
 for n in nodes:
  if isinstance(n,dict):
   if n['uuid']==ig:
    removed.update(x for x in n['children'] if isinstance(x,str));continue
   n['children']=clean(n['children'])
  out.append(n)
 return out
base['outliner']=clean(base['outliner']);base['groups']=[g for g in base['groups'] if g['uuid']!=ig];base['elements']=[e for e in base['elements'] if e['uuid'] not in removed]
base['name']='Jugador / anatomia sin implantes';save(base,R/'jugador-rayos-x.bbmodel')
keep={'housing','link_lower','link_upper','blade','slide_mid','slide_tip','blade_slider','cutting_edge','lift_carriage'}
T=Rotation.from_euler('x',-90,degrees=True).as_matrix()
manifest={'version':1,'status':'asset_preview_not_runtime_integration','slot':'cyberware/arms','empty':'states/avatar-none.bbmodel','items':{}}
for variant in ['normal','thermal','electric']:
 pair=None
 for side,cx in [('right',-6.15),('left',6.15)]:
  src=SRC/f'mantis_{variant}_{side}.bbmodel';d=json.loads(src.read_text());(R/'combat-source'/src.name).write_bytes(src.read_bytes())
  names={g['uuid']:g['name'] for g in d['groups']};members={}
  def walk(nodes,parent=None):
   for n in nodes:
    if isinstance(n,str):members[n]=parent
    else:walk(n['children'],names[n['uuid']])
  walk(d['outliner']);els=[];groups=[];out=[]
  for part in sorted(keep):
   g={'name':f'{side}_{part}','uuid':uid(),'origin':[0,0,0],'rotation':[0,0,0],'visibility':True,'export':True,'isOpen':True};children=[]
   for e in d['elements']:
    if members.get(e['uuid'])!=part:continue
    e=copy.deepcopy(e);old=np.array(e['origin']);origin=T@(old*.6)+[cx,15.5,-.5]
    for key in ['from','to']:e[key]=((np.array(e[key])-old)*.6+origin).round(6).tolist()
    e['origin']=origin.round(6).tolist();e['rotation']=Rotation.from_matrix(T@Rotation.from_euler('xyz',e['rotation'],degrees=True).as_matrix()).as_euler('xyz',degrees=True).round(6).tolist();e['uuid']=uid();e['name']=side+'_'+e['name'];els.append(e);children.append(e['uuid'])
   if children:groups.append(g);out.append({'uuid':g['uuid'],'children':children,'isOpen':True})
  mod={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':f'Mantis rayos X / {variant} / {side}','resolution':d['resolution'],'elements':els,'groups':groups,'outliner':out,'textures':d['textures'],'animations':[]}
  save(mod,R/f'modules/mantis_xray_{variant}_{side}.bbmodel')
  if pair is None:pair=copy.deepcopy(mod)
  else:
   for key in ['elements','groups','outliner']:pair[key]+=mod[key]
 pair['name']=f'Mantis rayos X / {variant} / ambos brazos';save(pair,R/f'modules/mantis_xray_{variant}_pair.bbmodel')
 state=copy.deepcopy(base);state['textures']+=pair['textures']
 for e in pair['elements']:
  for f in e['faces'].values():
   if f['texture'] is not None:f['texture']+=3
 for key in ['elements','groups','outliner']:state[key]+=pair[key]
 state['name']=f'Jugador / Mantis {variant}';save(state,R/f'states/avatar-{variant}.bbmodel')
 item='apogee:mantis_blades'+('' if variant=='normal' else '_'+variant)
 manifest['items'][item]={'variant':variant,'model':f'modules/mantis_xray_{variant}_pair.bbmodel','both_arms':True,'preview':f'textures/avatar-{variant}.png','state':f'states/avatar-{variant}.bbmodel','cube_count':len(pair['elements'])}
save(base,R/'states/avatar-none.bbmodel');save(manifest,R/'implant-manifest.json')
print({k:v['cube_count'] for k,v in manifest['items'].items()})
