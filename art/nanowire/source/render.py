"""CPU review renderer. Evaluates exported GeckoLib geometry and keyframes."""
import json,math
from functools import lru_cache
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from build import R,A,rot,mat,COLORS
@lru_cache(maxsize=12)
def load(p):return json.loads(Path(p).read_text())
_tracks={}
def sample(v,t):
 if not isinstance(v,dict):return np.array(v,float)
 track=_tracks.get(id(v))
 if track is None or track[0] is not v:
  keys=sorted((float(k),vv) for k,vv in v.items())
  track=(v,np.array([k for k,_ in keys]),np.array([vv for _,vv in keys],float));_tracks[id(v)]=track
 _,times,values=track
 i=int(np.searchsorted(times,t))
 if i==0:return values[0].copy()
 if i==len(times):return values[-1].copy()
 return values[i-1]+(values[i]-values[i-1])*(t-times[i-1])/(times[i]-times[i-1])

def scene(clip='idle',t=0,reference=True,model='nanowire_arms'):
 geo=load(str(A/'geo'/f'{model}.geo.json'))['minecraft:geometry'][0];ap=A/'animations'/f'{model}.animation.json';anims=load(str(ap))['animations'] if ap.exists() else {};pose=anims.get('animation.nanowire.'+clip,{}).get('bones',{});mats={};out=[]
 for b in geo['bones']:
  p=np.array(b['pivot'],float);p[0]*=-1;ch={k:sample(v,t) for k,v in pose.get(b['name'],{}).items()}
  for k in ['rotation','position']:
   if k in ch:ch[k][0]*=-1
  if 'rotation' in ch:ch['rotation'][1]*=-1
  br=np.array(b.get('rotation',[0,0,0]),float);br[:2]*=-1
  local=mat(ch.get('rotation',br),ch.get('position',[0,0,0]),p,ch.get('scale',[1,1,1]));world=mats.get(b.get('parent'),np.eye(4))@local;mats[b['name']]=world
  if 'scale' in ch and max(ch['scale'])<1e-7:continue
  for c in b.get('cubes',[]):
   o=np.array(c['origin'],float);s=np.array(c['size'],float);o[0]=-o[0]-s[0];m=world
   if 'rotation' in c:
    pp=np.array(c['pivot'],float);pp[0]*=-1;rr=np.array(c['rotation'],float);rr[:2]*=-1;m=m@mat(rr,pivot=pp)
   u,v=c['uv']['north']['uv'];out.append((o,s,int(u//16+v//16*4),m))
 if reference and model=='nanowire_arms':
  spec=load(str(R/'source/model_spec.json'))
  for c in spec['cubes']:
   if c['bone'].startswith('reference_'):out.append((np.array(c['origin']),np.array(c['size']),c['material'],mats[c['bone'].replace('reference_','')+'_arm']))
 return out,mats
OFF=np.array([[0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1]])
FACES=[([0,3,2,1],[0,0,-1]),([4,5,6,7],[0,0,1]),([0,4,7,3],[-1,0,0]),([1,2,6,5],[1,0,0]),([3,7,6,2],[0,1,0]),([0,1,5,4],[0,-1,0])]
def camera(eye,target):
 back=np.array(eye)-target;back=back/np.linalg.norm(back);right=np.cross([0,1,0],back);right=right/np.linalg.norm(right);up=np.cross(back,right);return np.array([right,up,back])
def render(cubes,size=(900,600),eye=(0,25,32),target=(0,0,0),span=26,perspective=True):
 w,h=size;target=np.array(target,float);eye=np.array(eye,float);distance=np.linalg.norm(eye-target);cam=camera(eye,target);rgb=np.zeros((h,w,3),np.uint8);rgb[:]=[21,29,38];depth=np.full((h,w),-np.inf);tex=np.array(Image.open(A/'textures/nanowire/nanowire_red.png').convert('RGB'));light=np.array([-.25,.9,.45]);light/=np.linalg.norm(light)
 for o,s,material,m in cubes:
  vs=(np.c_[o+OFF*s,np.ones(8)]@m.T)[:,:3];pv=(vs-target)@cam.T;factor=distance/np.maximum(.1,distance-pv[:,2]) if perspective else np.ones(8);project=np.c_[w/2+pv[:,0]*w/span*factor,h/2-pv[:,1]*w/span*factor,pv[:,2]]
  for ids,normal in FACES:
   n=m[:3,:3]@normal;nn=np.linalg.norm(n)
   if nn<1e-9:continue
   n=n/nn
   if n@(eye-vs[ids].mean(axis=0) if perspective else cam[2])<-.001:continue
   bright=1 if material==6 else .65+.35*max(0,n@light);p=project[ids];uv=np.array([[0,0],[15,0],[15,15],[0,15]])
   for triids in [[0,1,2],[0,2,3]]:
    tri=p[triids];tuv=uv[triids];xmin=max(0,int(np.floor(tri[:,0].min())));xmax=min(w-1,int(np.ceil(tri[:,0].max())));ymin=max(0,int(np.floor(tri[:,1].min())));ymax=min(h-1,int(np.ceil(tri[:,1].max())))
    if xmax<xmin or ymax<ymin:continue
    a,b,c=tri;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
    if abs(den)<1e-9:continue
    yy,xx=np.mgrid[ymin:ymax+1,xmin:xmax+1];xx=xx+.5;yy=yy+.5;l0=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;l1=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;l2=1-l0-l1;
    if perspective:
     q0=l0/(distance-a[2]);q1=l1/(distance-b[2]);q2=l2/(distance-c[2]);denq=np.maximum(1e-9,q0+q1+q2);l0=q0/denq;l1=q1/denq;l2=q2/denq
    zz=l0*a[2]+l1*b[2]+l2*c[2];dep=depth[ymin:ymax+1,xmin:xmax+1];mask=(l0>=0)&(l1>=0)&(l2>=0)&(zz>dep)
    uu=np.clip(np.rint(l0*tuv[0,0]+l1*tuv[1,0]+l2*tuv[2,0]),0,15).astype(int);vv=np.clip(np.rint(l0*tuv[0,1]+l1*tuv[1,1]+l2*tuv[2,1]),0,15).astype(int);pixels=tex[material//4*16+vv,material%4*16+uu]*bright;rgb[ymin:ymax+1,xmin:xmax+1][mask]=np.clip(pixels[mask],0,255).astype('uint8');dep[mask]=zz[mask]
 return Image.fromarray(rgb)
def font(n):return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',n)
def poster():
 im=Image.new('RGB',(1600,1160),'#10161d');d=ImageDraw.Draw(im);d.text((30,24),'NANOCABLE / ROJO PURO',font=font(34),fill='#f1e9e9');d.text((32,74),'v0.3 · Antebrazo interior · Carcasa negra · Cable sujeto en reposo · Golpe doble en S',font=font(19),fill='#aebac5')
 panels=[('01 / CARCASA Y BOQUILLA','item',0,False,(12,18,-12),(2,2.5,3),9),('02 / REPOSO — CABLE ENTRE MANOS','idle',0,True,(0,25,32),(0,1,1),25),('03 / CLICK IZQ. 1 — BRAZO DERECHO','attack_right',.36,True,(0,30,36),(0,1,-7),36),('04 / CLICK IZQ. 2 — BRAZO IZQUIERDO','attack_left',.36,True,(0,30,36),(0,1,-7),36),('05 / CLICK DER. — DOS CURVAS EN S','heavy',.64,True,(0,30,36),(0,1,-7),36),('06 / OBJETO DE INVENTARIO','item',0,False,(17,23,25),(0,2,3),13)]
 for i,(title,clip,t,ref,eye,target,span) in enumerate(panels):
  x=25+i%3*525;y=128+i//3*470;cubes,_=scene(clip,t,ref,model='nanowire_item' if clip=='item' else 'nanowire_arms')
  if i==0:cubes=[c for c in cubes if c[0][0]>0.5 and c[2]!=6] # keep only solid hardware for detail
  picture=render(cubes,(505,405),eye,target,span);im.paste(picture,(x,y+36));d.text((x+4,y),title,font=font(18),fill='#dce4eb')
 d.text((32,1090),'Geometría y poses leídas de los JSON exportados. Brazos de muestra; no es una captura de Minecraft.',font=font(17),fill='#a4b3c1');d.text((32,1120),'7 clips, sin inspección ni piezas en las manos. Integración Punchy y prueba en Minecraft pendientes.',font=font(17),fill='#a4b3c1');im.save(R/'previews/nanowire-modelo.png')
def motion():
 frames=[];durations=[];timeline=[('stowed',0,100)]*3+[(c,float(t),int(round(d*1000/n/10))*10) for c,d,n in [('deploy',1.1,18),('idle',2,12),('attack_right',.72,25),('idle',.5,8),('attack_left',.72,25),('idle',.5,8),('heavy',1.15,38),('idle',.5,8),('retract',1,16)] for t in np.linspace(0,d,n)]
 for clip,t,ms in timeline:
  cubes,_=scene(clip,t);im=render(cubes,(800,500),(0,30,36),(0,1,-5),38);d=ImageDraw.Draw(im);d.text((22,18),'NANOCABLE / '+{'attack_right':'CLICK IZQ. 1 - DERECHA','attack_left':'CLICK IZQ. 2 - IZQUIERDA','heavy':'CLICK DER. - DOBLE'}.get(clip,clip.upper()),font=font(22),fill='#ffffff');d.text((22,473),'JSON v0.3 · Agarre en reposo · Barridos opuestos · Doble en S',font=font(14),fill='#aab9c8');frames.append(im);durations.append(ms)
 frames[0].save(R/'previews/nanowire-movimiento.gif',save_all=True,append_images=frames[1:],duration=durations,loop=0,optimize=True)
if __name__=='__main__':poster();motion();print('Preview + animation rendered from exports.')
