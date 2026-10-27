const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const code=fs.readFileSync(require('node:path').join(__dirname,'../frontend/assets/js/novo-chamado.js'),'utf8');
function destination(search){let target;vm.runInNewContext(code,{URLSearchParams,location:{protocol:'https:',search,hash:'',replace:value=>target=value}});return target;}
test('printed QR links keep their location parameters when opening the public form',()=>{
 const query='?qr=62c99bb1-6302-435a-b0c9-be6b92d5b9dc&filial=1&bloco=Central&sala=101';
 assert.equal(destination(query),'solicitar.html'+query);
});
test('the retired staff creation page returns to the ticket list without a QR',()=>{
 assert.equal(destination(''),'chamados.html');assert.equal(destination('?filial=1'),'chamados.html');
});
test('the legacy QR reader preserves the printed QR location and fragment',()=>{
 const reader=fs.readFileSync(require('node:path').join(__dirname,'../frontend/assets/js/ler-qrcode.js'),'utf8');let target;
 vm.runInNewContext(reader,{location:{protocol:'https:',search:'?qr=11111111-2222-3333-4444-555555555555',hash:'#local',replace:value=>target=value}});
 assert.equal(target,'solicitar.html?qr=11111111-2222-3333-4444-555555555555#local');
});
