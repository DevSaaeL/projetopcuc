const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const path=require('node:path');
const shared=fs.readFileSync(path.join(__dirname,'../frontend/assets/js/public-tracking.js'),'utf8');
const page=fs.readFileSync(path.join(__dirname,'../frontend/assets/js/acompanhar.js'),'utf8');
const flush=()=>new Promise(resolve=>setImmediate(resolve));
function setup(){
 const elements=new Map(),timers=new Map();let sequence=0,requests=0;
 class Element{children=[];hidden=false;disabled=false;textContent='';listeners={};append(child){this.children.push(child);}replaceChildren(){this.children=[];}addEventListener(type,fn){this.listeners[type]=fn;}}
 const get=id=>{if(!elements.has(id))elements.set(id,new Element());return elements.get(id);};
 let result={protocolo:'#000002',titulo:'Impressora',status:'ABERTO',posicao_fila:2,acompanhamento_token:'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'};
 const context={document:{getElementById:get,createElement:()=>new Element()},location:{hash:'#aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'},API:{get:async()=>{requests++;if(result instanceof Error)throw result;return result;}},setTimeout:fn=>{const id=++sequence;timers.set(id,fn);return id;},clearTimeout:id=>timers.delete(id),addEventListener(){},Date};context.window=context;
 vm.createContext(context);vm.runInContext(shared,context);vm.runInContext(page,context);
 return {get,timers,context,setResult:value=>result=value,get requests(){return requests;},poll:async()=>{const [id,fn]=timers.entries().next().value;timers.delete(id);await fn();}};
}
test('requester sees live queue updates and final status stops automatic polling',async()=>{
 const s=setup();await flush();assert.match(s.get('tracking').children[2].textContent,/2º na fila/);
 s.setResult({protocolo:'#000002',titulo:'Impressora',status:'EM_ATENDIMENTO',posicao_fila:1});await s.poll();
 assert.match(s.get('tracking').children[1].textContent,/assumido/);assert.match(s.get('tracking').children[2].textContent,/1º na fila/);
 s.setResult({protocolo:'#000002',titulo:'Impressora',status:'FINALIZADO',posicao_fila:null});await s.poll();
 assert.match(s.get('tracking').children[1].textContent,/finalizado/);assert.equal(s.timers.size,0);
});
test('tracking preserves last known status and retries a connection failure',async()=>{
 const s=setup();await flush();s.setResult(Error('Sem conexão'));await s.poll();
 assert.equal(s.get('trackingError').hidden,false);assert.match(s.get('tracking').children[2].textContent,/2º na fila/);assert.equal(s.timers.size,1);
 s.setResult({protocolo:'#000002',titulo:'Impressora',status:'ABERTO',posicao_fila:1});await s.poll();assert.equal(s.get('trackingError').hidden,true);
});
test('unknown tracking links stop polling and ticket text is rendered as text',async()=>{
 const s=setup();await flush();const error=Error('Não encontrado');error.status=404;s.setResult(error);await s.poll();assert.equal(s.timers.size,0);
 const container=s.get('safe');s.context.PublicTracking.render(container,{protocolo:'#1',titulo:'<script>private()</script>',status:'ABERTO',posicao_fila:1});assert.match(container.children[0].textContent,/<script>/);
});
