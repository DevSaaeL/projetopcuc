let logsPage=1,logsLoading=false;
const logLabels={LOGIN:'Entrada no sistema',LOGOUT:'Saída do sistema',ABERTO:'Chamado aberto',EM_ATENDIMENTO:'Chamado assumido',AGUARDANDO_USUARIO:'Aguardando usuário',FINALIZADO:'Chamado finalizado',CANCELADO:'Chamado cancelado',ANEXO:'Anexo adicionado',USUARIO:'Usuário atualizado',SENHA_TEMPORARIA:'Acesso cadastrado ou redefinido',PRIMEIRA_SENHA:'Primeira senha definida',SENHA_ALTERADA:'Senha alterada',PREFERENCIAS:'Preferências atualizadas',FOTO_PERFIL:'Foto de perfil atualizada',PERFIL:'Perfil atualizado',FILIAL:'Unidade atualizada',FILIAL_INATIVA:'Unidade desativada',QR_CRIADO:'QR Code criado',QR_EXCLUIDO:'QR Code desativado',SESSOES_ENCERRADAS:'Sessões encerradas'};
async function carregarLogs(){
 if(logsLoading)return;logsLoading=true;
 const controls=[...document.querySelectorAll('#logsForm button,#refreshLogs,#logsPrevious,#logsNext')];controls.forEach(el=>el.disabled=true);App.$('logsRows').setAttribute('aria-busy','true');
 try{
  const query=new URLSearchParams({pagina:String(logsPage)});
  for(const [key,id] of [['usuario_id','logsUsuario'],['acao','logsAcao'],['inicio','logsInicio'],['fim','logsFim']])if(App.$(id).value)query.set(key,App.$(id).value);
  const data=await API.get('/logs?'+query);logsPage=data.pagina;
  const selected=App.$('logsUsuario').value;App.options('logsUsuario',data.usuarios,'id','nome',Auth.admin()?'Todos os usuários permitidos':'Minhas atividades');
  if(Auth.admin())App.$('logsUsuario').add(new Option('Solicitantes sem conta (QR Code)','publico'));
  App.$('logsUsuario').value=selected;
  const date=value=>new Date(value).toLocaleString('pt-BR',{timeZone:'America/Sao_Paulo'});
  App.$('logsRows').innerHTML=data.registros.map(row=>`<tr><td class="logs-date">${App.esc(date(row.criado_em))}</td><td><strong>${App.esc(row.autor)}</strong><small>${App.esc(row.email||'Solicitante sem conta')}</small></td><td><span class="log-action">${App.esc(logLabels[row.acao]||App.labels[row.acao]||row.acao)}</span></td><td>${row.chamado_id?`<a href="chamado-detalhes.html?id=${row.chamado_id}">#${String(row.chamado_id).padStart(6,'0')}</a>`:'—'}</td><td class="logs-description">${App.esc(row.descricao)}</td></tr>`).join('');
  App.$('logsEmpty').hidden=data.total>0;App.text('logsCount',data.total+' registro(s)');App.text('logsPage','Página '+data.pagina+' de '+Math.max(1,Math.ceil(data.total/data.por_pagina)));
  App.$('logsPrevious').dataset.disabled=String(data.pagina<=1);App.$('logsNext').dataset.disabled=String(data.pagina*data.por_pagina>=data.total);
 }finally{logsLoading=false;App.$('logsRows').removeAttribute('aria-busy');controls.forEach(el=>el.disabled=el.dataset.disabled==='true');}
}
App.start(async user=>{
 if(!Auth.admin()){App.$('logsUsuario').disabled=true;App.text('logsScope','Você pode consultar suas próprias atividades e os chamados das suas unidades.');}
 App.$('logsForm').addEventListener('submit',event=>{event.preventDefault();logsPage=1;App.run(carregarLogs);});
 App.$('clearLogs').addEventListener('click',()=>{App.$('logsForm').reset();logsPage=1;App.run(carregarLogs);});
 App.$('refreshLogs').addEventListener('click',()=>App.run(carregarLogs));
 App.$('logsPrevious').addEventListener('click',()=>{logsPage--;App.run(carregarLogs);});
 App.$('logsNext').addEventListener('click',()=>{logsPage++;App.run(carregarLogs);});
 await carregarLogs();
});
