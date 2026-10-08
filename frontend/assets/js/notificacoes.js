let notificacoes=[],limite=20;
async function carregarNotificacoes(){notificacoes=await API.get('/notificacoes');filtrarNotificacoes();}
document.addEventListener('helpdesk:notifications',event=>{if(!Auth.user)return;notificacoes=event.detail;filtrarNotificacoes();});
function filtrarNotificacoes(){const q=App.$('searchInput').value.toLowerCase(),type=App.$('typeFilter').value,read=App.$('readFilter').value;const rows=notificacoes.filter(n=>(!q||(n.titulo+' '+n.chamado_id).toLowerCase().includes(q))&&(!type||n.tipo===type)&&(!read||(read==='read')===n.lida));App.$('notificationList').innerHTML=rows.slice(0,limite).map(n=>`<div class="notification-item ${n.lida?'':'unread'}" data-id="${n.id}" role="button" tabindex="0"><div class="notification-icon primary"><i class="bi bi-${n.tipo==='mensagem'?'chat-left-text':'ticket-detailed'}"></i></div><div class="notification-content"><div class="notification-top"><strong>${App.esc(n.titulo)}</strong><span class="notification-time">${App.date(n.criado_em)}</span></div><p>Chamado #${String(n.chamado_id).padStart(6,'0')}</p></div>${n.lida?'':'<div class="notification-status"><span class="unread-dot"></span></div>'}</div>`).join('');App.text('notificationResultCount',rows.length);App.text('showingCount',Math.min(rows.length,limite));const unread=notificacoes.filter(n=>!n.lida).length;App.text('unreadCount',unread);App.text('sidebarNotificationCount',unread);App.$('topNotificationDot').hidden=!unread;App.$('emptyState').classList.toggle('d-none',rows.length>0);const counts=document.querySelectorAll('.stat-content strong');counts[1].textContent=notificacoes.length;counts[2].textContent=notificacoes.filter(n=>n.titulo.includes('finalizado')).length;}
function limparFiltros(){for(const id of ['searchInput','typeFilter','readFilter'])App.$(id).value='';filtrarNotificacoes();}
function marcarTodasComoLidas(){App.run(async()=>{await API.put('/notificacoes/lidas');await carregarNotificacoes();});}
function limparNotificacoes(){if(confirm('Limpar todas as notificações?'))App.run(async()=>{await API.delete('/notificacoes');await carregarNotificacoes();});}
function carregarMais(){limite+=20;filtrarNotificacoes();}
App.start(async()=>{const open=event=>{const item=event.target.closest('[data-id]');if(item)App.run(async()=>{const n=notificacoes.find(n=>n.id===Number(item.dataset.id));await API.put('/notificacoes/'+n.id+'/lida');location.href='chamado-detalhes.html?id='+n.chamado_id;});};App.$('notificationList').addEventListener('click',open);App.$('notificationList').addEventListener('keydown',e=>{if(e.key==='Enter')open(e);});await carregarNotificacoes();});

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-1]')?.addEventListener('click', function(event) { logout(); });
document.querySelector('[data-event-2]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-3]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-4]')?.addEventListener('click', function(event) { logout(); });
document.querySelector('[data-event-5]')?.addEventListener('click', function(event) { marcarTodasComoLidas(); });
document.querySelector('[data-event-6]')?.addEventListener('input', function(event) { filtrarNotificacoes(); });
document.querySelector('[data-event-7]')?.addEventListener('change', function(event) { filtrarNotificacoes(); });
document.querySelector('[data-event-8]')?.addEventListener('change', function(event) { filtrarNotificacoes(); });
document.querySelector('[data-event-9]')?.addEventListener('click', function(event) { limparFiltros(); });
document.querySelector('[data-event-10]')?.addEventListener('click', function(event) { limparNotificacoes(); });
document.querySelector('[data-event-11]')?.addEventListener('click', function(event) { limparFiltros(); });
document.querySelector('[data-event-12]')?.addEventListener('click', function(event) { carregarMais(); });
