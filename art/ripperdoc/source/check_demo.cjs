const fs=require('fs'),vm=require('vm'),path=require('path');
const html=fs.readFileSync(path.join(__dirname,'../mesa-matasanos-demo.html'),'utf8');const js=html.match(/<script>([\s\S]*)<\/script>/)[1];new vm.Script(js);
const ctx2d=new Proxy({},{get:(t,k)=>t[k]||(()=>{}),set:(t,k,v)=>(t[k]=v,true)});const controls=[],events={};const nodes={canvas:{getContext:()=>ctx2d},'#stage':{hidden:false,addEventListener(){}},'#open':{hidden:true},'#message':{textContent:''},'#controls':{append:b=>controls.push(b)}};
const document={querySelector:s=>nodes[s],createElement:()=>({setAttribute(){},dataset:{},style:{}}),addEventListener:(k,f)=>events[k]=f};class Image{set src(v){queueMicrotask(()=>this.onload())}};
const s={document,Image,window:{},console,queueMicrotask};vm.createContext(s);vm.runInContext(js,s);
setImmediate(()=>{try{const ev=c=>vm.runInContext(c,s);let n=0;const ck=(x,m)=>{n++;if(!x)throw Error(m)};ck(s.window.demoReady,'init');const count=ev('slots.filter(s=>s.item).length');
 ck(!controls.some(b=>['install','remove','close','close_top'].includes(b.dataset.id)),'removed buttons');ck(ev("equippedVariant()==='thermal'"),'initial thermal');
 ev("interact('arms_0')");ck(ev("equippedVariant()==='none'&&held==='mantis_thermal'"),'pickup removes overlay');
 ev("interact('optics_0')");ck(ev("held==='mantis_thermal'&&!slots.find(s=>s.id==='optics_0').item"),'incompatible blocked');
 ev("interact('inventory_3')");ck(ev('held===null'),'deposit');
 ev("interact('inventory_0',true)");ck(ev("equippedVariant()==='normal'"),'quick equip normal');
 ev("interact('inventory_2',true)");ck(ev("equippedVariant()==='normal'"),'occupied quick blocked');
 ev("interact('inventory_2');interact('arms_0')");ck(ev("equippedVariant()==='electric'&&held==='mantis_normal'"),'swap electric');
 ev('close()');ck(ev('held===null'),'close returns cursor');ck(ev('slots.filter(s=>s.item).length')===count,'item count preserved');ck(nodes['#stage'].hidden,'close');nodes['#open'].onclick();ck(!nodes['#stage'].hidden,'reopen');
 controls.find(b=>b.dataset.id==='skin').onclick({});ck(ev('!xray'),'skin toggle');
 let prevented=0;events.keydown({key:'e',code:'KeyE',preventDefault(){prevented++}});ck(nodes['#stage'].hidden&&prevented===1,'E closes');nodes['#open'].onclick();events.keydown({key:'Escape',code:'Escape',preventDefault(){}});ck(nodes['#stage'].hidden,'Escape closes');
 console.log(`PASS: ${n} assertions; slot logic and input events in mock DOM. Not an in-game or browser validation.`)
 }catch(e){console.error(e);process.exitCode=1}});
