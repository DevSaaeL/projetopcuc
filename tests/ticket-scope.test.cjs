const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const code=fs.readFileSync(require('node:path').join(__dirname,'../frontend/assets/js/ticket-scope.js'),'utf8');
function setup(){
 const elements=new Map();for(const id of ['priorityFilter','categoryFilter','slaFilter','cityFilter','branchFilter','technicianFilter']){const field={hidden:true};elements.set(id,{value:'previous-selection',disabled:false,closest:()=>field,field});}
 elements.set('ticketScope',{hidden:true,textContent:''});
 const context={App:{$:id=>elements.get(id)}};context.window=context;vm.runInNewContext(code,context);
 return {elements,apply:context.TicketScope.apply};
}
test('single-branch technician gets branch context and only basic filters',()=>{
 const s=setup();s.apply({perfil:'SUPORTE',locais:[{id:1,nome:'PUC LONDRINA'}]},false);
 for(const [id,input] of s.elements){if(id==='ticketScope')continue;assert.equal(input.field.hidden,true,id);assert.equal(input.disabled,true,id);assert.equal(input.value,'',id);}
 assert.equal(s.elements.get('ticketScope').hidden,false);assert.equal(s.elements.get('ticketScope').textContent,'Filial de atuação: PUC LONDRINA');
});
test('multi-branch technician may switch branches but has no city or technician management filter',()=>{
 const s=setup();s.apply({perfil:'SUPORTE',locais:[{id:1,nome:'Londrina'},{id:2,nome:'Curitiba'}]},false);
 assert.equal(s.elements.get('branchFilter').field.hidden,false);assert.equal(s.elements.get('branchFilter').disabled,false);
 for(const id of ['cityFilter','technicianFilter','priorityFilter','categoryFilter','slaFilter'])assert.equal(s.elements.get(id).field.hidden,true,id);
 assert.match(s.elements.get('ticketScope').textContent,/Londrina, Curitiba/);
});
test('administrators retain all filters and an unassigned technician has no general branch selector',()=>{
 const s=setup();s.apply({perfil:'ADMIN',locais:[{id:1,nome:'Londrina'}]},true);
 for(const [id,input] of s.elements){if(id==='ticketScope')continue;assert.equal(input.field.hidden,false,id);assert.equal(input.disabled,false,id);}
 s.apply({perfil:'SUPORTE',locais:[]},false);assert.match(s.elements.get('ticketScope').textContent,/Nenhuma filial atribuída/);assert.equal(s.elements.get('branchFilter').disabled,true);
});
test('technician basic controls share one row and the empty administrative row is hidden',()=>{
 const s=setup(),first={append(field){field.parentElement=this;}},second={hidden:false};
 const fields={};for(const id of ['searchInput','statusFilter','dateFilter']){const field={parentElement:id==='dateFilter'?second:first};fields[id]=field;s.elements.set(id,{closest:()=>field});}
 s.apply({perfil:'SUPORTE',locais:[{id:1,nome:'Londrina'}]},false);
 assert.equal(fields.dateFilter.parentElement,first);assert.equal(second.hidden,true);assert.equal(fields.searchInput.className,'col-12 col-lg-6');assert.equal(fields.statusFilter.className,'col-6 col-lg-3');
});
