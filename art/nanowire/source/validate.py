from pathlib import Path
import json,math
import numpy as np
from PIL import Image
from render import R,A,load,scene
from build import pose,SEG,CLIPS,mat,tx,rot
results=[]
for p in (A/'geo').glob('*.json'):
 geo=load(str(p))['minecraft:geometry'][0];bones=geo['bones'];names=[b['name'] for b in bones];assert len(names)==len(set(names));seen=set()
 for b in bones:
  assert not b.get('parent') or b['parent'] in seen;seen.add(b['name'])
  for c in b.get('cubes',[]):
   assert all(float(s)>0 for s in c['size'])
   for face in c['uv'].values():
    assert all(0<=v<=64 for v in face['uv']);assert all(0<=u+s<=64 for u,s in zip(face['uv'],face['uv_size']))
 results.append(f'{p.name}: bone hierarchy, UVs and positive cube dimensions OK')
anim=load(str(A/'animations/nanowire_arms.animation.json'))['animations'];names={b['name'] for b in load(str(A/'geo/nanowire_arms.geo.json'))['minecraft:geometry'][0]['bones']}
assert len(anim)==7
for name,a in anim.items():
 assert set(a['bones'])<=names
 for b,chs in a['bones'].items():
  for ch,values in chs.items():
   assert min(map(float,values))>=0 and max(map(float,values))<=a['animation_length']+1e-6
   assert all(math.isfinite(v) for vs in values.values() for v in vs)
   if ch=='scale':assert all(v>=0 for vs in values.values() for v in vs)
results.append('7 animations: matching bones, finite channels, valid time ranges and nonnegative scales')
# Verify the exported animation sample stays joined: independent bones can introduce tiny subframe gaps.
max_gap=0
for clip,(duration,loop) in CLIPS.items():
 for t in np.linspace(0,duration,31):
  _,mats=scene(clip,float(t),False)
  for side in ['left','right']:
   for i in range(SEG-1):
    current=mats[f'wire_{side}_{i:02}'];nxt=mats[f'wire_{side}_{i+1:02}'];end=tx(current,[0,0,1]);start=tx(nxt,[0,0,0]);gap=float(np.linalg.norm(end-start));max_gap=max(max_gap,gap)
assert max_gap<.14,max_gap
results.append(f'Cable continuity sampled across 7 clips: maximum endpoint offset {max_gap:.4f} model units; wire diameter 0.13')
# Avoid intersecting the two arm proxy volumes in the initial pullout pose.
p=pose('stowed',0);centers=[]
for side,x in [('left',-6),('right',6)]:centers.append(x+p[side+'_arm']['position'][0])
assert centers[1]-centers[0]>4
results.append('Initial forearm separation exceeds 4-unit arm width')
for clip in ['attack_left','attack_right','heavy']:
 start=pose(clip,0);end=pose(clip,CLIPS[clip][0]);idle=pose('idle',0)
 for bone,channels in idle.items():
  for ch,v in channels.items():
   cv=rot if ch=='rotation' else np.array
   assert np.allclose(cv(start[bone][ch]),cv(v),atol=1e-5) and np.allclose(cv(end[bone][ch]),cv(v),atol=1e-5),(clip,bone,ch)
results.append('Attack clips return to their common idle pose')
mask=np.array(Image.open(A/'textures/nanowire/nanowire_red_glowmask.png'));assert np.all(mask[mask[:,:,3]>0][:,:3]==[255,0,0]);results.append('All emissive pixels are exactly #FF0000')
for p in (R/'blockbench').glob('*.bbmodel'):
 d=load(str(p));ids=[e['uuid'] for e in d['elements']]+[g['uuid'] for g in d['groups']];assert len(ids)==len(set(ids));gids={g['uuid'] for g in d['groups']}
 for a in d.get('animations',[]):assert set(a['animators'])<=gids
results.append('Blockbench IDs and animation targets OK')
assert not any('inspect' in n or 'block' in n for n in anim)
spec=load(str(R/'source/model_spec.json'))
assert not any('palm' in c['name'] or 'grip' in c['bone'] for c in spec['cubes'])
for side,x,sg in [('left',-6,-1),('right',6,1)]:
    mount=next(b for b in spec['bones'] if b['name']==side+'_inner_mount')
    assert mount['rotation']==[0,0,sg*90]
    for i in range(SEG):
        assert next(b for b in spec['bones'] if b['name']==f'wire_{side}_{i:02}')['parent']==side+'_arm'
