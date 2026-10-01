from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
from avatar_renderer import render
R=Path(__file__).resolve().parents[1]
font=lambda n:ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',n)
sheet=Image.new('RGBA',(1440,920),'#1c2329');d=ImageDraw.Draw(sheet)
d.text((35,24),'MESA MATASANOS / MÓDULOS MANTIS',font=font(30),fill='#e8edeb')
d.text((35,69),'Modelos .bbmodel · Ambos brazos · Skin translúcida · Cambios según el slot equipado',font=font(17),fill='#a6b7be')
labels={'none':'SIN IMPLANTE','normal':'FÍSICA','thermal':'TÉRMICA','electric':'ELÉCTRICA'}
for i,(v,label) in enumerate(labels.items()):
 p=R/f'states/avatar-{v}.bbmodel'
 a=render(p,(108,168),yaw=-8,pitch=0,span=34,opacity=.23);a.save(R/f'textures/avatar-{v}.png')
 x=20+i*355
 d.rectangle((x,115,x+335,655),fill='#242d33',outline='#46565f')
 d.text((x+20,131),label,font=font(22),fill={'thermal':'#ffb04e','electric':'#62d6ef'}.get(v,'#d0dbdc'))
 im=render(p,(320,468),yaw=-8,pitch=0,span=34,opacity=.23);sheet.alpha_composite(im,(x+8,174))
 if v!='none':
  # Close-up reads actual exported geometry; scale fixed across all variants.
  close=render(p,(330,190),yaw=-18,pitch=8,span=8,center=(-6.15,15.5,0),skin=False)
  sheet.alpha_composite(close,(x+3,689));d.text((x+10,663),'DETALLE / ANTEBRAZO',font=font(15),fill='#acbac0')
 else:
  d.text((x+15,702),'Sin mecanismo Mantis.',font=font(18),fill='#acbac0');d.text((x+15,732),'Se conserva la anatomía.',font=font(18),fill='#acbac0')
d.text((35,891),'Vista calculada desde la geometría. Assets de interfaz; no modifica las animaciones de combate.',font=font(16),fill='#a6b7be')
sheet.save(R/'previews/mantis-xray-variantes.png')
