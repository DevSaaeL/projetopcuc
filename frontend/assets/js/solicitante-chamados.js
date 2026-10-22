window.RequesterTickets=(()=>{
 const storage='helpdesk-requester-tickets-v1';let saved=[];
 try{const data=JSON.parse(localStorage.getItem(storage)||'[]');if(Array.isArray(data))saved=data.filter(v=>Number.isSafeInteger(v.id)&&typeof v.chave==='string'&&/^[a-f0-9]{64}$/.test(v.chave));}catch{}
 const el=(tag,text,className)=>{const e=document.createElement(tag);if(text)e.textContent=text;if(className)e.className=className;return e;};
 const labels={ABERTO:'Aberto',EM_ATENDIMENTO:'Em atendimento',AGUARDANDO_USUARIO:'Aguardando usuário',FINALIZADO:'Finalizado',CANCELADO:'Cancelado'};
 async function show(qr){
  const section=document.getElementById('requesterTickets'),list=document.getElementById('requesterTicketList');list.replaceChildren();
  const items=saved.filter(v=>v.qr===qr);section.hidden=!items.length;
  for(const ticket of items){
   const card=el('article',null,'requester-ticket'),title=el('h3',ticket.protocolo),status=el('p','Consultando situação…'),note=el('p',null,'requester-ticket-note'),error=el('p');error.setAttribute('role','alert');error.className='requester-error';error.hidden=true;
   const open=el('button','Cancelar este chamado','requester-cancel');open.type='button';open.hidden=true;
   const form=el('form'),label=el('label','Por que você deseja cancelar?'),reason=el('textarea'),confirm=el('button','Confirmar cancelamento'),back=el('button','Voltar','requester-back');
   reason.id='cancelReason-'+ticket.id;label.htmlFor=reason.id;reason.required=true;reason.maxLength=1000;reason.rows=3;reason.placeholder='Explique o motivo do cancelamento.';back.type='button';confirm.type='submit';form.hidden=true;form.append(label,reason,el('small','Até 1000 caracteres.'),confirm,back);
   card.append(title,status,note,open,form,error);list.append(card);
   const update=data=>{status.textContent=labels[data.status]||data.status;const closed=['FINALIZADO','CANCELADO'].includes(data.status);open.hidden=closed;form.hidden=true;note.textContent=data.cancelado_por_solicitante?'Você cancelou este chamado. Motivo: '+data.motivo_cancelamento:closed?'Este chamado já foi encerrado.':'Você pode cancelar este chamado informando o motivo.';};
   open.addEventListener('click',()=>{form.hidden=false;open.hidden=true;reason.focus();});back.addEventListener('click',()=>{form.hidden=true;open.hidden=false;error.hidden=true;});
   form.addEventListener('submit',async event=>{
    event.preventDefault();const motivo=reason.value.trim();if(!motivo){reason.setCustomValidity('Informe o motivo do cancelamento.');reason.reportValidity();return;}
    confirm.disabled=back.disabled=true;error.hidden=true;
    try{update(await API.post('/public/chamados/'+ticket.id+'/cancelar',{chave:ticket.chave,motivo}));status.setAttribute('role','status');}
    catch(e){error.textContent=e.message;error.hidden=false;try{update(await API.post('/public/chamados/'+ticket.id+'/consultar',{chave:ticket.chave}));}catch{}}
    finally{confirm.disabled=back.disabled=false;}
   });reason.addEventListener('input',()=>reason.setCustomValidity(''));
   try{update(await API.post('/public/chamados/'+ticket.id+'/consultar',{chave:ticket.chave}));}catch(e){status.textContent='Consulta indisponível';note.textContent='Este acesso pode ter expirado ou o chamado pode estar em um novo atendimento.';error.textContent=e.message;error.hidden=false;}
  }
 }
 function remember(result,qr,chave){if(!Number.isSafeInteger(result.id))return;saved=[{id:result.id,protocolo:result.protocolo,qr,chave},...saved.filter(v=>v.id!==result.id)].slice(0,20);try{localStorage.setItem(storage,JSON.stringify(saved));}catch{document.getElementById('requesterStorageNote').textContent='O navegador não permitiu salvar o acesso. Mantenha esta página aberta para poder cancelar.';}return show(qr);}
 return {show,remember};
})();
