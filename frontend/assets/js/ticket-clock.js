window.TicketClock=(()=>{
 const terminal=t=>['FINALIZADO','CANCELADO'].includes(t.status);
 const duration=seconds=>{seconds=Math.max(0,Math.floor(Math.abs(seconds)));return [Math.floor(seconds/3600),Math.floor(seconds/60)%60,seconds%60].map(n=>String(n).padStart(2,'0')).join(':');};
 function state(ticket,elapsed=0){
  const running=!terminal(ticket),taken=!!ticket.atendimento;
  const response=Number(ticket.tempo_resposta_segundos)+(!taken&&running?elapsed:0);
  const resolution=Number(ticket.tempo_atendimento_segundos)+(taken&&running?elapsed:0);
  const responseLeft=Number(ticket.sla_resposta)*60-response,resolutionLeft=Number(ticket.sla_resolucao)*60-resolution;
  return {response,resolution,responseLeft,resolutionLeft,total:Number(ticket.tempo_total_segundos)+(running?elapsed:0),taken,terminal:!running,late:ticket.status!=='CANCELADO'&&(responseLeft<0||resolutionLeft<0)};
 }
 function countdown(seconds){return seconds<0?'Atrasado '+duration(seconds):duration(seconds)+' restantes';}
 function summary(ticket,elapsed=0){
  const s=state(ticket,elapsed);
  if(s.terminal)return {label:ticket.status==='CANCELADO'?'Cancelado':'Finalizado',text:'Total: '+duration(s.total),late:s.late};
  return {label:s.taken?'Para finalizar':'Para assumir',text:countdown(s.taken?s.resolutionLeft:s.responseLeft),late:s.late};
 }
 function render(element,ticket,elapsed=0){const s=summary(ticket,elapsed);element.classList.toggle('ticket-countdown-late',s.late);element.replaceChildren();const label=document.createElement('small');label.textContent=s.label;const value=document.createElement('strong');value.textContent=s.text;element.append(label,value);}
 return {state,summary,countdown,duration,render};
})();
