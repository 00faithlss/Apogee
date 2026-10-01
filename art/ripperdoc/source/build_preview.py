from pathlib import Path
import json,base64
R=Path(__file__).resolve().parents[1]
def data(p):return 'data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode()
assets={p.stem:data(p) for p in (R/'textures').glob('*.png')}
layout=json.loads((R/'layout.json').read_text());font=json.loads((R/'source/pixel-font.json').read_text())
html='''<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Mesa matasanos · maqueta Minecraft</title><style>
*{box-sizing:border-box}body{margin:0;background:#080f16;color:#e6e6e2;font:15px system-ui,sans-serif;display:grid;justify-items:center;padding:22px}header{max-width:960px;width:100%;margin-bottom:14px}h1{font-size:22px;margin:0 0 7px}p{margin:0;line-height:1.5;color:#bac0b8}#stage{position:relative;width:min(960px,96vw);aspect-ratio:480/356}canvas{width:100%;height:100%;display:block;image-rendering:pixelated}#controls{position:absolute;inset:0}#controls button{position:absolute;border:0;background:transparent;cursor:pointer;padding:0;color:transparent}#controls button:hover{background:#ffffff20}#controls button:focus-visible{outline:2px solid #f7d77d;outline-offset:0}#message{min-height:44px;margin-top:14px;max-width:960px;width:100%;color:#e2d4a5}#open{padding:14px 24px;border:3px outset #aaa;background:#174553;color:white;font-weight:bold;font-size:16px;cursor:pointer}#open[hidden],#stage[hidden]{display:none}footer{max-width:960px;margin-top:10px;font-size:13px;color:#a5aba2}
</style><header><h1>Mesa matasanos</h1><p>Maqueta interactiva: mueve implantes entre slots con clic o Mayús + clic y observa ambos antebrazos. Los cambios de esta demostración no afectan a Minecraft.</p></header><button id="open" hidden>Abrir mesa matasanos</button><main id="stage"><canvas width="480" height="356" aria-label="Interfaz de implantes con avatar central"></canvas><div id="controls"></div></main><p id="message" role="status" aria-live="polite">Selecciona un slot o un implante del inventario.</p><footer>Vista de diseño para la futura mesa matasanos. Skin de muestra; el juego deberá usar la del jugador conectado. Mantis física, térmica y eléctrica disponibles en la maqueta. Los otros sistemas requieren sus módulos. Esc o E cierra la mesa. La instalación real aún no está implementada.</footer><script>
const ASSETS=__ASSETS__,LAYOUT=__LAYOUT__,FONT=__FONT__;
const c=document.querySelector('canvas'),g=c.getContext('2d');g.imageSmoothingEnabled=false;
const imgs={},slots=LAYOUT.slots.map(s=>({...s}));let xray=true,selected='arms_0',hover=null,held=null,heldOrigin=null,mouse=[360,240];
const info={mantis_normal:['Mantis fisicas','arms'],mantis_thermal:['Mantis termicas','arms'],mantis_electric:['Mantis electricas','arms'],os:['Sandevistan','os'],optics:['Opticas','optics']};
const categories=Object.fromEntries(LAYOUT.categories.map(x=>[x.id,x.label]));
function txt(x,y,str,col='#a1bac7'){g.fillStyle=col;str=str.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toUpperCase();for(const ch of str){if(ch===' '){x+=4;continue}const rows=(FONT[ch]||FONT['?']).split('/');for(let yy=0;yy<rows.length;yy++)for(let xx=0;xx<rows[yy].length;xx++)if(rows[yy][xx]==='1')g.fillRect(x+xx,y+yy,1,1);x+=rows[0].length+1}}
function slotBox(x,y){g.fillStyle='#17242c';g.fillRect(x,y,18,18);g.fillStyle='#0b1219';g.fillRect(x+1,y+1,16,16);g.fillStyle='#080e14';g.fillRect(x,y,17,1);g.fillRect(x,y,1,17);g.fillStyle='#49636e';g.fillRect(x+1,y+17,17,1);g.fillRect(x+17,y+1,1,17)}
function equippedVariant(){const item=slots.find(s=>s.id==='arms_0').item;return item?item.replace('mantis_',''):'none'}
function draw(){g.drawImage(imgs.mesa_matasanos_gui,0,0);g.drawImage(imgs[xray?'avatar-'+equippedVariant():'avatar-skin'],186,53);
 g.fillStyle='#0a1118';g.fillRect(44,97,23,23);for(const s of slots){const [x,y]=s.rect;slotBox(x,y);if(s.item)g.drawImage(imgs['item-'+s.item],x+1,y+1);if(s.id===selected){g.strokeStyle='#ec4e8c';g.lineWidth=1;g.strokeRect(x-1.5,y-1.5,21,21)}}
 const sel=slots.find(s=>s.id===selected);g.fillStyle='#0a1118';g.fillRect(17,260,128,46);if(sel){txt(18,264,sel.item?info[sel.item][0]:(categories[sel.category]||'Inventario'),'#eda1b7');txt(18,280,sel.category==='arms'?'Ambos brazos':sel.category==='inventory'?'En inventario':'Slot corporal');txt(18,294,sel.item?(sel.category==='inventory'?'Disponible':'Instalado'):'Vacio','#61c2b4')}
 g.fillStyle=xray?'#174553':'#18252e';g.fillRect(185,230,51,13);g.fillStyle=xray?'#18252e':'#174553';g.fillRect(244,230,51,13);txt(188,233,'Rayos X','#fff');txt(255,233,'Skin','#fff');
 if(held){g.drawImage(imgs['item-'+held],Math.min(460,mouse[0]+8),Math.min(336,mouse[1]+8));return}if(hover){const s=slots.find(s=>s.id===hover);if(s){const label=s.item?info[s.item][0]:(categories[s.category]||'Inventario')+' / vacio';const tw=Math.min(150,label.length*6+8),tx=Math.min(480-tw-4,s.rect[0]+20),ty=Math.max(5,s.rect[1]-20);g.fillStyle='#180d25';g.fillRect(tx,ty,tw,16);g.strokeStyle='#493069';g.strokeRect(tx+.5,ty+.5,tw-1,15);txt(tx+4,ty+5,label,'#f0e9fa')}}}
function msg(s){document.querySelector('#message').textContent=s}
function accepts(slot,item){return slot.category==='inventory'||info[item][1]===slot.category}
function interact(id,quick=false){
 const src=slots.find(s=>s.id===id);selected=id;if(!src)return;
 if(quick&&!held){if(!src.item)return;const target=slots.find(s=>!s.item&&(src.category==='inventory'?s.category===info[src.item][1]:s.category==='inventory'));if(!target){msg('No hay una ranura compatible libre.');return}target.item=src.item;src.item=null;msg('Implante transferido. La anatomía refleja lo equipado.');draw();return}
 if(!held){if(src.item){held=src.item;heldOrigin=src.id;src.item=null;msg('Implante en el cursor. Haz clic en una ranura compatible.')}}
 else if(accepts(src,held)){const old=src.item;src.item=held;held=old;if(!held)heldOrigin=null;msg(held?'Intercambio realizado; coloca el implante que llevas.':'Implante colocado. La anatomía refleja lo equipado.')}
 else msg('Este implante corresponde a '+categories[info[held][1]]+'.');
 draw();
}
function close(){
 if(held){let target=slots.find(s=>s.id===heldOrigin&&!s.item&&accepts(s,held))||slots.find(s=>s.category==='inventory'&&!s.item)||slots.find(s=>!s.item&&accepts(s,held));if(!target){msg('Coloca el implante del cursor antes de salir.');return}target.item=held;held=null;heldOrigin=null}
 document.querySelector('#stage').hidden=true;document.querySelector('#open').hidden=false;hover=null;msg('Mesa cerrada. Tus implantes permanecen guardados en esta demostración.');
}
function control(rect,label,click,id){const b=document.createElement('button');b.type='button';b.setAttribute('aria-label',label);b.dataset.id=id;const [x,y,w,h]=rect;b.style.cssText=`left:${x/480*100}%;top:${y/356*100}%;width:${w/480*100}%;height:${h/356*100}%`;b.onclick=e=>{hover=null;click(e)};b.onmouseenter=()=>{hover=id;draw()};b.onmouseleave=()=>{hover=null;draw()};document.querySelector('#controls').append(b)}
Promise.all(Object.entries(ASSETS).map(([k,v])=>new Promise((ok,bad)=>{const i=new Image;i.onload=()=>{imgs[k]=i;ok()};i.onerror=bad;i.src=v}))).then(()=>{
 for(const s of slots)control(s.rect,(categories[s.category]||'Inventario')+' '+s.id,e=>interact(s.id,e.shiftKey),s.id);
 control([183,228,55,17],'Mostrar rayos X',()=>{xray=true;draw()},'xray');control([242,228,55,17],'Mostrar skin',()=>{xray=false;draw()},'skin');draw();window.demoReady=true;
});document.querySelector('#stage').addEventListener('mousemove',e=>{const r=c.getBoundingClientRect();mouse=[(e.clientX-r.left)*480/r.width,(e.clientY-r.top)*356/r.height];if(held)draw()});document.querySelector('#open').onclick=()=>{document.querySelector('#stage').hidden=false;document.querySelector('#open').hidden=true;msg('Mesa abierta: selecciona un implante.');draw()};document.addEventListener('keydown',e=>{if(!document.querySelector('#stage').hidden&&(e.key==='Escape'||e.code==='KeyE'||e.key.toLowerCase()==='e')){e.preventDefault();close()}});
</script></html>'''
html=html.replace('__ASSETS__',json.dumps(assets)).replace('__LAYOUT__',json.dumps(layout)).replace('__FONT__',json.dumps(font));(R/'mesa-matasanos-demo.html').write_text(html)
print('Standalone interactive HTML generated.')
