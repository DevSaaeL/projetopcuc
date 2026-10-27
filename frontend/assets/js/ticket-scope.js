window.TicketScope=(()=>{
 function apply(user,admin){
  const locations=user.locais||[],technician=user.perfil==='SUPORTE';
  for(const id of ['priorityFilter','categoryFilter','slaFilter','cityFilter','branchFilter','technicianFilter']){
   const select=App.$(id);if(!select)continue;
   const visible=admin||(technician&&id==='branchFilter'&&locations.length>1);
   select.closest('[data-ticket-filter]').hidden=!visible;select.disabled=!visible;
   if(!visible)select.value='';
  }
  if(technician){
   const search=App.$('searchInput')?.closest('[data-basic-filter]'),status=App.$('statusFilter')?.closest('[data-basic-filter]'),period=App.$('dateFilter')?.closest('[data-basic-filter]');
   if(search&&status&&period){
    const previousRow=period.parentElement,row=search.parentElement;
    if(previousRow!==row){row.append(period);if(locations.length>1)row.append(App.$('branchFilter').closest('[data-ticket-filter]'));previousRow.hidden=true;}
    search.className='col-12 col-lg-6';status.className=period.className='col-6 col-lg-3';
   }
  }
  const scope=App.$('ticketScope');if(!scope)return;
  const priorities=user.blocos_prioritarios||[],only=App.$('onlyMyArea'),priorityScope=App.$('ticketPriorityScope');
  if(only){only.disabled=!technician||!priorities.length;if(only.disabled)only.checked=false;App.$('onlyMyAreaField').hidden=only.disabled;}
  if(priorityScope){priorityScope.hidden=!technician;priorityScope.textContent=priorities.length?'Blocos prioritários: '+priorities.map(block=>block.nome+' ('+block.filial_nome+')').join(', ')+'. Todos os blocos da filial continuam disponíveis.':'Sem blocos prioritários definidos. Todos os chamados das suas filiais continuam disponíveis.';}
  scope.hidden=!technician;
  if(technician)scope.textContent=locations.length===1?'Filial de atuação: '+locations[0].nome:locations.length?'Filiais de atuação: '+locations.map(location=>location.nome).join(', '):'Nenhuma filial atribuída. Procure o administrador para configurar seu acesso.';
 }
 return {apply};
})();