results.append('No inspection/guard clips or palm hardware; mirrored medial mounts and per-arm cable parents OK')
# Check attachment accuracy and proxy skin penetration using the actual exported transforms.
max_anchor_error=0.;penetrations=[];min_tips=1e9
for clip,(duration,_) in CLIPS.items():
    for t in np.linspace(0,duration,31):
        _,mats=scene(clip,float(t),False)
        tips=[]
        for side,x in [('left',-6),('right',6)]:
            first=mats[f'wire_{side}_00']; nozzle=tx(mats[side+'_inner_mount'],[x,2.61,.26])
            max_anchor_error=max(max_anchor_error,float(np.linalg.norm(tx(first,[0,0,0])-nozzle)))
            tips.append(tx(mats[f'wire_{side}_{SEG-1:02}'],[0,0,1]))
            if np.linalg.norm(first[:3,:3])<1e-7:continue
            for i in range(SEG):
                # endpoints and midpoint, against both forearms and hands (not an exhaustive sweep).
                for f in [0.,.5,1.]:
                    pt=tx(mats[f'wire_{side}_{i:02}'],[0,0,f])
                    for target,xt in [('left',-6),('right',6)]:
                        local=tx(np.linalg.inv(mats[target+'_arm']),pt)
                        for lo,hi in [(np.array([xt-2,-2,-.4]),np.array([xt+2,2,8.4])),(np.array([xt-2,-1.8,-4.5]),np.array([xt+2,1.8,-.4]))]:
                            if np.all(local>lo+.01) and np.all(local<hi-.01):penetrations.append((clip,round(float(t),3),side,i,target))
        if clip=='heavy' and .30<=t<=.85:min_tips=min(min_tips,float(np.linalg.norm(tips[0]-tips[1])))
assert max_anchor_error<1e-4,max_anchor_error
assert not penetrations,penetrations[:5]
assert min_tips>2,min_tips
results.append(f'Wire roots remain in nozzles (max offset {max_anchor_error:.8f}); no proxy hand/forearm penetrations at sampled points')
results.append(f'Double attack tips stay separated during active stroke (minimum {min_tips:.3f} units over sampled frames)')

# Idle is physically held at both hands and joined in the centre in the baked rig.
for t in np.linspace(0,2,11):
    _,ms=scene('idle',float(t),False)
    tips=[tx(ms[f'wire_{side}_{SEG-1:02}'],[0,0,1]) for side in ['left','right']]
    assert np.linalg.norm(tips[0]-tips[1])<.06
results.append('Idle cable meets between both hands throughout its loop')
# Verify the requested opposing sweep directions from actual exported geometry.
for side,sign in [('right',1),('left',-1)]:
    xs=[]
    for t in [.72*.28,.72*.68]:
        _,ms=scene('attack_'+side,t,False);xs.append(tx(ms[f'wire_{side}_{SEG-1:02}'],[0,0,1])[0])
    assert sign*xs[0]>8 and sign*xs[1]<-8,(side,xs)
results.append('Single attacks cross opposite directions: right +X to -X; left -X to +X')
# S has both signs of signed curvature in the XZ plane beyond the grip.
_,ms=scene('heavy',.64,False)
for side in ['left','right']:
    pts=np.array([tx(ms[f'wire_{side}_{i:02}'],[0,0,0]) for i in range(SEG)])
    pts=pts[pts[:,2]<-10][:,[0,2]];ds=np.diff(pts,axis=0)
    cross=ds[:-1,0]*ds[1:,1]-ds[:-1,1]*ds[1:,0]
    assert np.any(cross>.025) and np.any(cross<-.025),(side,cross.min(),cross.max())
results.append('Both double-attack filaments show opposing curvature lobes (S shape)')

# Per-arm attachments reproduce the same local mechanism/wire without driving skin poses.
for side,x in [('left',-6),('right',6)]:
    off=np.array([x,0,7.]);pre=mat(p=-off);post=mat(p=off)
    path=A/'animations'/f'nanowire_{side}_attachment.animation.json'
    attachment_anim=load(str(path))['animations']
    assert len(attachment_anim)==7
    assert all(side+'_arm' not in a['bones'] for a in attachment_anim.values())
    for clip,(duration,_) in CLIPS.items():
        for t in [0,duration*.37,duration*.73,duration]:
            _,old=scene(clip,t,False);_,new=scene(clip,t,False,'nanowire_'+side+'_attachment')
            arm_inverse=np.linalg.inv(old[side+'_arm'])
            for bone,M in new.items():
                if bone in ['root',side+'_arm']:continue
                expected=pre@arm_inverse@old[bone]
                if not bone.startswith('wire_'):expected=expected@post
                assert np.allclose(M,expected,atol=1e-5),(side,clip,bone)
results.append('Independent attachment exports match bilateral rig in arm-local coordinates; no skin pose tracks')
report={'checks':results,'limits':['CPU evaluator, not GeckoLib runtime','No collision/damage implementation','No Blockbench WebGL or Minecraft playtest']}
(R/'validation.json').write_text(json.dumps(report,indent=2));print('\n'.join(results))
