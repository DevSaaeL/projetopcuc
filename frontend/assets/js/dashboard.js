function localDoChamado(chamado) {
 const setor = chamado.setor?.trim();
 if (setor) return setor;
 const local = [chamado.bloco?.trim(), chamado.sala?.trim()].filter(Boolean).join(' / ');
 return local || chamado.filial_nome?.trim() || 'Não informado';
}
async function carregarDashboard(){
 const data=await API.get('/dashboard');const counts=data.por_status||{};
 document.querySelectorAll('.stat-value').forEach((el,i)=>el.textContent=[counts.ABERTO||0,counts.EM_ATENDIMENTO||0,data.finalizados_hoje,data.fora_sla][i]);
 document.querySelector('tbody').innerHTML=data.chamados.slice(0,5).map(t=>`<tr><td><a href="chamado-detalhes.html?id=${t.id}">${App.esc(t.protocolo)}</a></td><td>${App.esc(t.titulo)}</td><td>${App.esc(localDoChamado(t))}</td><td><span class="priority priority-${App.priorityClass[t.prioridade]}">${App.labels[t.prioridade]}</span></td><td><span class="status-badge status-${App.statusClass[t.status]}">${App.labels[t.status]}</span></td></tr>`).join('')||'<tr><td colspan="5" class="text-center text-muted">Nenhum chamado registrado.</td></tr>';
 const cards=[...document.querySelectorAll('.dashboard-card')];const sla=cards.find(c=>c.querySelector('h5')?.textContent.trim()==='SLA');sla.querySelectorAll('strong').forEach((e,i)=>e.textContent=[App.duration(data.tempo_medio_resposta),App.duration(data.tempo_medio_resolucao),data.dentro_sla_percentual.toFixed(1)+'%'][i]);sla.querySelectorAll('.progress-bar').forEach((e,i)=>e.style.width=(i===2?data.dentro_sla_percentual:0)+'%');
 const statuses=cards.find(c=>c.querySelector('h5')?.textContent.trim()==='Chamados por status');statuses.querySelectorAll('strong').forEach((e,i)=>e.textContent=[counts.ABERTO||0,(counts.EM_ATENDIMENTO||0)+(counts.AGUARDANDO_USUARIO||0),counts.FINALIZADO||0,data.fora_sla][i]);
 const categories=cards.find(c=>c.querySelector('h5')?.textContent.trim()==='Principais categorias');categories.innerHTML='<h5 class="fw-bold mb-4">Principais categorias</h5>'+Object.entries(data.por_categoria).sort((a,b)=>b[1]-a[1]).slice(0,4).map(([name,count])=>`<div class="d-flex justify-content-between mb-3"><span>${App.esc(name||'Sem categoria')}</span><strong>${count}</strong></div><div class="progress mb-3"><div class="progress-bar" data-percent="${data.total?count/data.total*100:0}"></div></div>`).join('');categories.querySelectorAll('[data-percent]').forEach(e=>e.style.width=e.dataset.percent+'%');
 document.querySelectorAll('[href="chamados.html"] .menu-badge').forEach(e=>e.textContent=data.total);
}
App.start(async user=>{
 document.querySelector('.page-content h2').textContent='Olá, '+user.nome+' ';
 await carregarDashboard();LiveUpdates.subscribe(carregarDashboard);
});

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { window.location.href='notificacoes.html'; });
