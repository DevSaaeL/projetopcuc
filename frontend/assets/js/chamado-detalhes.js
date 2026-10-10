const chamadoId=Number(new URLSearchParams(location.search).get('id'));let chamadoAtual;
function voltarChamados(){location.href='chamados.html';}
function finalizarChamado(){bootstrap.Modal.getOrCreateInstance(App.$('modalFinalizar')).show();}
async function agir(acao,solucao=''){await API.post('/chamados/'+chamadoId+'/acoes',{acao,solucao});await carregarDetalhes();}
function assumirChamado(){if(confirm('Deseja assumir este chamado?'))App.run(()=>agir('assumir'));}
function cancelarChamado(){if(confirm('Deseja cancelar este chamado?'))App.run(()=>agir('cancelar'));}
function aguardarUsuario(){if(confirm('Aguardar resposta do usuário?'))App.run(()=>agir('aguardar'));}
function confirmarFinalizacao(){App.run(async()=>{const solucao=App.$('solucaoFinal').value.trim();if(!solucao)throw Error('Informe a solução aplicada.');await agir('finalizar',solucao);bootstrap.Modal.getInstance(App.$('modalFinalizar')).hide();});}
async function carregarHistorico(){const history=await API.get('/chamados/'+chamadoId+'/historico');document.querySelector('.timeline').innerHTML=history.map(h=>`<div class="timeline-item"><div class="timeline-icon"><i class="bi bi-clock"></i></div><div class="timeline-title">${App.esc(App.labels[h.acao]||h.acao)}</div><div class="timeline-text">${App.esc(h.autor)}: ${App.esc(h.descricao)}</div><div class="timeline-time">${App.date(h.criado_em)}</div></div>`).join('');}
async function carregarAnexos(){const rows=await API.get('/chamados/'+chamadoId+'/anexos');App.$('anexosLista').innerHTML=rows.map(a=>`<div class="attachment-item"><div class="attachment-info"><div class="attachment-icon"><i class="bi bi-file-earmark"></i></div><div>${App.esc(a.nome)}<br><small>${(a.tamanho/1024).toFixed(1)} KB</small></div></div><button class="btn btn-sm btn-outline-puc" data-download="${a.id}" data-name="${App.esc(a.nome)}" title="Baixar"><i class="bi bi-download"></i></button></div>`).join('')||'<p class="text-muted">Nenhum anexo.</p>';}
async function carregarDetalhes(){
 chamadoAtual=await API.get('/chamados/'+chamadoId);const t=chamadoAtual;
 document.querySelector('.breadcrumb-custom strong').textContent=t.protocolo;document.querySelector('.ticket-header .ticket-number').textContent='Chamado '+t.protocolo;document.querySelector('.ticket-header .ticket-title').textContent=t.titulo;
 const badges=document.querySelectorAll('.ticket-header .badge-status');badges[0].className='badge-status status-'+App.statusClass[t.status];badges[0].textContent=App.labels[t.status];badges[1].className='badge-status priority-'+App.priorityClass[t.prioridade];badges[1].textContent=App.labels[t.prioridade]+' prioridade';badges[2].textContent=t.categoria;
 const values={Solicitante:t.solicitante,'E-mail':t.solicitante_email,Telefone:t.solicitante_telefone,Cidade:t.cidade,Filial:t.filial_nome,Setor:t.setor,Bloco:t.bloco,Sala:t.sala,'Técnico responsável':t.tecnico||'Não atribuído'};
 document.querySelectorAll('.info-item').forEach(e=>e.querySelector('.info-value').textContent=values[e.querySelector('.info-label').textContent.trim()]||'—');document.querySelector('.description-box').textContent=t.descricao;
 const vals=[App.duration(t.tempo_resposta_segundos),App.duration(t.atendimento?t.tempo_total_segundos-t.tempo_resposta_segundos:0),t.fora_sla?'Fora do prazo':'Dentro do prazo',App.duration(t.tempo_total_segundos)];document.querySelectorAll('.sla-card .sla-value').forEach((e,i)=>e.textContent=vals[i]);
 const small=document.querySelectorAll('.sla-card small');small[0].textContent='SLA: '+t.sla_resposta+' minutos';small[1].textContent='SLA: '+t.sla_resolucao+' minutos';small[2].textContent=t.finalizacao?'Encerrado em '+App.date(t.finalizacao):App.duration(Math.max(0,t.sla_resolucao*60-t.tempo_total_segundos))+' restantes';
 const sla=document.querySelector('.col-lg-4 .card-custom:last-child');sla.querySelectorAll('strong').forEach((e,i)=>e.textContent=(i===0?t.sla_resposta:t.sla_resolucao)+' minutos');sla.querySelectorAll('.progress-bar').forEach((e,i)=>e.style.width=Math.min(100,(i===0?t.tempo_resposta_segundos/(t.sla_resposta*60):t.tempo_total_segundos/(t.sla_resolucao*60))*100)+'%');sla.querySelector('.alert').textContent=t.fora_sla?'Chamado fora do SLA.':'Chamado dentro do SLA.';
 const terminal=['FINALIZADO','CANCELADO'].includes(t.status);
 document.querySelectorAll('[data-ticket-action]').forEach(b=>{b.hidden=!Auth.support();b.disabled=terminal;});
 await Promise.all([carregarHistorico(),carregarAnexos()]);
}
App.start(async()=>{
 if(!Number.isSafeInteger(chamadoId)||chamadoId<1)throw Error('Selecione um chamado na lista para visualizar os detalhes.');
 App.$('anexoInput').addEventListener('change',()=>App.run(async()=>{for(const file of App.$('anexoInput').files){const data=new FormData();data.append('arquivo',file);await API.post('/chamados/'+chamadoId+'/anexos',data);}App.$('anexoInput').value='';await carregarAnexos();}));
 document.querySelectorAll('[data-anexar]').forEach(b=>b.addEventListener('click',()=>App.$('anexoInput').click()));
 App.$('anexosLista').addEventListener('click',event=>{const b=event.target.closest('[data-download]');if(b)App.run(async()=>App.saveFile(await API.request('/chamados/'+chamadoId+'/anexos/'+b.dataset.download,{blob:true}),b.dataset.name));});
 await carregarDetalhes();const timer=setInterval(()=>{if(!document.hidden)App.run(carregarDetalhes);},15000);window.addEventListener('pagehide',()=>clearInterval(timer));
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
