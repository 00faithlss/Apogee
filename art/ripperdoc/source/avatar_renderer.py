"""Original editable cyberware UI asset. Python 3 + Pillow + numpy.
All previews are rendered from the saved BBModel cube geometry and textures.
"""
from scipy.spatial.transform import Rotation
from pathlib import Path
import base64, io, json, uuid, math, argparse
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
def save(im,path):
    b=io.BytesIO(); im.save(b,format='PNG'); Path(path).write_bytes(b.getvalue())
def uid(): return str(uuid.uuid4())
def font(n): return ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',n)
COLORS=['#d7e8de','#8daaa9','#466477','#ff8e9c','#d95270','#81eeed','#182938','#edb865','#dd535b','#eefbf4']
FACES=['north','south','west','east','up','down']
def texture(im,name):
    b=io.BytesIO(); im.save(b,format='PNG')
    return dict(name=name,uuid=uid(),id=name,mode='bitmap',internal=True,uv_width=im.width,uv_height=im.height,width=im.width,height=im.height,render_mode='default',render_sides='front',source='data:image/png;base64,'+base64.b64encode(b.getvalue()).decode())
def palette():
    im=Image.new('RGBA',(64,64)); d=ImageDraw.Draw(im)
    for i,c in enumerate(COLORS):
        x=i%4*16;y=i//4*16;d.rectangle((x,y,x+15,y+15),fill=c)
        rgb=tuple(int(c[j:j+2],16) for j in (1,3,5))
        for k in range(8):
            xx=x+(k*7)%16; yy=y+(k*11)%16
            d.rectangle((xx,yy,min(xx+1,x+15),min(yy+1,y+15)),fill=tuple(max(0,v-12) for v in rgb)+(255,))
    return im
def skin_sample():
    # Original neutral skin; 64x64 Minecraft classic UVs, no imported assets.
    im=Image.new('RGBA',(64,64),(0,0,0,0));d=ImageDraw.Draw(im)
    for box,c in [((0,0,31,15),'#be957e'),((16,16,39,31),'#377b88'),((40,16,55,31),'#be957e'),((32,48,47,63),'#be957e'),((0,16,15,31),'#3e4b64'),((16,48,31,63),'#3e4b64')]:d.rectangle(box,fill=c)
    d.rectangle((0,0,31,7),fill='#493d43');d.rectangle((8,8,15,9),fill='#493d43')
    d.rectangle((8,10,8,12),fill='#493d43');d.rectangle((15,10,15,12),fill='#493d43')
    for x in (10,13):d.point((x,11),fill='#b4e9e8');d.point((x,12),fill='#35374a')
    d.rectangle((11,14,12,14),fill='#8b5c5d')
    for x,y in [(4,20),(20,52)]: d.rectangle((x,y+10,x+3,y+11),fill='#20293c')
    return im
def translucent(im,alpha=.23):
    a=np.array(im.convert('RGBA'));a[:,:,3]=(a[:,:,3].astype(float)*alpha).astype('uint8');return Image.fromarray(a)
def skin_uv(x,y,w,h,d):
    return {'up':[x+d,y,x+d+w,y+d],'down':[x+d+w,y,x+d+2*w,y+d],
      'west':[x,y+d,x+d,y+d+h],'north':[x+d,y+d,x+d+w,y+d+h],
      'east':[x+d+w,y+d,x+2*d+w,y+d+h],'south':[x+2*d+w,y+d,x+2*d+2*w,y+d+h]}
class Model:
    def __init__(self,textures):self.elements=[];self.groups=[];self.outliner=[];self.nodes={};self.textures=textures
    def group(self,name,parent=None):
        u=uid();g=dict(name=name,uuid=u,origin=[0,0,0],rotation=[0,0,0],visibility=True,export=True,isOpen=True,color=0)
        node=dict(uuid=u,isOpen=True,children=[]);self.groups.append(g);self.nodes[name]=node
        (self.nodes[parent]['children'] if parent else self.outliner).append(node)
    def cube(self,name,a,b,mat=0,group='skeleton',uv=None,tex=0):
        if uv is None:
            x=mat%4*16;y=mat//4*16;uv={f:[x,y,x+16,y+16] for f in FACES}
        e=dict(name=name,type='cube',uuid=uid(),origin=[0,0,0],rotation=[0,0,0],**{'from':a,'to':b},box_uv=False,autouv=0,color=0,visibility=True,export=True,faces={f:{'uv':uv.get(f,[0,0,0,0]),'texture':tex if f in uv else None} for f in FACES})
        self.elements.append(e);self.nodes[group]['children'].append(e['uuid'])
    def data(self,name):return dict(meta=dict(format_version='5.0',model_format='free',box_uv=False),name=name,model_identifier='ripperdoc_xray',visible_box=[3,3,1],resolution=dict(width=64,height=64),elements=self.elements,groups=self.groups,outliner=self.outliner,textures=self.textures,animations=[])

