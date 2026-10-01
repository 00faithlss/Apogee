from pathlib import Path
import base64
import copy
import json
import uuid

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/apogee'
FACES = ('north', 'east', 'south', 'west', 'up', 'down')


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'apogee/nanowire/' + name))


def hardware(side):
    names = (side + '_palm_feed', side + '_wire_outlet')
    bones = [dict(name=names[0], parent=side + '_arm', pivot=[0, 0, -7], cubes=[]),
             dict(name=names[1], parent=names[0], pivot=[3.6 if side == 'right' else -3.6, .65, -10.15], cubes=[])]
    cubes = []

    def cube(name, lo, hi, material, outlet=False):
        lo, hi = list(lo), list(hi)
        if side == 'left':
            lo[0], hi[0] = -hi[0], -lo[0]
        u, v = material % 4 * 16, material // 4 * 16
        faces = {face: dict(uv=[u, v], uv_size=[16, 16]) for face in FACES}
        data = dict(origin=[-hi[0], lo[1], lo[2]], size=[hi[i] - lo[i] for i in range(3)], uv=faces)
        bones[int(outlet)]['cubes'].append(data)
        cubes.append((names[int(outlet)], name))

    cube('wrist_feed_bridge', [-2.78, -.32, -8.05], [-2.36, .32, -6.45], 1)
    cube('palm_mount', [-2.68, -1.65, -11.25], [-2.30, 1.65, -7.30], 0)
    cube('palm_cover', [-2.98, -1.22, -10.95], [-2.65, 1.22, -7.60], 1)
    for y in (-1.58, -.46, .66):
        cube('ceramic_grip_pad', [-2.35, y, -11.88], [-.55, y + .94, -11.53], 1)
        cube('grip_pad_edge', [-2.39, y + .08, -11.91], [-2.25, y + .86, -11.87], 2)
        for x in (-1.94, -1.36, -.78):
            cube('grip_pad_inset', [x, y + .24, -11.895], [x + .18, y + .70, -11.877], 4)
    for y in (-1.55, 1.35):
        cube('steel_edge', [-2.73, y, -11.12], [-2.67, y + .20, -7.48], 2)
    for z in (-10.88, -7.60):
        cube('end_guard', [-2.73, -1.35, z], [-2.67, 1.35, z + .20], 2)
    for z in (-10.65, -8.00):
        for y in (-.98, .98):
            cube('mount_bolt', [-3.025, y - .10, z], [-2.975, y + .10, z + .20], 3)
    cube('recessed_feed', [-3.025, -.20, -10.52], [-2.975, .20, -7.82], 4)
    for z in (-10.45, -9.85, -9.25, -8.65):
        cube('ceramic_palm_inset', [-2.99, .22, z], [-2.98, .93, z + .20], 4)
    cube('status_bezel', [-3.065, -.85, -9.30], [-2.975, -.40, -8.62], 2)
    cube('status_light', [-3.075, -.73, -9.14], [-3.06, -.52, -8.78], 6)
    cube('socket_base', [-3.28, .09, -10.71], [-2.92, 1.21, -9.59], 0, True)
    for y in (.09, 1.02):
        cube('outlet_top_bottom', [-3.60, y, -10.71], [-3.25, y + .19, -9.59], 2, True)
    for z in (-10.71, -9.78):
        cube('outlet_side', [-3.60, .28, z], [-3.25, 1.02, z + .19], 2, True)
    for y in (.27, .94):
        cube('inner_red_lip', [-3.605, y, -10.52], [-3.58, y + .09, -9.78], 6, True)
    return bones, cubes


