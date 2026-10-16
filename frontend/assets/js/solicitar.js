(async()=>{
 const form=document.getElementById('publicTicketForm'),error=document.getElementById('error'),button=document.getElementById('send');
 const qrId=new URLSearchParams(location.search).get('qr');
 const showError=message=>{error.textContent=message;error.hidden=false;};
 let requestId=crypto.randomUUID();
 try{requestId=sessionStorage.getItem('ticket-request-'+qrId)||requestId;sessionStorage.setItem('ticket-request-'+qrId,requestId);}catch{}
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
  try{const result=await API.post('/public/chamados',{qrId,nome,assuntoId,requestId});document.getElementById('protocol').textContent=result.protocolo;form.hidden=true;document.getElementById('success').hidden=false;try{sessionStorage.removeItem('ticket-request-'+qrId);}catch{}}
  catch(e){showError(e.message);button.disabled=false;button.textContent='Enviar chamado →';}
 });
})();