def anatomy(skin):
    m=Model([texture(palette(),'anatomia.png'),texture(translucent(skin),'skin_translucida.png'),texture(skin,'skin_original.png')])
    m.group('AVATAR')
    for g in ['skeleton','brain','nerves','implants','skin']:m.group(g,'AVATAR')
    c=m.cube
    # Open skull frame, leaving the brain readable. This is stylized game anatomy.
    for x in [-2.9,2.25]:c('cranium_side',[x,25.2,-2.7],[x+.65,30.6,2.6],1)
    c('occipital',[-2.9,25.4,2.1],[2.9,30.3,2.8],1)
    for x in [-2.8,1.1]:c('orbital_brow',[x,27.25,-2.85],[x+1.7,27.8,-2.25],0)
    for x in [-2.65,2.0]:c('cheekbone',[x,25.9,-2.8],[x+.65,27.2,-2.2],0)
    c('nasal_bridge',[-.3,26.25,-2.8],[.3,27.6,-2.15],0)
    c('jaw',[-2.35,24.8,-2.4],[2.35,25.35,1.9],0)
    for x in np.arange(-1.7,2,.48):c('tooth',[float(x),25.25,-2.75],[float(x+.34),25.8,-2.28],9)
    # Voxel cerebral lobes, central fissure and raised folds.
    for side in [-1,1]:
        x0=-2.1 if side<0 else .18
        c('brain_lobe',[x0,28,-1.9],[x0+1.92,30.35,1.95],3,'brain')
        c('brain_crown',[x0+.25,30.35,-1.45],[x0+1.65,30.8,1.45],3,'brain')
        for k in range(5):
            z=-1.6+k*.7
            c('cerebral_fold',[x0+.05,30.22,z],[x0+1.82,30.57,z+.27],4 if k%2 else 3,'brain')
            c('cerebral_fold_front',[x0+.25+(k%2)*.35,28.3+k*.34,-2.05],[x0+1.5,28.5+k*.34,-1.87],4,'brain')
    c('cerebellum',[-1.5,26.65,1.1],[1.5,27.85,2.05],4,'brain')
    c('brain_stem',[-.38,23.5,.6],[.38,27.2,1.4],5,'nerves')
    # Vertebrae + cyan spinal cord.
    for i in range(16):
        y=12.6+i*.71;c('vertebra_%02d'%i,[-.72,y,.62],[.72,y+.48,1.65],0)
        c('spinous_process',[-.22,y+.05,1.6],[.22,y+.35,1.97],1)
    c('spinal_cord',[-.22,12.4,.1],[.22,24.3,.55],5,'nerves')
    c('sternum',[-.38,17.7,-1.8],[.38,22.6,-1.15],0)
    c('sacrum',[-.65,12.5,.5],[.65,15,1.3],1)
    for side in [-1,1]:
        lo,hi=sorted([side*.6,side*3.05]);c('clavicle',[lo,22.7,-1.55],[hi,23.25,-.8],0)
        for i in range(6):
            y=18+i*.73;reach=2.65-(abs(i-3)*.15)
            lo,hi=sorted([side*.45,side*reach]);c('rib_front',[lo,y,-1.65],[hi,y+.29,-1.15],0)
            x=side*reach;c('rib_side',[x-.18,y,-1.3],[x+.18,y+.32,1.15],0)
            lo,hi=sorted([side*.65,side*reach]);c('rib_back',[lo,y,.95],[hi,y+.3,1.35],1)
        lo,hi=sorted([side*.7,side*2.9]);c('pelvis_wing',[lo,12.6,-1.1],[hi,14.4,1.2],0)
        lo,hi=sorted([side*.45,side*2.4]);c('pelvis_rim',[lo,12.05,-1.3],[hi,12.55,.8],0)
        x=side*6.15
        c('shoulder_socket',[x-.95,22.2,-.9],[x+.95,23.6,.9],1)
        c('humerus',[x-.44,18.5,-.4],[x+.44,22.4,.5],0)
        c('elbow',[x-.68,17.7,-.65],[x+.68,18.5,.65],1)
        for dx in [-.65,.35]:c('radius_ulna',[x+dx,13.4,-.34],[x+dx+.3,17.75,.4],0)
        c('carpals',[x-.8,12.8,-.6],[x+.8,13.4,.6],1)
        for dx in [-.7,-.23,.24,.71]:c('hand_bone',[x+dx,12.15,-.35],[x+dx+.2,12.85,.35],0)
        leg=side*2.05
        c('femur',[leg-.48,6.8,-.35],[leg+.48,12.4,.6],0)
        c('knee',[leg-.72,6.0,-.85],[leg+.72,6.8,.55],1)
        for dx in [-.52,.37]:c('tibia_fibula',[leg+dx,.9,-.35],[leg+dx+.33,6.05,.5],0)
        c('foot',[leg-.85,.35,-1.6],[leg+.85,1.0,.65],0)
        # Visible implant housings and electrodes, both forearms.
        c('mantis_housing',[x-.86,14.05,.7],[x+.86,17.6,1.55],6,'implants')
        for k in range(4):c('electrode',[x-.68,14.4+k*.75,-.66],[x+.68,14.6+k*.75,-.39],5,'implants')
        c('arm_bus',[x+.95,13.4,.6],[x+1.14,22.8,.83],5,'nerves')
    c('cortex_port',[1.0,28.6,-2.2],[1.9,29.5,-1.9],7,'implants')
    c('cardiac_pump',[.65,19.1,-.9],[1.75,20.4,.5],8,'implants')
    c('optic_left',[-2.3,26.5,-2.75],[-1.25,27.05,-2.4],5,'implants')
    c('optic_right',[1.25,26.5,-2.75],[2.3,27.05,-2.4],5,'implants')
    # Correct classic (4-pixel arm) per-face UVs. Original texture retained for swapping.
    for name,a,b,uv in [
      ('head',[-4,24,-4],[4,32,4],skin_uv(0,0,8,8,8)),
      ('torso',[-4,12,-2],[4,24,2],skin_uv(16,16,8,12,4)),
      ('right_arm',[-8.15,12,-2],[-4.15,24,2],skin_uv(40,16,4,12,4)),
      ('left_arm',[4.15,12,-2],[8.15,24,2],skin_uv(32,48,4,12,4)),
      ('right_leg',[-4.05,0,-2],[-.05,12,2],skin_uv(0,16,4,12,4)),
      ('left_leg',[.05,0,-2],[4.05,12,2],skin_uv(16,48,4,12,4))]:
        c('skin_'+name,a,b,group='skin',uv=uv,tex=1)
    # Optional outer skin layer (hat/jacket/sleeves/trousers), preserving alpha holes.
    for name,a,b,uv in [
      ('hat',[-4.5,23.5,-4.5],[4.5,32.5,4.5],skin_uv(32,0,8,8,8)),
      ('jacket',[-4.25,11.75,-2.25],[4.25,24.25,2.25],skin_uv(16,32,8,12,4)),
      ('right_sleeve',[-8.4,11.75,-2.25],[-3.9,24.25,2.25],skin_uv(40,32,4,12,4)),
      ('left_sleeve',[3.9,11.75,-2.25],[8.4,24.25,2.25],skin_uv(48,48,4,12,4)),
      ('right_trouser',[-4.3,-.25,-2.25],[.2,12.25,2.25],skin_uv(0,32,4,12,4)),
      ('left_trouser',[-.2,-.25,-2.25],[4.3,12.25,2.25],skin_uv(0,48,4,12,4))]:
        c('skin_overlay_'+name,a,b,group='skin',uv=uv,tex=1)
    return m