def blockbench(geo, animations, side, cube_names):
    name = 'nanowire_' + side + '_attachment'
    bones = geo['minecraft:geometry'][0]['bones']
    elements, groups, children = [], [], {}
    named = {bone: [] for bone in (side + '_palm_feed', side + '_wire_outlet')}
    for bone, cube in cube_names:
        named[bone].append(cube)
    for bone in bones:
        group_id = uid(name + '/' + bone['name'])
        pivot = bone.get('pivot', [0, 0, 0])
        rotation = bone.get('rotation', [0, 0, 0])
        groups.append(dict(name=bone['name'], uuid=group_id, origin=[-pivot[0], pivot[1], pivot[2]],
                           rotation=[-rotation[0], -rotation[1], rotation[2]], visibility=True, export=True, isOpen=True))
        children[bone['name']] = []
        for i, cube in enumerate(bone.get('cubes', [])):
            origin, size = cube['origin'], cube['size']
            lo = [-origin[0] - size[0], origin[1], origin[2]]
            cube_id = uid(name + '/' + bone['name'] + '/' + str(i))
            pivot = cube.get('pivot', [0, 0, 0])
            rotation = cube.get('rotation', [0, 0, 0])
            faces = {}
            for face, uv in cube['uv'].items():
                u, v = uv['uv']
                w, h = uv['uv_size']
                faces[face] = dict(uv=[u, v, u + w, v + h], texture=0)
            cube_name = named[bone['name']][i] if bone['name'] in named else bone['name'] + '_' + str(i)
            elements.append(dict(name=cube_name, type='cube', uuid=cube_id, **{'from': lo},
                                 to=[lo[j] + size[j] for j in range(3)], origin=[-pivot[0], pivot[1], pivot[2]],
                                 rotation=[-rotation[0], -rotation[1], rotation[2]], box_uv=False,
                                 autouv=0, visibility=True, export=True, faces=faces))
            children[bone['name']].append(cube_id)

    def tree(bone):
        return dict(uuid=uid(name + '/' + bone['name']), isOpen=True,
                    children=children[bone['name']] + [tree(child) for child in bones if child.get('parent') == bone['name']])

    clips = []
    for clip_name, clip in animations['animations'].items():
        animators = {}
        for bone_name, tracks in clip['bones'].items():
            frames = []
            for channel, keys in tracks.items():
                for time, value in keys.items():
                    value = copy.deepcopy(value)
                    if channel in ('position', 'rotation'):
                        value[0] *= -1
                    if channel == 'rotation':
                        value[1] *= -1
                    frames.append(dict(channel=channel, data_points=[dict(zip('xyz', value))], time=float(time),
                                       uuid=uid(name + clip_name + bone_name + channel + time), interpolation='linear'))
            animators[uid(name + '/' + bone_name)] = dict(name=bone_name, type='bone', keyframes=frames)
        clips.append(dict(uuid=uid(name + clip_name), name=clip_name, loop='loop' if clip.get('loop') else 'once',
                          length=clip['animation_length'], snapping=60, animators=animators))
    texture = ASSETS / 'textures/nanowire/nanowire_red.png'
    return dict(meta=dict(format_version='5.0', model_format='geckolib_model', box_uv=False), name=name,
                model_identifier=name, resolution=dict(width=64, height=64), elements=elements, groups=groups,
                outliner=[tree(bone) for bone in bones if not bone.get('parent')], animations=clips,
                geckolib_modid='apogee', geckolib_model_type='Entity',
                textures=[dict(name=texture.name, uuid=uid('texture'), id='0', mode='bitmap', internal=True,
                               width=64, height=64, uv_width=64, uv_height=64,
                               source='data:image/png;base64,' + base64.b64encode(texture.read_bytes()).decode())])


if __name__ == '__main__':
    projects = ROOT / 'art/nanowire/blockbench'
    projects.mkdir(parents=True, exist_ok=True)
    for side in ('right', 'left'):
        name = 'nanowire_' + side + '_attachment'
        path = ASSETS / 'geo' / (name + '.geo.json')
        geo = json.loads(path.read_text())
        bones = geo['minecraft:geometry'][0]['bones']
        bones[:] = [bone for bone in bones if bone['name'] not in (side + '_palm_feed', side + '_wire_outlet')]
        new_bones, cube_names = hardware(side)
        bones.extend(new_bones)
        path.write_text(json.dumps(geo, indent=1) + '\n')
        animations = json.loads((ASSETS / 'animations' / (name + '.animation.json')).read_text())
        project = blockbench(geo, animations, side, cube_names)
        (projects / (name + '.bbmodel')).write_text(json.dumps(project, separators=(',', ':')) + '\n')
        print(name, len(project['elements']), 'cubes,', len(project['groups']), 'bones')
