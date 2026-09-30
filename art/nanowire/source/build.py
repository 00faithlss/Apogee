"""Original Minecraft nanowire asset. BB coordinates, explicit GeckoLib export.
Python 3 + numpy + Pillow. No Cyberpunk meshes, textures or animation files copied.
"""
from pathlib import Path
import json,math,uuid,base64,copy
import numpy as np
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/apogee';F=['north','east','south','west','up','down'];SEG=64
COLORS=['#0c0e11','#181c22','#353c44','#535b63','#030406','#24272d','#ff0000','#97242a','#bd947c','#243847','#202225','#c8bba1','#286467','#592a31','#969fa3','#13232c']
def uid(s):return str(uuid.uuid5(uuid.NAMESPACE_URL,'nanowire-red-v1/'+s))
def save(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2))
def rot(v):
 x,y,z=np.radians(v);cx,sx=np.cos(x),np.sin(x);cy,sy=np.cos(y),np.sin(y);cz,sz=np.cos(z),np.sin(z)
 return np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])@np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])
def mat(r=(0,0,0),p=(0,0,0),pivot=(0,0,0),scale=(1,1,1)):
 m=np.eye(4);m[:3,:3]=rot(r)@np.diag(scale);m[:3,3]=np.array(pivot)+p-m[:3,:3]@pivot;return m
def tx(m,p):return (m@np.r_[p,1])[:3]
class Model:
 def __init__(self):self.bones=[];self.cubes=[]
 def bone(self,n,p=None,pivot=(0,0,0),rotation=(0,0,0)):self.bones.append({'name':n,'parent':p,'pivot':list(pivot),'rotation':list(rotation)});return n
 def cube(self,n,o,s,m,b,r=None,p=None):
  d={'name':n,'origin':list(o),'size':list(s),'material':m,'bone':b}
  if r is not None:d.update(rotation=list(r),pivot=list(p if p is not None else np.array(o)+np.array(s)/2))
  self.cubes.append(d)
 def bar(self,n,a,b,w,m,bone):
  a=np.array(a);v=np.array(b)-a;L=np.linalg.norm(v);r=[-math.degrees(math.asin(v[1]/L)),math.degrees(math.atan2(v[0],v[2])),0]
  self.cube(n,a+[-w/2,-w/2,0],[w,w,L],m,bone,r,a)
def mechanism(m,side,x,mounted=True):
 sg=-1 if side=='left' else 1
 root=m.bone(side+'_arm','root',[x,0,7]);mount=m.bone(side+'_inner_mount',root,[x,0,0],[0,0,sg*90 if mounted else 0])
 body=m.bone(side+'_housing',mount,[x,2,3]);lid=m.bone(side+'_service_cover',body,[x,2.45,5.4]);spool=m.bone(side+'_spool',body,[x,2.56,3.15]);gate=m.bone(side+'_port_shutter',body,[x,2.63,.24]);ref=m.bone('reference_'+side,root,[x,0,7])
 C=m.cube
 C('forearm',[x-2,-2,-.4],[4,4,8.8],8,ref);C('hand',[x-2,-1.8,-4.5],[4,3.6,4.1],8,ref);C('cuff',[x-2.08,-2.08,7.7],[4.16,4.16,1.4],9,ref)
 # Everything remains proximal to the wrist: no palm / finger components.
 C('rubber_gasket',[x-1.12,2.015,.65],[2.24,.14,5.6],4,body)
 C('black_lower_housing',[x-1.04,2.15,.72],[2.08,.30,5.42],0,body)
 C('recess',[x-.77,2.46,1.05],[1.54,.09,4.75],4,body)
 for sign in [-1,1]:
  C('chamfered_shell',[x+sign*.92-.13,2.44,1.0],[.26,.30,4.9],1,body,r=[0,0,sign*15])
  C('recessed_seam',[x+sign*.91-.035,2.75,1.45],[.07,.025,3.8],2,body)
  C('rear_anchor',[x+sign*.53-.16,2.15,6.0],[.32,.20,.65],0,body,r=[0,sign*12,0])
  for z in [1.35,2.25,4.45,5.6]:
   C('black_oxide_screw',[x+sign*.84-.09,2.76,z],[.18,.06,.18],2,body)
   C('screw_slot',[x+sign*.84-.014,2.824,z+.03],[.028,.008,.12],4,body)
 # Recessed winding drum; cover exposes only its small service window.
 for i in range(12):
  a=2*math.pi*i/12;b=2*math.pi*(i+1)/12
  m.bar('reel_edge',[x+.49*math.cos(a),2.57,3.15+.49*math.sin(a)],[x+.49*math.cos(b),2.57,3.15+.49*math.sin(b)],.10,2,spool)
 C('reel_hub',[x-.15,2.55,3.0],[.30,.10,.30],1,spool)
 for z in [1.35,1.78]:C('feed_roller',[x-.4,2.48,z],[.80,.20,.18],2,body)
 C('feed_channel',[x-.14,2.47,.90],[.28,.04,1.9],4,body)
 C('rear_shell',[x-.73,2.68,4.0],[1.46,.16,1.85],0,lid)
 for j in range(6):C('heat_vent',[x-.55+j*.20,2.845,4.65],[.08,.014,.65],4,lid)
 C('front_shell_left',[x-.72,2.69,1.1],[.47,.14,1.4],0,body)
 C('front_shell_right',[x+.25,2.69,1.1],[.47,.14,1.4],0,body)
 C('status_bezel',[x+.29,2.855,4.00],[.31,.04,.65],2,body)
 for z in [4.05,4.38]:C('red_status',[x+.35,2.90,z],[.19,.015,.22],6,body)
 # Tapered nozzle with a real hollow opening, not a painted black face.
 C('nozzle_neck',[x-.49,2.28,.68],[.98,.53,.42],0,body)
 for i in range(8):
  a=2*math.pi*i/8;b=2*math.pi*(i+1)/8
  m.bar('exit_ring',[x+.23*math.cos(a),2.61+.23*math.sin(a),.34],[x+.23*math.cos(b),2.61+.23*math.sin(b),.34],.10,2,body)
 for sign in [-1,1]:
  C('nozzle_side',[x+sign*.34-.10,2.26,.36],[.20,.68,.50],0,body)
  C('nozzle_lip',[x-.24,2.61+sign*.29-.065,.36],[.48,.13,.50],1,body)
 C('sliding_shutter',[x-.18,2.43,.29],[.36,.36,.035],1,gate)
 return root