def ui():
    im=Image.new('RGBA',(1600,1000),'#080f18');d=ImageDraw.Draw(im)
    red='#f36a74';cyan='#85e8df';muted='#748b9c';light='#dce7ec';line='#253b48'
    for x in range(480,1120,32):d.line((x,148,x,887),fill='#101f2b')
    for y in range(160,890,32):d.line((480,y,1120,y),fill='#101f2b')
    d.rectangle((30,26,37,88),fill=red);d.text((55,25),'MATASANOS',font=font(34),fill=light)
    d.text((56,69),'C I B E R W A R E   /   DIAGNÓSTICO CORPORAL',font=font(15),fill=red)
    d.text((1180,35),'MANTIS / CLINIC 01',font=font(20),fill=cyan)
    d.text((1180,67),'VISTA DE DISEÑO · v0.1',font=font(13),fill=muted)
    d.line((30,112,1570,112),fill=red,width=2)
    d.text((38,135),'01 / SISTEMAS',font=font(17),fill=muted)
    labels=[('CORTEZA FRONTAL','Procesamiento neural'),('SISTEMA OCULAR','Ópticas y escáner'),('SISTEMA OPERATIVO','Módulo central'),('BRAZOS','Mantis · izquierda + derecha'),('ESQUELETO','Estructura y articulaciones'),('CIRCULATORIO','Bomba y circulación'),('PIERNAS','Tendones y movilidad')]
    for i,(a,b) in enumerate(labels):
        y=178+i*94;active=i==3
        d.rectangle((35,y,426,y+79),fill='#252532' if active else '#101d28',outline=red if active else line,width=2 if active else 1)
        d.rectangle((49,y+19,84,y+54),outline=red if active else cyan,width=1)
        d.text((59,y+24),str(i+1),font=font(18),fill=red if active else cyan)
        d.text((101,y+14),a,font=font(17),fill=light);d.text((102,y+44),b,font=font(13),fill=muted)
        d.rectangle((386,y+28,402,y+44),outline=red if active else muted)
    d.text((554,141),'02 / AVATAR · CAPAS ANATÓMICAS',font=font(17),fill=cyan)
    d.text((577,173),'SKIN 23%     /     ESTRUCTURA 100%',font=font(13),fill=muted)
    d.ellipse((609,821,991,878),outline='#2b5663',width=2)
    d.line((800,218,800,854),fill='#264450',width=1)
    # Clear routing labels beside the model; leader endpoints match front view.
    for title,pts,pos in [('NEURAL',[(707,275),(627,275),(610,251)],(509,232)),('ÓPTICAS',[(851,314),(975,314),(989,287)],(984,265)),('COSTILLAS',[(743,447),(640,447),(621,422)],(493,401)),('MANTIS / L',[(678,532),(606,532),(586,558)],(491,566)),('MANTIS / R',[(919,532),(989,532),(1009,558)],(986,566)),('SOPORTE',[(852,699),(977,699),(993,724)],(988,733))]:
        d.line(pts,fill='#4ba5ad',width=2);x,y=pts[0];d.rectangle((x-3,y-3,x+3,y+3),fill=cyan);d.text(pos,title,font=font(13),fill=cyan)
    d.rectangle((1150,144,1566,883),fill='#101c28',outline=line)
    d.text((1173,169),'03 / IMPLANTE SELECCIONADO',font=font(15),fill=muted)
    d.text((1173,212),'MANTIS BLADES',font=font(28),fill=light)
    d.text((1174,253),'BRAZOS   /   INSTALACIÓN BILATERAL',font=font(12),fill=red)
    # Original angular technical icon, not an imported promotional image.
    for off in [0,32]:
        d.line((1190,365+off,1233,326+off,1518,305+off),fill=cyan if off else red,width=4)
        d.line((1190,365+off,1270,381+off,1320,358+off),fill='#90a8b6',width=5)
    d.line((1174,445,1543,445),fill=line)
    d.text((1174,464),'VARIANTE',font=font(13),fill=muted)
    for x,label,active in [(1174,'FÍSICA',False),(1298,'TÉRMICA',False),(1422,'ELÉCTRICA',True)]:
        d.rectangle((x,491,x+111,533),fill='#203d48' if active else '#101c28',outline=cyan if active else line)
        d.text((x+10,506),label,font=font(12),fill=cyan if active else muted)
    for y,a,b in [(565,'ALOJAMIENTO','Ambos antebrazos'),(607,'EFECTO','Descarga eléctrica'),(649,'CAPACIDAD','Por configurar')]:
        d.text((1174,y),a,font=font(12),fill=muted);d.text((1174,y+18),b,font=font(17),fill=light)
    d.rectangle((1174,721,1541,774),fill=red);d.text((1260,737),'INSTALAR',font=font(20),fill='#10121b')
    d.rectangle((1174,789,1541,839),outline=line);d.text((1263,805),'RETIRAR',font=font(18),fill=muted)
    d.line((30,909,1570,909),fill=line)
    d.text((37,931),'CAPACIDAD',font=font(13),fill=muted)
    for i in range(18):d.rectangle((164+i*15,934,174+i*15,947),fill=cyan if i<8 else '#223541')
    d.text((460,932),'VALORES DE MUESTRA',font=font(12),fill=muted)
    d.text((888,932),'SKIN SUSTITUIBLE   ·   HUESOS + CEREBRO 3D',font=font(13),fill=cyan)
    d.text((37,974),'MAQUETA VISUAL · Los controles y la instalación se conectan después al mod Fabric.',font=font(11),fill=muted)
    return im

