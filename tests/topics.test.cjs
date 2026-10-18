const test = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const code = fs.readFileSync(require('node:path').join(__dirname, '../frontend/assets/js/assuntos.js'), 'utf8');
async function setup() {
 const elements = new Map(), writes = [], errors = [];
 const choices = [{value:'1',checked:true},{value:'2',checked:false}];
 const get = id => {
  if (!elements.has(id)) elements.set(id, {value:'',checked:false,hidden:false,disabled:false,
   listeners:{}, addEventListener(event, handler){this.listeners[event]=handler;}, focus(){},
   querySelectorAll(selector){return selector === 'input:checked' ? choices.filter(c=>c.checked) : choices;}});
  return elements.get(id);
 };
 let ready;
 const context = {App:{$:get,esc:String,text:(id,text)=>get(id).textContent=text,
  options(){},start:fn=>ready=fn(),run:async fn=>{try{await fn();}catch(error){errors.push(error.message);}}},
  API:{get:async path=>path==='/filiais'?[{id:1,nome:'A'},{id:2,nome:'B'}]:[{id:10,nome:'Existente',filial_id:1,filial_nome:'A',ativo:true}],
   post:async(path,body)=>writes.push({path,body}),put:async(path,body)=>writes.push({path,body})}};
 vm.runInNewContext(code,context);await ready;
 return {get,choices,writes,errors,submit:async()=>{get('topicForm').listeners.submit({preventDefault(){}});await new Promise(resolve=>setImmediate(resolve));}};
}
test('topic form sends all selected branches in one batch',async()=>{
 const s=await setup();s.get('selectAllTopicBranches').listeners.click();
 s.get('topicName').value=' Novo assunto ';s.get('topicActive').checked=true;
 await s.submit();assert.equal(s.writes.length,1);
 assert.equal(s.writes[0].path,'/assuntos/lote');
 assert.deepEqual(Array.from(s.writes[0].body.filialIds),[1,2]);
 assert.equal(s.writes[0].body.nome,'Novo assunto');
 assert.match(s.get('topicFeedback').textContent,/2 filial/);
});
test('empty branch selection prevents submission',async()=>{
 const s=await setup();s.get('clearTopicBranches').listeners.click();await s.submit();
 assert.equal(s.writes.length,0);assert.match(s.errors[0],/Selecione ao menos uma filial/);
 assert.equal(s.get('saveTopic').disabled,false);
});
test('editing a topic affects only its original branch',async()=>{
 const s=await setup();
 s.get('topicList').listeners.click({target:{closest:selector=>selector==='[data-edit-topic]'?{dataset:{editTopic:'10'}}:null}});
 assert.equal(s.get('topicBranchesField').hidden,true);
 s.get('topicName').value='Renomeado';await s.submit();
 assert.equal(s.writes[0].path,'/assuntos/10');assert.equal(s.writes[0].body.filialId,1);
 assert.equal(s.get('topicBranchesField').hidden,false);
});