def arms_model():
 m=Model();m.bone('root')
 for s,x in [('left',-6),('right',6)]:mechanism(m,s,x)
 for side in ['left','right']:
  for i in range(SEG):
   b=m.bone(f'wire_{side}_{i:02}',side+'_arm');m.cube('filament',[-.065,-.065,0],[.13,.13,1],6,b)
 return m

def ease(x):x=max(0,min(1,x));return x*x*(3-2*x)
def pose(clip,t):
 transforms={};progress=1.;opening=1.
 if clip=='stowed':progress=opening=0.
 elif clip=='deploy':progress=ease((t-.22)/.64);opening=ease((t-.04)/.14)
 elif clip=='retract':progress=1-ease(t/.68);opening=1-ease((t-.72)/.20)
 single=clip in ['attack_left','attack_right'];double=clip=='heavy'
 duration=.72 if single else 1.15
 u=t/duration;release=(ease((u-.06)/.18)*(1-ease((u-.78)/.22))) if single or double else 0.
 sweep=ease((u-.23)/.48);wind=math.sin(math.pi*min(1,u/.28)) if single or double else 0.
 arm_mats={};ports={};guides={};grips={};forwards={}
 for side,x,sg in [('left',-6,-1),('right',6,1)]:
  p=np.zeros(3);r=np.array([-5.,sg*-8,sg*5.])
  if clip in ['deploy','retract','stowed']:
   f=progress;p=[sg*.5*(1-f),-1.2*(1-f),.8*(1-f)];r=[-5*f,sg*-8*f,sg*5*f]
  elif clip=='idle':p[1]=.08*math.sin(2*math.pi*t/2)
  elif single:
   if side==clip.split('_')[1]:
    p=[sg*(1.2-2.1*sweep)*release,(.4+1.1*math.sin(math.pi*sweep))*release,-2.2*release]
    r=[-5-12*release,sg*(-8+(16-32*sweep)*release),sg*(5+(8-16*sweep)*release)]
   else:p=[sg*.35*release,-.3*release,.4*release]
  elif double:
   p=[sg*(.8*wind+.6*release),1.4*wind+.4*release,-2.4*release]
   r=[-5-16*wind-7*release,sg*(-8-6*release),sg*(5+6*wind)]
  transforms[side+'_arm']={'position':list(p),'rotation':list(r)}
  transforms[side+'_port_shutter']={'position':[.45*opening,0,0]}
  transforms[side+'_service_cover']={'rotation':[0,0,0]}
  arm=mat(r,p,[x,0,7]);arm_mats[side]=arm
  mount=mat([0,0,sg*90],pivot=[x,0,0]);ports[side]=tx(arm@mount,[x,2.61,.26])
  forwards[side]=arm[:3,:3]@np.array([0,0,-1.])
  guides[side]=tx(arm,[x-sg*2.61,0,-4.85])
  # Contact on the front face of the normal Minecraft hand. No extra palm hardware.
  grips[side]=tx(arm,[x-sg*1.15,0,-4.57])
 mid=(grips['left']+grips['right'])/2+np.array([0,-.22,-.35])
 for side,x,sg in [('left',-6,-1),('right',6,1)]:
  arm=arm_mats[side];a=ports[side];g=guides[side];grip=grips[side];forward=forwards[side]
  vs=np.linspace(0,1,321);held=[];attack=[]
  for v in vs:
   held.append(grip*(1-v)+mid*v+np.array([0,-.12*math.sin(math.pi*v),-.18*math.sin(math.pi*v)]))
   if double:
    # Two smooth opposite-curvature lobes (S); trailing phase varies along the stroke.
    tip=np.array([sg*4+(sweep-.5)*8,1.6+sg*1.3,-28.])
    base=grip*(1-v)+tip*v
    wave=11.5*math.sin(2*math.pi*v-.35*math.sin(math.pi*sweep))*math.sin(math.pi*v)
    base+=np.array([wave,1.3*math.sin(2*math.pi*v)*math.sin(math.pi*v),0])
    attack.append(base)
   elif single and side==clip.split('_')[1]:
    # Physical RIGHT travels +X to -X; physical LEFT is the exact X mirror.
    tip=np.array([sg*(17-34*sweep),1.5,-21-6*math.sin(math.pi*sweep)])
    c1=grip+np.array([sg*(7-14*sweep),1,-5.])
    c2=tip+np.array([sg*12,2,7.])
    attack.append((1-v)**3*grip+3*(1-v)**2*v*c1+3*(1-v)*v*v*c2+v**3*tip)
   else:
    attack.append(grip+forward*(1.3*v)+np.array([0,-.4*v,0]))
  curve=np.array(held)*(1-release)+np.array(attack)*release
  # Lead remains outside the bare hand; only the section beyond the grip is released.
  dense=np.vstack([np.linspace(a,g,65)[:-1],np.linspace(g,grip,33)[:-1],curve])
  lengths=np.r_[0,np.cumsum(np.linalg.norm(np.diff(dense,axis=0),axis=1))]
  distance=lengths[-1]*progress
  # Fixed segment boundaries at guide and grip prevent subdivisions sliding around a hard corner.
  lead_length=np.linalg.norm(g-a);grip_length=lead_length+np.linalg.norm(grip-g)
  full_samples=np.r_[np.linspace(0,lead_length,9)[:-1],np.linspace(lead_length,grip_length,5)[:-1],np.linspace(grip_length,lengths[-1],SEG-12+1)]
  samples=np.minimum(full_samples,distance)
  ps=np.column_stack([np.interp(samples,lengths,dense[:,j]) for j in range(3)])
  inverse=np.linalg.inv(arm);ps=np.array([tx(inverse,pt) for pt in ps])
  full_ps=np.column_stack([np.interp(full_samples,lengths,dense[:,j]) for j in range(3)])
  full_ps=np.array([tx(inverse,pt) for pt in full_ps])
  transforms[side+'_spool']={'rotation':[0,distance*26,0]}
  visible=min(1.,progress*50)
  for i in range(SEG):
   vec=ps[i+1]-ps[i];L=np.linalg.norm(vec)
   # Hidden segments retain their eventual tangent, avoiding a twist as they emerge.
   tangent=full_ps[i+1]-full_ps[i];tangent_length=np.linalg.norm(tangent)
   rr=[0.,0.,0.] if tangent_length<1e-8 else [-math.degrees(math.asin(np.clip(tangent[1]/tangent_length,-1,1))),math.degrees(math.atan2(tangent[0],tangent[2])),0.]
   seg_visible=visible if L>1e-7 else 0.
   transforms[f'wire_{side}_{i:02}']={'position':ps[i].tolist(),'rotation':rr,'scale':[seg_visible,seg_visible,max(0,L+.018*seg_visible)]}
 return transforms

