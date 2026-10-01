from pathlib import Path
import sys,json,base64,io,copy,uuid,unicodedata
from PIL import Image,ImageDraw
from avatar_renderer import render,texture
ROOT=Path(__file__).resolve().parents[1];W,H=480,356
# Original 5x7 bitmap alphabet, rendered at native GUI pixels.
PATTERNS={
'A':'01110/10001/10001/11111/10001/10001/10001','B':'11110/10001/10001/11110/10001/10001/11110','C':'01111/10000/10000/10000/10000/10000/01111','D':'11110/10001/10001/10001/10001/10001/11110','E':'11111/10000/10000/11110/10000/10000/11111','F':'11111/10000/10000/11110/10000/10000/10000','G':'01111/10000/10000/10111/10001/10001/01111','H':'10001/10001/10001/11111/10001/10001/10001','I':'111/010/010/010/010/010/111','J':'00111/00010/00010/00010/10010/10010/01100','K':'10001/10010/10100/11000/10100/10010/10001','L':'10000/10000/10000/10000/10000/10000/11111','M':'10001/11011/10101/10101/10001/10001/10001','N':'10001/11001/10101/10011/10001/10001/10001','O':'01110/10001/10001/10001/10001/10001/01110','P':'11110/10001/10001/11110/10000/10000/10000','Q':'01110/10001/10001/10001/10101/10010/01101','R':'11110/10001/10001/11110/10100/10010/10001','S':'01111/10000/10000/01110/00001/00001/11110','T':'11111/00100/00100/00100/00100/00100/00100','U':'10001/10001/10001/10001/10001/10001/01110','V':'10001/10001/10001/10001/10001/01010/00100','W':'10001/10001/10001/10101/10101/10101/01010','X':'10001/10001/01010/00100/01010/10001/10001','Y':'10001/10001/01010/00100/00100/00100/00100','Z':'11111/00001/00010/00100/01000/10000/11111',
'0':'01110/10001/10011/10101/11001/10001/01110','1':'010/110/010/010/010/010/111','2':'01110/10001/00001/00010/00100/01000/11111','3':'11110/00001/00001/01110/00001/00001/11110','4':'10010/10010/10010/11111/00010/00010/00010','5':'11111/10000/10000/11110/00001/00001/11110','6':'01110/10000/10000/11110/10001/10001/01110','7':'11111/00001/00010/00100/01000/01000/01000','8':'01110/10001/10001/01110/10001/10001/01110','9':'01110/10001/10001/01111/00001/00001/01110',
'-':'000/000/000/111/000/000/000','/':'00001/00001/00010/00100/01000/10000/10000',':':'0/1/1/0/1/1/0','%':'11001/11010/00100/00100/01011/10011/00000','+':'000/010/010/111/010/010/000','?':'1110/0001/0010/0100/0100/0000/0100','<':'001/010/100/010/001/000/000','>':'100/010/001/010/100/000/000'}
def norm(s):return ''.join(c for c in unicodedata.normalize('NFD',s.upper()) if not unicodedata.combining(c))
def width(s):return sum(4 if c==' ' else len(PATTERNS.get(c,PATTERNS['?']).split('/')[0])+1 for c in norm(s))-1
def text(d,xy,s,color='#afc1ca',center=False,shadow=False):
 x,y=xy
 if center:x-=width(s)//2
 if shadow:text(d,(x+1,y+1),s,'#202020')
 for c in norm(s):
  if c==' ':x+=4;continue
  pat=PATTERNS.get(c,PATTERNS['?']).split('/')
  for yy,row in enumerate(pat):
   for xx,v in enumerate(row):
    if v=='1':d.point((x+xx,y+yy),fill=color)
  x+=len(pat[0])+1

def inset(d,x,y,w,h,fill='#8b8b8b'):
 d.rectangle((x,y,x+w-1,y+h-1),fill='#17242c');d.rectangle((x+1,y+1,x+w-2,y+h-2),fill='#0b1219');d.line((x,y,x+w-2,y),fill='#080e14');d.line((x,y,x,y+h-2),fill='#080e14');d.line((x+1,y+h-1,x+w-1,y+h-1),fill='#49636e');d.line((x+w-1,y+1,x+w-1,y+h-1),fill='#49636e')
