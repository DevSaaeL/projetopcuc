const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const code = fs.readFileSync(require('node:path').join(__dirname, '../frontend/assets/js/api.js'), 'utf8');
function setup(fetch) {
 const context = {fetch, FormData}; context.window = context;
 vm.runInNewContext(code, context); return context.API;
}
const response = data => ({ok:true, text:async()=>JSON.stringify(data)});
test('simultaneous writes share one CSRF initialization', async () => {
 let release, reads = 0; const writes = [];
 const pending = new Promise(resolve => { release = resolve; });
 const api = setup(async (path, options) => {
  if (path.endsWith('/auth/csrf')) { reads++; await pending; return response({headerName:'X-CSRF-TOKEN', token:'shared'}); }
  writes.push(options); return response({ok:true});
 });
 const first = api.post('/first', {}), second = api.put('/second', {});
 release(); await Promise.all([first, second]);
 assert.equal(reads, 1); assert.equal(writes.length, 2);
 assert.ok(writes.every(write => write.headers['X-CSRF-TOKEN'] === 'shared'));
});
test('a failed CSRF initialization can be retried', async () => {
 let reads = 0;
 const api = setup(async path => {
  if (path.endsWith('/auth/csrf')) { if (++reads === 1) throw Error('offline'); return response({headerName:'X-CSRF-TOKEN', token:'retry'}); }
  return response({ok:true});
 });
 await assert.rejects(api.post('/first', {}), /conectar/);
 await api.post('/first', {}); assert.equal(reads, 2);
});

test('concurrent GETs share one request but subsequent reads fetch fresh data',async()=>{let release,calls=0;const wait=new Promise(r=>release=r);const api=setup(async()=>{calls++;await wait;return response({version:calls});});const a=api.get('/chamados'),b=api.get('/chamados');assert.equal(calls,1);release();assert.deepEqual(await a,await b);await api.get('/chamados');assert.equal(calls,2);});
test('failed GET is not cached and can be retried',async()=>{let calls=0;const api=setup(async()=>{if(++calls===1)throw Error('offline');return response([]);});await assert.rejects(api.get('/chamados'));await api.get('/chamados');assert.equal(calls,2);});
