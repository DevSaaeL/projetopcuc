TicketList.init(true);
function filtrarChamados(){TicketList.filter();}
function limparFiltros(){TicketList.clear();}
function exportar(type){TicketList.export(type);}

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-1]')?.addEventListener('click', function(event) { logout(); });
document.querySelector('[data-event-2]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-3]')?.addEventListener('click', function(event) { toggleSidebar(); });
document.querySelector('[data-event-4]')?.addEventListener('click', function(event) { window.location.href='notificacoes.html'; });
document.querySelector('[data-event-5]')?.addEventListener('click', function(event) { logout(); });
document.querySelector('[data-event-6]')?.addEventListener('input', function(event) { filtrarChamados(); });
document.querySelector('[data-event-7]')?.addEventListener('change', function(event) { filtrarChamados(); });
document.querySelector('[data-event-8]')?.addEventListener('change', function(event) { filtrarChamados(); });
document.querySelector('[data-event-9]')?.addEventListener('change', function(event) { filtrarChamados(); });
document.querySelector('[data-event-10]')?.addEventListener('click', function(event) { limparFiltros(); });
document.querySelector('[data-event-11]')?.addEventListener('click', function(event) { exportar('PDF'); });
document.querySelector('[data-event-12]')?.addEventListener('click', function(event) { exportar('Excel'); });
document.querySelector('[data-event-13]')?.addEventListener('click', function(event) { exportar('CSV'); });
document.querySelector('[data-event-14]')?.addEventListener('click', function(event) { limparFiltros(); });