def button(d,x,y,w,h,label,active=False):
 d.rectangle((x,y,x+w-1,y+h-1),fill='#111111');d.rectangle((x+1,y+1,x+w-2,y+h-2),fill='#18252e' if not active else '#174553');d.line((x+1,y+1,x+w-3,y+1),fill='#467581');d.line((x+1,y+1,x+1,y+h-3),fill='#467581');d.line((x+2,y+h-2,x+w-2,y+h-2),fill='#080f15');d.line((x+w-2,y+2,x+w-2,y+h-2),fill='#080f15');text(d,(x+w//2,y+(h-7)//2),label,'white',center=True,shadow=True)
def icon(kind,color=None):
 if kind.startswith('mantis'):
  path=ROOT/'item-model-previews'/({'mantis_normal':'mantis_blades','mantis_thermal':'mantis_blades_thermal','mantis_electric':'mantis_blades_electric'}[kind]+'-16.png')
  if path.exists():return Image.open(path).convert('RGBA')
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 if kind.startswith('mantis'):
  c=color or {'mantis_thermal':'#ff9f32','mantis_electric':'#6cf0ff','mantis_normal':'#d2dce0'}.get(kind,'#d2dce0')
  d.line((2,12,5,7,13,3),fill='#282f35',width=3);d.line((3,12,6,8,14,3),fill='#98a5ad',width=1);d.line((5,7,13,3),fill=c,width=1);d.rectangle((2,11,4,13),fill='#53616b');d.point((7,7),fill=c);d.point((9,6),fill=c)
 elif kind=='os':
  d.rectangle((5,1,10,14),fill='#514831');d.rectangle((6,2,9,13),fill='#cfb267')
  for y in [3,6,9,12]:d.rectangle((3,y,12,y+1),fill='#e1cf8a')
 elif kind=='cortex':
  d.rectangle((4,4,11,12),fill='#8b485b');d.rectangle((3,5,12,10),fill='#d88499');d.line((7,4,7,11),fill='#6d4356');d.line((4,7,10,7),fill='#e1a0af')
 elif kind=='optics':
  d.polygon([(1,8),(5,4),(10,4),(14,8),(10,11),(5,11)],fill='#d3d9d8');d.rectangle((6,5,9,10),fill='#4dcace');d.rectangle((7,6,8,9),fill='#273b42')
 elif kind=='skeleton':
  d.line((4,12,11,4),fill='#e3dec4',width=3)
  for x,y in [(3,11),(5,13),(10,3),(12,5)]:d.rectangle((x-1,y-1,x+1,y+1),fill='#f1edda')
 elif kind=='circulatory':
  d.polygon([(2,4),(5,3),(8,5),(11,3),(14,5),(13,9),(8,14),(3,10)],fill='#aa3f45');d.line((3,6,5,5,7,6),fill='#ec7579',width=2)
 elif kind=='legs':
  d.rectangle((5,2,9,10),fill='#53646b');d.rectangle((4,10,12,13),fill='#758e8d');d.line((4,13,12,13),fill='#c6d5d1')
 else:
  d.line((8,1,8,14),fill='#7bd5cf',width=2)
  for y in [4,8,12]:d.line((3,y-2,8,y,12,y-2),fill='#82b4b0',width=1)
 return im

categories=[('cortex','Corteza frontal',3,47,52),('arms','Brazos',1,47,87),('skeleton','Esqueleto',2,47,122),('nerves','Sistema nervioso',2,47,157),('integumentary','Tegumentario',3,47,192),('os','Sistema operativo',1,326,52),('optics','Sistema ocular',1,326,87),('hands','Manos',2,326,122),('circulatory','Circulatorio',3,326,157),('legs','Piernas',1,326,192)]
slots=[]
for key,label,n,x,y in categories:
 for i in range(n):slots.append(dict(id=f'{key}_{i}',category=key,rect=[x+i*20,y+13,18,18],item='mantis_thermal' if key=='arms' else None))
for row in range(3):
 for col in range(9):slots.append(dict(id=f'inventory_{row*9+col}',category='inventory',rect=[159+18*col,263+18*row,18,18],item=['mantis_normal','mantis_thermal','mantis_electric'][col] if row==0 and col<3 else None))
for col in range(9):slots.append(dict(id=f'hotbar_{col}',category='inventory',rect=[159+18*col,322,18,18],item=None))
def background(xray=True):
 im=Image.new('RGBA',(W,H),'#0a1118');d=ImageDraw.Draw(im)
 # Pixel frame, restrained cyan edge and magenta selection accents.
 d.rectangle((1,1,W-2,H-2),outline='#466571',width=1)
 d.rectangle((4,4,W-5,H-5),outline='#1b333f',width=1)
 for x,y,sx,sy in [(1,1,1,1),(W-2,1,-1,1),(1,H-2,1,-1),(W-2,H-2,-1,-1)]:
  d.line((x,y,x+sx*14,y),fill='#36a8bd',width=2)
  d.line((x,y,x,y+sy*14),fill='#36a8bd',width=2)
 text(d,(W//2,16),'Mesa matasanos','#d6e8ed',center=True)
 d.line((196,29,284,29),fill='#e14a85',width=2)
 text(d,(18,33),'Diagnostico','#7193a4')
 inset(d,183,50,114,174)
 d.rectangle((186,53,293,220),fill='#111e27')
 for x,y,sx,sy in [(188,55,1,1),(291,55,-1,1),(188,218,1,-1),(291,218,-1,-1)]:
  d.line((x,y,x+sx*8,y),fill='#2e8494');d.line((x,y,x,y+sy*8),fill='#2e8494')
 for key,label,n,x,y in categories:text(d,(x,y),label)
 for slot in slots:
  x,y,w,h=slot['rect'];inset(d,x,y,w,h)
  if slot['item']:im.alpha_composite(icon(slot['item']),(x+1,y+1))
 # Outline the selected body slot, rather than an installation button.
 d.rectangle((46,99,65,118),outline='#ec4e8c');d.rectangle((45,98,48,99),fill='#ec4e8c')
 button(d,183,228,55,17,'Rayos X',xray);button(d,242,228,55,17,'Skin',not xray)
 d.line((47,249,433,249),fill='#213641')
 text(d,(159,252),'Inventario','#a9c1cd')
 text(d,(18,264),'Mantis termicas','#eda1b7');text(d,(18,280),'Ambos brazos','#a1bac7');text(d,(18,294),'Instaladas','#61c2b4')
 text(d,(18,317),'Capacidad','#7393a2');text(d,(18,330),'Por definir','#627d8c')
 return im

im=background();im.save(ROOT/'textures/mesa_matasanos_gui.png')
avatar=ROOT/'jugador-rayos-x.bbmodel'
for label,opacity in [('rayos-x',.23),('skin',1)]:
 a=render(ROOT/'states/avatar-thermal.bbmodel' if label=='rayos-x' else avatar,(108,168),yaw=-8,pitch=0,span=34,opacity=opacity);a.save(ROOT/f'textures/avatar-{label}.png')
 preview=background(label=='rayos-x');preview.alpha_composite(a,(186,53));preview.resize((W*3,H*3),Image.Resampling.NEAREST).save(ROOT/f'previews/mesa-matasanos-{label}.png')
# Save independently reusable widget and item sprites, native resolution.
for k in ['mantis_normal','mantis_thermal','mantis_electric','os','optics','cortex','skeleton','circulatory','legs','nerves']:icon(k).save(ROOT/f'textures/item-{k}.png')
# Updated composition plane aligned with the central avatar. Avatar geometry is preserved.
d=json.loads((ROOT/'states/avatar-thermal.bbmodel').read_text());d['name']='Mesa matasanos / diagnostico oscuro';d['textures'].append(texture(im,'mesa_matasanos_gui.png'))
gid=str(uuid.uuid4());eid=str(uuid.uuid4());d['groups'].append(dict(name='UI_REFERENCE',uuid=gid,origin=[0,0,0],rotation=[0,0,0],visibility=True,export=True,isOpen=True))
scale=168/34;cx=240;cy=137
x0=-cx/scale;x1=(W-cx)/scale;y1=16+cy/scale;y0=16-(H-cy)/scale
el=dict(name='UI_mesa_matasanos',type='cube',uuid=eid,origin=[0,0,0],rotation=[0,0,0],box_uv=False,autouv=0,visibility=True,export=True,**{'from':[x0,y0,5.5],'to':[x1,y1,5.51]},faces={f:dict(uv=[0,0,W,H] if f=='north' else [0,0,0,0],texture=len(d['textures'])-1 if f=='north' else None) for f in ['north','south','east','west','up','down']})
d['elements'].append(el);d['outliner'].append(dict(uuid=gid,children=[eid]));(ROOT/'mesa-matasanos.bbmodel').write_text(json.dumps(d,ensure_ascii=False,indent=2))
layout={'version':3,'reference_size':[W,H],'status':'visual_prototype','style_reference':'Project: Edgerunner 1.1.0 gallery; original UI assets','capacity_value':None,'entry_point':{'type':'block_interaction','block':'mesa_matasanos','status':'future_block_not_implemented'},'viewport':{'rect':[183,50,114,174],'skin_alpha':.23,'use_connected_player_skin':True},'categories':[{'id':k,'label':label,'slots':n} for k,label,n,x,y in categories],'slots':slots,'actions':[],'close_keys':['Escape','KeyE'],'interaction':'cursor pickup/place/swap; Shift-click transfer','skin_controls':[{'id':'xray','rect':[183,228,55,17]},{'id':'skin','rect':[242,228,55,17]}],'slot_size':18,'item_size':16,'scale':'integer GUI scale when possible; centered; nearest-neighbor for all pixel textures','demo_items':True,'capacity':'not_yet_defined'}
(ROOT/'layout.json').write_text(json.dumps(layout,ensure_ascii=False,indent=2))
(ROOT/'source/pixel-font.json').write_text(json.dumps(PATTERNS))
print('Minecraft GUI, two previews, editable composition, slot layout and sprites generated.')
