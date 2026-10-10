window.QrCamera = (() => {
 let stream, timer, generation=0, onRead;
 const el=id=>document.getElementById(id);
 function stop(){
  generation++; clearTimeout(timer);
  stream?.getTracks().forEach(track=>track.stop());stream=null;
  const video=el('qrVideo');if(video){video.srcObject=null;video.hidden=true;}
  if(el('startCamera'))el('startCamera').disabled=false;
  if(el('stopCamera'))el('stopCamera').hidden=true;
 }
 function parse(text){
  const url=new URL(text);
  const id=url.searchParams.get('qr');
  if(url.origin!==location.origin||!['novo-chamado.html','solicitar.html'].includes(url.pathname.split('/').pop())||!id||!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id))throw Error('Este QR Code não pertence ao sistema.');
  return id;
 }
 async function start(){
  stop();const attempt=++generation;el('startCamera').disabled=true;el('stopCamera').hidden=false;
  el('cameraStatus').textContent='Permita o acesso à câmera para ler o QR Code.';
  try{
   if(!navigator.mediaDevices?.getUserMedia)throw Error('A câmera precisa de HTTPS e de um navegador com acesso à câmera.');
   const opened=await navigator.mediaDevices.getUserMedia({video:{facingMode:{ideal:'environment'}},audio:false});
   if(attempt!==generation){opened.getTracks().forEach(track=>track.stop());return;}
   stream=opened;const video=el('qrVideo');video.srcObject=stream;video.hidden=false;await video.play();
   if(attempt!==generation)return;
   el('cameraStatus').textContent='Aponte a câmera para o QR Code da sala.';
   const canvas=el('qrCanvas'),context=canvas.getContext('2d',{willReadFrequently:true});
   let lastCode='', lastRead=0;
   const scan=async()=>{
    if(attempt!==generation)return;
    try{
     if(video.readyState>=2&&video.videoWidth){
      canvas.width=Math.min(640,video.videoWidth);canvas.height=Math.round(video.videoHeight*canvas.width/video.videoWidth);
      context.drawImage(video,0,0,canvas.width,canvas.height);
      const frame=context.getImageData(0,0,canvas.width,canvas.height),code=jsQR(frame.data,frame.width,frame.height);
      if(code&&(code.data!==lastCode||Date.now()-lastRead>3000)){lastCode=code.data;lastRead=Date.now();await onRead(parse(code.data));if(attempt!==generation)return;stop();el('cameraStatus').textContent='Local identificado. Preencha a descrição do chamado.';return;}
     }
    }catch(error){el('cameraStatus').textContent=error.message;}
    if(attempt===generation)timer=setTimeout(scan,200);
   };
   scan();
  }catch(error){if(attempt!==generation)return;stop();el('cameraStatus').textContent=error.name==='NotAllowedError'?'Acesso à câmera negado. Libere a permissão nas configurações do navegador e tente novamente.':error.name==='NotFoundError'?'Nenhuma câmera encontrada neste dispositivo.':error.name==='NotReadableError'?'A câmera está em uso por outro aplicativo. Feche-o e tente novamente.':error.message;}
 }
 function mount(callback){onRead=callback;el('startCamera').addEventListener('click',start);el('stopCamera').addEventListener('click',()=>{stop();el('cameraStatus').textContent='Câmera fechada.';});window.addEventListener('pagehide',stop);document.addEventListener('visibilitychange',()=>{if(document.hidden)stop();});}
 return {mount,stop,parse};
})();