# Renderer: opaque anatomy depth pass, sorted translucent skin pass. Reads BBModel.
OFF=np.array([[0,0,0],[1,0,0],[1,1,0],[0,1,0],[0,0,1],[1,0,1],[1,1,1],[0,1,1]])
FACE={ 'north':([3,2,1,0],[0,0,-1]),'south':([6,7,4,5],[0,0,1]),'west':([7,3,0,4],[-1,0,0]),'east':([2,6,5,1],[1,0,0]),'up':([7,6,2,3],[0,1,0]),'down':([0,1,5,4],[0,-1,0])}
def render(path,size=(680,740),yaw=-8,pitch=3,span=37,center=(0,16,0),skin=True,opacity=None):
    model=json.loads(Path(path).read_text());textures=[np.array(Image.open(io.BytesIO(base64.b64decode(t['source'].split(',')[1]))).convert('RGBA')) for t in model['textures']]
    if opacity is not None:textures[1][:,:,3]=(textures[2][:,:,3].astype(float)*opacity).astype('uint8')
    w,h=size;rgb=np.zeros((h,w,4),float);depth=np.full((h,w),-np.inf)
    a,b=np.radians([yaw,pitch]);R=np.array([[np.cos(a),0,np.sin(a)],[0,1,0],[-np.sin(a),0,np.cos(a)]]);R=np.array([[1,0,0],[0,np.cos(b),-np.sin(b)],[0,np.sin(b),np.cos(b)]])@R
    fs=[]
    for el in model['elements']:
        if el['name'].startswith('UI_') or (not skin and el['name'].startswith('skin_')):continue
        Q=Rotation.from_euler('xyz',el.get('rotation',[0,0,0]),degrees=True).as_matrix()
        pivot=np.array(el.get('origin',[0,0,0]));raw=np.array(el['from'])+OFF*(np.array(el['to'])-el['from'])
        v=((raw-pivot)@Q.T+pivot-center)@R.T
        for f,face in el['faces'].items():
            if face['texture'] is None:continue
            ids,n=FACE[f];n=R@Q@np.array(n)
            if n[2]>.0001:continue
            verts=v[ids];p=np.c_[w/2+verts[:,0]*h/span,h/2-verts[:,1]*h/span,-verts[:,2]]
            u0,v0,u1,v1=face['uv'];uv=np.array([[u0,v0],[u1,v0],[u1,v1],[u0,v1]])
            tex=face['texture'];brightness=.75+.25*max(0,np.dot(n,[-.3,.65,-.7]));alpha=tex==1
            fs.append((alpha,float(p[:,2].mean()),p,uv,textures[tex],brightness))
    fs.sort(key=lambda f:(f[0],f[1]))
    for alpha,zmean,p,uv,tex,bright in fs:
        for ids in [[0,1,2],[0,2,3]]:
            tri=p[ids];tx=uv[ids];xmin=max(0,int(np.floor(tri[:,0].min())));xmax=min(w-1,int(np.ceil(tri[:,0].max())));ymin=max(0,int(np.floor(tri[:,1].min())));ymax=min(h-1,int(np.ceil(tri[:,1].max())))
            if xmin>xmax or ymin>ymax:continue
            aa,bb,cc=tri;den=(bb[1]-cc[1])*(aa[0]-cc[0])+(cc[0]-bb[0])*(aa[1]-cc[1])
            if abs(den)<1e-8:continue
            Y,X=np.mgrid[ymin:ymax+1,xmin:xmax+1];X=X+.5;Y=Y+.5
            l0=((bb[1]-cc[1])*(X-cc[0])+(cc[0]-bb[0])*(Y-cc[1]))/den;l1=((cc[1]-aa[1])*(X-cc[0])+(aa[0]-cc[0])*(Y-cc[1]))/den;l2=1-l0-l1
            z=l0*aa[2]+l1*bb[2]+l2*cc[2];dep=depth[ymin:ymax+1,xmin:xmax+1];mask=(l0>=0)&(l1>=0)&(l2>=0)&(z>dep+1e-6)
            U=np.clip(np.floor(l0*tx[0,0]+l1*tx[1,0]+l2*tx[2,0]),0,tex.shape[1]-1).astype(int);V=np.clip(np.floor(l0*tx[0,1]+l1*tx[1,1]+l2*tx[2,1]),0,tex.shape[0]-1).astype(int)
            pix=tex[V,U].astype(float);mask &=pix[:,:,3]>0
            sub=rgb[ymin:ymax+1,xmin:xmax+1];A=pix[:,:,3:4]/255;src=pix[:,:,:3]*bright
            # Work in premultiplied RGBA, unpremultiply once at output.
            out=src*A+sub[:,:,:3]*(1-A);oa=pix[:,:,3]+sub[:,:,3]*(1-A[:,:,0])
            sub[:,:,:3][mask]=out[mask];sub[:,:,3][mask]=oa[mask]
            if not alpha:dep[mask]=z[mask]
    a=rgb[:,:,3:4]/255;rgb[:,:,:3]=np.divide(rgb[:,:,:3],a,out=np.zeros_like(rgb[:,:,:3]),where=a>0)
    return Image.fromarray(np.uint8(np.clip(rgb,0,255)))