CLIPS={'stowed':(.1,True),'deploy':(1.1,False),'idle':(2.,True),'attack_right':(.72,False),'attack_left':(.72,False),'heavy':(1.15,False),'retract':(1.,False)}

def animations():
 out={}
 for clip,(duration,loop) in CLIPS.items():
  times=np.linspace(0,duration,round(duration*60)+1);bones={}
  for t in times:
   for b,channels in pose(clip,t).items():
    for ch,v in channels.items():bones.setdefault(b,{}).setdefault(ch,{})[f'{t:.5f}']=[round(float(x),6) for x in v]
  # Euler unwrap prevents long reverse turns across +-180 degrees.
  for b,chs in bones.items():
   if 'rotation' in chs:
    keys=list(chs['rotation']);vs=np.rad2deg(np.unwrap(np.deg2rad([chs['rotation'][k] for k in keys]),axis=0))
    chs['rotation']={k:np.round(v,6).tolist() for k,v in zip(keys,vs)}
  out[clip]={'loop':loop,'animation_length':duration,'bones':bones}
 return out

def make_texture():
 im=Image.new('RGBA',(64,64));glow=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im);gd=ImageDraw.Draw(glow)
 for i,c in enumerate(COLORS):
  x=i%4*16;y=i//4*16;d.rectangle((x,y,x+15,y+15),fill=c)
  if i==6:gd.rectangle((x,y,x+15,y+15),fill='#ff0000');continue
  rgb=tuple(bytes.fromhex(c[1:]));dark=tuple(max(0,a-6) for a in rgb)
  for k in range(9):d.point((x+k*7%16,y+k*11%16),fill=dark)
 for image,n in [(im,'nanowire_red.png'),(glow,'nanowire_red_glowmask.png')]:image.save(A/'textures/nanowire'/n)

