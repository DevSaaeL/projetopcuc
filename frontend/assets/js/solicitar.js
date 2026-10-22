(async()=>{
 const form=document.getElementById('publicTicketForm'),error=document.getElementById('error'),button=document.getElementById('send');
 const qrId=new URLSearchParams(location.search).get('qr');
 const showError=message=>{error.textContent=message;error.hidden=false;};
 let pending;try{pending=JSON.parse(sessionStorage.getItem('ticket-pending-'+qrId));}catch{}
 if(!pending||!/^[a-f0-9]{64}$/.test(pending.chave)){pending={requestId:crypto.randomUUID(),chave:Array.from(crypto.getRandomValues(new Uint8Array(32)),v=>v.toString(16).padStart(2,'0')).join('')};try{sessionStorage.setItem('ticket-pending-'+qrId,JSON.stringify(pending));}catch{}}
 const {requestId,chave}=pending;RequesterTickets.show(qrId);
 try{
  QrCamera.mount(async id=>{await API.get('/public/qrcodes/'+encodeURIComponent(id));location.replace('solicitar.html?qr='+encodeURIComponent(id));});
  if(!qrId){document.getElementById('locationName').textContent='Leia o QR Code da sala';return;}
  const local=await API.get('/public/qrcodes/'+encodeURIComponent(qrId));
  document.getElementById('locationName').textContent=local.filial_nome;
  document.getElementById('locationDetails').textContent=[local.cidade,'Bloco '+local.bloco,'Sala '+local.sala].join(' · ');
  const topics=local.assuntos||[];const options=document.getElementById('topicOptions');
  for(const topic of topics){const label=document.createElement('label');label.className='topic-option';const input=document.createElement('input');input.type='radio';input.name='assuntoId';input.value=topic.id;input.required=true;const text=document.createElement('span');text.textContent=topic.nome;label.append(input,text);options.append(label);}
  document.getElementById('topicsEmpty').hidden=topics.length>0;button.disabled=!topics.length;
  form.hidden=false;document.querySelector('.qr-camera-panel').hidden=true;
 }catch(e){document.getElementById('locationName').textContent='Local não identificado';showError(e.message);return;}
 form.addEventListener('submit',async event=>{
  event.preventDefault();if(button.disabled)return;
  const nome=document.getElementById('nome').value.trim();
  const assuntoId=Number(form.querySelector('[name="assuntoId"]:checked')?.value);
  if(!nome){showError('Informe seu nome para abrir o chamado.');return;}
  if(!assuntoId){showError('Selecione o assunto do chamado.');return;}
  error.hidden=true;button.disabled=true;button.textContent='Enviando…';
  try{const result=await API.post('/public/chamados',{qrId,nome,assuntoId,requestId,chave});document.getElementById('protocol').textContent=result.protocolo;form.hidden=true;document.getElementById('success').hidden=false;await RequesterTickets.remember(result,qrId,chave);try{sessionStorage.removeItem('ticket-pending-'+qrId);}catch{}}
  catch(e){showError(e.message);button.disabled=false;button.textContent='Enviar chamado →';}
 });
})();