def main(skin_path=None):
    for folder in ['textures','previews']: (ROOT/folder).mkdir(exist_ok=True)
    skin=Image.open(skin_path).convert('RGBA') if skin_path else skin_sample()
    if skin.size!=(64,64):raise ValueError('Se necesita una skin classic 64x64; Slim no está soportada en esta v0.1.')
    save(skin,ROOT/'textures/skin_original.png');save(translucent(skin),ROOT/'textures/skin_translucida.png');save(palette(),ROOT/'textures/anatomia.png')
    m=anatomy(skin);avatar=ROOT/'jugador-rayos-x.bbmodel';avatar.write_text(json.dumps(m.data('Jugador / diagnóstico por capas'),ensure_ascii=False,indent=2))
    layout=ui();save(layout,ROOT/'textures/interfaz.png');m.textures.append(texture(layout,'interfaz.png'));m.group('UI_REFERENCE')
    # Scale 20 screen pixels per model unit, front orthographic camera.
    m.cube('UI_background',[-40,-9,5.5],[40,41,5.51],group='UI_REFERENCE',tex=3,uv={'north':[0,0,1600,1000]})
    (ROOT/'matasanos-interfaz.bbmodel').write_text(json.dumps(m.data('Matasanos / composición de interfaz'),ensure_ascii=False,indent=2))
    preview=layout.copy();player=render(avatar,(680,740),span=37,yaw=-8,pitch=3)
    preview.alpha_composite(player,(460,140));save(preview,ROOT/'previews/matasanos-interfaz.png')
    sheet=Image.new('RGBA',(1600,1000),'#0b1420');d=ImageDraw.Draw(sheet)
    d.text((40,28),'AVATAR / ANATOMÍA EDITABLE',font=font(32),fill='#e1eaf0')
    d.text((42,77),'Geometría real del .bbmodel · piel, cerebro, esqueleto, nervios e implantes en grupos separados',font=font(17),fill='#8096a8')
    for pos,title,opts in [((25,145),'01 / SKIN TRANSLÚCIDA',dict(size=(500,740),span=36)),((550,145),'02 / ESTRUCTURA INTERNA',dict(size=(500,740),span=36,yaw=22,skin=False)),((1080,180),'03 / CEREBRO + CRÁNEO',dict(size=(485,420),span=10.5,yaw=-22,pitch=18,center=(0,28,0),skin=False))]:
        im=render(avatar,**opts);sheet.alpha_composite(im,pos);d.text((pos[0]+15,pos[1]-30),title,font=font(19),fill='#85e8df')
    for y,t in [(653,'• Dos lóbulos y pliegues cúbicos'),(691,'• Cráneo abierto para lectura interna'),(729,'• Costillas, vértebras y pelvis'),(767,'• Huesos de brazos y piernas'),(805,'• Puertos de Mantis bilaterales')]:d.text((1090,y),t,font=font(16),fill='#b8c9d2')
    d.text((42,956),'Skin original de muestra · Classic 64×64 · Anatomía estilizada para un videojuego',font=font(17),fill='#8199aa');save(sheet,ROOT/'previews/anatomia-detalle.png')
    print(f'Built {len(m.elements)-1} avatar cubes / {len(m.groups)-1} groups; two editable BBModels and geometry previews.')
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--skin');args=p.parse_args();main(args.skin)
