const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const context={window:{}};vm.runInNewContext(fs.readFileSync('frontend/assets/js/ticket-priority.js','utf8'),context);const compare=context.window.TicketPriority.compare;
const ticket=(id,extra={})=>({id,prioridade:'MEDIA',minha_area:false,fora_sla:false,abertura:'2026-01-01T00:00:00Z',...extra});
test('critical then overdue precede block affinity, then oldest ticket wins',()=>{
 const calls=[ticket(1,{minha_area:true}),ticket(2,{fora_sla:true}),ticket(3,{prioridade:'CRITICA'}),ticket(4),ticket(5,{minha_area:true,abertura:'2025-12-31T00:00:00Z'})];
 assert.deepEqual(calls.sort(compare).map(t=>t.id),[3,2,5,1,4]);
});
test('coverage retains other blocks and ordering leaves public queue values untouched',()=>{
 const calls=[ticket(1,{posicao_fila:1}),ticket(2,{minha_area:true,posicao_fila:2})];
 assert.deepEqual(calls.sort(compare).map(t=>t.posicao_fila),[2,1]);assert.equal(calls.length,2);
});
