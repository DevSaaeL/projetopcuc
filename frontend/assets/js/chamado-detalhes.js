const detailQuery=new URLSearchParams(location.search),historyView=detailQuery.has('atendimento'),attendanceId=Number(detailQuery.get('atendimento')),chamadoId=Number(detailQuery.get('id'));const detailResource=()=>historyView?'/atendimentos/'+attendanceId:'/chamados/'+chamadoId;let chamadoAtual,receivedAt=0,loadingDetails=false,transferSourceId=null,transferLoading=false;
function voltarChamados(){location.href=historyView?'meus-chamados.html':'chamados.html';}
function finalizarChamado(){bootstrap.Modal.getOrCreateInstance(App.$('modalFinalizar')).show();}
async function agir(acao,solucao=''){const result=await API.post('/chamados/'+chamadoId+'/acoes',{acao,solucao});if(result.registro_atendimento){location.replace('chamado-detalhes.html?atendimento='+result.registro_atendimento);return;}await carregarDetalhes();}
let assumingTicket=false;
function assumirChamado(){if(chamadoAtual?.status!=='ABERTO'){finalizarChamado();return;}if(assumingTicket)return;assumingTicket=true;App.run(async()=>{try{await agir('assumir');}finally{assumingTicket=false;}});}
function cancelarChamado(){if(!Auth.admin())return;if(confirm('Deseja cancelar este chamado?'))App.run(()=>agir('cancelar'));}
function confirmarFinalizacao(){App.run(async()=>{const solucao=App.$('solucaoFinal').value.trim();if(!solucao)throw Error('Informe a solução aplicada.');await agir('finalizar',solucao);bootstrap.Modal.getInstance(App.$('modalFinalizar')).hide();});}
async function carregarHistorico(){const history=await API.get(detailResource()+'/historico');document.querySelector('.timeline').innerHTML=history.map(h=>`<div class="timeline-item"><div class="timeline-icon"><i class="bi bi-clock"></i></div><div class="timeline-title">${App.esc(App.labels[h.acao]||h.acao)}</div><div class="timeline-text">${App.esc(h.autor)}: ${App.esc(h.descricao)}</div><div class="timeline-time">${App.date(h.criado_em)}</div></div>`).join('');}
function atualizarTemporizadores(){
 if(!chamadoAtual)return;const t=chamadoAtual,s=TicketClock.state(t,Math.floor((performance.now()-receivedAt)/1000));
 const cards=document.querySelectorAll('.sla-card');
 const set=(i,label,value,note,late=false)=>{cards[i].querySelector('.sla-label').textContent=label;const el=cards[i].querySelector('.sla-value');el.textContent=value;el.className='sla-value '+(late?'sla-danger':'sla-success');cards[i].querySelector('small').textContent=note;};
 set(0,s.taken?'Assumido em':'Para assumir',s.taken?App.duration(s.response):s.terminal?'Não assumido':TicketClock.countdown(s.responseLeft),'Prazo: '+t.sla_resposta+' minutos'+(t.posicao_fila?' · '+t.posicao_fila+'º na fila':''),s.responseLeft<0&&t.status!=='CANCELADO');
 set(1,s.terminal?'Tempo em atendimento':'Para finalizar',s.taken?(s.terminal?App.duration(s.resolution):TicketClock.countdown(s.resolutionLeft)):'—','Prazo: '+t.sla_resolucao+' minutos'+(s.taken?' a partir da primeira assunção':' após assumir o chamado'),s.resolutionLeft<0&&t.status!=='CANCELADO');
 set(2,'SLA atual',t.status==='CANCELADO'?'Cancelado':s.late?'Fora do prazo':'Dentro do prazo',s.terminal?'Encerrado em '+App.date(t.finalizacao):s.taken?'Prazo de finalização em andamento':'Prazo para assumir em andamento',s.late);
 set(3,'Tempo total',App.duration(s.total),'Desde a abertura');
 const sla=document.querySelector('.col-lg-4 .card-custom:last-child');sla.querySelectorAll('strong').forEach((e,i)=>e.textContent=(i===0?t.sla_resposta:t.sla_resolucao)+' minutos');sla.querySelectorAll('.progress-bar').forEach((e,i)=>e.style.width=Math.min(100,(i===0?s.response/(t.sla_resposta*60):s.resolution/(t.sla_resolucao*60))*100)+'%');sla.querySelector('.alert').textContent=t.status==='CANCELADO'?'Chamado cancelado.':s.late?'Chamado fora do SLA.':'Chamado dentro do SLA.';
}
async function carregarDetalhes(){
 if(loadingDetails)return;loadingDetails=true;try{chamadoAtual=await API.get(detailResource());receivedAt=performance.now();const t=chamadoAtual;
 document.querySelector('.breadcrumb-custom strong').textContent=t.protocolo;document.querySelector('.ticket-header .ticket-number').textContent='Chamado '+t.protocolo;document.querySelector('.ticket-header .ticket-title').textContent=t.titulo;
 const badges=document.querySelectorAll('.ticket-header .badge-status');badges[0].className='badge-status status-'+App.statusClass[t.status];badges[0].textContent=App.labels[t.status];badges[1].className='badge-status priority-'+App.priorityClass[t.prioridade];badges[1].textContent=App.labels[t.prioridade]+' prioridade';badges[2].textContent=t.categoria;
 const values={Solicitante:t.solicitante,Cidade:t.cidade,Filial:t.filial_nome,Setor:t.setor,Bloco:t.bloco,Sala:t.sala,'Técnico responsável':t.tecnico||'Não atribuído'};
 document.querySelectorAll('.info-item').forEach(e=>e.querySelector('.info-value').textContent=values[e.querySelector('.info-label').textContent.trim()]||'—');document.querySelector('.description-box').textContent=t.descricao;
 atualizarTemporizadores();
 const cancellation=App.$('requesterCancellationNotice');cancellation.hidden=!t.cancelado_por_solicitante;cancellation.textContent=t.cancelado_por_solicitante?'Chamado cancelado pelo solicitante '+t.solicitante+'. Motivo: '+t.motivo_cancelamento:'';
 App.$('transferTicket').hidden=historyView||t.somente_leitura||!['EM_ATENDIMENTO','AGUARDANDO_USUARIO'].includes(t.status)||!t.tecnico_id||!(Auth.admin()||t.tecnico_id===Auth.user.id);
 const terminal=['FINALIZADO','CANCELADO'].includes(t.status);
 const readOnly=historyView||terminal||t.somente_leitura,canAct=Auth.support()&&(Auth.admin()||t.status==='ABERTO'||t.tecnico_id===Auth.user.id);
 document.querySelectorAll('[data-ticket-action]').forEach(b=>{const event=(Object.keys(b.dataset).find(key=>key.startsWith('event'))||'').replaceAll('-','');b.hidden=readOnly||!canAct||(['event2','event8'].includes(event)&&!Auth.admin());b.disabled=terminal;});
 App.$('readOnlyActions').hidden=!readOnly&&canAct;App.$('readOnlyActions').textContent=readOnly?'Registro disponível somente para consulta.':'Atendimento sob responsabilidade de outro técnico.';App.$('backToHistory').hidden=!readOnly;App.$('reopenTicket').hidden=!Auth.admin()||!t.pode_reabrir;
 App.$('solutionCard').hidden=!readOnly&&!t.solucao;App.text('recordedSolution',t.solucao||'Nenhuma solução registrada para este encerramento.');App.text('closureBy','Encerrado em '+App.date(t.finalizacao)+(t.finalizado_por_nome?' · Registrado por '+t.finalizado_por_nome:''));
 App.$('attendanceNotice').hidden=!historyView;if(historyView)App.text('attendanceNotice','Atendimento #'+t.registro_id+' · Registro da conclusão, somente para consulta. '+(['ABERTO','EM_ATENDIMENTO','AGUARDANDO_USUARIO'].includes(t.status_atual)?'Este chamado está em um novo ciclo de atendimento. ':'')+(t.encerramento_estimado?'O horário original de encerramento deste registro antigo não está disponível.':''));
 const primary=document.querySelector('[data-event-5]');const finishing=t.status!=='ABERTO';primary.disabled=readOnly||!canAct;primary.className='btn '+(finishing?'btn-success':'btn-puc-primary');primary.textContent=finishing?'Finalizar chamado':'Assumir chamado';primary.setAttribute('aria-label',primary.textContent);
 await carregarHistorico();}finally{loadingDetails=false;}
}
App.start(async()=>{
 if(!Number.isSafeInteger(historyView?attendanceId:chamadoId)||(historyView?attendanceId:chamadoId)<1)throw Error('Selecione um chamado na lista para visualizar os detalhes.');
 App.$('transferTicket').addEventListener('click',async()=>{
  if(transferLoading)return;transferSourceId=chamadoAtual.tecnico_id;App.$('transferForm').reset();App.$('transferError').hidden=true;App.$('confirmTransfer').disabled=true;App.options('transferTechnician',[],'id','nome','Carregando técnicos…');bootstrap.Modal.getOrCreateInstance(App.$('transferModal')).show();
  transferLoading=true;
  try{const options=await API.get('/chamados/'+chamadoId+'/tecnicos-transferencia');App.options('transferTechnician',options,'id','nome',options.length?'Selecione o técnico':'Nenhum outro técnico disponível nesta unidade');App.$('confirmTransfer').disabled=!options.length;}
  catch(e){App.$('transferError').textContent=e.message;App.$('transferError').hidden=false;}
  finally{transferLoading=false;}
 });
 App.$('transferForm').addEventListener('submit',async event=>{
  event.preventDefault();if(transferLoading)return;const motivo=App.$('transferReason').value.trim();if(!motivo){App.$('transferError').textContent='Informe o motivo da transferência.';App.$('transferError').hidden=false;return;}
  transferLoading=true;App.$('confirmTransfer').disabled=true;App.$('transferError').hidden=true;
  try{await API.post('/chamados/'+chamadoId+'/transferir',{tecnicoId:Number(App.$('transferTechnician').value),tecnicoAtualId:transferSourceId,motivo});bootstrap.Modal.getInstance(App.$('transferModal')).hide();App.text('transferFeedback','Chamado transferido com sucesso. O novo técnico foi notificado.');App.$('transferFeedback').hidden=false;await carregarDetalhes();}
  catch(e){App.$('transferError').textContent=e.message;App.$('transferError').hidden=false;}
  finally{transferLoading=false;App.$('confirmTransfer').disabled=false;}
 });
 App.$('reopenTicket').addEventListener('click',()=>{if(confirm('Reabrir este chamado? O registro anterior continuará preservado.'))App.run(async()=>{await API.post('/chamados/'+chamadoAtual.id+'/acoes',{acao:'reabrir'});location.replace('chamado-detalhes.html?id='+chamadoAtual.id);});});
 await carregarDetalhes();const clockTimer=setInterval(()=>{if(!document.hidden)atualizarTemporizadores();},1000);document.addEventListener('visibilitychange',()=>{if(!document.hidden){atualizarTemporizadores();App.run(carregarDetalhes);}});window.addEventListener('pagehide',()=>clearInterval(clockTimer));const unsubscribe=LiveUpdates.subscribe(carregarDetalhes);window.addEventListener('pagehide',unsubscribe);
});

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { voltarChamados(); });
document.querySelector('[data-event-2]')?.addEventListener('click', function(event) { cancelarChamado(); });
document.querySelector('[data-event-5]')?.addEventListener('click', function(event) { assumirChamado(); });
document.querySelector('[data-event-8]')?.addEventListener('click', function(event) { cancelarChamado(); });
document.querySelector('[data-event-9]')?.addEventListener('click', function(event) { confirmarFinalizacao(); });
