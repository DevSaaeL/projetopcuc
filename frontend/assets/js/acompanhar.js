(()=>{
 const token=location.hash.slice(1);let timer,running=false,stopped=false;
 const error=document.getElementById('trackingError'),button=document.getElementById('refreshTracking');
 async function refresh(){
  if(running||stopped)return;clearTimeout(timer);running=true;button.disabled=true;
  try{
   if(!/^[a-f0-9-]{36}$/i.test(token)){stopped=true;throw Error('Link de acompanhamento inválido. Abra o link recebido ao enviar seu chamado.');}
   const ticket=await API.get('/public/acompanhamento/'+encodeURIComponent(token));
   PublicTracking.render(document.getElementById('tracking'),ticket);error.hidden=true;
   document.getElementById('trackingUpdated').textContent='Atualizado às '+new Date().toLocaleTimeString('pt-BR');
   if(PublicTracking.terminal(ticket))stopped=true;
  }catch(e){error.textContent=e.message;error.hidden=false;if(e.status===404)stopped=true;}
  finally{running=false;button.disabled=false;if(!stopped)timer=setTimeout(refresh,10000);}
 }
 button.addEventListener('click',()=>{stopped=false;refresh();});
 window.addEventListener('pagehide',()=>{stopped=true;clearTimeout(timer);});
 refresh();
})();
