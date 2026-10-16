let topics=[],editingTopic=null,savingTopic=false;
function resetTopicForm(){editingTopic=null;App.$('topicName').value='';App.$('topicActive').checked=true;App.$('topicBranch').disabled=false;App.$('cancelTopic').hidden=true;App.text('topicFormTitle','Cadastrar assunto');App.text('saveTopic','Cadastrar assunto');}
async function loadTopics(){
 const branch=App.$('topicBranch').value;
 topics=branch?await API.get('/assuntos?filial_id='+encodeURIComponent(branch)):[];
 App.text('topicCount',topics.length+' assunto(s)');
 App.$('topicList').innerHTML=topics.map(topic=>`<div class="topic-admin-row"><div><strong>${App.esc(topic.nome)}</strong><small>${topic.ativo?'Disponível para solicitantes':'Desativado'}</small></div><div class="topic-admin-actions"><button type="button" class="btn btn-sm btn-outline-secondary" data-edit-topic="${topic.id}">Editar</button><button type="button" class="btn btn-sm ${topic.ativo?'btn-outline-danger':'btn-outline-success'}" data-toggle-topic="${topic.id}">${topic.ativo?'Desativar':'Reativar'}</button></div></div>`).join('')||'<p class="topic-admin-empty">Nenhum assunto cadastrado. Selecione uma unidade e cadastre o primeiro assunto.</p>';
}
App.start(async()=>{
 const branches=await API.get('/filiais');App.options('topicBranch',branches,'id','nome','Selecione a unidade');if(branches.length)App.$('topicBranch').value=branches[0].id;
 App.$('saveTopic').disabled=!branches.length;
 App.$('topicBranch').addEventListener('change',()=>{resetTopicForm();App.run(loadTopics);});
 App.$('cancelTopic').addEventListener('click',resetTopicForm);
 App.$('topicForm').addEventListener('submit',event=>{event.preventDefault();if(savingTopic)return;App.run(async()=>{
  savingTopic=true;App.$('saveTopic').disabled=true;
  try{const body={filialId:Number(App.$('topicBranch').value),nome:App.$('topicName').value.trim(),ativo:App.$('topicActive').checked};await API.request('/assuntos'+(editingTopic?'/'+editingTopic:''),{method:editingTopic?'PUT':'POST',body});resetTopicForm();await loadTopics();}
  finally{savingTopic=false;App.$('saveTopic').disabled=false;}
 });});
 App.$('topicList').addEventListener('click',event=>{
  if(savingTopic)return;const edit=event.target.closest('[data-edit-topic]'),toggle=event.target.closest('[data-toggle-topic]');const id=Number(edit?.dataset.editTopic||toggle?.dataset.toggleTopic),topic=topics.find(t=>t.id===id);if(!topic)return;
  if(edit){editingTopic=id;App.$('topicName').value=topic.nome;App.$('topicActive').checked=topic.ativo;App.$('topicBranch').disabled=true;App.$('cancelTopic').hidden=false;App.text('topicFormTitle','Editar assunto');App.text('saveTopic','Salvar alterações');App.$('topicName').focus();}
  else App.run(async()=>{savingTopic=true;toggle.disabled=true;try{await API.put('/assuntos/'+id,{filialId:topic.filial_id,nome:topic.nome,ativo:!topic.ativo});if(editingTopic===id)resetTopicForm();await loadTopics();}finally{savingTopic=false;toggle.disabled=false;}});
 });
 await loadTopics();
});
