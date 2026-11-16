window.TicketPriority={
 compare(a,b){
  return Number(Boolean(b.minha_filial_prioritaria))-Number(Boolean(a.minha_filial_prioritaria))||Number(Boolean(b.minha_area))-Number(Boolean(a.minha_area))||new Date(a.inicio_ciclo||a.abertura)-new Date(b.inicio_ciclo||b.abertura)||Number(a.id)-Number(b.id);
 }
};
