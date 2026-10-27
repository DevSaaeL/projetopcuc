window.PublicTracking = (() => {
 const labels={ABERTO:'Aguardando a equipe',EM_ATENDIMENTO:'Chamado assumido pela equipe',AGUARDANDO_USUARIO:'Aguardando retorno do solicitante',FINALIZADO:'Atendimento finalizado',CANCELADO:'Chamado cancelado'};
 const terminal=ticket=>['FINALIZADO','CANCELADO'].includes(ticket.status);
 function message(ticket){return terminal(ticket)?'Este chamado foi encerrado.':ticket.posicao_fila?ticket.posicao_fila+'º na fila da filial. A fila inclui chamados já assumidos e ainda não encerrados.':'Atualizando posição na fila…';}
 function url(ticket){return 'acompanhar.html#'+encodeURIComponent(ticket.acompanhamento_token);}
 function render(container,ticket){
  container.replaceChildren();
  for(const text of [ticket.protocolo+' · '+ticket.titulo,labels[ticket.status]||ticket.status,message(ticket)]){const line=document.createElement('p');line.textContent=text;container.append(line);}
 }
 return {labels,terminal,message,url,render};
})();
