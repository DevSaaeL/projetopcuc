TicketList.init(false);
function exportTickets(type){TicketList.export(type);}

// Eventos da estrutura HTML, registrados sem atributos executáveis.
document.querySelector('[data-event-0]')?.addEventListener('click', function(event) { exportTickets('excel'); });
document.querySelector('[data-event-1]')?.addEventListener('click', function(event) { exportTickets('csv'); });
document.querySelector('[data-event-2]')?.addEventListener('click', function(event) { exportTickets('pdf'); });
