const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const code = fs.readFileSync(require('node:path').join(__dirname, '../frontend/assets/js/app.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));
function setup({support = true, preferences = '{}'} = {}) {
 const delays=[],elements = new Map(), listeners = new Map(), timers = new Map(), storage = new Map(), events = [], redirects = [];
 let sequence = 0, response = [], requests = 0, tones = 0;
 class Element {
  constructor() { this.children = []; this.dataset = {}; this.parent = null; }
  append(...children) { for (const child of children) { child.parent = this; this.children.push(child); if (child.id) elements.set(child.id, child); } }
  remove() { if (this.parent) this.parent.children = this.parent.children.filter(e => e !== this); }
  setAttribute() {}
  addEventListener() {}
  get firstElementChild() { return this.children[0]; }
 }
 const document = {readyState:'loading', hidden:false, body:new Element(), getElementById:id=>elements.get(id), querySelector:()=>null, querySelectorAll:()=>[], createElement:()=>new Element(),
  addEventListener:(name,fn)=>{ if (!listeners.has(name)) listeners.set(name,[]); listeners.get(name).push(fn); },
  dispatchEvent:event=>{events.push(event); for(const fn of listeners.get(event.type)||[])fn(event);}};
 class Audio {
  state='running'; currentTime=0; destination={};
  async resume() {}
  createGain(){return {gain:{setValueAtTime(){},exponentialRampToValueAtTime(){}},connect(){},disconnect(){}};}
  createOscillator(){return {frequency:{},connect(){},disconnect(){},start(){tones++;},stop(){}};}
 }
 const context = {document, console, CustomEvent:class {constructor(type,options){this.type=type;this.detail=options.detail;}}, location:{pathname:'/pages/chamados.html',search:'',replace:url=>redirects.push(url)}, localStorage:{getItem:key=>storage.get(key),setItem:(key,value)=>storage.set(key,value)}, Auth:{support:()=>support},
  API:{get:async()=>{requests++;if(response instanceof Error)throw response;return await response;}},
  setTimeout:(fn,delay)=>{delays.push(delay);const id=++sequence;timers.set(id,fn);return id;},clearTimeout:id=>timers.delete(id),addEventListener:()=>{},AudioContext:Audio};
 context.window=context;vm.createContext(context);vm.runInContext(code,context);
 return {delays,app:context.App,live:context.LiveUpdates,document,elements,timers,events,redirects,setResponse:v=>response=v,get requests(){return requests;},get tones(){return tones;},
  start:()=>context.LiveUpdates.start({id:7,preferencias:preferences}),
  poll:async()=>{const [id,fn]=timers.entries().next().value;timers.delete(id);await fn();await flush();}};
}
const ticket = id => ({id,chamado_id:45,tipo:'chamado',titulo:'Novo chamado',lida:false});
test('new tickets refresh the page, show one popup and ring after a gesture; old notifications do not repeat',async()=>{
 const s=setup();let refreshes=0;s.live.subscribe(()=>refreshes++);s.setResponse([ticket(10)]);s.start();await flush();
 assert.equal(s.elements.has('liveTicketAlerts'),false);assert.equal(refreshes,1);
 s.document.dispatchEvent({type:'pointerdown',isTrusted:true,target:{closest:()=>null}});await flush();
 s.setResponse([ticket(11),ticket(10)]);await s.poll();
 assert.equal(s.elements.get('liveTicketAlerts').children.length,1);assert.equal(s.tones,2);assert.equal(refreshes,2);
 await s.poll();assert.equal(s.elements.get('liveTicketAlerts').children.length,1);assert.equal(s.tones,2);assert.equal(refreshes,3);
});
test('a network failure retries without losing the next notification',async()=>{
 const s=setup();s.start();await flush();s.setResponse(new Error('offline'));await s.poll();assert.equal(s.timers.size,1);
 s.setResponse([ticket(1)]);await s.poll();assert.equal(s.elements.get('liveTicketAlerts').children.length,1);
});
test('polling never overlaps, and an expired session stops polling',async()=>{
 const s=setup();let resolve;s.setResponse(new Promise(r=>resolve=r));s.start();s.document.dispatchEvent({type:'visibilitychange'});
 assert.equal(s.requests,1);resolve([]);await flush();const error=new Error('expired');error.status=401;s.setResponse(error);await s.poll();
 assert.equal(s.timers.size,0);assert.equal(s.redirects.length,1);
});
test('preferences and requester role suppress alerts while data still refreshes',async()=>{
 for(const options of [{support:false},{preferences:'{"systemNotifications":false,"notificationSound":false}'}]){
  const s=setup(options);let refreshes=0;s.live.subscribe(()=>refreshes++);s.start();await flush();s.setResponse([ticket(1)]);await s.poll();
  assert.equal(s.elements.has('liveTicketAlerts'),false);assert.equal(s.tones,0);assert.equal(refreshes,2);
 }
});
test('background tabs receive alerts and refresh their list as soon as they become visible',async()=>{
 const s=setup();let refreshes=0;s.live.subscribe(()=>refreshes++);s.start();await flush();s.document.hidden=true;s.setResponse([ticket(1)]);await s.poll();
 assert.equal(refreshes,1);assert.equal(s.elements.get('liveTicketAlerts').children.length,1);
 s.document.hidden=false;s.document.dispatchEvent({type:'visibilitychange'});await flush();assert.equal(refreshes,2);
});
test('background polling reduces traffic and restores the foreground interval immediately',async()=>{
 const s=setup();s.start();await flush();s.document.hidden=true;await s.poll();assert.equal(s.delays.at(-1),15000);
 s.document.hidden=false;s.document.dispatchEvent({type:'visibilitychange'});await flush();assert.equal(s.delays.at(-1),3000);
});


test('refresh is scheduled every three seconds and identical notifications do not redraw',async()=>{const s=setup();s.start();await flush();assert.equal(s.delays.at(-1),3000);const count=s.events.length;await s.poll();assert.equal(s.events.length,count);assert.equal(s.delays.at(-1),3000);});
test('render signature ignores ticking clocks but preserves status and SLA changes',()=>{const s=setup();const a={id:1,status:'ABERTO',servidor_agora:'a',tempo_resposta_segundos:1,fora_sla:false};assert.equal(s.app.signature(a),s.app.signature({...a,servidor_agora:'b',tempo_resposta_segundos:4}));assert.notEqual(s.app.signature(a),s.app.signature({...a,status:'EM_ATENDIMENTO'}));assert.notEqual(s.app.signature(a),s.app.signature({...a,fora_sla:true}));});

test('a transfer notification rings and opens a popup once for the destination technician',async()=>{const s=setup();s.start();await flush();s.document.dispatchEvent({type:'pointerdown',isTrusted:true,target:{closest:()=>null}});await flush();s.setResponse([{...ticket(30),titulo:'Chamado transferido para você'}]);await s.poll();assert.equal(s.elements.get('liveTicketAlerts').children.length,1);assert.equal(s.elements.get('liveTicketAlerts').children[0].children[0].textContent,'Chamado transferido para você');assert.equal(s.tones,2);await s.poll();assert.equal(s.tones,2);});

test('cancellation alerts identify the ticket and do not repeat',async()=>{for(const title of ['Chamado cancelado','Chamado cancelado pelo solicitante']){const s=setup();s.start();await flush();s.setResponse([{...ticket(31),titulo:title}]);await s.poll();const alerts=s.elements.get('liveTicketAlerts');assert.equal(alerts.children.length,1);assert.match(alerts.children[0].children[0].textContent,/#000045 cancelado/);await s.poll();assert.equal(alerts.children.length,1);}});
