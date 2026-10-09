(async()=>{
 const form=document.getElementById('publicTicketForm'),error=document.getElementById('error'),button=document.getElementById('send');
 const qrId=new URLSearchParams(location.search).get('qr');
 const showError=message=>{error.textContent=message;error.hidden=false;};
 let requestId=crypto.randomUUID();
 try{requestId=sessionStorage.getItem('ticket-request-'+qrId)||requestId;sessionStorage.setItem('ticket-request-'+qrId,requestId);}catch{}
 try{
  if(!qrId)throw Error('Leia o QR Code do local para abrir um chamado.');
  const local=await API.get('/public/qrcodes/'+encodeURIComponent(qrId));
  document.getElementById('locationName').textContent=local.filial_nome;
  document.getElementById('locationDetails').textContent=[local.cidade,'Bloco '+local.bloco,'Sala '+local.sala].join(' · ');
  form.hidden=false;
 }catch(e){document.getElementById('locationName').textContent='Local não identificado';showError(e.message);return;}
 form.addEventListener('submit',async event=>{
  event.preventDefault();if(button.disabled)return;
  const nome=document.getElementById('nome').value.trim(),descricao=document.getElementById('descricao').value.trim();
  if(!nome||!descricao){showError('Preencha seu nome e descreva o problema.');return;}
  error.hidden=true;button.disabled=true;button.textContent='Enviando…';
  try{const result=await API.post('/public/chamados',{qrId,nome,descricao,requestId});document.getElementById('protocol').textContent=result.protocolo;form.hidden=true;document.getElementById('success').hidden=false;try{sessionStorage.removeItem('ticket-request-'+qrId);}catch{}}
  catch(e){showError(e.message);button.disabled=false;button.textContent='Enviar chamado →';}
 });
})();
