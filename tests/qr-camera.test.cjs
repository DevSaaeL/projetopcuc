const test=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
function setup(media){
 const elements={},events={};
 for(const id of ['startCamera','stopCamera','qrVideo','qrCanvas','cameraStatus'])elements[id]={hidden:true,disabled:false,addEventListener(type,fn){this[type]=fn;}};
 elements.qrVideo.play=async()=>{};elements.qrVideo.readyState=0;
 elements.qrCanvas.getContext=()=>({});
 const context={window:{addEventListener(type,fn){events[type]=fn;}},document:{getElementById:id=>elements[id],addEventListener(){}},navigator:{mediaDevices:{getUserMedia:media}},location:{origin:'https://desk.test'},URL,setTimeout:()=>1,clearTimeout(){}};
 vm.runInNewContext(fs.readFileSync('frontend/assets/js/qr-camera.js','utf8'),context);
 return {camera:context.window.QrCamera,elements,events};
}
test('QR URLs from other origins and malformed identifiers are rejected',()=>{
 const {camera}=setup();const id='11111111-2222-3333-4444-555555555555';
 assert.equal(camera.parse('https://desk.test/pages/novo-chamado.html?qr='+id),id);
 for(const url of ['https://evil.test/pages/novo-chamado.html?qr='+id,'https://desk.test/pages/login.html?qr='+id,'https://desk.test/pages/solicitar.html?qr=../usuarios','javascript:alert(1)'])assert.throws(()=>camera.parse(url));
});
test('camera opens only after click and stops when leaving',async()=>{
 let opened=0,stopped=0;const {camera,elements,events}=setup(async()=>{opened++;return {getTracks:()=>[{stop(){stopped++;}}]};});
 camera.mount(()=>{});assert.equal(opened,0);await elements.startCamera.click();assert.equal(opened,1);assert.equal(elements.qrVideo.hidden,false);
 events.pagehide();assert.equal(stopped,1);assert.equal(elements.qrVideo.srcObject,null);assert.equal(elements.startCamera.disabled,false);
});
test('closing while permission is pending stops the delayed camera stream',async()=>{
 let resolve,stopped=0;const {camera,elements}=setup(()=>new Promise(r=>resolve=r));camera.mount(()=>{});
 const opening=elements.startCamera.click();elements.stopCamera.click();resolve({getTracks:()=>[{stop(){stopped++;}}]});await opening;
 assert.equal(stopped,1);assert.equal(elements.qrVideo.hidden,true);
});
test('denied permission offers retry without leaving camera active',async()=>{
 const {camera,elements}=setup(async()=>{const error=new Error();error.name='NotAllowedError';throw error;});camera.mount(()=>{});await elements.startCamera.click();
 assert.match(elements.cameraStatus.textContent,/negado/);assert.equal(elements.startCamera.disabled,false);assert.equal(elements.qrVideo.hidden,true);
});