def export(m,anims,name,reference=False):
 bones=[b for b in m.bones if reference or not b['name'].startswith('reference_')];bn={b['name'] for b in bones};cubes=[c for c in m.cubes if c['bone'] in bn]
 children={b['name']:[] for b in bones};els=[];groups=[];gb=[]
 for b in bones:
  groups.append({'name':b['name'],'uuid':uid(name+b['name']),'origin':b['pivot'],'rotation':b.get('rotation',[0,0,0]),'visibility':True,'export':not b['name'].startswith('reference_'),'isOpen':True})
  geo={'name':b['name'],'pivot':[-b['pivot'][0],b['pivot'][1],b['pivot'][2]]}
  if b['parent']:geo['parent']=b['parent']
  br=b.get('rotation',[0,0,0]);geo['rotation']=[-br[0],-br[1],br[2]]
  cs=[]
  for index,c in enumerate(cubes):
   if c['bone']!=b['name']:continue
   o=c['origin'];s=c['size'];u=c['material']%4*16;v=c['material']//4*16;id=uid(name+'cube'+str(index))
   el={'name':c['name'],'type':'cube','uuid':id,'from':o,'to':[o[i]+s[i] for i in range(3)],'origin':c.get('pivot',[0,0,0]),'rotation':c.get('rotation',[0,0,0]),'box_uv':False,'autouv':0,'visibility':True,'export':not b['name'].startswith('reference_'),'faces':{f:{'uv':[u,v,u+16,v+16],'texture':0} for f in F}};els.append(el);children[b['name']].append(id)
   cc={'origin':[-o[0]-s[0],o[1],o[2]],'size':s,'uv':{f:{'uv':[u,v],'uv_size':[16,16]} for f in F}}
   for f in ['up','down']:cc['uv'][f]={'uv':[u+16,v+16],'uv_size':[-16,-16]}
   if 'rotation' in c:
    p=c['pivot'];r=c['rotation'];cc.update(pivot=[-p[0],p[1],p[2]],rotation=[-r[0],-r[1],r[2]])
   cs.append(cc)
  if cs:geo['cubes']=cs
  gb.append(geo)
 def tree(b):return {'uuid':uid(name+b['name']),'children':children[b['name']]+[tree(c) for c in bones if c['parent']==b['name']]}
 ba=[];ga={}
 for key,a in anims.items():
  ab={};ag={}
  for b,chs in a['bones'].items():
   if b not in bn:continue
   keys=[];ag[b]={}
   for ch,vs in chs.items():
    ag[b][ch]={}
    for t,v in vs.items():
     keys.append({'channel':ch,'data_points':[dict(zip('xyz',v))],'uuid':uid(name+key+b+ch+t),'time':float(t),'interpolation':'linear'})
     vv=v.copy()
     if ch in ['position','rotation']:vv[0]*=-1
     if ch=='rotation':vv[1]*=-1
     ag[b][ch][t]=vv
   ab[uid(name+b)]={'name':b,'type':'bone','keyframes':keys}
  ba.append({'uuid':uid(name+key),'name':'animation.nanowire.'+key,'loop':'loop' if a['loop'] else 'once','length':a['animation_length'],'snapping':60,'animators':ab})
  ga['animation.nanowire.'+key]={'loop':a['loop'],'animation_length':a['animation_length'],'bones':ag}
 tex=A/'textures/nanowire/nanowire_red.png'
 bb={'meta':{'format_version':'5.0','model_format':'free','box_uv':False},'name':name,'model_identifier':name,'resolution':{'width':64,'height':64},'elements':els,'groups':groups,'outliner':[tree(bones[0])],'textures':[{'name':tex.name,'uuid':uid('texture'),'id':'0','mode':'bitmap','internal':True,'width':64,'height':64,'uv_width':64,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode(tex.read_bytes()).decode()}],'animations':ba}
 save(R/'blockbench'/f'{name}.bbmodel',bb)
 if not reference:
  save(A/'geo'/f'{name}.geo.json',{'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.'+name,'texture_width':64,'texture_height':64,'visible_bounds_width':8,'visible_bounds_height':6,'visible_bounds_offset':[0,0,-.5]},'bones':gb}]})
  if ga:save(A/'animations'/f'{name}.animation.json',{'format_version':'1.8.0','animations':ga})
 return len(cubes),len(bones)

