const detailQuery=new URLSearchParams(location.search),historyView=detailQuery.has('atendimento'),attendanceId=Number(detailQuery.get('atendimento')),chamadoId=Number(detailQuery.get('id'));const detailResource=()=>historyView?'/atendimentos/'+attendanceId:'/chamados/'+chamadoId;let chamadoAtual,receivedAt=0,loadingDetails=false;
function voltarChamados(){location.href=historyView?'meus-chamados.html':'chamados.html';}
function finalizarChamado(){bootstrap.Modal.getOrCreateInstance(App.$('modalFinalizar')).show();}
async function agir(acao,solucao=''){const result=await API.post('/chamados/'+chamadoId+'/acoes',{acao,solucao});if(result.registro_atendimento){location.replace('chamado-detalhes.html?atendimento='+result.registro_atendimento);return;}await carregarDetalhes();}
function assumirChamado(){if(confirm('Deseja assumir este chamado?'))App.run(()=>agir('assumir'));}
function cancelarChamado(){if(!Auth.admin())return;if(confirm('Deseja cancelar este chamado?'))App.run(()=>agir('cancelar'));}
function aguardarUsuario(){if(confirm('Aguardar resposta do usuário?'))App.run(()=>agir('aguardar'));}
function confirmarFinalizacao(){App.run(async()=>{const solucao=App.$('solucaoFinal').value.trim();if(!solucao)throw Error('Informe a solução aplicada.');await agir('finalizar',solucao);bootstrap.Modal.getInstance(App.$('modalFinalizar')).hide();});}
async function carregarHistorico(){const history=await API.get(detailResource()+'/historico');document.querySelector('.timeline').innerHTML=history.map(h=>`<div class="timeline-item"><div class="timeline-icon"><i class="bi bi-clock"></i></div><div class="timeline-title">${App.esc(App.labels[h.acao]||h.acao)}</div><div class="timeline-text">${App.esc(h.autor)}: ${App.esc(h.descricao)}</div><div class="timeline-time">${App.date(h.criado_em)}</div></div>`).join('');}
async function carregarAnexos(){const rows=await API.get(detailResource()+'/anexos');App.$('anexosLista').innerHTML=rows.map(a=>`<div class="attachment-item"><div class="attachment-info"><div class="attachment-icon"><i class="bi bi-file-earmark"></i></div><div>${App.esc(a.nome)}<br><small>${(a.tamanho/1024).toFixed(1)} KB</small></div></div><button class="btn btn-sm btn-outline-puc" data-download="${a.id}" data-name="${App.esc(a.nome)}" title="Baixar"><i class="bi bi-download"></i></button></div>`).join('')||'<p class="text-muted">Nenhum anexo.</p>';}
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
 const values={Solicitante:t.solicitante,'E-mail':t.solicitante_email,Telefone:t.solicitante_telefone,Cidade:t.cidade,Filial:t.filial_nome,Setor:t.setor,Bloco:t.bloco,Sala:t.sala,'Técnico responsável':t.tecnico||'Não atribuído'};
 document.querySelectorAll('.info-item').forEach(e=>e.querySelector('.info-value').textContent=values[e.querySelector('.info-label').textContent.trim()]||'—');document.querySelector('.description-box').textContent=t.descricao;
 atualizarTemporizadores();
 const terminal=['FINALIZADO','CANCELADO'].includes(t.status);
 const readOnly=historyView||terminal||t.somente_leitura,canAct=Auth.support()&&(Auth.admin()||t.status==='ABERTO'||t.tecnico_id===Auth.user.id);
 document.querySelectorAll('[data-ticket-action]').forEach(b=>{const event=(Object.keys(b.dataset).find(key=>key.startsWith('event'))||'').replaceAll('-','');b.hidden=readOnly||!canAct||(['event2','event8'].includes(event)&&!Auth.admin());b.disabled=terminal||(['event1','event6'].includes(event)&&!['EM_ATENDIMENTO','AGUARDANDO_USUARIO'].includes(t.status))||(event==='event5'&&!['ABERTO','AGUARDANDO_USUARIO'].includes(t.status))||(event==='event7'&&t.status!=='EM_ATENDIMENTO');});
 document.querySelectorAll('[data-anexar]').forEach(button=>button.hidden=readOnly||!canAct);
 App.$('readOnlyActions').hidden=!readOnly&&canAct;App.$('readOnlyActions').textContent=readOnly?'Registro disponível somente para consulta.':'Atendimento sob responsabilidade de outro técnico.';App.$('backToHistory').hidden=!readOnly;App.$('reopenTicket').hidden=!Auth.admin()||!t.pode_reabrir;
 App.$('solutionCard').hidden=!readOnly&&!t.solucao;App.text('recordedSolution',t.solucao||'Nenhuma solução registrada para este encerramento.');App.text('closureBy','Encerrado em '+App.date(t.finalizacao)+(t.finalizado_por_nome?' · Registrado por '+t.finalizado_por_nome:''));
 App.$('attendanceNotice').hidden=!historyView;if(historyView)App.text('attendanceNotice','Atendimento #'+t.registro_id+' · Registro da conclusão, somente para consulta. '+(['ABERTO','EM_ATENDIMENTO','AGUARDANDO_USUARIO'].includes(t.status_atual)?'Este chamado está em um novo ciclo de atendimento. ':'')+(t.encerramento_estimado?'O horário original de encerramento deste registro antigo não está disponível.':''));
 await Promise.all([carregarHistorico(),carregarAnexos()]);}finally{loadingDetails=false;}
}
App.start(async()=>{
 if(!Number.isSafeInteger(historyView?attendanceId:chamadoId)||(historyView?attendanceId:chamadoId)<1)throw Error('Selecione um chamado na lista para visualizar os detalhes.');
 App.$('reopenTicket').addEventListener('click',()=>{if(confirm('Reabrir este chamado? O registro anterior continuará preservado.'))App.run(async()=>{await API.post('/chamados/'+chamadoAtual.id+'/acoes',{acao:'reabrir'});location.replace('chamado-detalhes.html?id='+chamadoAtual.id);});});
 App.$('anexoInput').addEventListener('change',()=>App.run(async()=>{for(const file of App.$('anexoInput').files){const data=new FormData();data.append('arquivo',file);await API.post('/chamados/'+chamadoId+'/anexos',data);}App.$('anexoInput').value='';await carregarAnexos();}));
 document.querySelectorAll('[data-anexar]').forEach(b=>b.addEventListener('click',()=>App.$('anexoInput').click()));
 App.$('anexosLista').addEventListener('click',event=>{const b=event.target.closest('[data-download]');if(b)App.run(async()=>App.saveFile(await API.request(detailResource()+'/anexos/'+b.dataset.download,{blob:true}),b.dataset.name));});
 await carregarDetalhes();const clockTimer=setInterval(()=>{if(!document.hidden)atualizarTemporizadores();},1000);document.addEventListener('visibilitychange',()=>{if(!document.hidden){atualizarTemporizadores();App.run(carregarDetalhes);}});window.addEventListener('pagehide',()=>clearInterval(clockTimer));const timer=setInterval(()=>{if(!document.hidden)App.run(carregarDetalhes);},15000);window.addEventListener('pagehide',()=>clearInterval(timer));
});

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { voltarChamados(); });
document.querySelector('[data-event-1]')?.addEventListener('click', function(event) { finalizarChamado(); });
document.querySelector('[data-event-2]')?.addEventListener('click', function(event) { cancelarChamado(); });
document.querySelector('[data-event-5]')?.addEventListener('click', function(event) { assumirChamado(); });
document.querySelector('[data-event-6]')?.addEventListener('click', function(event) { finalizarChamado(); });
document.querySelector('[data-event-7]')?.addEventListener('click', function(event) { aguardarUsuario(); });
document.querySelector('[data-event-8]')?.addEventListener('click', function(event) { cancelarChamado(); });
document.querySelector('[data-event-9]')?.addEventListener('click', function(event) { confirmarFinalizacao(); });
