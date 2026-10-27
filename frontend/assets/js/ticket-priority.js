window.TicketPriority={
 compare(a,b){
  const urgent=ticket=>ticket.prioridade==='CRITICA'?0:ticket.fora_sla?1:2;
  return urgent(a)-urgent(b)||Number(Boolean(b.minha_area))-Number(Boolean(a.minha_area))||new Date(a.inicio_ciclo||a.abertura)-new Date(b.inicio_ciclo||b.abertura)||Number(a.id)-Number(b.id);
 }
};