def item_model():
 m=Model();m.bone('root');mechanism(m,'left',-2.0,False);mechanism(m,'right',2.0,False)
 m.bones=[b for b in m.bones if not b['name'].startswith('reference_')];m.cubes=[c for c in m.cubes if not c['bone'].startswith('reference_')]
 m.bone('stored_wire','root')
 for i in range(40):
  a=i*2*math.pi/40;b=(i+1)*2*math.pi/40;m.bar('coiled_red_wire',[3.4*math.cos(a),3.3,3.2+2.4*math.sin(a)],[3.4*math.cos(b),3.3,3.2+2.4*math.sin(b)],.11,6,'stored_wire')
 return m
def attachment_model(source,anims,side):
 """Rebase one implant at its own arm pivot; external renderer owns arm pose."""
 m=Model();m.bone('root');offset=np.array([-6 if side=='left' else 6,0,7],float)
 selected={b['name'] for b in source.bones if b['name'].startswith(side+'_') or b['name'].startswith('wire_'+side+'_')}
 for orig in source.bones:
  if orig['name'] not in selected:continue
  b=copy.deepcopy(orig)
  b['pivot']=[0,0,0] if b['name'].startswith('wire_') else (np.array(b['pivot'])-offset).tolist()
  m.bones.append(b)
 for orig in source.cubes:
  if orig['bone'] not in selected:continue
  c=copy.deepcopy(orig)
  if not c['bone'].startswith('wire_'):
   c['origin']=(np.array(c['origin'])-offset).tolist()
   if 'pivot' in c:c['pivot']=(np.array(c['pivot'])-offset).tolist()
  m.cubes.append(c)
 out={}
 for name,clip in anims.items():
  cc=copy.deepcopy(clip);cc['bones']={k:v for k,v in cc['bones'].items() if k in selected and k!=side+'_arm'}
  for b,chs in cc['bones'].items():
   if b.startswith('wire_'):
    chs['position']={t:(np.array(v)-offset).tolist() for t,v in chs['position'].items()}
  out[name]=cc
 return m,out

if __name__=='__main__':
 make_texture();m=arms_model();a=animations();print('runtime',export(m,a,'nanowire_arms'));print('review',export(m,a,'nanowire_arms_with_reference',True));print('item',export(item_model(),{},'nanowire_item'))
 for side in ['left','right']:
  am,aa=attachment_model(m,a,side);print(side+' attachment',export(am,aa,'nanowire_'+side+'_attachment'))
 save(R/'source/model_spec.json',{'bones':m.bones,'cubes':m.cubes,'clips':CLIPS,'segment_count_per_half':SEG,'red':'#FF0000'})
 save(R/'animation-events.json',{'status':'proposed integration markers; no damage code included','deploy':{'wire_starts':.22,'ready':1.1},'attack_left':{'active_window':[.22,.46],'end':.72},'attack_right':{'active_window':[.22,.46],'end':.72},'heavy':{'active_window':[.30,.95],'end':1.15},'retract':{'hidden':.68,'end':1.0}})
