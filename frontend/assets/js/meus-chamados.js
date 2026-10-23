let attendanceRows=[],attendanceFiltered=[],attendancePage=1,attendanceLoading=false;
let attendanceSignature;
const attendanceSize=10;
const historyValue=id=>App.$(id).value;
const historyDate=value=>new Date(value).toLocaleDateString('en-CA',{timeZone:'America/Sao_Paulo'});
function filterAttendance(reset=true){
 if(reset)attendancePage=1;
 const query=historyValue('historySearch').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
 attendanceFiltered=attendanceRows.filter(row=>{
  const text=[row.protocolo,row.titulo,row.solicitante,row.solucao,row.tecnico,row.filial_nome].join(' ').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase(),date=historyDate(row.finalizacao);
  return (!query||text.includes(query))&&(!historyValue('historyUnit')||String(row.filial_id)===historyValue('historyUnit'))&&(!historyValue('historyTechnician')||String(row.tecnico_id)===historyValue('historyTechnician'))&&(!historyValue('historyStatus')||row.status===historyValue('historyStatus'))&&(!historyValue('historyStart')||date>=historyValue('historyStart'))&&(!historyValue('historyEnd')||date<=historyValue('historyEnd'));
 });
 attendancePage=Math.max(1,Math.min(attendancePage,Math.ceil(attendanceFiltered.length/attendanceSize)||1));
 const rows=attendanceFiltered.slice((attendancePage-1)*attendanceSize,attendancePage*attendanceSize);
 App.$('attendanceRows').innerHTML=rows.map(row=>'<tr><td><strong>'+App.esc(row.protocolo)+'</strong><small>Atendimento #'+row.registro_id+'</small>'+(['ABERTO','EM_ATENDIMENTO','AGUARDANDO_USUARIO'].includes(row.status_atual)?'<small>Chamado em novo atendimento</small>':'')+'</td><td><strong>'+App.esc(row.titulo)+'</strong><small>'+App.esc(row.solicitante)+'</small></td><td>'+App.esc(row.filial_nome)+'<small>'+App.esc([row.bloco,row.sala].filter(Boolean).join(' / '))+'</small></td><td>'+App.esc(row.tecnico||'Não atribuído')+'</td><td class="logs-date">'+App.date(row.finalizacao)+(row.encerramento_estimado?'<small>Horário original indisponível</small>':'')+'</td><td><span class="log-action">'+App.labels[row.status]+'</span></td><td class="logs-description">'+App.esc(row.solucao?row.solucao.slice(0,120):'Sem solução registrada')+'</td><td><a class="btn btn-sm btn-outline-primary" href="chamado-detalhes.html?atendimento='+row.registro_id+'">Consultar</a></td></tr>').join('');
 App.$('attendanceEmpty').hidden=attendanceFiltered.length>0;App.text('attendanceCount',attendanceFiltered.length+' atendimento(s)');App.text('attendancePage','Página '+attendancePage+' de '+Math.max(1,Math.ceil(attendanceFiltered.length/attendanceSize)));App.$('attendancePrevious').disabled=attendancePage<=1;App.$('attendanceNext').disabled=attendancePage*attendanceSize>=attendanceFiltered.length;
}
async function loadAttendance(reset=false){
 if(attendanceLoading)return;attendanceLoading=true;App.$('refreshAttendance').disabled=true;
 try{attendanceRows=await API.get('/atendimentos');const signature=App.signature(attendanceRows);if(signature===attendanceSignature){if(reset)filterAttendance();return;}attendanceSignature=signature;for(const [id,key,label] of [['historyUnit','filial_id','filial_nome'],['historyTechnician','tecnico_id','tecnico']]){const old=historyValue(id),options=[...new Map(attendanceRows.filter(row=>row[key]!=null).map(row=>[row[key],row])).values()];App.options(id,options,key,label,'Todos');App.$(id).value=old;}filterAttendance(reset);}
 finally{attendanceLoading=false;App.$('refreshAttendance').disabled=false;}
}
App.start(async()=>{
 App.text('attendanceTitle',Auth.admin()?'Histórico de chamados':'Meus atendimentos');App.$('historyTechnicianField').hidden=!Auth.admin();
 App.text('attendanceScope',Auth.admin()?'Consulte os atendimentos concluídos das unidades sob sua administração.':'Consulte seus atendimentos anteriores, inclusive quando o chamado foi reaberto ou seus locais de atuação mudaram.');
 App.$('attendanceFilters').addEventListener('submit',event=>{event.preventDefault();if(historyValue('historyStart')&&historyValue('historyEnd')&&historyValue('historyEnd')<historyValue('historyStart')){App.error(Error('A data final deve ser igual ou posterior à inicial.'));return;}App.clearError();filterAttendance();});
 App.$('clearAttendance').addEventListener('click',()=>{App.$('attendanceFilters').reset();App.clearError();filterAttendance();});
 App.$('refreshAttendance').addEventListener('click',()=>App.run(()=>loadAttendance()));
 App.$('attendancePrevious').addEventListener('click',()=>{attendancePage--;filterAttendance(false);});App.$('attendanceNext').addEventListener('click',()=>{attendancePage++;filterAttendance(false);});
 await loadAttendance();LiveUpdates.subscribe(()=>loadAttendance());
});
