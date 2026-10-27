const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const load=name=>fs.readFileSync(path.join(__dirname,'../frontend/assets/js/'+name),'utf8');
const flush=()=>new Promise(resolve=>setImmediate(resolve));
async function setup(result){
 const elements=new Map(),writes=[],remembered=[];
 class Element{children=[];value='';hidden=false;disabled=false;textContent='';listeners={};append(...children){this.children.push(...children);}replaceChildren(){this.children=[];}addEventListener(type,fn){this.listeners[type]=fn;}querySelector(){return {value:'1'};}}
 const get=id=>{if(!elements.has(id))elements.set(id,new Element());return elements.get(id);};
 const context={document:{getElementById:get,createElement:()=>new Element(),querySelector:()=>get('camera')},location:{search:'?qr=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'},URLSearchParams,crypto:require('node:crypto').webcrypto,
 sessionStorage:{getItem:()=>null,setItem(){},removeItem(){}},QrCamera:{mount(){}},RequesterTickets:{show(){},remember:async(...args)=>remembered.push(args)},
 API:{get:async()=>({filial_nome:'Londrina',cidade:'Londrina',bloco:'Central',sala:'101',assuntos:[{id:1,nome:'Impressora'}],chamados_ativos:[]}),post:async(path,body)=>{writes.push({path,body});if(result instanceof Error)throw result;return result;}},setTimeout:()=>1,clearTimeout(){},addEventListener(){}};
 context.window=context;vm.createContext(context);vm.runInContext(load('public-tracking.js'),context);vm.runInContext(load('solicitar.js'),context);await flush();get('nome').value='Maria';
 await get('publicTicketForm').listeners.submit({preventDefault(){}});
 return {get,writes,remembered};
}
const ticket={id:1,protocolo:'#000001',titulo:'Impressora',status:'ABERTO',posicao_fila:2,acompanhamento_token:'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'};
test('new submission preserves owner cancellation access and shows tracking link',async()=>{
 const s=await setup(ticket);assert.equal(s.remembered.length,1);assert.equal(s.writes[0].body.chave.length,64);
 assert.equal(s.get('publicTicketForm').hidden,true);assert.match(s.get('trackingLink').href,/acompanhar.html#/);assert.match(s.get('successTracking').children[2].textContent,/2º na fila/);
});
test('duplicate shows existing queue position without claiming another requester cancellation access',async()=>{
 const s=await setup({...ticket,ja_existente:true});assert.equal(s.remembered.length,0);assert.match(s.get('successTitle').textContent,/Já existe/);assert.match(s.get('successTracking').children[2].textContent,/2º na fila/);
});
test('a submission failure keeps the form available to retry',async()=>{
 const s=await setup(Error('Sem conexão'));assert.equal(s.get('send').disabled,false);assert.equal(s.get('publicTicketForm').hidden,false);assert.equal(s.get('error').textContent,'Sem conexão');assert.equal(s.remembered.length,0);
});
