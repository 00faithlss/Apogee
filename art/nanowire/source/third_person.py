from render import *
def player():
 cubes,_=scene('idle',0);placement=mat([-90,0,0],[0,17,-2]);cubes=[(o,s,k,placement@m) for o,s,k,m in cubes]
 for o,s,k in [([-4,12,-2],[8,12,4],9),([-4,24,-4],[8,8,8],8),([-4,0,-2],[3.9,12,4],0),([.1,0,-2],[3.9,12,4],0),([-4.05,29.9,-4.05],[8.1,2.2,8.1],0),([-2.5,26.7,-4.05],[1.2,.7,.1],3),([1.3,26.7,-4.05],[1.2,.7,.1],3)]:cubes.append((np.array(o),np.array(s),k,np.eye(4)))
 return cubes
if __name__=='__main__':
 im=render(player(),(900,1050),(18,25,-54),(0,16,0),35);d=ImageDraw.Draw(im);d.text((26,24),'NANOCABLE / TERCERA PERSONA',font=font(26),fill='#eef2f4');d.text((26,990),'Estudio de montaje en maniquí Minecraft.',font=font(18),fill='#a8b8c7');d.text((26,1020),'La skin real y el renderer Java se conectan al integrar.',font=font(16),fill='#a8b8c7');im.save(R/'previews/nanowire-tercera-persona.png')
