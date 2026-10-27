async function loadRecurrence(){
 const params=new URLSearchParams();for(const [id,key] of [['insightBranch','filial_id'],['insightStart','inicio'],['insightEnd','fim']])if(App.$(id).value)params.set(key,App.$(id).value);
 const result=await API.get('/admin/recorrencias?'+params);
 App.text('insightTotal',result.total+' chamado(s) no período selecionado.');
 const render=(id,rows,fields)=>{App.$(id).innerHTML=rows.map(row=>'<tr>'+fields.map(field=>'<td>'+App.esc(row[field]??'—')+'</td>').join('')+'</tr>').join('')||'<tr><td colspan="'+fields.length+'">Nenhum chamado encontrado.</td></tr>';};
 render('recurringLocations',result.por_local,['filial_nome','bloco','sala','total','ativos']);
 render('recurringTopics',result.por_assunto,['filial_nome','categoria','total','ativos']);
}
async function loadHealth(){
 const button=App.$('refreshHealth');button.disabled=true;
 try{
  const health=await API.get('/admin/saude');
  App.text('healthSummary',health.status==='SAUDAVEL'?'Aplicação e banco de dados disponíveis.':'Sistema com funcionamento degradado.');
  App.$('healthSummary').className='alert '+(health.status==='SAUDAVEL'?'alert-success':'alert-warning');
  const fields=[['Aplicação',health.aplicacao],['Banco de dados',health.banco],['Resposta do banco',health.banco_latencia_ms+' ms'],['Tempo ativo',App.duration(health.tempo_ativo_segundos)],['Versão',health.versao],['Migração do banco',health.migracao]];
  App.$('healthDetails').innerHTML=fields.map(([name,value])=>'<dt class="col-sm-4">'+App.esc(name)+'</dt><dd class="col-sm-8">'+App.esc(value)+'</dd>').join('');
  App.text('healthUpdated','Verificado em '+App.date(health.verificado_em));
 }catch(e){App.text('healthSummary','Não foi possível verificar o sistema. Confira a conexão e tente novamente.');App.$('healthSummary').className='alert alert-danger';App.$('healthDetails').replaceChildren();App.text('healthUpdated','Última tentativa: '+new Date().toLocaleString('pt-BR'));throw e;}
 finally{button.disabled=false;}
}
App.start(async()=>{
 if(!Auth.admin())return;
 App.options('insightBranch',await API.get('/filiais'),'id','nome','Todas as minhas filiais');
 App.$('insightFilters').addEventListener('submit',event=>{event.preventDefault();App.run(loadRecurrence);});
 App.$('refreshHealth').addEventListener('click',()=>App.run(loadHealth));
 await Promise.all([loadRecurrence(),loadHealth()]);
});
